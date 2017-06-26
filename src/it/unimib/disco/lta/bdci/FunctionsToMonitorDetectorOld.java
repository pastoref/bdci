package it.unimib.disco.lta.bdci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import console.ProjectSetup;

import util.JavaRunner;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManager;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManagerException;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.configuration.MonitoringConfiguration;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.serialization.MonitoringConfigurationDeserializer;
import cpp.gdb.FunctionMonitoringData;
import cpp.gdb.FunctionMonitoringDataSerializer;
import edu.emory.mathcs.backport.java.util.Arrays;

public class FunctionsToMonitorDetectorOld {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ConfigurationFilesManagerException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, ConfigurationFilesManagerException {
		
		String srcV0 = args[0];
		String execV0 = args[1];
		
		String srcV1 = args[2];
		String execV1 = args[3];
		
		String srcV2 = args[4];
		String execV2 = args[5];
		
		String analysisV0 = SetupBDCI.ANALYSIS_V0+".change";
		String analysisV1 = SetupBDCI.ANALYSIS_V1+".change";
		String analysisV2 = SetupBDCI.ANALYSIS_V2+".change";
		String[] argsV0 = new String[]{analysisV0,srcV0,execV0,srcV1,execV1};
		String[] argsV1 = new String[]{analysisV1,srcV1,execV1,srcV0,execV0};
		String[] argsV2 = new String[]{analysisV2,srcV2,execV2,srcV0,execV0};
		
		JavaRunner.runMainInClass(console.GenerateModifiedFunctionsAndCallersScripts.class, Arrays.asList(argsV0), 0, true);
		
		JavaRunner.runMainInClass(console.GenerateModifiedFunctionsAndCallersScripts.class, Arrays.asList(argsV1), 0, true);
		
		JavaRunner.runMainInClass(console.GenerateModifiedFunctionsAndCallersScripts.class, Arrays.asList(argsV2), 0, true);
		
		Map<String, FunctionMonitoringData> funcsV0 = retrieveFuntionsToMonitor(analysisV0);
		Map<String, FunctionMonitoringData> funcsV1 = retrieveFuntionsToMonitor(analysisV1);
		Map<String, FunctionMonitoringData> funcsV2 = retrieveFuntionsToMonitor(analysisV2);
		
		HashSet<String> toMonitor = new HashSet<String>();
		toMonitor.addAll(funcsV0.keySet());
		toMonitor.addAll(funcsV1.keySet());
		toMonitor.addAll(funcsV2.keySet());
		
		System.out.println("Functions to monitor in the three versions:");
		for (String f : toMonitor ){
			System.out.print(f+";");
		}
		
		System.out.println();
	}

	private static Map<String, FunctionMonitoringData> retrieveFuntionsToMonitor(String analysisV0)
			throws FileNotFoundException, IOException, ClassNotFoundException,
			ConfigurationFilesManagerException {
		ProjectSetup.setupProject(analysisV0);
		File mcV0F = new File(analysisV0+"/BCT/BCT.bctmc");
		MonitoringConfiguration mcV0 = MonitoringConfigurationDeserializer.deserialize(mcV0F);
		Map<String, FunctionMonitoringData> funcsV0 = FunctionMonitoringDataSerializer.load(ConfigurationFilesManager.getMonitoredFunctionsDataFile(mcV0));
		
		return funcsV0;
	}

}
