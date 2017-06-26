package theoremProver.z3;



import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.ProcessRunner;




public class Z3 {
	private String constraint1;
	private String constraint2;
	private String nameFile;
	
	private String path;
	private String tmpDir = "./"; //directory in which save all files to pass to the theorem prover 

	private ArrayList<Variables> variablesTypeList = new ArrayList<Variables>();
	private boolean checkEquivalence;
	
	
	private static Logger log = Logger.getLogger(Z3.class.getCanonicalName());
	

	
	private class Variables{
		private String variable;
		private String type;
		
		public Variables(String variable, String type){
			this.variable = variable;
			this.type = type;
		}

		public String getVariable() {
			return variable;
		}

		public void setVariable(String variable) {
			this.variable = variable;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
		
		
	}
	/*
	 * ricevo i vincoli da confrontare
	 * ConstraintsValidator:
	 * ***** replace orig
	 * ***** convertNegativeNum 
	 * scrivo file:
	 * ***** costruisci intestazione
	 * ************* recupero l'elenco delle variabili
	 * ************* recupero il metodo a cui si riferisce
	 * ************* recupero il tipo delle variabili
	 * ************* aggiungo l'header: (declare-fun F (tipi delle variabili) bool)
	 * ***** aggiungo l'assert:
	 * ************* forall (variabili degli invarianti)
	 * ************* implies (predicati in and)
	 * ******************* pattern
	 * ***** aggiungo footer con check-sat
	 * ***** scrivo file
	 * invoco z3
	 * ***** se sat, return true
	 * ***** altrimenti false
	 */
	
	/**
	 * Costruttore: fa il set-up dell'ambiente
	 */
	public Z3() {
		try {
			tmpDir = Z3Properties.getInstance().getInvariantsDir();
			path = Z3Properties.getInstance().getZ3path();
		} catch (Exception e) {}
	}
	
	
	/**
	 * Controlla se sat o unsat
	 * @param idConstraint1
	 * @param constraint1
	 * @param idConstraint2
	 * @param constraint2
	 * @param in
	 * @param checkEquivalence se true valuta il doppio implica (se e solo se: <->)
	 * @param allStructure 
	 * @param allVariables 
	 * @param variabili
	 * @param strutture
	 * @param strategy
	 * @return sat/unsat
	 */
	public String checkConstraints (int idConstraint1, String constraint1, int idConstraint2, String constraint2, boolean in, boolean checkEquivalence, ArrayList<String> allVariables, ArrayList<String> allStructure){
		//boolean result = false;
		String result=null;
		setParameters(idConstraint1, constraint1, idConstraint2, constraint2, in, checkEquivalence);
		write(allVariables, allStructure);

		try {
			result = runZ3();//isSat();
		} catch (IOException e) {
			// 
			e.printStackTrace();
		}
		
		return result;
	}

	
//	public boolean checkConstraintsPercuoco (String methodName, State workingStatesString_i, String constraint1, State workingStatesString_j, String constraint2, boolean checkEquivalence){
//		boolean result = false;
//		///BEGIN from method setParameters
//		this.nameFile = String.valueOf(method + "_" + workingStatesString_i.getID() + "_" + workingStatesString_j.getID() + ".smt2");
//
//		ConstraintsValidator cv = new ConstraintsValidator();
//		this.constraint1 = cv.validate(constraint1);
//		this.constraint2 = cv.validate(constraint2);
//		this.checkEquivalence = checkEquivalence;
//		///END from method setParameters
//		
//		write();
//		
//		try {
//			result = isSat();
//		} catch (IOException e) {
//			// 
//			e.printStackTrace();
//		}
//		return result;
//	}
	
	
	/**
	 * Si salva alcuni parametri passati come argomento al metodo e convalida i vincoli
	 * @param idMethodCall1
	 * @param constraint1
	 * @param idMethodCall2
	 * @param constraint2
	 * @param in
	 * @param checkEquivalence
	 */
	private void setParameters(int idMethodCall1, String constraint1, int idMethodCall2, String constraint2, boolean in, boolean checkEquivalence){

		this.nameFile = "_" + idMethodCall1 + "-" + idMethodCall2 + ".smt2";
		if (in){
			/*
			 * input invariants
			 */
			this.nameFile = "IN_" + this.nameFile;
		}
		else {
			this.nameFile = "OUT_" + this.nameFile;
		}
		ConstraintsValidator cv = new ConstraintsValidator();
		this.constraint1 = cv.validate(constraint1);
		this.constraint2 = cv.validate(constraint2);
		this.checkEquivalence = checkEquivalence;
	}
	
	

	/**
	 * Costruisce tutta la formula e la salva in un file
	 * @param variabili
	 * @param strutture
	 * @param strategy
	 * @return la stringa completa //TODO mi sembra inutile restituirla
	 */
	private String write(ArrayList<String> variabili, ArrayList<String> strutture){
		String header = buildHeader(strutture);
		
		String strategy = "-using (then qe smt)";
		
		//if (!header.equals("")) {
			String footer = "(check-sat"+strategy+")\n(exit)";
			FileOutputStream file;
			try {
				file = new FileOutputStream(tmpDir + nameFile);
				PrintStream Output = new PrintStream(file);
				String body = buildBody(variabili);
				Output.print(header + body + footer);
				
				System.out.println(header + body + footer);
				
				return (header + body + footer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		//}
		return null;
	}
	
	//redaelli
	private String addAssert(ArrayList<String> variabili){
		String body;
		if ( variabili.size() == 0 ){
			body = "(assert ";
		} else {
			body = "(assert (forall (\n";
			for(int i=0; i<variabili.size();i++){
				body+=variabili.get(i);
			}
			body+=")\n";	
		}
		
		body += "(implies\n";
		return body;
	}
	

	
	private String addPattern(ArrayList<String> variabili){
		String body ="";
		body += ") ";
		if ( variabili.size() == 0 ){
			body += ")\n";
		} else {
			body += "))\n";
		}
		return body;
	}
	
	private String buildBody(ArrayList<String> variabili) {
		String body = addAssert(variabili);
		body += constraint2 +" " + constraint1;
		//body += "(and " + constraint1 + ") ";
		body += addPattern(variabili);
		
		if (checkEquivalence){
			body+=addAssert(variabili);
			body += constraint1 +" " + constraint2;
			body += addPattern(variabili);
		}
		return body;
	}
	


	private String buildPattern() {
		String pattern = "";
		for (int i = 0; i < variablesTypeList.size(); i++){
			pattern += variablesTypeList.get(i).getVariable() + " ";
		}
		return pattern;
	}

	private String buildHeader(ArrayList<String> strutture){
		String header="";
		
		for(int i=0; i<strutture.size();i++){
			header+=strutture.get(i)+"\n";
		}
		//System.out.println(header);
		return header;
	}
	
	private ArrayList<String> findVariables() {
		ArrayList<String> variables = new ArrayList();
		String regex = "c\\d+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(constraint1 + constraint2);
		String s;
		while (m.find()) {
			s = m.group();
			if (!variables.contains(s)){
				variables.add(s);
			}
		}
	
		regex = "c\\d+-c\\d+";
		p = Pattern.compile(regex);
		m = p.matcher(constraint1 + constraint2);
		while (m.find()) {
			s = m.group();
			if (!variables.contains(s)){
				variables.add(s);
			}
		}
		return variables;
		
	}
	
	private boolean isSat() throws IOException{
		String result = runZ3();
		if(result.equals("sat")) {
			//log.info(result);
			return true;
		}else if(result.equals("unsat")) {
			//log.info(result);
			return false;
		}else if(result.equals("unknown")) {
			//log.info(result);
			return false;
		}else {
			log.warning(nameFile + "---> " + result);
			throw new IOException();
			//return false;
		}
	}
	
	
	/**
	 * Metodo principale che avvia ed esegue Z3
	 * @return sat/unsat
	 * @throws IOException
	 */
	private String runZ3() throws IOException {
		
		StringWriter out = new StringWriter();
		BufferedWriter w = new BufferedWriter(out);
		
		StringWriter eout = new StringWriter();
		BufferedWriter e = new BufferedWriter(eout);
		
		List<String> cmd = new ArrayList<>();
		cmd.add( path );
		cmd.add( tmpDir+nameFile );
		
		System.out.println("Executing Z3 on file "+tmpDir+nameFile);
		
		Long me = Z3Properties.getInstance().getMaxExecutionTime() ;
		ProcessRunner.run(cmd, w, e, me.intValue() );
		w.close();
		e.close();
		
		System.out.println("==================== Z3 outputs =================");
		System.out.println("Z3 output stream:");
		System.out.println(out.toString());
		System.out.println("Z3 error stream:");
		System.out.println(eout.toString());
		System.out.println("==================== Z3 outputs end =================");
		
		return out.toString();
	}
	
	
	
	
	
	
	public static void main(String [] args) throws Exception{

		/*String invariante = "(>= (select items *product) 0)(= (select out_of_catalogue *product) 0)" +
							"(not (product null))(>= (select items *product)" +
							" (select out_of_catalogue *product))\n";*/
		
		//String invariante2 = "(= c13 4)(and (= c19 55)(= c3 -74))\n";
		String invariante="(or (= return -1) (= return 0) (= return 1))";					
		String invariante2="(or (= return -1) (= return 0) (= return 1))";
		Z3 intermediate = new Z3();
		ArrayList<String> variabili=new ArrayList<String>();
		variabili.add("(return Int)");
		ArrayList<String> strutture=new ArrayList<String>();
		String in = intermediate.checkConstraints(1, invariante, 2, invariante2, true, true, variabili, strutture);	//In sto esempio non definisco strategie
//		boolean out = intermediate.checkConstraints(idMethodCall, constraintOut2, arrayListIdMethodCall, constraintOut1, false, true);
		System.out.println("equivalente?"+in);
		
	}
	
//	private String buildImplies(String constraint) {
//	// 
//	String implies = "";
//	String parenthesis = "";
//	ArrayList<String> invariants = getInvariants(constraint);
//	if (invariants.size()==1){
//		implies = invariants.get(0);
//	}
//	else {
//		
//		for (int i=0; i<invariants.size(); i++){
//			implies = "(and " + invariants.get(i) + implies;
//			parenthesis += ")";
//		}
//	}
//	return "(" + implies + parenthesis +")";
//}
//
//private ArrayList<String> getInvariants(String constraint){
//	ArrayList<String> invariants = new ArrayList<String> ();
//	int countL = 0;
//	int countR = 0;
//	int start = 0;
//
//	for (int i = 0; i < constraint.length(); i++){
//		if ( constraint.charAt(i)== '('){
//			countL++;
//			if (countL == 1)
//				start = i;
//		}
//		else if (constraint.charAt(i) == ')'){
//			countR++;
//		}
//		if (countL!=0 && countL == countR){
//			invariants.add(constraint.substring(start, i+1));
//			countL = 0;
//			countR = 0;
//		}				
//	}
//	return invariants;
//}
	
	
//	///BEGIN DA RIMUOVERE	
//		public static void main(String[] args) {
//			String constraint1 = "\n(= c0 -2)\n(= c0 c0-c0)\n(= c0 -2.9)\n(= c00 0.0)\n";
//			String constraint2 = "\n(= c0 orig(c0))\n(= c00 -9)\n(> c0 c00)\n";
//			Intermediate intermediate = new Intermediate();
//			intermediate.checkConstraintsPercuoco2(constraint2, constraint1, true);
//		}
//		
//		public boolean checkConstraintsPercuoco2 (String constraint1, String constraint2, boolean checkEquivalence){
//			boolean result = false;
//			///BEGIN from method setParameters
//			this.nameFile = String.valueOf(method + "TESTZ3.smt2");
//
//			ConstraintsValidator cv = new ConstraintsValidator();
//			this.constraint1 = cv.validate(constraint1);
//			this.constraint2 = cv.validate(constraint2);
//			this.checkEquivalence = checkEquivalence;
//			///END from method setParameters
//			
//			write();
//			
//			try {
//				result = isSat();
//			} catch (IOException e) {
//				
//				e.printStackTrace();
//			}
//			return result;
//		}
//	///// FINE DA RIMUOVER	
	
}
