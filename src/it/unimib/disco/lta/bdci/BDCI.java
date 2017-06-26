package it.unimib.disco.lta.bdci;

import java.io.File;

public class BDCI {
	
	public static void main(String[] args){
		
		
		//path should point to a folder containing folders: Analysis_V0  Analysis_V1  Analysis_V2
		//the users should already have monitored program executions and run RADAR model inference
		String path = args[0];
		
		File directory=new File(path);
//		String[] cartelle= directory.list();
		
		//Mantiene solo le cartelle chiamate "Analysis"
//		ArrayList<String> analysis=new ArrayList<String>();
//		for(int i=0; i<cartelle.length;i++){
//			if(cartelle[i].contains("Analysis")){
//				analysis.add(cartelle[i]);
//			}
//		}
		
//		System.out.println("Cartelle:");
//		for (String s : analysis) {
//			System.out.println(s);
//		}
//		System.out.println("Fine");
//		System.exit(0);
		
		String [] arg = new String[4];
		//String [] arg1 = new String[3];
		arg[0]=path;
		arg[1]="AnalysisV0";
		arg[2]="AnalysisV1";
		arg[3]="AnalysisV2";
		
		
		
		
		try {
			VersionControl.main(arg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		
	}
	

}
