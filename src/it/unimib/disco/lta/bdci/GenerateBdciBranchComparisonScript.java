package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManagerException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import console.GenerateBranchComparisonScripts;
import console.ScriptsGenerator;
import edu.emory.mathcs.backport.java.util.Arrays;

public class GenerateBdciBranchComparisonScript extends ScriptsGenerator {

	public GenerateBdciBranchComparisonScript(List<String> modifiedFunctions) {
		this.setTraceAllLinesOfMonitoredFunctions(false);
		this.setTraceAllLinesOfChildren(false);
		this.setCompleteMonitoring(false);
		this.setMonitorAllIfNoChange(false);
		this.setCustomOnly(true);
		this.setMonitorGlobalVariables(true);
		this.setMonitorCallersOfModifiedFunctions(false);
//		this.setMonitorFunctionsCalledByTargetFunctions(false);
		
		this.setCustomInclusionRules(modifiedFunctions);
		
		String dllString = System.getProperty("dll");
		if ( dllString != null ){
			boolean dll = Boolean.parseBoolean(dllString);
			this.setDll(dll);
		}
	}

	public static void main( String args[]) throws FileNotFoundException, CoreException, ConfigurationFilesManagerException{
		
		
		ArrayList<String> modifiedFunctions = new ArrayList<>();
		for ( int i = 5; i < args.length; i++ ){
			modifiedFunctions.add(args[i]);
		}
		
		System.out.println("Functions to monitor:");
		for( String function : modifiedFunctions ){
			System.out.println(function);
		}
		

		
		ScriptsGenerator generator = new GenerateBdciBranchComparisonScript(modifiedFunctions);

		Object[] newArgs = Arrays.copyOfRange(args, 0, 5);
		args=(String[]) newArgs;
		ScriptsGenerator.execute(args, generator);
	}
}
