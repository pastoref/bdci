package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipsecpp.core.util.CDTStandaloneCFileAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import util.FileUtil;

import cpp.gdb.CSourceAnalyzerRegistry;
import cpp.gdb.FunctionMonitoringData;
import cpp.gdb.FunctionMonitoringDataSerializer;
import cpp.gdb.Parameter;


public class CreateKvasirConfigList {

	

	private static final String DAIKON_NEW_ENTER_POINT = " ___ENTER:::POINT";
	private static final String DAIKON_NEW_EXIT_POINT = " ___EXIT:::POINT";
	private static final String DAIKON_ENTER_KEY = " :::ENTER";
	private static final String DAIKON_EXIT_KEY = " :::EXIT1";

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, IOException {
		
		CSourceAnalyzerRegistry.setCSourceAnalyzer(new CDTStandaloneCFileAnalyzer());
		
		File directory_v0 = new File( args[0] );
		repairDeclarationFilesInFolder( directory_v0 );
		

		
		
		
	}

	public static void repairDeclarationFilesInFolder(File directory_v1) throws FileNotFoundException, ClassNotFoundException, IOException {
		File monitoredFunctionsFileV1 = new File ( directory_v1.getAbsolutePath()+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions.original.ser" );
		Map<String, FunctionMonitoringData> functionsDataV1 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV1);		
		
		File kvasirList = new File ( directory_v1.getAbsolutePath()+"/BCT/BCT_DATA/BCT/conf/files/scripts/kvasir.list.txt" );
		
		
		List<String> monitored = new ArrayList<>();
		for( FunctionMonitoringData fd : functionsDataV1.values() ){
			if ( fd.isCallerOfTargetFunction() || fd.isTargetFunction() ){
				String key = ".."+fd.getMangledName()+"(";
				boolean first = true;
				for ( Parameter p : fd.getAllArgs() ){
					if ( first ){
						first=false;
					} else {
						key+=",";
					}
					key += p.getType();
				}
				key+=")";
				monitored.add(key);
			}
		}
		
		FileUtil.writeToTextFile(monitored, kvasirList);
	}


}
