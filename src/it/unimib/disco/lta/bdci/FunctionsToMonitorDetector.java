package it.unimib.disco.lta.bdci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import console.GenerateModifiedFunctionsAndCallersScripts;
import console.ProjectSetup;

import util.JavaRunner;
import util.componentsDeclaration.Component;
import util.componentsDeclaration.MatchingRule;
import util.componentsDeclaration.MatchingRuleExclude;
import util.componentsDeclaration.MatchingRuleInclude;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManager;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManagerException;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.configuration.MonitoringConfiguration;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.serialization.MonitoringConfigurationDeserializer;
import cpp.gdb.FunctionMonitoringData;
import cpp.gdb.FunctionMonitoringDataSerializer;
import edu.emory.mathcs.backport.java.util.Arrays;

public class FunctionsToMonitorDetector {

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
		
		HashSet<String> toMonitor = identifyModifiedInOneVersionAtLeast(srcV0,
				execV0, srcV1, execV1, srcV2, execV2);
		
		//HashSet<String> toMonitor = identifyModifiedFunctions(srcV1, execV1, srcV2, execV2)
		
		System.out.println("Differences between the three versions:");
		for (String f : toMonitor ){
			System.out.print(f+";");
		}
		
		toMonitor = identifyModifiedFunctions(srcV1, execV1, srcV2, execV2);
		
		System.out.println("Differences between V1 v2:");
		for (String f : toMonitor ){
			System.out.print(f+";");
		}
		
		System.out.println();
	}

	public static HashSet<String> identifyModifiedInOneVersionAtLeast(
			String srcV0, String execV0, String srcV1, String execV1,
			String srcV2, String execV2) throws IOException,
			FileNotFoundException, ClassNotFoundException,
			ConfigurationFilesManagerException {
		String analysisV0 = SetupBDCI.ANALYSIS_V0+".change";
		String analysisV1 = SetupBDCI.ANALYSIS_V1+".change";
		String analysisV2 = SetupBDCI.ANALYSIS_V2+".change";
		String[] argsV0 = new String[]{analysisV0,srcV0,execV0,srcV1,execV1};
		String[] argsV1 = new String[]{analysisV1,srcV1,execV1,srcV0,execV0};
		String[] argsV2 = new String[]{analysisV2,srcV2,execV2,srcV0,execV0};
		
//		try {
//			GenerateModifiedFunctionsAndCallersScripts.main(argsV0);
//		} catch (CoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		JavaRunner.runMainInClass(GenerateBdciChangesDetectionScript.class, Arrays.asList(argsV0), 0, true);
		
		JavaRunner.runMainInClass(GenerateBdciChangesDetectionScript.class, Arrays.asList(argsV1), 0, true);
		
		JavaRunner.runMainInClass(GenerateBdciChangesDetectionScript.class, Arrays.asList(argsV2), 0, true);
		
		Set<String> funcsV0 = retrieveFuntionsToMonitor(analysisV0);
		Set<String> funcsV1 = retrieveFuntionsToMonitor(analysisV1);
		Set<String> funcsV2 = retrieveFuntionsToMonitor(analysisV2);
		
		HashSet<String> toMonitor = new HashSet<String>();
		toMonitor.addAll(funcsV0);
		toMonitor.addAll(funcsV1);
		toMonitor.addAll(funcsV2);
		return toMonitor;
	}
	
	
	public static HashSet<String> identifyModifiedFunctions(
			String srcV1, String execV1,
			String srcV2, String execV2) throws IOException,
			FileNotFoundException, ClassNotFoundException,
			ConfigurationFilesManagerException {

		String analysisV1 = SetupBDCI.ANALYSIS_V1+"."+SetupBDCI.ANALYSIS_V2+".change";
		String[] argsV1 = new String[]{analysisV1,srcV1,execV1,srcV2,execV2};
		
		JavaRunner.runMainInClass(GenerateBdciChangesDetectionScript.class, Arrays.asList(argsV1), 0, true);
		
		Set<String> funcsV1 = retrieveFuntionsToMonitor(analysisV1);
		
		HashSet<String> toMonitor = new HashSet<String>();
		toMonitor.addAll(funcsV1);
		return toMonitor;
	}

	private static HashSet<String> retrieveFuntionsToMonitor(String analysisV0)
			throws FileNotFoundException, IOException, ClassNotFoundException,
			ConfigurationFilesManagerException {
		ProjectSetup.setupProject(analysisV0);
		File mcV0F = new File(analysisV0+"/BCT/BCT.bctmc");
		MonitoringConfiguration mcV0 = MonitoringConfigurationDeserializer.deserialize(mcV0F);
		
		HashSet<String> included = new HashSet<>();
		
		for ( Component c : mcV0.getComponentsConfiguration().getComponents() ){
			for ( MatchingRule r : c.getRules() ){
				if ( r instanceof MatchingRuleExclude ){
					throw new IllegalStateException("Unexpected Exclude Rule");
				}
				if ( r instanceof MatchingRuleInclude ){
					included.add( r.getMethodExpr() );
				}
			}
		}
		
		//Map<String, FunctionMonitoringData> funcsV0 = FunctionMonitoringDataSerializer.load(ConfigurationFilesManager.getMonitoredFunctionsDataFile(mcV0));
		
		return included;
	}

}
