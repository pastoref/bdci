package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipsecpp.core.util.CDTStandaloneCFileAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.FileUtil;

import cpp.gdb.CSourceAnalyzerRegistry;
import cpp.gdb.FunctionMonitoringData;
import cpp.gdb.FunctionMonitoringDataSerializer;
import cpp.gdb.Parameter;


public class RepairDeclarationFiles {

	

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
		
		RepairDeclarationFiles r = new RepairDeclarationFiles();
		r.repairDeclarationFilesInFolder( directory_v0 );
		
	}
	
	private Map<String,Set<String>> variablesWithUndefinedType = new HashMap<>();
	
	
	public void identifyVariablesWithWronglyDeclaredType(File directory_v1) throws FileNotFoundException, ClassNotFoundException, IOException {
		
		
		
		Map<String, FunctionMonitoringData> functionsDataV1 = retrieveFunctionsDataInAnalysisFolder(directory_v1);		
		
		File[] decls = directory_v1.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".decls");
			}
		});
		
		for ( File declFile : decls ){
			System.out.println("File: "+declFile.getName()+" "+directory_v1.getAbsolutePath());
			identifyVariablesWithWronglyDeclaredType(declFile, functionsDataV1);
		}
	}
	
	public void identifyVariablesWithWronglyDeclaredType(File declFile,Map<String, FunctionMonitoringData> functionsData)  {
		
		Set<String> vars = new HashSet<String>();
		
		List<String> lines;
		try {
			lines = FileUtil.getLines(declFile);
		
			System.out.println("Identifying wrong types: "+declFile.getAbsolutePath());
			
			String fname = declFile.getName();
			fname = fname.substring(0, fname.length()-6);


			FunctionMonitoringData function = functionsData.get(fname);
			if ( function == null ){
				function = VersionControl.findFunctionInPresenceOfTrimmedName(fname, functionsData);
			}

			if ( function == null ){
				System.out.println("Function not found: "+fname);
				return;
			}

			HashMap<String, Parameter> args = new HashMap<String,Parameter>();
			for ( Parameter arg : function.getAllArgs() ){
				args.put(arg.getName(), arg);
			}

			Iterator<String> it = lines.iterator();
			
			it.next();//DECLARE
			it.next();//ppname
			
			while ( it.hasNext() ){
				String line = it.next();

				if ( line.isEmpty() ){
					break;
				}
				String vname = line;



				String vd1 = it.next();
				String vd2 = it.next();
				String vg = it.next();

				if ( ! vname.equals("BCT_return") ){
					Parameter p = args.get(vname);
					if ( p == null ){
						System.out.println("Missing parameter: "+vname);
						vars.add(vname);
					}
				}


			}

			variablesWithUndefinedType.put(fname, vars);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void repairDeclarationFilesInFolder(File directory_v1) throws FileNotFoundException, ClassNotFoundException, IOException {
		
		
		
		Map<String, FunctionMonitoringData> functionsDataV1 = retrieveFunctionsDataInAnalysisFolder(directory_v1);		
		
		File[] decls = directory_v1.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".decls");
			}
		});
		
		
		for ( File declFile : decls ){
			System.out.println("File: "+declFile.getName()+" "+directory_v1.getAbsolutePath());
			try {
			boolean repairDTrace = repairDeclarationFile( declFile, functionsDataV1 );
			
			if( repairDTrace ){
				File dtraceFile = new File( declFile.getAbsolutePath().replace(".decls", ".dtrace") );
				try {
					repairDTraceFile( dtraceFile, functionsDataV1 );
				} catch ( Exception e ){
					System.err.println("Exception while processing "+dtraceFile.getAbsolutePath());
					throw e;
				}
			}
			
			} catch ( Exception e ){
				System.err.println("Exception while processing "+declFile.getAbsolutePath());
				throw e;
			}
		}
	}

	public static Map<String, FunctionMonitoringData> retrieveFunctionsDataInAnalysisFolder(
			File directory_v1) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		
		String kind = "original";
		
//		if ( directory_v1.getName().endsWith("V0")  ){
//			kind = "original";
//		} else {
//			kind = "modified";
//		}
		
			File monitoredFunctionsFileV1 = new File ( directory_v1.getAbsolutePath()+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions."+kind+".ser" );
		
			Map<String, FunctionMonitoringData> functionsDataV1 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV1);
		
		if ( functionsDataV1.size() == 0 ){
			kind = "modified";
			monitoredFunctionsFileV1 = new File ( directory_v1.getAbsolutePath()+"/BCT/BCT_DATA/BCT/conf/files/scripts/monitoredFunctions."+kind+".ser" );
			functionsDataV1 = FunctionMonitoringDataSerializer.load(monitoredFunctionsFileV1);			
		}
		
		return functionsDataV1;
	}

	private static void repairDTraceFile(File dtraceFile,
			Map<String, FunctionMonitoringData> functionsDataV1) throws IOException {
		
		BufferedReader br = new BufferedReader(new FileReader( dtraceFile ) );

		File dest = new File(dtraceFile.getAbsolutePath()+".tmp");
		BufferedWriter bw = new BufferedWriter(new FileWriter(dest));
		
		String line;
		while ( ( line = br.readLine() ) != null ){
			if ( line.contains(DAIKON_EXIT_KEY) ){
				line = line.replace(DAIKON_EXIT_KEY, DAIKON_NEW_EXIT_POINT);
			} else if ( line.contains(DAIKON_ENTER_KEY) ){
				line = line.replace(DAIKON_ENTER_KEY, DAIKON_NEW_ENTER_POINT);
			}
			
			
			bw.append(line);
			bw.newLine();
		}

		br.close();
		bw.close();
		
		
		
		
		boolean deleted = dtraceFile.delete();
		if ( ! deleted ){
			throw new IOException("Cannot delete "+dtraceFile.getAbsolutePath() );
		}
		
		boolean renamed = dest.renameTo(dtraceFile);
		if ( ! renamed ){
			throw new IOException("Cannot rename "+dtraceFile.getAbsolutePath()+" to "+ dest.getAbsolutePath());
		}
	}

	private boolean repairDeclarationFile(File declFile,
			Map<String, FunctionMonitoringData> functionsData) throws IOException {
		List<String> lines = FileUtil.getLines(declFile);
		
		System.out.println("Repairing "+declFile.getAbsolutePath());
		String fname = declFile.getName();
		fname = fname.substring(0, fname.length()-6);
		
		
		FunctionMonitoringData function = functionsData.get(fname);
		if ( function == null ){
			function = VersionControl.findFunctionInPresenceOfTrimmedName(fname, functionsData);
		}
		
		if ( function == null ){
			System.out.println("Function not found: "+fname);
			return false;
		}
		
		
		Set<String> undefinedVariables = variablesWithUndefinedType.get(fname);
		
		if ( undefinedVariables == null ){
			undefinedVariables = Collections.EMPTY_SET;
		}
		
		HashMap<String, Parameter> args = new HashMap<String,Parameter>();
		for ( Parameter arg : function.getAllArgs() ){
			args.put(arg.getName(), arg);
		}
		
		args.put("BCT_return", function.getReturnParameter() );
		
		
		
		
		int pointerCounter = 10;
		boolean repairDTrace = false;
		
		ArrayList<String> newContent = new ArrayList<String>();
		Iterator<String> it = lines.iterator();
		while ( it.hasNext() ){
			String line = it.next();
			
			if ( line.equals("DECLARE") ){
				
				newContent.add(line);
				
				line = it.next();
				
				if ( VersionControl.REPLACE_ENTER_EXIT_DECL ){
					if ( line.endsWith(DAIKON_EXIT_KEY) ){
						line = line.replace(DAIKON_EXIT_KEY, DAIKON_NEW_EXIT_POINT);
						repairDTrace = true;
					} else if ( line.endsWith(DAIKON_ENTER_KEY) ){
						line = line.replace(DAIKON_ENTER_KEY, DAIKON_NEW_ENTER_POINT);
						repairDTrace = true;
					}
				}
				
				newContent.add(line);
			} else {
				String vname = line;
				
				if ( vname.trim().isEmpty() ){
					newContent.add(vname);
					continue;
				}
				
				String vd1 = it.next();
				String vd2 = it.next();
				String vg = it.next();
				
				Parameter p = args.get(vname);
				if ( p == null || undefinedVariables.contains(vname) ){
					System.err.println("Function "+fname);
					System.err.println("Cannot find parameter "+vname);
					
					printParametersOfFunction(function, args);
					
					vd2 = "hashcode";
					vg = String.valueOf( pointerCounter++ );
				} else {
					if ( p.isPointer() ){
						vd2 = "hashcode";
						vg = String.valueOf( pointerCounter++ );
					}
				}
				
				newContent.add(vname);
				newContent.add(vd1);
				newContent.add(vd2);
				newContent.add(vg);
			}
		}
		
		FileUtil.writeToTextFile(newContent, declFile);
		
		return repairDTrace;
	}

	public static void printParametersOfFunction(
			FunctionMonitoringData function, HashMap<String, Parameter> args) {
		System.out.println("Parameters for "+function.getMangledName());
		for ( String par : args.keySet() ){
			System.out.print(par);
			System.out.print(", ");
		}
		System.out.println("\n");
	}

}
