package theoremProver.z3;


/**
 * @author Paola Percuoco
 *
 */
public class ConstraintsValidator {
	private String constraint;
	
	public ConstraintsValidator(){}
	
	public String validate(String constraint){
		this.constraint = constraint;

		replaceOrig();
		convertNegativeNum();
		//convertRealPower();
		convertModOperation();
		deleteStore();
		
		return this.constraint;
	}
	
	private void replaceOrig(){
		/*
		 * sostituzione dell'invariante orig(xxx) con xxx-xxx 
		 */
		/*String regex = "orig\\((c\\d+)+\\)";
		//System.out.println("CONSTAINTS BEFORE ORIG " + constraint);
		 */
		String regex="[a-zA-Z]+ % orig\\(([a-zA-Z_0-9])\\) == [a-zA-Z_0-9]";
		//this.constraint = constraint.replaceAll(regex, "");
		//System.out.println("CONSTAINTS AFTER ORIG " + constraint);
	}
	
	private void convertNegativeNum() {
		/*
		 * poich� Z3 non ha i numeri negativi, questi vengono sostuiti nel seguente modo:
		 * -4 diventa (- 0 4)
		 */
		String regexDouble = "-(\\d+\\W\\d+)";
		String regexInt = "-(\\d+)";
		//System.out.println("BEFORE " + this.constraint);
		//if(this.constraint.contains("."))
			this.constraint = constraint.replaceAll(regexDouble, "(- 0 $1)");
		//else	
			this.constraint = constraint.replaceAll(regexInt, "(- 0 $1)");
		//System.out.println(this.constraint);
	}
	
	//redaelli
	private void convertRealPower(){
		String regex = "\\d+.\\d+[a-z]\\(- \\d+\\W\\d+\\)";
		this.constraint=constraint.replaceAll(regex, "(^ $0)");
		this.constraint=constraint.replace("d(", " (");
	}
	
	private void convertModOperation(){
		String regex= "([a-zA-Z_0-9]+) % ([a-zA-Z_0-9]+) == ([0-9]+)";
		this.constraint=constraint.replaceAll(regex, "(= (mod $1 $2) $3)");
	}
	
	
	private void deleteStore(){
		/*
		 * cancellazione dell'invariante store() 
		 */
		String regex = "store\\(c\\d+\\)";
		constraint = constraint.replaceAll(regex +"\n", "");
	}
	
	
//	private void runZ3(){
////		tmpDir= "D://InvTest//";
//		File file = new File(tmpDir);
//		File [] fileList = file.listFiles();
//		for (int i = 0; i<fileList.length; i++){
//			try {
//				runZ3(fileList[i].getName());
//			} catch (Exception e) {
//				
//				e.printStackTrace();
//			}
//		}
//	}
	
	

}
