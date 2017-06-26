package it.unimib.disco.lta.bdci.diff;

import it.unimib.disco.lta.bdci.RepairDeclarationFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.FileUtil;

import cpp.gdb.FunctionMonitoringData;

public class FilterLowOrderConflicts {

	private static final String LOW_ORDER_CONFLICT_FUNCTION = "LOW-ORDER CONFLICT: function ";

	public static void main(String args[]) throws FileNotFoundException, ClassNotFoundException, IOException{
		
		File file = new File( args[0] );
		
		BufferedReader r = new BufferedReader(new FileReader(file));
		
		String line;
		
		
		
		int c=0;
		int p=0;
		while ( ( line = r.readLine() ) != null ){
			if ( line.startsWith(LOW_ORDER_CONFLICT_FUNCTION) ){
				
				
				
				
				
				c++;
				String fname = line.substring(LOW_ORDER_CONFLICT_FUNCTION.length()).trim();
				
				boolean enter;
				if ( fname.endsWith("_ENTER") ) {
					enter=true;
					fname = fname.substring(0,fname.length()-6);
				} else {
					enter=false;
					fname = fname.substring(0,fname.length()-5);
				}
				
				File parent = file.getParentFile();
				
				boolean isWeak = isWeakHigherOrderConflict(fname, enter, parent);
				
				if ( isWeak ){
					System.out.println(line);
					p++;
					for ( int i = 0; i < 6; i++ ){
						System.out.println(r.readLine());
					}

					break;
				}
			}
		}
		
		System.out.println("Total processed: "+c);
		System.out.println("Total included: "+p);
		
	}

	public static boolean isWeakHigherOrderConflict(String fname, boolean enter,
			File dataFolder) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		
		
		
		File directory_v1 = new File(dataFolder,"AnalysisV1");
		File directory_v2 = new File(dataFolder,"AnalysisV2");
		
		Map<String, FunctionMonitoringData> functionsDataV1 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(directory_v1);
		Map<String, FunctionMonitoringData> functionsDataV2 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(directory_v2);
		
		return isWeakHigherOrderConflict(fname, enter, functionsDataV1, functionsDataV2);
	}

	public static boolean isWeakHigherOrderConflict(String fname,
			boolean enter, Map<String, FunctionMonitoringData> functionsDataV1,
			Map<String, FunctionMonitoringData> functionsDataV2)
			throws FileNotFoundException {
		Set<String> funcToProcess = new HashSet<>();
		Map<String,FunctionMonitoringData> mapV1 = new HashMap<>();
		Map<String,FunctionMonitoringData> mapV2 = new HashMap<>();
		if ( enter ){
			
			
			FunctionMonitoringData fV1 = functionsDataV1.get(fname);
			if ( fV1 == null ){
				System.out.println("!!!NULL DATA (FV1) FOR: "+fname);
			}
			for ( FunctionMonitoringData cf : fV1.getCallers() ){
				funcToProcess.add(cf.getMangledName());
				mapV1.put(cf.getMangledName(), cf);
			}
			
			FunctionMonitoringData fV2 = functionsDataV2.get(fname);
			if ( fV2 == null ){
				System.out.println("!!!NULL DATA (fV2) FOR: "+fname);
			}
			for ( FunctionMonitoringData cf : fV2.getCallers() ){
				funcToProcess.add(cf.getMangledName());
				mapV2.put(cf.getMangledName(), cf);
			}
			
			
			
		} else {			
			funcToProcess.add(fname);
		}
		
		for ( String f : funcToProcess ){
			if ( mapV1.containsKey(f) && mapV2.containsKey(f) ){
				FunctionMonitoringData fV1 = mapV1.get(f);
				FunctionMonitoringData fV2 = mapV2.get(f);
				
				if ( fV1.isTargetFunction() && fV2.isTargetFunction() ){
					if ( ! SameImplementationInV1V2.sameFunctionImplementation(fV1,fV2) ){
						return true;
					}
				}
			} else {
				if ( ! SameImplementationInV1V2.sameFunctionImplementationInV1V2(f,functionsDataV1,functionsDataV2) ){
					return true;
				}
			}	
		}
		
		return false;
	}
	
	public static boolean sameFunctionImplementationInV1V2( File parent, String functionName ) throws FileNotFoundException, ClassNotFoundException, IOException{
		File directory_v1 = new File(parent,"AnalysisV1");
		File directory_v2 = new File(parent,"AnalysisV2");
		
		Map<String, FunctionMonitoringData> functionsDataV1 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(directory_v1);
		Map<String, FunctionMonitoringData> functionsDataV2 = RepairDeclarationFiles.retrieveFunctionsDataInAnalysisFolder(directory_v2);
		
		
		FunctionMonitoringData fV1 = functionsDataV1.get(functionName);
		File filePos = fV1.getAbsoluteFile();
		
		System.out.println(filePos);
		
		
		FunctionMonitoringData fV2 = functionsDataV2.get(functionName);
		File filePosV2 = fV2.getAbsoluteFile();
		
		System.out.println(filePosV2);
		
		List<String> linesV1 = fV1.getSourceCodeLines();
		List<String> linesV2 = fV2.getSourceCodeLines();
		
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
