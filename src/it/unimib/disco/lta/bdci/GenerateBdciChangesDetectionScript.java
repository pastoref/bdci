package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManagerException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import console.GenerateBranchComparisonScripts;
import console.ScriptsGenerator;
import edu.emory.mathcs.backport.java.util.Arrays;

public class GenerateBdciChangesDetectionScript extends ScriptsGenerator {

	public GenerateBdciChangesDetectionScript() {
		this.setTraceAllLinesOfMonitoredFunctions(false);
		this.setTraceAllLinesOfChildren(false);
		this.setCompleteMonitoring(false);
		this.setMonitorAllIfNoChange(false);
		this.setCustomOnly(false);
		this.setMonitorGlobalVariables(false);

		
		String dllString = System.getProperty("dll");
		if ( dllString != null ){
			boolean dll = Boolean.parseBoolean(dllString);
			this.setDll(dll);
		}
	}

	public static void main( String args[]) throws FileNotFoundException, CoreException, ConfigurationFilesManagerException{
		
		String[] argsGen = (String[]) Arrays.copyOfRange(args, 0, 5);
		
		ArrayList<String> modifiedFunctions = new ArrayList<>();
		for ( int i = 5; i < argsGen.length; i++ ){
			modifiedFunctions.add(args[i]);
		}
		ScriptsGenerator generator = new GenerateBdciChangesDetectionScript();
		
		ScriptsGenerator.execute(args, generator);
	}
}
