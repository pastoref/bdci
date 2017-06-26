package it.unimib.disco.lta.bdci.diff;

import it.unimib.disco.lta.bdci.RepairDeclarationFiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import util.FileUtil;

import cpp.gdb.FunctionMonitoringData;
import difflib.DiffUtils;

public class SameImplementationInV1V2 {

	public static void main(String args[]) throws FileNotFoundException, ClassNotFoundException, IOException{

		File parent = new File( args[0] );

		String functionName = args[1];

		if ( sameFunctionImplementationInV1V2( parent, functionName ) ){
			System.out.println("SAME");
			System.exit(0);
		}

		System.out.println("DIFFERENT");
		System.exit(1);

	}

	public static boolean sameFunctionImplementationInV1V2( File parent, String functionName ) throws FileNotFoundException, ClassNotFoundException, IOException{
		File directory_v1 = new File(parent,"AnalysisV1");
		File directory_v2 = new File(parent,"AnalysisV2");

		Map<String, FunctionMonitoringData> functionsDataV1 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(directory_v1);
		Map<String, FunctionMonitoringData> functionsDataV2 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(directory_v2);


		return sameFunctionImplementationInV1V2(functionName, functionsDataV1, functionsDataV2);
	}

	public static boolean sameFunctionImplementationInV1V2(String functionName,
			Map<String, FunctionMonitoringData> functionsDataV1,
			Map<String, FunctionMonitoringData> functionsDataV2)
					throws FileNotFoundException {
		FunctionMonitoringData fV1 = functionsDataV1.get(functionName);
		FunctionMonitoringData fV2 = functionsDataV2.get(functionName);

		if ( fV1 == null ){
			if ( fV2 == null ){
				System.out.println("Missing in both V1/V2: "+functionName);
				return true;
			}
			System.out.println("Missing in V1: "+functionName);
			return false;
		}


		File filePos = fV1.getAbsoluteFile();

		System.out.println(functionName);

		System.out.println(filePos);



		if ( fV2 == null ){
			System.out.println("Missing in V2: "+functionName);
			return false;
		}
		File filePosV2 = fV2.getAbsoluteFile();

		System.out.println(filePosV2);

		return sameFunctionImplementation(fV1, fV2);
	}

	public static boolean sameFunctionImplementation(
			FunctionMonitoringData fV1, FunctionMonitoringData fV2)
					throws FileNotFoundException {
		try {
			List<String> linesV1 = fV1.getSourceCodeLines();
			List<String> linesV2 = fV2.getSourceCodeLines();

			return sameContent(linesV1, linesV2);

		} catch ( RuntimeException e ){
			e.printStackTrace();
			//			System.err.println("Diffing the full file");
			//			
			//			File f1 = fV1.getAbsoluteFile();
			//			File f2 = fV2.getAbsoluteFile();
			//			
			//			List<String> linesV1 = FileUtil.getLines(f1);
			//			List<String> linesV2 = FileUtil.getLines(f2);
			//			
			//			//if the two files are the same, also the implemented functiojns are
			//			return sameContent(linesV1, linesV2); //TODO: what happens if teh function is missing in one version?

			return false;
		}
	}

	public static boolean sameContent(List<String> linesV1, List<String> linesV2) {
		if ( linesV1.size() != linesV2.size() ){
			return false;
		}

		for ( int i = 0, m=linesV1.size(); i < m; i++ ){
			String v1 = linesV1.get(i).trim();
			String v2 = linesV2.get(i).trim();

			if ( ! v1.equals(v2) ){
				return false;
			}
		}

		return true;
	}
}
