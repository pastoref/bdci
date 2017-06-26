package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipse.core.utils.DaikonPropertiesCreator;
import it.unimib.disco.lta.bct.bctjavaeclipsecpp.core.util.CDTStandaloneCFileAnalyzer;
import it.unimib.disco.lta.bdci.diff.FilterLowOrderConflicts;
import it.unimib.disco.lta.bdci.diff.SameImplementationInV1V2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import theoremProver.z3.Z3;
import util.FileUtil;
import util.JavaRunner;
import cpp.gdb.CSourceAnalyzerRegistry;
import cpp.gdb.FunctionMonitoringData;
import cpp.gdb.FunctionMonitoringDataSerializer;
import cpp.gdb.Parameter;
import daikon.Daikon;


/**
 * Main class for BDCI
 * @author Davide Redaelli
 */
public class VersionControl{

	/*----------------parametri globali ------------------------*/
	//final static String path="D:\\uni\\stage\\Esempi\\esempi_tutor\\";
	final static String dtrace = File.separator+"BCT"+File.separator+"BCT_DATA"+File.separator+"BCT"+File.separator+"Preprocessing"+File.separator+"dtrace"+File.separator;
	final static String decls = File.separator+"BCT"+File.separator+"BCT_DATA"+File.separator+"BCT"+File.separator+"Preprocessing"+File.separator+"decls"+File.separator;

	private final static Logger logger = Logger.getLogger(VersionControl.class.getName());

	public static final boolean POINTERS_COMPARISON_ENABLED = false;
	public static final boolean REPLACE_ENTER_EXIT_DECL = true;
	
	///Delete clean trace files generated in AnalysisV* (default false)
	private static final boolean DELETE_CLEAN_TRACE_FILES = Boolean.parseBoolean(System.getProperty("bdci.deleteCleanTraceFiles","false"));
	
	///Does not generate clean trace files if tehy exist (default true)
	private static final boolean SKIP_IF_CLEAN_DTRACE_EXISTS = Boolean.parseBoolean(System.getProperty("bdci.skipCreatingCleanTraceFilesIfExist","true"));
	private static final boolean PROCESS_PARTIAL_DAIKON_TRACES = false;

	/*---------------metodi sui file---------------------------*/

	/**
	 * Pulisce il file decls o dtrace per poter creare correttamente smt2
	 * temp e' la variabile dove viene salvato tutto il contenuto del file pulito
	 * @param file file decls o dtracce
	 * @param pathToSave percorso in cui salvare il file "pulito"
	 */
	private static void fileEdit(File file, String pathToSave){		
		//System.out.println("fileEdit:" +file.getAbsolutePath()+ " "+pathToSave);
		BufferedReader reader;

		try {
			reader = FileUtil.createBufferedReaderForTrace(file);
					//new BufferedReader(new FileReader(file));
			String line= null;

			BufferedWriter wr = new BufferedWriter(new FileWriter(pathToSave));

			while ((line=reader.readLine())!=null){

				if ( line.startsWith("\"")){
					if ( line.equals("\"\"") ){
						line="0";
					} else {
						line=line.trim();
						line=line.substring(1, line.length()-1);
						line=""+line.hashCode();
					}
				}
				//				if(line.contains("returnValue.eax") && file.getAbsolutePath().endsWith(".decls")) {		//elimino (o meglio, non copio) eax e le 3 righe successive
				//					reader.readLine();
				//					reader.readLine();
				//					reader.readLine();
				//					System.out.println("decls!");
				//					continue;
				//				}
				//				
				//				if(line.contains("returnValue.eax") && file.getAbsolutePath().endsWith(".dtrace")) {		//elimino (o meglio, non copio) eax e le 2 righe successive
				//					reader.readLine();
				//					reader.readLine();
				//					System.out.println("dtrace!");
				//					continue;
				//				}

				//Fabrizio: removed on 2016-03-09: replaces names of variables with numbers... is not necessary, I nevers saw inf/-inf in a Daikon trace..
				//				if(line.contains("-inf")) {		//-infinito
				//					line = Integer.MIN_VALUE+"";
				//				}
				//
				//				if(line.contains("inf")) {		//+infinito
				//					line = Integer.MAX_VALUE+"";
				//				}


				wr.append(line);
				wr.newLine();
			}

			wr.close();
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();

			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();

			//e.printStackTrace();
		}

	}


	/**
	 * "fileEditAll" cicla su tutti i file decls e dtrace presenti in una cartella passata come parametro
	 * e richiama per ogni file di questo la funzione "fileEdit"
	 * @param directory cartella in cui sono contenuti i file
	 * @return un intero che indica il numero dei file dtrace nella cartella specificata
	 */
	private static int fileEditAll(String directory){

		
		File directory_dtrace=new File(directory+dtrace);

		System.out.println("Processing "+directory_dtrace.getAbsolutePath());

		int counter = 0;
		for(String fs : directory_dtrace.list()){

			if ( !( fs.endsWith("dtrace") || fs.endsWith("dtrace.gz") ) ){
				continue;
			}
			File f=new File(directory_dtrace,fs);
			String fname = getNameFunction(f.getAbsolutePath(), 1);
			File destTrace = new File ( directory+File.separator+fname+".dtrace");
			
			if ( SKIP_IF_CLEAN_DTRACE_EXISTS ){
				if ( destTrace.exists() ){
					continue;
				}
			}
			
			
			if ( functionsFilter != null ){
				if ( !functionsFilter.contains(fname) ){
					continue;
				}
			}

			fileEdit(f, directory+File.separator+fname+".dtrace");
			counter++;
		}

		File directory_decls=new File(directory+decls);

		for(String fs : directory_decls.list()){

			if ( !fs.endsWith("decls") ){
				continue;
			}

			File f=new File(directory_decls,fs);
			String fname = getNameFunction(f.getAbsolutePath(), 2);

			File declFile = new File ( directory+File.separator+fname+".decls" );
			if ( SKIP_IF_CLEAN_DTRACE_EXISTS ){
				if ( declFile.exists() ){
					continue;
				}
			}
			
			if ( functionsFilter != null ){
				if ( !functionsFilter.contains(fname) ){
					System.out.println("Excluding: "+fname);
					continue;
				}
			}

			fileEdit(f, directory+File.separator+fname+".decls");
		}

		return counter;
	}


	/**
	 * Ritorna il nome della funzione contenuta in quel file
	 * @param filePath il file di cui dare il nome
	 * @param type 1 = dtrace; 2 = decl
	 * @return il nome della funzione
	 */
	private static String getNameFunction(String filePath, int type) {
		if (type == 1) {
			BufferedReader reader;

			try {

				//The following is required because we may need to gzip traces
				reader = FileUtil.createBufferedReaderForTrace(new File(filePath));
				//		;new BufferedReader(new FileReader(filePath));
				String line = reader.readLine();		//Stringa del tipo nomeFunz :::ENTER

				if (line == null) {
					throw new IOException("Error parsing dtrace file!");
				} else {
					String[] tmp = line.split(" ");
					return tmp[0];
				}


			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("Error! Trace files not found!");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error! Trace files not found!");
			}
		} else {
			BufferedReader reader;

			try {

				reader = new BufferedReader(new FileReader(filePath));
				String line = reader.readLine();		//Stringa "DECLARE": la scarto

				if (line == null) {
					throw new IOException("Error parsing decls file!");
				} else {

					line = reader.readLine();		//Stringa del tipo nomeFunz :::ENTER

					if (line == null) {
						throw new IOException("Error parsing decls file!");
					} else {
						String[] tmp = line.split(" ");
						return tmp[0];
					}

				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("Error! Trace files not found!");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error! Trace files not found!");
			}

		}
		return filePath;

	}

	/*--------------------metodi sul recupero delle variabili -------------------------------*/

	private static ArrayList<String> allVariables = new ArrayList<String>();
	private static ArrayList<String> allStructure = new ArrayList<String>();
	private static ArrayList<String> toExclude = new ArrayList<String>();
	//hashmap di nil memorizza i possibili nil associati alle strutture, cosi' facendo
	//ovviamo al problema che non possiamo avere lo stesso nil per piu' di una struttura
	private static Map<String, String> nilOfStructure = new HashMap<>();
	static NotToBeConsidered keyword = null;
	static StructuresTree s = new StructuresTree();
	private static int z3Counter;
	private static Pattern returnPattern = Pattern.compile("\\( eax returnValue\\) \\d\\d\\d\\d\\d\\d\\d\\)");

	//uninitialized variables often contain high values, but this is just a workaround
	private static Pattern uninitVariable = Pattern.compile(" \\d\\d\\d\\d\\d\\d\\d\\)");
	private static boolean missingEnter;
	private static boolean tracesAlreadyEdited = Boolean.getBoolean("bdci.alreadyEdited");

	/** 
	 * Il metodo cicla sul file e ne recupera il contenuto, lo divide tramite la scritta declare,
	 * ogni elemento del nuovo array viene a sua volta splittato e recuperata variabili per variabile ogni 4 righe, 
	 * saltando sempre la prima in quanto contiene l'indicazione di enter o exit (infatti parto da indice i nel ciclo for)
	 * secondo la struttura predefinita di decls
	 * @param directory cartella dove e' contenuto il file decls
	 * @param part	1=enter, 2=exit
	 * @param fun nome della funzione = indica quale file dobbiamo recuperare (ogni file decls corrisponde ad una funzione)
	 * @return arrayList di stringhe contenenti la coppia nomeVariabile Tipo (i=nome i+1=valore)
	 */
	private static ArrayList<String> getVariable(String directory, int part, String fun){

		BufferedReader reader = null;
		ArrayList<String> to_return=new ArrayList<String>(); ;

		if(directory != null) {
			File decls = getDeclarationFile(directory, fun);
			try {
				System.out.println("--- getVariable ---");
				System.out.println("dir: "+directory+" part: "+part+" fun: "+fun+" decls: "+directory+File.separator+fun+".decls");
				reader = new BufferedReader(new FileReader(decls));
				String line; 
				String tmp = "";;

				//leggo il file .decls
				while ((line=reader.readLine())!=null){
					tmp+=line+"\n";
				}

				//			System.out.println("Stringa letta: " + tmp);

				//il contenuto di ogni "DECLARE" lo metto in un componente dell'array = array di stringhe dove divide
				//la prima parte dalla seconda (tramite la scritta declare)
				String[] tmp_splitted = tmp.split("DECLARE\n");

				//			System.out.println("Split DECLARE:");
				//			for (String s : tmp_splitted) {
				//				System.out.println(s);
				//			}

				//guardo entry/exit in base al parametro "parte"
				String[] string_tmp_splitted = tmp_splitted[part].split("\n");

				//			ogni componente di "string_tmp_splitted" e' di questa forma:
				//			
				//			x
				//			double
				//			double
				//			1
				//			y
				//			double
				//			double
				//			1
				//			
				//			io prendo "x" + "double" e "y" + "double"
				for(int i = 1; i<string_tmp_splitted.length; i+=4){ 
					to_return.add(string_tmp_splitted[i]);
					to_return.add(string_tmp_splitted[i+1]);
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("Error! Trace files not found!");
				//e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error! Trace files not found!");
				//e.printStackTrace();
			} finally {
				try {
					if ( reader != null ){
						reader.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		return to_return;
	}


	public static File getDeclarationFile(String directory, String fun) {
		File decls = new File(directory+File.separator+fun+".decls");
		return decls;
	}


	/**
	 * "getVariableType" riceve in ingresso le due cartelle dove recuperare il file decls, la funzione da recuperare
	 * indicante il numero del file e la parte da recuperare, 1:enter, 2:exit
	 * il metodo richiama il getVariable su entrambe le cartelle per lo stesso file e parte, ottiene due arraylist di stringhe
	 * cicla sulle arraylist e salva la coppia nomevariabile tipo in un unica stringa
	 * ritorna infine un arraylist di stringhe contenenti tutte le variabili
	 * @param directory0 cartella0
	 * @param directory1 cartella1
	 * @param part 1=enter, 2=exit
	 * @param fun nome della funzione
	 * @return arraylist di stringhe contenenti tutte le variabili
	 */
	private static ArrayList<String> getVariableType(String directory0, String directory1, int part, String fun){
		ArrayList<String> variables0 = getVariable(directory0, part, fun);		//su prima cartella
		ArrayList<String> variables1 = getVariable(directory1, part, fun);		//su seconda cartella
		//		ArrayList<String> variables_tmp = new ArrayList<String>();




		HashSet<String> variables_tmp = new HashSet<String>();
		HashSet<String> integers = new HashSet<String>();
		HashSet<String> strings = new HashSet<String>();

		for(int i=0; i<variables0.size(); i++){

			if(i%2!=0){

				// Della coppia nomeVariabile-tipo guardo solo il tipo e faccio la conversione int->Int, boolean->Bool...
				switch(variables0.get(i)){

				case "int":{
					variables_tmp.add(variables0.get(i-1)+" Int");
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
					integers.add( variables0.get(i-1) );
				}break;

				case "boolean":{
					variables_tmp.add(variables0.get(i-1)+" Bool");
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "double":{
					variables_tmp.add(variables0.get(i-1)+" Real");
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "float":{
					variables_tmp.add(variables0.get(i-1)+" Float");
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "real" :{
					variables_tmp.add(variables0.get(i-1)+" Real");
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "Object" :{
					//variabili_tmp.add(variabili.get(i-1)+" Object");
					//gli oggetti li togliamo dato che la maggior parte degli elementi con oggetto e'
					//un puntatore, quindi non ci interessa analizzarlo
					//MODIFICA: ora li teniamo e gli mettiamo come tipo "Struct"+nomeVar (per tenere la corrispondenza con le strutture definite in "declare-datatypes")

					keyword.setKeyword(variables0.get(i-1));		//keyword=NotToBeConsidered
					System.out.println("Caso Object: "+variables0.get(i-1));
					//COD1
					//						if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
					//							String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
					//							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
					//						}
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "java.lang.String" :{
					//variabili_tmp.add(variabili.get(i-1)+" String");
					keyword.setKeyword(variables0.get(i-1));
					strings.add( variables0.get(i-1) );
					System.out.println("Caso String: "+variables0.get(i-1));
					//TODO Non so se servve sto codice
					//						if (variables0.get(i-1).contains(".")) {
					//							String [] array= variables0.get(i-1).split("\\.");
					//							variables_tmp.add(array[0] + " java.lang.String");
					//						}
					//COD1
					if (variables0.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables0.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;


				default:{
					//keyword.setKeyword(variabili.get(i-1));
				}break;
				}
			}
		}

		for ( String stringVar : strings ){
			String intVar = "*"+stringVar;
			if (integers.contains(intVar)){
				variables_tmp.remove(intVar);
				keyword.setKeyword(intVar);
			}
		}

		HashSet<String> variablesV0 = variables_tmp;
		variables_tmp=new HashSet<>();




		// prima su "variabili", ora su "variabili1"
		for(int i=0; i<variables1.size(); i++){		

			if(i%2!=0){

				switch(variables1.get(i)){

				case "int":{
					variables_tmp.add(variables1.get(i-1)+" Int");
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
					integers.add( variables1.get(i-1) );
				}break;

				case "boolean":{
					variables_tmp.add(variables1.get(i-1)+" Bool");
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "double":{
					variables_tmp.add(variables1.get(i-1)+" Real");
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "float":{
					variables_tmp.add(variables1.get(i-1)+" Real");
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "real" :{
					variables_tmp.add(variables1.get(i-1)+" Real");
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
				}break;

				case "Object" :{
					//variabili_tmp.add(variabili.get(i-1)+" Object");

					keyword.setKeyword(variables1.get(i-1));
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}

				}break;

				case "java.lang.String" :{
					//variabili_tmp.add(variabili.get(i-1)+" String");

					keyword.setKeyword(variables1.get(i-1));
					//COD1
					if (variables1.get(i-1).contains(".")) {		//Solo s contiene un punto e' davvero una struttura
						String [] array=variables1.get(i-1).split("\\.");	//Separo in base al punto
						if(!variables_tmp.contains(array[0] + " Struct"+array[0])) {	//Evito duplicati
							variables_tmp.add(array[0] + " Struct"+array[0]);	//Il nome della variabile e' il primo campo
						}
					}
					strings.add( variables1.get(i-1) );
				}break;

				default:{
					//daNonConsiderare.add(variabili.get(i-1));
					//keyword.setKeyword(variabili.get(i-1));
				}break;
				}
			}
		}


		//dereferencing a pointer to an array of chars lead to properties about the first char of the string, which is not ok most of the time...
		for ( String stringVar : strings ){
			String intVar = "*"+stringVar;
			if (integers.contains(intVar)){
				variables_tmp.remove(intVar);
				keyword.setKeyword(intVar);
			}
		}

		HashSet<String> variablesV1 = variables_tmp;

		HashSet<String> result = filterVariables( directory1, variablesV0, variablesV1 );

		ArrayList<String> variables_smt = new ArrayList<String>();
		variables_smt.addAll(result);
		return variables_smt;
		//		
		//		//		l'ultimo ciclo e' utile per non avere variabili ridondanti
		//		//		alla fine ritorno solo "variabili_smt" che e' "variabili_tmp" senza duplicati
		//		for(int i=0; i<variables_tmp.size();i++){
		//
		//			if(variables_smt.size()==0){
		//
		//				//TODO: se e' vuoto lo inserisco(sicuramente non e' duplicato) -> penso sia inutile sto if
		//				variables_smt.add(variables_tmp.get(i));
		//			} else {
		//
		//				//se non è gia' presente lo inserisco
		//				if(!variables_smt.contains(variables_tmp.get(i)) ){
		//
		//					variables_smt.add(variables_tmp.get(i));
		//				}
		//			}
		//
		//		}
		//
		//		return variables_smt;
	}


	private static HashSet<String> filterVariables(String directory1, HashSet<String> variablesV0,
			HashSet<String> variablesV1) {
		if ( directory1 != null ){
			HashSet<String> missingV0 = new HashSet<>();
			missingV0.addAll(variablesV1);
			missingV0.removeAll(variablesV0);

			HashSet<String> missingV1 = new HashSet<>();
			missingV1.addAll(variablesV0);
			missingV1.removeAll(variablesV1);

			HashSet<String> result = new HashSet<>();
			result.addAll(variablesV0);
			result.retainAll(variablesV1);

			for ( String s : missingV0 ){
				String vname = s.split(" ")[0];
				keyword.setKeyword(vname);
			}

			for ( String s : missingV1 ){
				String vname = s.split(" ")[0];
				keyword.setKeyword(vname);
			}
			return result;
		} else {
			HashSet<String> result = new HashSet<>();
			result.addAll(variablesV0);
			return result;
		}
	}


	/**
	 * Se l'arraylist e' vuota allora non fa niente, altrimenti cicla su ogni elemento;
	 * al primo elemento visitato verifica che non ci sia gia' un nome struttura, se non e' presente lo aggiunge 
	 * e inizia a creare la stringa di definizione del data-types, eliminando infine l'elemento dall'arraylist iniziale,
	 * se invece esiste il nome della struttura verifica che quello dell'elemento recuperato sia uguale a quello esistente, 
	 * in questo caso aggiunge la variabile alla dichiarazione di una struttura e elimina l'elemento dall'arraylist
	 * altrimenti se e' diverso va avanti nel ciclo.
	 * infine viene aggiunta la definizione della struttura alla variabile globale allStrutture e la dichiarazione di questa in allVariabili
	 * richiama poi se stesso sull'arraylist rimasta per verificare la presenza di altre strutture
	 * @param structureTmp  arraylist contenenti tutte le variabili della forma: nomestruttura.nomeVariabile
	 * @param singles arraylist contenenti tutte le variabili (singole senza punto)
	 */
	private static void createStructure(ArrayList<String> structureTmp, ArrayList<String> singles){
		List<String> declarationsPreProcess = s.getDeclarations();
		List<String> declarations = new ArrayList<>();

		System.out.println("Elimino doppioni");
		//Elimino i doppioni
		for(String decl : declarationsPreProcess){
			if ( decl.contains("[") ){
				//HACK: ignore arrays
				System.out.println("Array decl: "+decl);
			} else {
				System.out.println("decl:"+decl);
				declarations = addStructureIfIsNotContained(decl, declarations);
			}
		}

		for(String decl : declarations){		//rimuove la stringa "declare-datatypes"
			//TODO Da togliere
			System.out.println("aaDecl: "+decl);
			//COD1
			String stringWithoutDeclare = decl.substring(24);	//rimuove la stringa "(declare-datatypes () (("
			String [] array = stringWithoutDeclare.split(" ");
			singles.add(array[0].substring(6) + " " + array[0]);	//partendo da "Struct*obj" creo la stringa "*obj Struct*obj"
			allStructure.add(decl);
		}

	}


	/**
	 * Acciunge decl a declarations se non e' gia' contenuta
	 * @param decl
	 * @param declarations
	 * @return declarations se decl e' gia'contenuto, decaration+sdecl altrimenti
	 */
	private static List<String> addStructureIfIsNotContained(String decl, List<String> declarations) {
		String declDaSostituire = "";
		int posDaSostituire = -1;

		String stringWithoutDeclare = decl.substring(24);	//rimuove la stringa "(declare-datatypes () (("
		System.out.println("stringWithoutDeclare: "+stringWithoutDeclare);
		int pos = stringWithoutDeclare.indexOf("(");
		String structName = stringWithoutDeclare.substring(0, pos);
		System.out.println("Name: "+structName);

		System.out.println("Declarations list:");
		for (int i = 0; i < declarations.size(); i++) {
			System.out.println("s: "+declarations.get(i));
		}

		for (int i = 0; i < declarations.size(); i++) {
			System.out.println("s: "+declarations.get(i));
			String stringWithoutDeclare2 = declarations.get(i).substring(24);	//rimuove la stringa "(declare-datatypes () (("
			System.out.println("stringWithoutDeclare2: "+stringWithoutDeclare2);
			int pos2 = stringWithoutDeclare2.indexOf("(");
			String structName2 = stringWithoutDeclare2.substring(0, pos2);
			System.out.println("Name2: "+structName2);

			if (structName.equals(structName2)) {
				System.out.println("Struttura gia' contenuta!");
				String contenutoDecl = stringWithoutDeclare.substring(pos+7, stringWithoutDeclare.length()-6);	//+7 per togliere il "cons"
				System.out.println("contenutoDecl: "+contenutoDecl);
				String contenutoDecl2 = stringWithoutDeclare2.substring(pos+7, stringWithoutDeclare2.length()-6);	//+7 per togliere il "cons"
				System.out.println("contenutoDecl2: "+contenutoDecl2);
				String contenutoDaSostituire = getContents(contenutoDecl, contenutoDecl2);
				System.out.println("contenutoDaSostituire: "+contenutoDaSostituire);
				declDaSostituire = "(declare-datatypes () (("+(stringWithoutDeclare2.substring(0, pos2)+contenutoDaSostituire)+" ) )))";
				System.out.println("daSostituire: "+declDaSostituire);
				posDaSostituire = i;
				break;
			}
		}

		if (posDaSostituire == -1) {
			declarations.add(decl);
			System.out.println("Declarations list dopo1:");
			for (int i = 0; i < declarations.size(); i++) {
				System.out.println("s: "+declarations.get(i));
			}
		} else {
			System.out.println("posDaSostituire: "+posDaSostituire);
			System.out.println("ddd: "+declDaSostituire);
			declarations.remove(posDaSostituire);	//Rimuovo la vecchia
			declarations.add(posDaSostituire, declDaSostituire);	//Aggiungo la nuova nello stesso posto di quella vecchia (se no sballa l'ordine)
			System.out.println("Declarations list dopo2:");
			for (int i = 0; i < declarations.size(); i++) {
				System.out.println("s: "+declarations.get(i));
			}
		}

		return declarations;
	}


	/**
	 * Ritorna il contenuto della struttura da riempire
	 * @return il contenuto della struttura da riempire
	 */
	private static String getContents(String content1, String content2) {
		List<String> contentList1 = new ArrayList<String>();
		List<String> contentList2 = new ArrayList<String>();
		List<String> contentListFinal = new ArrayList<String>();

		while (!content1.equals("")) {
			int posParentesiAperta = content1.indexOf("(");
			int posParentesiChiusa = content1.indexOf(")");
			String valoreEstratto = content1.substring(posParentesiAperta+1, posParentesiChiusa);
			System.out.println("valoreEstratto1: "+valoreEstratto);
			contentList1.add(valoreEstratto);
			content1 = content1.substring(posParentesiChiusa+1);
			System.out.println("content1: "+content1);
		}

		System.out.println("Contenuto Lista 1");
		for (String s : contentList1) {
			System.out.println(s);
		}

		while (!content2.equals("")) {
			int posParentesiAperta = content2.indexOf("(");
			int posParentesiChiusa = content2.indexOf(")");
			String valoreEstratto = content2.substring(posParentesiAperta+1, posParentesiChiusa);
			System.out.println("valoreEstratto2: "+valoreEstratto);
			contentList2.add(valoreEstratto);
			content2 = content2.substring(posParentesiChiusa+1);
			System.out.println("content2: "+content2);
		}

		System.out.println("Contenuto Lista 2");
		for (String s : contentList2) {
			System.out.println(s);
		}

		contentListFinal.addAll(contentList1);

		for (String content : contentList2) {
			if (!contentList1.contains(content)) {
				contentListFinal.add(content);
			}
		}

		System.out.println("Contenuto Lista Finale");
		for (String s : contentListFinal) {
			System.out.println(s);
		}

		String finalContent = "( cons ";

		for (String content : contentListFinal) {
			finalContent = finalContent+"("+content+")";
		}

		System.out.println("Final content: "+finalContent);

		return finalContent;
	}


	/** 
	 * Cicla su un arraylist di stringhe e aggiunge ad un arraylist globale chiamato
	 * "allVariabili" la dichiarazione della variabile
	 * @param singles le variabili singole (senza punto)
	 */
	private static void createVariableToAssert(ArrayList<String> singles) {

		for(int i = 0; i<singles.size(); i++){

			if (allVariables.size()==0) {
				allVariables.add( "("+singles.get(i)+") ");
			} else {
				//l'if evita di inserire duplicati
				if (!allVariables.contains("("+singles.get(i)+") ") ) {
					allVariables.add( "("+singles.get(i)+") ");
				}
			}

		}
		System.out.println(allVariables);
	}


	/**
	 * Richiama il metodo "getVariableType" e divide le variabili reuperate: 
	 * - se la var contiene il punto allora è una struttura, quindi viene aggiunta all'arraylist "struttureTmp";
	 * - altrimenti è una variabile singola, quindi viene aggiunta all'arraylist "singole".
	 * Infine vengono richiamati i metodi "createStructure" e "createVariableToAssert"
	 * @param directory0 cartella0 dove recuperare i file .decls
	 * @param directory1 cartella1 dove recuperare i file .decls
	 * @param part parte da recuperare (1=enter, 2=exit)
	 * @param fun nome funzione
	 */
	private static void divideVariables(String directory0, String directory1, int part, String fun){
		allStructure.clear();
		allVariables.clear();
		nilOfStructure.clear();
		toExclude.clear();
		keyword = new NotToBeConsidered();
		ArrayList<String> variables = getVariableType(directory0, directory1, part, fun);		//coppie nomeVariabile-tipo
		ArrayList<String> structureTmp = new ArrayList<String>();
		ArrayList<String> singles = new ArrayList<String>();

		System.out.println("Contenuto variables:");
		for (String v : variables) {
			System.out.println(v);
		}



		for (String variable : variables){


			if ( variable.contains("[") ){
				System.out.println("Array in variable name: "+variable);

				variable = processPropertyWithArrayVariable(variable);

				continue;
			}

			//			if ( variable.startsWith("*") ){
			//			toExclude.add(variable); //TODO: still to remove the constraints
			//				System.out.println("Pointer in variable name: "+variable);
			//				continue;
			//			}

			//controllo se contiene il punto
			if (variable.contains(".")){
				System.out.println("aavariables.get(i): "+variable);
				String [] var = variable.split(" ");
				//var[0]=var[0].replaceAll("\\*", "");
				System.out.println("aavar[0]: "+var[0]);
				System.out.println("aavar[1]: "+var[1]);
				s.addSubtree(var[0], var[1]);
				structureTmp.add(var[0]);
			} else { 
				singles.add(variable);
			}

		}



		createStructure(structureTmp, singles);


		createVariableToAssert(singles);

	}


	private static String processPropertyWithArrayVariable(String variable) {
		while ( ! variable.isEmpty() ){
			int bracketPos = variable.indexOf('[');
			if ( bracketPos == -1 ){
				break;
			}

			String chunk = variable.substring(0, bracketPos);
			int dotPos = chunk.lastIndexOf('.');
			int spacePos = chunk.lastIndexOf(' ');

			int start = Math.max(dotPos, spacePos);
			if ( start == -1 ){
				start = 0;
			}
			String arrayName = chunk.substring(start);
			System.out.println("Excluding array variable: "+arrayName);
			keyword.setKeyword(arrayName);

			int closedBracketPos = variable.indexOf(']');
			variable = variable.substring(closedBracketPos+1).trim();
		}
		return variable;
	}


	/*-------------------metodi sul formato di daikon --------------------------------------*/

	/**
	 * Richiama il metodo di Daikon per ricavarne il modello in smt2
	 * passandogli dei parametri utili per ricavarne il modello
	 * @param directory cartella in cui è gia' statomonitorato il progetto con GDB
	 * @param fun nome funzione
	 * @return il modello da analizzare (Appendable = interfaccia)
	 * @throws IOException
	 */
	private static String executeDaikonSMT(String directory, String fun) throws IOException{
		return executeDaikonSMT(directory,fun,true);
	}
	
	private static String executeDaikonSMT(String directory, String fun, boolean useCache) throws IOException{
		System.out.println("Executing Daikon "+directory+" "+fun);

		String cacheFileName = directory+File.separator+fun+".cache.smt.txt";

		File cacheFile = new File ( cacheFileName );
		if ( useCache && cacheFile.exists() ){
			System.out.println("Using cached result from "+cacheFile.getAbsolutePath());

			return FileUtil.getContent(cacheFile);
		}

		File daikonOptsFile = createDaikonOptsFile(directory);

		List<String> daikonArgs = new ArrayList<String>();		//argomenti per richiamare Daikon
		daikonArgs.add("--format");
		daikonArgs.add("smt");


		daikonArgs.add(directory+File.separator+fun+".decls");	
		daikonArgs.add(directory+File.separator+fun+".dtrace");
		daikonArgs.add("--conf_limit");		//--confLlimit=0 -> comando usato per dire che non vogliamo alcun livello di
		daikonArgs.add("0.95");				//confidenza nella visita degli invarianti

		daikonArgs.add("--config");
		daikonArgs.add(daikonOptsFile.getAbsolutePath());

		Appendable out = new StringBuffer();
		JavaRunner.runMainInClass(Daikon.class, new ArrayList<String>(), daikonArgs, 0, new ArrayList<String>(), true, out, out );

		String result = out.toString();
		FileUtil.writeToTextFile(result, cacheFile);

		if ( DELETE_CLEAN_TRACE_FILES ){
			File tmpDecls = new File(directory+File.separator+fun+".decls");
			tmpDecls.delete();
			File tmpDtrace = new File(directory+File.separator+fun+".dtrace");
			tmpDtrace.delete();
		}
		return result;
	}


	public static File createDaikonOptsFile(String directory)
			throws IOException {
		File daikonOptsFile = new File( directory+File.separator+"daikon.config.txt" );
		if ( ! daikonOptsFile.exists() ){
			Properties daikonOptions = DaikonPropertiesCreator.getDefaultDaikonProperties().get("essentials");
			FileWriter fw = new FileWriter(daikonOptsFile ) ;
			daikonOptions.store(fw, "Created by "+VersionControl.class.getCanonicalName());
			fw.close();

		}
		return daikonOptsFile;
	}


	/**
	 * Pilisce la stringa "output" ottenuta da Daikon; essa rappresenta il modello in formato smt
	 * "cleanString" prende in ingresso l'output di daikon e un intero che ne indica la parte da utilizzare <1:enter, 2:exit>.
	 * Utilizza il metodo split per dividere il contenuto di daikon:
	 * effettua prima una divissione tramite la stringa di uguali; calcola la lunghezza dell'array ottenuto; se e' di due dimensioni, allora parte=parte -1;
	 * poi divide le stringhe ottenute e, nel caso trovasse la keyWord "Preprocessing" allora ritorna la stringa vuota,
	 * in quanto e' la parte iniziale descrittiva di daikon. 
	 * infine controlla linea per linea il formato uscito da daikon, quando trova parole tra:
	 * - origin
	 * - argv
	 * - Exception in
	 * - at daikon
	 * - hash
	 * e cancella la linea del formato di daikon
	 * @param directory nome cartella
	 * @param fun nome funzione
	 * @param parteTmp	1=enter, 2=exit
	 * @param functionsData 
	 * @return la stringa "pulita"
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	private static String inferProperties(String directory, String fun, int parteTmp, Map<String, FunctionMonitoringData> functionsData) throws IOException, ClassNotFoundException{
		String parte;
		if (parteTmp==1) {
			parte="ENTER";
		} else {
			parte="EXIT";
		}



		FunctionMonitoringData function = functionsData.get(fun);
		if ( function == null ){
			function = findFunctionInPresenceOfTrimmedName(fun, functionsData);
		}

		System.out.println("Cleaning up model "+directory+ " "+ fun + " "+parte);

		String output = executeDaikonSMT(directory, fun);
		System.out.println("============================ Daikon SMT Model ========================  ");
		System.out.println( output);
		System.out.println("============================ Daikon SMT Model END ========================  ");

		if ( PROCESS_PARTIAL_DAIKON_TRACES && output.contains("No return from procedure observed") ){
			File declsFile = new File( directory+File.separator+fun+".decls");	
			File traceFile = new File(directory+File.separator+fun+".dtrace");
			
			replaceWithPOINT( declsFile );
			replaceWithPOINT( traceFile );
			
			output = executeDaikonSMT(directory, fun, false);
			System.out.println("============================ Daikon SMT Model POINT ========================  ");
			System.out.println( output);
			System.out.println("============================ Daikon SMT Model POINT END ========================  ");

			output=replaceBackPOINT(output);
			
			System.out.println("============================ Daikon SMT Model POINT BACK ========================  ");
			System.out.println( output);
			System.out.println("============================ Daikon SMT Model POINT BACK END  ========================  ");

			
		}

		return extractInvariantsInDaikonOutput(parte, function, output);
	}


	private static String replaceBackPOINT(String output) {
		BufferedReader br = new BufferedReader(new StringReader(output));
		
		String line;
		String newOutput = "";
		
			try {
				while ( ( line = br.readLine() ) != null ){
					if ( line.contains(":::POINT") ){
						int end = line.length()-8;
						if ( line.startsWith("ENTER_") ){
							line=line.substring(6,end)+":::ENTER";
						} else if ( line.startsWith("EXIT_") ){
							line=line.substring(5,end)+":::EXIT1";
						}
					}
					newOutput += newOutput+"\n"; 
				}
				
				return newOutput;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return output;
	}


	private static void replaceWithPOINT(File declsFile) {
		try {
			File dest = new File ( declsFile.getAbsolutePath()+".tmp" );
			BufferedReader r = new BufferedReader(new FileReader ( declsFile ) );
			BufferedWriter w = new BufferedWriter(new FileWriter ( dest ) );
			
			String line;
			
			try {
				while ( ( line = r.readLine() ) != null ){
					if ( line.endsWith(":::EXIT1") ){
						int end = line.indexOf(":::");
						line = "EXIT_"+line.substring(0, end )+":::POINT";
					} else if ( line.endsWith(":::ENTER") ){
						int end = line.indexOf(":::");
						line = "ENTER_"+line.substring(0, end )+":::POINT";
					}
					w.write(line);
					w.newLine();
				}
				
				w.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
			
			r.close();	
			
			File orig = new File(decls+".orig");
			System.out.println("RENAMING "+declsFile.getAbsolutePath()+" as "+orig.getAbsolutePath());
			declsFile.renameTo(orig);
			
			System.out.println("RENAMING "+dest.getAbsolutePath()+" as "+declsFile.getAbsolutePath());
			dest.renameTo(declsFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}


	public static String extractInvariantsInDaikonOutput(String parte,
			FunctionMonitoringData function, String output) {
		String [] smt = null;
		String [] temp = null;
		//la riga di uguali nel modello Daikon divide ENTER da EXIT
		smt = output.split("===========================================================================");		




		for(int i=0; i<smt.length;i++) {
			if(smt[i].contains(parte)) {
				temp=smt[i].split("\n");
			}
		}
		//se non contiene nulla ritorna niente
		if(temp==null) {
			return "";
		}



		List<String> includedProperties = new ArrayList<String>();

		//controlla dove arrivare a considerare le righe; cambia in base ad ingresso ed uscita: se è ENTER leggo tutto
		//altrimenti non leggo l'ultima riga (che e' "Exiting Daikon")
		int where_stop = temp.length-1;
		if(parte.equals("ENTER")){
			where_stop = temp.length;
		}

		ArrayList<String> pointerNames = retrievePointerParameterNames(function);


		for(int i=2; i<where_stop; i++){

			String currentProperty = temp[i].trim();

			//se contiene l'intestazione di daikon allora ritorna la stringa vuota
			if(currentProperty.contains("Processing trace data")){
				return "";
			}

			//se contiene il "footer" di daikon allora ritorna la stringa vuota
			if(currentProperty.contains("Exiting Daikon.")){
				continue;
			}

			//controllo sul null nel caso di strutture
			String regex="\\(([a-zA-Z_0-9]+) (null)\\)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(currentProperty);

			if (matcher.find()){
				String [] s = matcher.group().split(" ");

				s[0] = s[0].replaceAll("\\(", "");

				//per le potenze
				if(StructuresTree.getNilNameFromVarName("*"+s[0]+"")!=null){
					currentProperty=currentProperty.replaceAll(regex, "(= *$1 "+ StructuresTree.getNilNameFromVarName("*"+s[0])+")"); //*$1
				}
			}

			//currentProperty = processHashNullEquality( currentProperty );

			//controllo sulle variabili non considerate
			String [] temp_space = currentProperty.split(" ");

			//trovato?
			boolean find = false;

			for(int j=0; j<temp_space.length && !find; j++){
				temp_space[j] = temp_space[j].replaceAll("\\(", "");
				temp_space[j] = temp_space[j].replaceAll("\\)", "");

				if ( keyword.contains( temp_space[j] ) ){
					find = true;

				}

			} 

			if ( find ){
				System.out.println("Excluding property: "+currentProperty);
				continue;
			} 

			//			else {
			//				System.out.println("Including property: "+currentProperty);
			//			}

			if ( excludePropertyForSyntax(currentProperty) ){
				System.out.println("Excluding property for syntax: "+currentProperty);
				continue;
			}





			//controllo formato (not (var 0)) diventa (not (= var 0))
			String regex1 = "\\(([a-zA-Z_0-9]+) \\(([a-zA-Z_0-9]+) ([0-9]+)\\)\\)";
			currentProperty=currentProperty.replaceAll(regex1, "($1 (= $2 $3))");


			//controllo formato (not ((eax returnValue) 0)) diventa (not (=(eax returnValue) 0))
			String regex2 = "\\((not) \\(\\( (.+) (.+)\\) ([0-9]+)\\)\\)";
			currentProperty=currentProperty.replaceAll(regex2, "($1 (=($2 $3) $4))");


			//controllo formato ( flags *obj) % ( eax returnValue) == 0 diventa (= (mod ( flags *obj) ( eax returnValue)) 0)
			String regex3 = "\\( (.+) (.+)\\) (%) \\( (.+) (.+)\\) (==) ([0-9]+)";
			currentProperty=currentProperty.replaceAll(regex3, "(= (mod ( $1 $2) ($4 $5)) $7)");

			if( currentProperty.contains("[") ){ //Skip arrays
				System.out.println("Property contains array: "+currentProperty);
				continue;
			}

			//(not ( = argv null))
			boolean isNotNull = isNotNullInvariant( currentProperty, pointerNames );
			if ( isNotNull ){
				String inv = currentProperty;
				inv = inv.substring(0, inv.length() - 6);
				inv = inv + "0))";

				currentProperty = inv;
			}

			//(not ( = map 0))
			boolean exclude = containsPointerOperation(currentProperty, pointerNames);

			if ( exclude ){
				System.out.println("Excluding pointer operation: "+currentProperty);	
				continue;
			}

			System.out.println("Included property: "+currentProperty);


			includedProperties.add(currentProperty);


		}


		String to_return = unisce(includedProperties);


		System.out.println("Included variables:");
		System.out.println(to_return);

		return to_return;
	}

	private static Pattern hashNullPattern = Pattern.compile("\\(= \\(hash ([a-zA-Z_]+)\\) null\\)");
	private static boolean containErrors;

	public static String processHashNullEquality(String currentProperty) {
		Matcher m = hashNullPattern.matcher(currentProperty);
		if ( m.matches() ){
			String vname = m.group(1);
			return "(= "+vname+" 0)";
		}
		return currentProperty;
	}


	private static boolean isNotNullInvariant(String inv,
			ArrayList<String> pointerNames) {

		//(not ( = argv null))

		if ( inv.startsWith("(not ( = ") ){
			if ( inv.endsWith(" null))") ){

				String variableName = inv.substring(9, inv.length() - 7);

				if ( ! variableName.contains(" ") ){
					return true;
				}

			}	
		}



		return false;
	}


	public static ArrayList<String> retrievePointerParameterNames(
			FunctionMonitoringData function) {
		ArrayList<String> pointerNames = new ArrayList<>(); 
		List<Parameter> args = function.getAllArgs();
		if ( args == null ){
			logger.warning(FunctionMonitoringData.class.getName()+".getAllArgs() returned null for function "+function.getMangledName());
			return pointerNames;
		}
		for ( Parameter p : args ){
			if ( p.isPointer() ){
				pointerNames.add( p.getName());
			}
		}

		Parameter returnParameter = function.getReturnParameter();
		if ( returnParameter.isPointer() ){
			pointerNames.add( returnParameter.getName());
		}
		return pointerNames;
	}


	public static boolean containsPointerOperation(String inv, ArrayList<String> pointerParameters) {
		for ( String pointerName : pointerParameters ){
			if ( inv.contains(" "+pointerName+" ") || inv.contains(" "+pointerName+")") ){

				String[] splitted = inv.split("\\(|\\)");

				for ( String sub : splitted ){
					sub = sub.trim();
					if ( sub.contains(" "+pointerName+" ") || sub.endsWith(" "+pointerName) ){
						if ( sub.endsWith(" "+pointerName+" 0" ) ){
							continue;
						}
						if ( sub.endsWith(" "+pointerName+" null" ) ){
							continue;
						}

						if ( POINTERS_COMPARISON_ENABLED ){
							String[] vars = sub.split(" ");
							if ( pointerParameters.contains(vars[1]) ){
								if ( pointerParameters.contains(vars[2]) ){
									continue;
								}
								//								if ( StringUtils.isNumeric(vars[2]) ){
								//									continue;
								//								}
							}
						}

						return true;
					}
				}
				//				
				//				if ( ! inv.equals("(= "+pointerName+" 0)" ) && ! inv.equals("( = "+pointerName+" 0 )" ) ){
				//					return true;
				//				}
			}
		}
		return false;
	}


	public static FunctionMonitoringData findFunctionInPresenceOfTrimmedName(String fun, Map<String, FunctionMonitoringData> functionsData) {
		System.out.println("FUNCTION NOT FOUND: "+fun+" TRIMMED NAME?");
		for ( Entry<String, FunctionMonitoringData>  e : functionsData.entrySet() ){
			String fn = e.getKey();
			System.out.println("FunctionName: "+fn);
			if ( fn.length() == fun.length()+1 ){
				if ( fn.endsWith(fun) ){
					return e.getValue();
				}
			}
		}

		return null;
	}


	private static boolean excludePropertyForSyntax(String property) {

		if ( property.trim().isEmpty() ){
			System.out.println("Excluding empty property");
			return true;
		}


		if ( property.contains("is a power of") ){
			System.out.println("Excluding power of: "+property);
			return true;
		}

		if ( property.contains(" % ") ){
			System.out.println("Excluding mod operation: "+property);
			return true;
		}

		if ( property.contains("^") ){
			System.out.println("Excluding ^ operation: "+property);
			return true;
		}

		if ( property.contains("+") ){
			System.out.println("Excluding + operation: "+property);
			return true;
		}

		if ( property.contains("(*") ){
			System.out.println("Excluding * operation: "+property);
			return true;
		}

		if ( property.contains("(-") ){
			System.out.println("Excluding - operation: "+property);
			return true;
		}



		if ( returnPattern.matcher(property).find() ){
			System.out.println("Excluding return of pointer: "+property);
			return true;
		}

		if ( uninitVariable.matcher(property).find() ){
			System.out.println("Excluding not initialized variable : "+property);
			return true;
		}

		return false;
	}


	/**
	 * La funzione unisce le varie stringhe utilizzando l'operatore AND
	 * @param str stringa da unire
	 * @return stringa unita
	 */
	private static String unisce(List<String> properties) {
		String result = "";

		for(int i=0, m=properties.size(); i<m ; i++) {

			if(i==0) {
				result = properties.get(i);
			} else {
				result = "(and " + result + " " + properties.get(i) + " )";
			}

		}

		return result;

	}




	/*-------------------metodo principale ------------------------------------------*/

	/**
	 * Metodo "version_control"
	 * @param directory_v1 ad esempio: path/to/directory/AnalysisV0
	 * @param directory_v2 ad esempio: path/to/directory/AnalysisV1
	 * @param name1 ad esempio: V0
	 * @param name2 ad esempio: V1
	 * @param path  path/to/directory
	 * @return lista di array di stringhe contenente:
	 * - risultato enter (sat/unsat)
	 * - risultato exit (sat/unsat)
	 * - invarianti enter (separati da "<->")
	 * - invarianti exit (separati da "<->")
	 * - i nomi delle funzione nell'ordine in cui sono stati analizzati
	 * @throws Exception 
	 */
	private static ArrayList<String []> version_control(String directory_v1, String directory_v2, String name1, String name2, String path) throws Exception{

		ArrayList<String []> results = new ArrayList<String []>();
		//fileEditAll(String directory) recupera tutti i file dtrace e ne pulisce il contenuto trmite
		//fileEdit(String file_name), che li salva nella cartella root dell'analisi

		int how_many_V1;
		int how_many_V2;

		how_many_V1 = countTraceFiles(directory_v1);
		how_many_V2 = countTraceFiles(directory_v2);


		//		System.out.println("Numero file dtrace per directory1: "+ how_many + "; per la 2: " + how_many2);
		//		System.exit(0);

		//how_many e' utile per contare i file dtrace dai file decls
		//how_many e' utile successivamente per contare le occorrenze del quale fare il confronto
		if(how_many_V1 != how_many_V2) {
			System.out.println("Warning! The number of of methods is different!");
			//			results.add(null);
			//			results.add(null);
			//			return results;
		} else {
			System.out.println("The number of of methods is equals!");
		}

		//TODO Da togliere
		System.out.println("HOWMANY: " + how_many_V1 + " " + how_many_V2);

		if(how_many_V1 < 0) {
			throw new Exception("No file traces!");
		}


		//2 array nel quale salvo i risultati prodotti da z3 (sat/unsat)
		String [] enter = new String[how_many_V1];
		String [] exit = new String[how_many_V1];

		//2 array nel quale salvo gli invarianti prodotti da z3
		String [] enter_invariants = new String[how_many_V1];
		String [] exit_invariants = new String[how_many_V1];

		//array in cui metto i nomi delle funzioni
		String [] functions_name = new String[how_many_V1];




		//Folder principale che contiene i .decls e i .dtrace
		File directory = new File(directory_v1);



		//Ritorna i nomi dei file contenuti nella directory
		String [] fileNamesPreprocess = directory.list();
		List<String> fileNames = new ArrayList<String>();



		for(int i = 0; i < fileNamesPreprocess.length; i++){
			File file = new File(directory_v1+File.separator+fileNamesPreprocess[i]);
			//Se e' un file e non una directory e lo faccio solo per i ".dtrace" (se lo facevo anche per i ".decls" facevo 2 cose uguali)
			if (file.isFile() && fileNamesPreprocess[i].endsWith(".dtrace")) {
				fileNames.add(fileNamesPreprocess[i]);
			}
		}


		//		File monitoredFunctionsFileV1 = new File ( directory_v1+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions.original.ser" );
		//		Map<String, FunctionMonitoringData> functionsDataV1 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV1);

		Map<String, FunctionMonitoringData> functionsDataV1 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(new File(directory_v1));

		//		File monitoredFunctionsFileV2 = new File ( directory_v2+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions.original.ser" );
		//		Map<String, FunctionMonitoringData> functionsDataV2 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV2);

		Map<String, FunctionMonitoringData> functionsDataV2 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(new File(directory_v2));

		for(int i = 0; i < fileNames.size(); i++) {	
			try {
				String functionNameWithoutExtension = removeExtensionFromName(fileNames.get(i), ".dtrace");
				System.out.println("=====================================================================================================");
				System.out.println("Analysis_"+name1+"-->"+"Analysis_"+name2);
				System.out.println("funzione_"+functionNameWithoutExtension+"_enter:::");
				System.out.println("function id: "+i);

				String comparison= name1+"vs"+name2;

				functions_name[i] = functionNameWithoutExtension;

				//Controlla se quella finzione esiste anche nella seconda versione (directory_v2)
				boolean existOtherFile = existOtherFile(directory_v2, fileNames.get(i));

				//					//Recupero le funzioni che vanno confrontate
				//					int otherFunctionV2 = getOtherFunction(getFunctionName(directory_v1, i, "ENTER"), "ENTER", directory_v2);

				//cleanString(String nomecartella, int numeroFunzione, int parte<1:enter, 2:exit>)
				String enter1 = "";
				String enter2 = "";
				if (existOtherFile) {

					//divideVariables(String cartella1, String cartella2, int parte<1:enter,2:exit>, int numero_funzione)
					divideVariables(directory_v1, directory_v2, 1, functionNameWithoutExtension);	

					File declV1 = getDeclarationFile(directory_v1, functionNameWithoutExtension);

					File declV2 = getDeclarationFile(directory_v2, functionNameWithoutExtension);

					enter1 = inferProperties(directory_v1, functionNameWithoutExtension, 1, functionsDataV1);

					enter2 = inferProperties(directory_v2, functionNameWithoutExtension, 1, functionsDataV2);
				} else {
					System.out.println("Function missing from "+directory_v2+" "+functionNameWithoutExtension+" ENTER");

					divideVariables(directory_v1, null, 1, functionNameWithoutExtension);	

					enter1 = inferProperties(directory_v1, functionNameWithoutExtension, 1, functionsDataV1);
					enter2 = "missing";

				}

				enter1 = resetToTruIfEmpty(enter1,"enter1");
				enter2 = resetToTruIfEmpty(enter2,"enter2");

				//TODO Da togliere
				System.out.println("enter1: " + enter1 + ", enter2: " + enter2);

				//controllo che le stringhe di enter non siano nulle
				checkEquivalenceOfEnterExit(":::ENTER",enter, enter_invariants, i, enter1, enter2, comparison+":"+functionNameWithoutExtension );

				System.out.println("Fine Enter\n\n\n");
				System.out.println("=====================================================================================================");					
				System.out.println("Analysis_"+name1+"-->"+"Analysis_"+name2);
				System.out.println("funzione_"+functionNameWithoutExtension+"_exit:::");

				//Controlla se quella finzione esiste anche nella seconda versione (directory_v2)
				existOtherFile = existOtherFile(directory_v2, fileNames.get(i));

				//Recupero le funzioni che vanno confrontate
				//					otherFunctionV2 = getOtherFunction(getFunctionName(directory_v1, i, "EXIT"), "EXIT", directory_v2);

				String exit1 = "";
				String exit2 = "";
				if (existOtherFile) {

					divideVariables(directory_v1, directory_v2, 2, functionNameWithoutExtension);



					exit1 = inferProperties(directory_v1, functionNameWithoutExtension, 2, functionsDataV1);
					exit2 = inferProperties(directory_v2, functionNameWithoutExtension, 2, functionsDataV2);
				} else {
					System.out.println("Function missing from "+directory_v2+" "+functionNameWithoutExtension+" EXIT");

					divideVariables(directory_v1, null, 2, functionNameWithoutExtension);

					exit1 = inferProperties(directory_v1, functionNameWithoutExtension, 2, functionsDataV1);
					exit2 = "missing";
				}

				exit1 = resetToTruIfEmpty(exit1,"exit1");
				exit2 = resetToTruIfEmpty(exit2,"exit2");

				//TODO Da togliere
				System.out.println("exit1: " + exit1 + "\n, exit2: " + exit2);

				checkEquivalenceOfEnterExit(":::EXIT",exit, exit_invariants, i, exit1, exit2, comparison+":"+functionNameWithoutExtension );

				System.out.println("Fine\n\n\n");

				System.out.println("=====================================================================");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		results.add(enter);
		results.add(exit);
		results.add(enter_invariants);
		results.add(exit_invariants);
		results.add(functions_name);

		return results;
	}


	public static boolean functionIsImplemented(
			Map<String, FunctionMonitoringData> functionsDataV2,
			String functionNameWithoutExtension) {
		return functionsDataV2.containsKey(functionNameWithoutExtension);
	}


	private static int countTraceFiles(String analysisDirectory) {
		File dir = new File( analysisDirectory );
		String[] files = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".dtrace");
			}
		});

		return files.length;
	}


	private static String resetToTruIfEmpty(String exit1, String ppName) {
		if ( exit1.isEmpty() ){
			System.out.println("No model for "+ppName);
			exit1 = "true";
		}
		return exit1;
	}

	private static void checkEquivalenceOfEnterExit(String point, String[] exit,
			String[] exit_invariants, int posExit, String exit1, String exit2, String functionNameWithoutExtension) {
		Z3 z3=new Z3();
		if ( exit2.equals("missing") ){
			exit[posExit] = "missing";
		} else if ( exit1.equals("missing") ){
			exit[posExit] = "missing";
		} else if (!(exit1.isEmpty() || exit2.isEmpty() )) {

			z3Counter++;
			System.out.println("Z3 counter: "+z3Counter);
			System.out.println("Executing Z3 on "+functionNameWithoutExtension+point);
			String resultZ3 = z3.checkConstraints(z3Counter, exit1, z3Counter, exit2, true, true, allVariables, allStructure);

			System.out.println("Executed Z3 on "+functionNameWithoutExtension+point+" result:"+resultZ3);
			System.out.println("posExit: "+posExit);
			System.out.println(resultZ3);

			exit[posExit] = resultZ3;
			exit_invariants[posExit] = exit1+"<->"+exit2;

		} else {
			exit[posExit] = "nothing";
		}

		exit_invariants[posExit] = exit1+"<->"+exit2;

	}

	private static void _checkEquivalenceOfExit(String[] exit,
			String[] exit_invariants, int posExit, String exit1, String exit2, String functionNameWithoutExtension) {
		Z3 z3=new Z3();
		if ( exit2.equals("missing") ){
			exit[posExit] = "missing";
		} else if ( exit1.equals("missing") ){
			exit[posExit] = "missing";
		} else if (!(exit1.isEmpty() || exit2.isEmpty() )) {

			z3Counter++;
			System.out.println("Z3 counter: "+z3Counter);
			System.out.println("Executing Z3 on "+functionNameWithoutExtension+":::EXIT");
			String resultZ3 = z3.checkConstraints(z3Counter, exit1, z3Counter, exit2, true, true, allVariables, allStructure);

			System.out.println("Executed Z3 on "+functionNameWithoutExtension+":::EXIT result:"+resultZ3);
			System.out.println("posExit: "+posExit);
			System.out.println(resultZ3);

			exit[posExit] = resultZ3;
			exit_invariants[posExit] = exit1+"<->"+exit2;

		} else {
			exit[posExit] = "nothing";
		}

		exit_invariants[posExit] = exit1+"<->"+exit2;

	}


	private static void _checkEquivalenceOfEnter(String[] enter,
			String[] enter_invariants, int posEnter, String enter1, String enter2, String functionNameWithoutExtension) {
		Z3 z3=new Z3();
		if ( enter2.equals("missing") ){
			enter[posEnter] = "missing";
			enter_invariants[posEnter] = enter1+"<->"+"missing";
			System.out.println("Missing CALL: "+functionNameWithoutExtension);
		} else if(! (enter1.isEmpty() || enter2.isEmpty() )){

			z3Counter++;
			System.out.println("Z3 counter: "+z3Counter);
			System.out.println("Executing Z3 on "+functionNameWithoutExtension+":::ENTER");
			//checkConstraint(int idStringa1, String stringa1, int idStringa2, String stringa2, boolena in, boolean verificaEquivalenza, ArrayList<String> variabili, ArrayList<String> strutture)
			String resultZ3 = z3.checkConstraints(z3Counter, enter1, z3Counter, enter2, true, true, allVariables, allStructure);

			System.out.println("Executed Z3 on "+functionNameWithoutExtension+":::ENTER result:"+resultZ3);
			System.out.println("posExit: "+posEnter);
			System.out.println(resultZ3);		//sat/unsat
			//printer.println("funzione_"+i+"_enter:::"+risultato);;
			enter[posEnter] = resultZ3;
			enter_invariants[posEnter] = enter1+"<->"+enter2;

		} else {
			enter[posEnter] = "nothing";

			System.out.println("Missing properties!!!!");
		}

	}


	/**
	 * Rimuove l'estensione del file; es: fun.dtrace -> fun
	 * @param nameWithExtension nome del file con l'estensione
	 * @param extension l'estensione da eliminare (punto compreso)
	 * @return nome del file senza l'estensione
	 */
	private static String removeExtensionFromName(String nameWithExtension, String extension) {
		return nameWithExtension.substring(0, nameWithExtension.length() - extension.length());	
	}


	/**
	 * Dice se l'alatra funzione esiste o no
	 * @param filePath percorso in cui cercare il file
	 * @return true se il file esiste, false altrimenti
	 */
	private static boolean existOtherFile(String path, String fileName) {
		File directory = new File(path);
		String[] filesInDirectory = directory.list();

		if ( ! directory.isDirectory() ){
			throw new IllegalStateException("Not a directory "+directory.getAbsolutePath());
		}

		if( filesInDirectory == null ){
			return false;
		}

		for (int i = 0; i < filesInDirectory.length; i++) {
			if (filesInDirectory[i].equals(fileName)) {
				return true;
			}
		}

		return false;
	}


	/**
	 * Restituisce la posizione dell'elemento nell'array
	 * @param array l'array in cui cercare
	 * @param name l'elemento da cercare
	 * @return la posizione nell'array in cui si trova l'elemento, -1 se non e' presente
	 */
	private static int getPosition(String[] array, String name) {
		if(array != null && name != null) {
			for (int i = 0; i < array.length; i++) {
				if (array[i].equals(name)) {
					return i;
				}
			}
		}
		return -1;
	}



	enum ConflictType { WEAK, HIGH, SAME_IMPL, NONE, NONE_TOOL_ERROR };


	/*--------------------main -----------------------------------------*/

	/**
	 * Metodo main
	 * @param args 0 = path, 1 = AnalysisV0, 2 = AnalysisV1, 3 = AnalysisV2
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		CSourceAnalyzerRegistry.setCSourceAnalyzer(new CDTStandaloneCFileAnalyzer());

		//TODO Mettere il logger in tutto il codice
		logger.info("Start function main");

		String path = args[0];

		if ( !path.endsWith(File.separator) ){
			path = path + File.separator;
		}

		String name0 = args[1].substring(9);		//V0
		String name1 = args[2].substring(9);		//V1
		String name2 = args[3].substring(9);		//V2

		//creo gli array del controllo delle versioni
		populateFunctionsFilter();


		//padre vs figlio1 (V0 vs V1)
		ArrayList<String []> resultV0vsV1 = new ArrayList<String []>();
		String directory_v0 = path+args[1];
		String directory_v1 = path+args[2];
		String directory_v2 = path+args[3];


		VersionControl vc = new VersionControl();


		if ( ! tracesAlreadyEdited ){	
			fileEditAll(directory_v0);
			fileEditAll(directory_v1);
			fileEditAll(directory_v2);

			RepairDeclarationFiles r = new RepairDeclarationFiles();
			r.identifyVariablesWithWronglyDeclaredType(new File(directory_v0));
			r.identifyVariablesWithWronglyDeclaredType(new File(directory_v1));
			r.identifyVariablesWithWronglyDeclaredType(new File(directory_v2));


			r.repairDeclarationFilesInFolder(new File(directory_v0));
			r.repairDeclarationFilesInFolder(new File(directory_v1));
			r.repairDeclarationFilesInFolder(new File(directory_v2));


		}



		resultV0vsV1 = vc.version_control(directory_v0, directory_v1, name0, name1, path);
		String [] array0_enter = resultV0vsV1.get(0);
		String [] array0_exit = resultV0vsV1.get(1);
		String [] array0_enter_invariants = resultV0vsV1.get(2);
		String [] array0_exit_invariants = resultV0vsV1.get(3);
		String [] array0_function_name = resultV0vsV1.get(4);



		//padre vs figlio2 (V0 vs V2)
		ArrayList<String []> resultV0vsV2 = new ArrayList<String []>();
		resultV0vsV2 = vc.version_control(directory_v0, directory_v2, name0, name2, path);
		String [] array1_enter = resultV0vsV2.get(0);
		String [] array1_exit = resultV0vsV2.get(1);
		String [] array1_enter_invariants = resultV0vsV2.get(2);
		String [] array1_exit_invariants = resultV0vsV2.get(3);
		String [] array1_function_name = resultV0vsV2.get(4);


		//figlio1 vs figlio2 (V1 vs V2)
		ArrayList<String []> resultV1vsV2 = new ArrayList<String []>();

		resultV1vsV2 = vc.version_control(directory_v1, directory_v2, name1, name2, path);
		String [] array2_enter = resultV1vsV2.get(0);
		String [] array2_exit = resultV1vsV2.get(1);
		String [] array2_enter_invariants = resultV1vsV2.get(2);
		String [] array2_exit_invariants = resultV1vsV2.get(3);
		String [] array2_function_name = resultV1vsV2.get(4);








//		File monitoredFunctionsFileV0 = new File ( directory_v0+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions.original.ser" );
//		Map<String, FunctionMonitoringData> functionsDataV0 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV0);
		Map<String, FunctionMonitoringData> functionsDataV0 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(new File(directory_v0));
		
//		File monitoredFunctionsFileV1 = new File ( directory_v1+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions.original.ser" );
//		Map<String, FunctionMonitoringData> functionsDataV1 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV1);
		Map<String, FunctionMonitoringData> functionsDataV1 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(new File(directory_v1));

		
//		File monitoredFunctionsFileV2 = new File ( directory_v2+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions.original.ser" );
//		Map<String, FunctionMonitoringData> functionsDataV2 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV2);
		Map<String, FunctionMonitoringData> functionsDataV2 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(new File(directory_v2));







		//TODO Da sistemare
		if(array0_enter==null || array0_exit==null || array1_enter==null || array1_exit==null || array2_enter==null || array2_exit==null ) {
			System.out.println("Error! The number of methods is different or there aren't methods!");

			return;
		}


		//file dove salvo i risultati
		PrintStream fileOutputResults = new PrintStream(new FileOutputStream(path+File.separator+"bdci.results.txt"));

		//TODO Da togliere
		System.out.println("--------------------------------------------------");
		System.out.println("array0");
		for (int i = 0; i <array0_function_name.length; i++) {
			System.out.println(i);
			System.out.println(array0_function_name[i]);
			System.out.println(array0_enter[i]);
			System.out.println(array0_exit[i]);
			System.out.println(array0_enter_invariants[i]);
			System.out.println(array0_exit_invariants[i]);
		}

		//TODO Da togliere
		System.out.println("--------------------------------------------------");
		System.out.println("array1");
		for (int i = 0; i <array1_function_name.length; i++) {
			System.out.println(i);
			System.out.println(array1_function_name[i]);
			System.out.println(array1_enter[i]);
			System.out.println(array1_exit[i]);
			System.out.println(array1_enter_invariants[i]);
			System.out.println(array1_exit_invariants[i]);
		}

		//TODO Da togliere
		System.out.println("--------------------------------------------------");
		System.out.println("array2");
		for (int i = 0; i <array2_function_name.length; i++) {
			System.out.println(i);
			System.out.println(array2_function_name[i]);
			System.out.println(array2_enter[i]);
			System.out.println(array2_exit[i]);
			System.out.println(array2_enter_invariants[i]);
			System.out.println(array2_exit_invariants[i]);
		}


		System.out.println();
		System.out.println("=====================================================================================================");
		System.out.println();
		System.out.println("Results:");

		HashSet<String> allFunctionNames = new HashSet<String>();

		for (int i = 0; i <array0_function_name.length; i++) {
			allFunctionNames.add(array0_function_name[i]);
		}

		containErrors = false;


		for (String functionName : allFunctionNames) {
			processFunctionResults(name0, name1, name2,
					array0_enter, array0_exit, array0_enter_invariants,
					array0_exit_invariants, array0_function_name,
					array1_enter, array1_exit, array1_enter_invariants,
					array1_exit_invariants, array1_function_name,
					array2_enter, array2_exit, array2_enter_invariants,
					array2_exit_invariants, array2_function_name,
					functionsDataV0, functionsDataV1, functionsDataV2,
					fileOutputResults, containErrors, functionName);


		}

		fileOutputResults.close();

		logger.info("End function main");

		if ( containErrors ){
			System.out.println("Errors observed during the execution of Z3");
		}

	}


	public static boolean processFunctionResults(String name0, String name1,
			String name2, String[] array0_enter, String[] array0_exit,
			String[] array0_enter_invariants, String[] array0_exit_invariants,
			String[] array0_function_name, String[] array1_enter,
			String[] array1_exit, String[] array1_enter_invariants,
			String[] array1_exit_invariants, String[] array1_function_name,
			String[] array2_enter, String[] array2_exit,
			String[] array2_enter_invariants, String[] array2_exit_invariants,
			String[] array2_function_name,
			Map<String, FunctionMonitoringData> functionsDataV0,
			Map<String, FunctionMonitoringData> functionsDataV1,
			Map<String, FunctionMonitoringData> functionsDataV2,
			PrintStream fileOutputResults, boolean containErrors,
			String functionName) throws FileNotFoundException {
		//Contengono la posizione in cui cercare, per ogni funzione, i risultati per V0-V1
		int pos0 = getPosition(array0_function_name, functionName);
		//Contengono la posizione in cui cercare, per ogni funzione, i risultati per V0-V2
		int pos1 = getPosition(array1_function_name, functionName);
		//Contengono la posizione in cui cercare, per ogni funzione, i risultati per V1-V2
		int pos2 = getPosition(array2_function_name, functionName);

		System.out.println("FunctionName: "+functionName);
		System.out.println("pos V0-V1: "+pos0);
		System.out.println("pos V0-V2: "+pos1);
		System.out.println("pos V1-V2: "+pos2);

		//Risulti per V0-V1 enter (di base "nothing")
		String result0Enter = "missing"; //missing in V0
		if (pos0 != -1) {
			if ( array0_exit[pos0] == null ){
				result0Enter = "missing";	
			} else {
				result0Enter = array0_enter[pos0].trim();
			}
		}

		//Risulti per V0-V2 enter (di base "nothing")
		String result1Enter = "missing"; //missing in V0
		if (pos1 != -1) {
			if ( array1_exit[pos1] == null ){
				result1Enter = "missing";	
			} else {
				result1Enter = array1_enter[pos1].trim();
			}
		}

		//Risulti per V1-V2 enter (di base "nothing")
		String result2Enter = "missing"; //missing in V1
		if (pos2 != -1) {
			if ( array2_exit[pos2] == null ){
				result2Enter = "missing";	
			} else {
				result2Enter = array2_enter[pos2].trim();
			}
		}



		//Risulti per V0-V1 exit (di base "nothing")
		String result0Exit = "missing";
		if (pos0 != -1) {
			if ( array0_exit[pos0] == null ){
				result0Exit = "missing";	
			} else {
				result0Exit = array0_exit[pos0].trim();
			}
		}
		//Risulti per V0-V2 exit (di base "nothing")
		String result1Exit = "missing";
		if (pos1 != -1) {
			if ( array1_exit[pos1] == null ){
				result1Exit = "missing";	
			} else {
				result1Exit = array1_exit[pos1].trim();
			}
		}
		//Risulti per V1-V2 exit (di base "nothing")
		String result2Exit = "missing";
		if (pos2 != -1) {
			if ( array2_exit[pos2] == null ){
				result2Exit = "missing";	
			} else {
				result2Exit = array2_exit[pos2].trim();
			}
		}



		//Risulti invarianti enter per V0-V1 exit (di base "nothing")
		String result0EnterInvariants = "null";
		if (pos0 != -1) {
			result0EnterInvariants = array0_enter_invariants[pos0];
		}
		//Risulti invarianti enter per V0-V2 exit (di base "nothing")
		String result1EnterInvariants = "null";
		if (pos1 != -1) {
			result1EnterInvariants = array1_enter_invariants[pos1];
		}
		//Risulti invarianti enter per V1-V2 exit (di base "nothing")
		String result2EnterInvariants = "null";
		if (pos2 != -1) {
			result2EnterInvariants = array2_enter_invariants[pos2];
		}



		//Risulti invarianti exit per V0-V1 exit (di base "nothing")
		String result0ExitInvariants = "null";
		if (pos0 != -1) {
			result0ExitInvariants = array0_exit_invariants[pos0];
		}
		//Risulti invarianti exit per V0-V2 exit (di base "nothing")
		String result1ExitInvariants = "null";
		if (pos1 != -1) {
			result1ExitInvariants = array1_exit_invariants[pos1];
		}
		//Risulti invarianti exit per V1-V2 exit (di base "nothing")
		String result2ExitInvariants = "null";
		if (pos2 != -1) {
			result2ExitInvariants = array2_exit_invariants[pos2];
		}




		//Stampo i risultati
		System.out.println(functionName+"_ENTER" + " -> 0vs1: " + result0Enter + ", 0vs2: " + result1Enter + ", 1vs2: " + result2Enter);
		System.out.println(functionName+"_EXIT" + " -> 0vs1: " + result0Exit + ", 0vs2: " + result1Exit + ", 1vs2: " + result2Exit);


		ConflictType conflictType = identifyConflictType(
				name0,name1,name2,
				functionsDataV0, functionsDataV1, functionsDataV2,
				functionName, 
				result0Enter, result1Enter, result2Enter,
				result0EnterInvariants, result1EnterInvariants,
				result2EnterInvariants);

		if ( conflictType == ConflictType.NONE_TOOL_ERROR ){
			containErrors = true;
		}

		printConflictResult("_ENTER",name0, name1, name2,
				fileOutputResults, functionName,
				result0EnterInvariants, result1EnterInvariants,
				result2EnterInvariants, missingEnter, conflictType);

		conflictType = identifyConflictType(name0, name1, name2,
				functionsDataV0, functionsDataV1, functionsDataV2,
				functionName, result0Exit, result1Exit, result2Exit,
				result0ExitInvariants, result1ExitInvariants,
				result2ExitInvariants);


		printConflictResult("_EXIT",name0, name1, name2,
				fileOutputResults, functionName,
				result0ExitInvariants, result1ExitInvariants,
				result2ExitInvariants, missingEnter, conflictType);
		return containErrors;
	}


	public static ConflictType identifyConflictType(
			String model0, String model1, String model2,
			Map<String, FunctionMonitoringData> functionsDataV0,
			Map<String, FunctionMonitoringData> functionsDataV1,
			Map<String, FunctionMonitoringData> functionsDataV2,
			String functionName, 
			String result0Enter, String result1Enter, String result2Enter,
			String result0EnterInvariants, String result1EnterInvariants, String result2EnterInvariants) throws FileNotFoundException {
		missingEnter=false;
		{
			boolean missingMeansUnsat = false;
			if ( result0Enter.equals("unsat") || result1Enter.equals("unsat") ){
				missingMeansUnsat=true;
			}

			missingMeansUnsat=false; //emprically shown that is better to ignore missing models (maybe it's because of mismatching variable names etc) 
			result0Enter = parseMissing(result0Enter,functionName,functionsDataV0,functionsDataV1,missingMeansUnsat);
			result1Enter = parseMissing(result1Enter,functionName,functionsDataV0,functionsDataV2,missingMeansUnsat);
			result2Enter = parseMissing(result2Enter,functionName,functionsDataV1,functionsDataV2,missingMeansUnsat);
		}




		ConflictType conflictType;

		//Se tutti e 3 gli enter sono unsat ho high-order warning
		if(result0Enter.equals("unsat") && result1Enter.equals("unsat") && result2Enter.equals("unsat")){
			if ( missingInvariants( result0EnterInvariants, result1EnterInvariants, result2EnterInvariants ) ){
				conflictType = ConflictType.WEAK;
			} else {
				conflictType = ConflictType.HIGH;
			}
		} else if(result0Enter.equals("unsat") && result1Enter.equals("unsat") && result2Enter.equals("sat")){
			conflictType = ConflictType.SAME_IMPL;
			if ( FilterLowOrderConflicts.isWeakHigherOrderConflict(functionName, true, functionsDataV1, functionsDataV2 ) ){
				conflictType = ConflictType.WEAK;
			}
		} else {
			if ( invalidOutput(result0Enter, result1Enter, result2Enter) ){
				conflictType = ConflictType.NONE_TOOL_ERROR;
			} else {
				conflictType = ConflictType.NONE;
			}
		}
		return conflictType;
	}


	private static boolean missingInvariants(String result0EnterInvariants,
			String result1EnterInvariants, String result2EnterInvariants) {


		if ( result0EnterInvariants.startsWith("true<->")
				|| result1EnterInvariants.startsWith("true<->")
				|| result2EnterInvariants.startsWith("true<->")
				||result0EnterInvariants.endsWith("<->true")
				|| result1EnterInvariants.endsWith("<->true")
				|| result2EnterInvariants.endsWith("<->true")	
				){
			return true;
		}

		return false;
	}


	public static void printConflictResult(
			String position,
			String name0, String name1,
			String name2, PrintStream fileOutputResults, 
			String functionName, String result0EnterInvariants,
			String result1EnterInvariants, String result2EnterInvariants,
			boolean missingEnter2, ConflictType conflictType) {

		String missingCall="";
		if ( missingEnter2 ){
			missingCall = "(MISSING CALL)";
		}
		switch ( conflictType ){
		case HIGH:
			printHighOrderConflict(position, name0, name1, name2,
					fileOutputResults, functionName,
					result0EnterInvariants, result1EnterInvariants,
					result2EnterInvariants, missingCall);

			break;
		case SAME_IMPL:
			String head = "NO CONFLICT (SAME IMPLEMENTATION in V1/V2)";
			printConflict(
					head, position, name0, name1, name2,
					null, functionName,
					result0EnterInvariants, result1EnterInvariants,
					result2EnterInvariants, missingCall);
			break;

		case WEAK:
			conflictType = ConflictType.SAME_IMPL;
			head = "WEAK-HIGHER-ORDER CONFLICT";
			printConflict(head,position,name0, name1, name2,
					null, functionName,
					result0EnterInvariants, result1EnterInvariants,
					result2EnterInvariants, missingCall);
			break;

		case NONE_TOOL_ERROR:
			printNoConflictToolError(position,name0, name1, name2, functionName,
					result0EnterInvariants, result1EnterInvariants,
					result2EnterInvariants);
			break;

		case NONE:
			printNoConflict(position,name0, name1, name2, functionName,
					result0EnterInvariants, result1EnterInvariants,
					result2EnterInvariants);

		}
	}


	public static void printNoConflict(String position, String name0, String name1,
			String name2, String functionName, String result0EnterInvariants,
			String result1EnterInvariants, String result2EnterInvariants) {
		printConflict("NO CONFLICT", position, name0, name1, name2, null, functionName, result0EnterInvariants, result1EnterInvariants, result2EnterInvariants, "");

	}


	public static void printModels(String name0, String name1, String name2,
			String result0Invariants, String result1Invariants,
			String result2Invariants) {

		System.out.println("Model "+name0+"<->"+name1+": \n"+result0Invariants+";");
		System.out.println("Model "+name0+"<->"+name2+": \n"+result1Invariants+";");
		System.out.println("Model "+name1+"<->"+name2+": \n"+result2Invariants+";");

	}


	public static void printNoConflictToolError(String position, String name0, String name1,
			String name2, String functionName, String result0EnterInvariants,
			String result1EnterInvariants, String result2EnterInvariants) {
		printConflict("ERRONOUS RESULT", position, name0, name1, name2, null, functionName, result0EnterInvariants, result1EnterInvariants, result2EnterInvariants, "");

	}


	public static void printConflict(String head, String position, String name0, String name1,
			String name2, PrintStream fileOutputResults, String functionName,
			String result0EnterInvariants, String result1EnterInvariants,
			String result2EnterInvariants, String missingCall) {
		//Se solo V1 e V2 enter sono unsat ho low-order warning
		System.out.println(head+": function "+functionName+position+" "+missingCall);
		printModels(name0, name1, name2, result0EnterInvariants,
				result1EnterInvariants, result2EnterInvariants);
		System.out.println();

		if ( fileOutputResults != null ){
			fileOutputResults.println(head+": function "+functionName+position+" "+missingCall);
			fileOutputResults.println("Model "+name0+"<->"+name1+": \n"+result0EnterInvariants+";");
			fileOutputResults.println("Model "+name0+"<->"+name2+": \n"+result1EnterInvariants+";");
			fileOutputResults.println("Model "+name1+"<->"+name2+": \n"+result2EnterInvariants+";");
			fileOutputResults.println();
		}
	}

	public static void printHighOrderConflict(

			String position, String name0, String name1,
			String name2, PrintStream fileOutputResults, String functionName,
			String result0EnterInvariants, String result1EnterInvariants,
			String result2EnterInvariants, String missingCall) {

		printConflict("HIGHER-ORDER CONFLICT", position, name0, name1, name2, fileOutputResults, functionName, result0EnterInvariants, result1EnterInvariants, result2EnterInvariants, missingCall);


	}


	private static String parseMissing(String result0Enter, String functionName, Map<String, FunctionMonitoringData> functionsDataV0, Map<String, FunctionMonitoringData> functionsDataV1, boolean missingMeansUnsat) {
		if(result0Enter.equals("missing") ){

			if ( missingMeansUnsat == false ){
				return "sat";
			}

			boolean implementedInV0 = functionsDataV0.containsKey(functionName);
			boolean implementedInV1 = functionsDataV1.containsKey(functionName);

			if (implementedInV0 && implementedInV1 ){
				result0Enter="unsat";
			} else {
				result0Enter="sat";
			}
		}
		return result0Enter;
	}


	private static Set<String> functionsFilter;
	private static void populateFunctionsFilter() {
		String func = System.getProperty("bdci.functionsToInclude");

		if ( func == null ){
			return;
		}

		functionsFilter =  new HashSet<>();
		String[] list = func.split(";");
		for( String  f : list ){
			functionsFilter.add(f);
		}
	}


	private static boolean invalidOutput(String result0Enter,
			String result1Enter, String result2Enter) {
		result0Enter = result0Enter.trim();
		result1Enter = result1Enter.trim();
		result2Enter = result2Enter.trim();
		boolean valid = ( ( result0Enter.equals("unsat") || result0Enter.equals("sat") ) &&
				(result1Enter.equals("unsat") || result1Enter.equals("sat") ) && 
				(result2Enter.equals("unsat") || result2Enter.equals("sat") ) );

		return valid == false;
	}

}
