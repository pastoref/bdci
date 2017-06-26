package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManager;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManagerException;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConsoleHelper;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.configuration.MonitoringConfiguration;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.serialization.MonitoringConfigurationDeserializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import console.ProjectSetup;
import console.ScriptsGenerator;

import edu.emory.mathcs.backport.java.util.Arrays;
import util.JavaRunner;

public class SetupBDCI {

	public static final String ANALYSIS_V2 = "AnalysisV2";
	public static final String ANALYSIS_V1 = "AnalysisV1";
	public static final String ANALYSIS_V0 = "AnalysisV0";

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ConfigurationFilesManagerException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ConfigurationFilesManagerException, ClassNotFoundException {
		
		String srcV0 = args[0];
		String execV0 = args[1];
		
		String srcV1 = args[2];
		String execV1 = args[3];
		
		String srcV2 = args[4];
		String execV2 = args[5];
		
		
		
		HashSet<String> functionsToMonitor = FunctionsToMonitorDetector.identifyModifiedInOneVersionAtLeast(srcV0, execV0, srcV1, execV1, srcV2, execV2);
		
		//HashSet<String> functionsToMonitor = FunctionsToMonitorDetector.identifyModifiedFunctions(srcV1, execV1, srcV2, execV2);
		 
		String[] argsV0 = new String[]{ANALYSIS_V0,srcV0,execV0,srcV1,execV1};
		String[] argsV1 = new String[]{ANALYSIS_V1,srcV1,execV1,srcV0,execV0};
		String[] argsV2 = new String[]{ANALYSIS_V2,srcV2,execV2,srcV0,execV0};
		
		prepareForMonitoring(functionsToMonitor, argsV0);
		
		prepareForMonitoring(functionsToMonitor, argsV1);
		
		prepareForMonitoring(functionsToMonitor, argsV2);
			
		
		System.out.println("To monitor version V0: ");
		printHelperCommands(ANALYSIS_V0);
		
		System.out.println("To monitor version V1: ");
		printHelperCommands(ANALYSIS_V1);
		
		System.out.println("To monitor version V2: ");
		printHelperCommands(ANALYSIS_V2);
		
	}

	private static void prepareForMonitoring(
			HashSet<String> functionsToMonitor, String[] argsV0)
			throws IOException {
		List<String> v0list = Arrays.asList(argsV0);
		ArrayList<String> result = new ArrayList<String>( v0list.size() + functionsToMonitor.size() );
		result.addAll(v0list);
		result.addAll(functionsToMonitor);
		
		System.out.println("Functions to monitor:");
		for( String function : result ){
			System.out.println(function);
		}
		
		JavaRunner.runMainInClass(GenerateBdciBranchComparisonScript.class, result, 0, true);
	}

	public static void printHelperCommands(String projectDir)
			throws FileNotFoundException, ConfigurationFilesManagerException {
		
		ProjectSetup projectVars = ProjectSetup.setupProject(projectDir);
		MonitoringConfiguration mrc = MonitoringConfigurationDeserializer.deserialize(projectVars.getMonitoringConfigurationFile());
		
		File validTrace = ConfigurationFilesManager.getOriginalSoftwareGdbMonitoringConfig(mrc);
		
		System.out.println(ConsoleHelper.createGdbMonitoringcommandHelp(validTrace));
	}

}
