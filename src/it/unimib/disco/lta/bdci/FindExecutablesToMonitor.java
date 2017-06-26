package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.Logging;
import cpp.gdb.FileChangeInfo;
import cpp.gdb.ModifiedFunctionsDetector;
import cpp.gdb.ModifiedFunctionsDetectorObjDumpListener;
import cpp.gdb.ObjDumpParser;
import cpp.gdb.ObjDumper;

public class FindExecutablesToMonitor extends ModifiedFunctionsDetector {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		File v0 = new File( args[0] );
		File v1 = new File( args[1] );
		File v2 = new File( args[2] );
		
		ArrayList<String> exec = new ArrayList<>();
		
		for ( int i = 3; i < args.length; i++ ){
			exec.add(args[i]);
		}
		
		FindExecutablesToMonitor f = new FindExecutablesToMonitor(v0, v1);
		Set<String> executablesToMonitorV0V1 = f.identifyExecutablesWithChanges(exec);

		FindExecutablesToMonitor f2 = new FindExecutablesToMonitor(v0, v2);
		Set<String> executablesToMonitorV0V2 = f2.identifyExecutablesWithChanges(exec);
		
		Set<String> r = new HashSet<>();
		r.addAll(executablesToMonitorV0V1);
		r.addAll(executablesToMonitorV0V2);
		
		for( String s : r ){
			System.out.println(s);
		}
	}

	private File version1;
	private File version2;
	private List<FileChangeInfo> fileChangesV1V2;
	private List<FileChangeInfo> fileChangesV2V1;
	
	
	public FindExecutablesToMonitor( File version1, File version2 ){
		this.version1 = version1;
		this.version2 = version2;
	}
	
	
	public Set<String> identifyExecutablesWithChanges(List<String> executableNames){
		
		fileChangesV1V2 = extractDiffs(version1, version2);
		fileChangesV2V1 = extractDiffs(version2, version1);
		
		Set<String> result = new HashSet<>();
		
		for ( String executableName : executableNames ){

			boolean monitor = monitorExecutable(executableName);
			
			if ( monitor ){
				result.add(executableName);
			}
			
	
			
		}
		
		
		return result;
	}


	private boolean monitorExecutable(String executableName) {
		ObjDumper dumper = new ObjDumper();

		File dumpOriginal = new File( version1, executableName+".v0.dump" );
		File dumpModified = new File( version2, executableName+".v1.dump" );
		//dumper.


		
		File execOriginal = new File( version1, executableName );
		File execModified = new File( version2, executableName );

		
		if ( ! execOriginal.exists() || ! execModified.exists() ){
			return false;
		}
		
		
		try {
			dumper.dump(execOriginal, dumpOriginal);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			dumper.dump(execModified, dumpModified);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		boolean monitor = monitorExecutable(dumpOriginal, dumpModified);
		

		dumpOriginal.delete();
		dumpModified.delete();
		
		return monitor;
	}
	
	public boolean monitorExecutable(  File objdumpFile, File objdumpFileV2 ){
		boolean monitor = monitorExecutable(fileChangesV1V2, objdumpFile);
		
		if ( monitor ){
			return true;
		}
			
		
		monitor = monitorExecutable(fileChangesV1V2, objdumpFileV2);
		return monitor;
	}
	
	public boolean monitorExecutable( List<FileChangeInfo> fileChanges, File objdumpFile){
		
		ModifiedFilesDetectorObjDumpListener listener = new ModifiedFilesDetectorObjDumpListener(fileChanges);
		listener.setUseDemangledNames(false);
		
		ObjDumpParser parser = new ObjDumpParser();
		try {
			parser.parse(objdumpFile, listener);
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return listener.coverChanges();
	}

}
