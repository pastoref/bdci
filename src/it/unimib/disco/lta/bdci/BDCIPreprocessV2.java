package it.unimib.disco.lta.bdci;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import tools.gdbTraceParser.GdbTraceParser;
import util.JavaRunner;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

public class BDCIPreprocessV2 {
	
	final static String dtrace=File.separator+"BCT"+File.separator+"BCT_DATA"+File.separator+"BCT"+File.separator+"Preprocessing"+File.separator+"dtrace"+File.separator;
	
	public static void main(String[] args) throws IOException{
		
		//String path="D:\\uni\\stage\\Esempi\\finali\\";
		
		//path should point to a folder containing folders: Analysis_V0  Analysis_V1  Analysis_V2
		//the users should already have monitored program executions and run RADAR model inference
		String path = args[0];
		
		File directory=new File(path);
		String[] cartelle= directory.list();
		ArrayList<String> analysis=new ArrayList<String>();
		analysis.add("AnalysisV0");
		analysis.add("AnalysisV1");
		analysis.add("AnalysisV2");
//		for(int i=0; i<cartelle.length;i++){
//			if(cartelle[i].contains("Analysis")){
//				analysis.add(cartelle[i]);
//			}
//		}
		
		Collections.sort(analysis);
		
		File anV0 = new File ( directory, analysis.get(0) );
		File anV1 = new File ( directory, analysis.get(1) );
		File anV2 = new File ( directory, analysis.get(2) );
		
		int maxExecutionTime = 0;
		
		System.setProperty(GdbTraceParser.BCT_NO_FSA_MODELS, "true");
		
		
		JavaRunner.runMainInClass(console.AnomaliesIdentifier.class, Arrays.asList( new String[]{anV2.getAbsolutePath()} ), maxExecutionTime, true);
		
	}
	

}
