package it.unimib.disco.lta.bdci;

import java.io.FileNotFoundException;

import console.ProjectSetup;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.bctIntegrationLayer.ConfigurationFilesManagerException;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.configuration.CRegressionConfiguration;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.configuration.MonitoringConfiguration;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.serialization.MonitoringConfigurationDeserializer;
import it.unimib.disco.lta.bct.bctjavaeclipse.core.serialization.MonitoringConfigurationSerializer;

public class SetBCTOption_FilterAllNonTerminating {

	public static void main(String[] args) throws FileNotFoundException, ConfigurationFilesManagerException {
		String[] projects = new String []{"AnalysisV0","AnalysisV1","AnalysisV2"};
		for ( String p : projects ){
			setFilterAllNonTerminatingFunctions(p);
		}
	}

	public static void setFilterAllNonTerminatingFunctions(String projectDir)
			throws FileNotFoundException, ConfigurationFilesManagerException {
		
		ProjectSetup projectVars = ProjectSetup.setupProject(projectDir);
		MonitoringConfiguration mrc = MonitoringConfigurationDeserializer.deserialize(projectVars.getMonitoringConfigurationFile());
		
		CRegressionConfiguration config = (CRegressionConfiguration) mrc.getAdditionalConfiguration(CRegressionConfiguration.class);
		
		config.setFilterAllNonTerminatingFunctions(true);
		
		MonitoringConfigurationSerializer.serialize(projectVars.getMonitoringConfigurationFile(),mrc);
		
		
	}
}
