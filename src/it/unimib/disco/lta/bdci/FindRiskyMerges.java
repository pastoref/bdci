package it.unimib.disco.lta.bdci;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.ProcessRunner;
import cpp.gdb.ModifiedFunctionsDetector;
import cpp.gdb.SourceFileUtil;

public class FindRiskyMerges {

	private static boolean showDetails;
	private static String file = System.getProperty("outputFile");
	
	private static String subFolder = System.getProperty("subfolder",null);
	
	public static class ShowDetails{
		public static void main(String[] args) {
			showDetails = true;
			FindRiskyMerges.main(args);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if ( subFolder != null ){
			System.out.println("!!!Considering only sources in the subfolder "+subFolder);
		}
		
		ArrayList<RiskResult> result = new ArrayList<RiskResult>();
		
		for ( String arg : args ){
			RiskResult rr = identifyRiskyFunctionsInProject(arg);
			rr.setName( arg );
			result.add(rr);
		}
		
		Collections.sort(result, new Comparator<RiskResult>(){

			@Override
			public int compare(RiskResult o1, RiskResult o2) {
				return Double.compare(o1.getRiskRate(), o2.getRiskRate());
			}
			
		});
		
	
		System.out.println("\n\n\nResults:\n");
		System.out.println("Name" +

				"\tChangesInBothBranches" +
				"\tFilesWithChanges" +
				"\tFunctionsWithChanges" +
				"\tFileswithChangesInAllTheVersions" +
				"\tFileswithFunctionsModifiedInAllTheVersions" +
				"\tFunctionsWithConflictingChanges" +
				"\tFunctionsWithComplexConflictingChanges" +
				"\tFunctionsWithComplexConflictingChangesOnSameVars" +
				"\tDefinitionsImpactingOnChangedUses" +
				"\tConflictFunctionsToFunctions" +
				"\tFilesWithChangesInAllToFiles" +
				"\tMaxFuncsWithChangesInAFile" +
				"\tMaxFuncsWithConflictingChangesInAFile" +
				"\tNamesOfVariablesDefinedAndUsedInConflictingChanges" +
				"\tNamesOfVariablesAppearingInConflictingChanges" 
				);
		
		
		PrintStream out = System.out;
		
		if ( file != null ){
			try {
				out = new PrintStream( new FileOutputStream(new File( file ) ) );
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		for ( RiskResult prj : result ){
			printResults(out, prj);
		}
		
		if ( file != null ){
			out.close();
		}
	}



	public static void printResults(PrintStream out, RiskResult prj) {
		
		String variablesAppearingInConflictingChanges = getVariablesAppearingInConflictingChanges( prj );
		
		String variablesDefinedAndUsedInConflictingChanges = getVariablesDefinedAndUsedInConflictingChanges( prj );
		
		out.println(prj.getName()+
				"\t"+prj.hasChangesInBothBranches()+
				"\t"+prj.getTotalModifiedFiles()+
				"\t"+prj.getTotalModifiedFunctions()+
				"\t"+prj.getTotalFilesModifiedInAllTheBranches()+
				"\t"+prj.getTotalFilesWithFunctionsModifiedInAllTheBranches()+
				
				"\t"+prj.getFunctionsWConflictingChanges()+
				"\t"+prj.getFunctionsWComplexConflictingChanges()+
				"\t"+prj.getFunctionsWComplexConflictingChangesOnSameVarSet().size()+
				"\t"+prj.getDefinitionsImpactingOnChangedUses().size()+
				
				"\t"+prj.getRiskRate()+
				"\t"+prj.getRiskRateForFile()+
				"\t"+prj.getMaxFunctionsWithChanges()+
				"\t"+prj.getMaxFunctionsWithConflictingChanges()+
				"\t"+variablesDefinedAndUsedInConflictingChanges +
				"\t"+variablesAppearingInConflictingChanges );
		
		if ( showDetails ){
			printDetails(out,prj);
		}
	}



	private static String getVariablesDefinedAndUsedInConflictingChanges(RiskResult prj) {
		StringBuffer sb = new StringBuffer();
		
		for ( String v : prj.definitionsImpactingOnChangedUses ){
			sb.append(v);
			sb.append(",");
		}
		
		return sb.toString();
	}



	private static String getVariablesAppearingInConflictingChanges(RiskResult prj) {
		StringBuffer sb = new StringBuffer();
		
		for ( String v : prj.variablesAppearingInConflictingChanges ){
			sb.append(v);
			sb.append(",");
		}
		
		return sb.toString();
	}



	public static void printDetails(PrintStream out, RiskResult prj) {
		{
			out.println("Modified functions:");
			HashSet<String> funcs = prj.modifiedFunctions;
			for ( String s : funcs ){
				out.println(s);
			}
		}

		out.println("\n\n\n\n");
		
		{
			out.println("Modified in all:");
			HashSet<String> funcs = prj.functionsModifiedInAll;
			for ( String s : funcs ){
				out.println(s);
			}
		}
		
		out.println("\n\n\n\n");
		
		{
			out.println("Complex changes in all:");
			HashSet<String> funcs = prj.functionsWComplexConflictingChangesSet;
			for ( String s : funcs ){
				out.println(s);
			}
		}
		
		
		out.println("\n\n\n\n");
		
		{
			out.println("Complex changes with shared variables:");
			HashSet<String> funcs = prj.functionsWComplexConflictingChangesOnSameVarSet;
			for ( String s : funcs ){
				out.println(s);
			}
		}

		out.println("\n\n\n\n");
		
		
		{
			out.println("Modified v0 v1:");
			HashSet<String> funcs = prj.modifiedFunctionsV0V1;
			for ( String s : funcs ){
				out.println(s);
			}
		}

		out.println("\n\n\n\n");
		
		{
			out.println("Modified v0 v2:");
			HashSet<String> funcs = prj.modifiedFunctionsV0V2;
			for ( String s : funcs ){
				out.println(s);
			}
		}

		out.println("\n\n\n\n");
		
		{
			out.println("Modified v1 v2:");
			HashSet<String> funcs = prj.modifiedFunctionsV1V2;
			for ( String s : funcs ){
				out.println(s);
			}
		}

		out.println("\n\n\n\n");
		
		{
			out.println("Files modified in all the versions:");
			HashSet<String> funcs = prj.filesModifiedInAllTheVersions;
			for ( String s : funcs ){
				out.println(s);
			}
		}
	}

	
	
	public static RiskResult identifyRiskyFunctionsInProject(String prjName) {
		File prj = new File(prjName);
		
		
		File V0 = new File( prj, "V0" );
		File V1 = new File( prj, "V1" );
		File V2 = new File( prj, "V2" );
		
		if ( subFolder != null ){
			V0 = new File( V0.getAbsolutePath()+"/"+subFolder );
			V1 = new File( V1.getAbsolutePath()+"/"+subFolder );
			V2 = new File( V2.getAbsolutePath()+"/"+subFolder );
		}
		
		ChangeInfo changeInfoV1V2 = extractChangeInformation(V1, V2);
		
		ChangeInfo changeInfoV0V2 = extractChangeInformation(V0, V2);
		
		ChangeInfo changeInfoV0V1 = extractChangeInformation(V0, V1);
		

		
		
	
		
		
		return new RiskResult(changeInfoV1V2, changeInfoV0V1, changeInfoV0V2);
	}



	public static ChangeInfo extractChangeInformation(File V1, File V2) {
		Set<String> commonSourceFiles = ModifiedFunctionsDetector.identifyCommonSourceFiles(V1, V2);

		
		ChangeInfo riskyFunctions = new ChangeInfo();
		for ( String file : commonSourceFiles ){
			
			File f1 = new File(V1.getAbsolutePath()+"/"+file );
			File f2 = new File(V2.getAbsolutePath()+"/"+file );

			riskyFunctions.addDataForNewFile( changesInSameFunction( f1, f2 ) );
		}
		
		return riskyFunctions;
	}

	private static ChangeInfo changesInSameFunction(File f1, File f2) {
		
		List<String> command = Arrays.asList(new String[]{"diff","-p",f1.getAbsolutePath(), f2.getAbsolutePath() });
		
		Appendable ob = new StringBuffer();
		Appendable eb = new StringBuffer();
		

		
		int exitCode;
		boolean modified = false;
		
		HashSet<String> modifiedFunctions = new HashSet<String>();
		
		HashMap<String,Set<String>> changes = new HashMap<String,Set<String>>();
		
		try {
			exitCode = ProcessRunner.run(command, ob, eb, 0);
			
			if ( exitCode != 0 ){
				
				BufferedReader r = new BufferedReader( new StringReader(ob.toString()) );
				
				boolean added = false;
				boolean removed = false;
				boolean modifiedWithin = false;
				
				
				String line;
				String currentFunction = null;
				Set<String> currentChanges = new HashSet();
				
				while ( ( line = r.readLine() ) != null ){
					if ( line.startsWith("***************") ){
						modified = true;
						
						line = line.substring(15).trim();
						
						if ( line.isEmpty() ){
							continue;
						}
						
						if ( currentFunction != null ){
							changes.put( currentFunction, currentChanges );
						}
						
						
						
						currentChanges = new HashSet<String>();
						currentFunction = line;
						modifiedFunctions.add(currentFunction);
						
						
						added = false;
						removed = false;
						
						
					} else {
						if ( line.startsWith("+") && noCommentNext( line ) ){
							added = true;
							currentChanges.add( line );
						}
						if ( line.startsWith("-") && !line.startsWith("---") ){
							removed = true;
							currentChanges.add( line );
						}
						
						if ( line.startsWith("!") ){
							modifiedWithin = true;
							currentChanges.add( line );
						}
						
						
					}
				}
				
				
				if ( currentFunction != null ){
					changes.put( currentFunction, currentChanges );
				}
				
				r.close();
				
				
			}
		
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		ChangeInfo rr = new ChangeInfo(modified, f1.getName(), modifiedFunctions, changes);

		return rr;
		
	}
	
	public static class ChangeInfo{
		private HashMap<String,Set<String>> modifiedFunctions = new HashMap<>();
		private HashMap<String,Set<String>> changes = new HashMap<>();
		private HashSet<String> modifiedFiles = new HashSet<>();
		

		public ChangeInfo(){
			
		}
	
		public void addDataForNewFile(ChangeInfo newData) {
			modifiedFunctions.putAll(newData.modifiedFunctions);
			modifiedFiles.addAll(newData.modifiedFiles);
			changes.putAll( newData.changes );
		}

		public ChangeInfo(boolean modified, String fileName, HashSet<String> modifiedFunctions, HashMap<String,Set<String>> changes) {
			if( modified ){
				modifiedFiles.add(fileName);
			}
			this.modifiedFunctions.put(fileName, modifiedFunctions);
			
			for ( Entry<String, Set<String>> e : changes.entrySet() ){
				String functionName = e.getKey();
				this.changes.put(fileName+"::"+functionName, e.getValue() );
			}
		}

		public boolean modifiedFile(String file) {
			return modifiedFiles.contains(file);
		}

		public Collection<? extends String> getChanges(String file, String function) {
			return changes.get(file+"::"+function);
		}
	}
	
	public static class RiskResult {

		private int functionsWConflictingChanges;
		private int functionsWComplexConflictingChanges;
		
		private int totalModifiedFunctions;

		
		private int totalModifiedFiles = 0;
		private int totalFilesWithConflictingChanges = 0;

		private int maxFunctionsWithConflictingChanges;
		private int maxFunctionsWithChanges;
	
		private double riskRateForFile;
		private double riskRate;
		private String name;
		private HashSet<String> functionsModifiedInAll = new HashSet<>();
		private HashSet<String> modifiedFunctions = new HashSet<>();
		private int totalFilesModifiedInAllTheBranches;
		private int totalFilesWithFunctionsModifiedInAllTheBranches;
		
		
		private HashSet<String> modifiedFunctionsV0V1 = new HashSet<>();
		private HashSet<String> modifiedFunctionsV0V2 = new HashSet<>();
		private HashSet<String> modifiedFunctionsV1V2 = new HashSet<>();
		
		private HashSet<String> filesModifiedInAllTheVersions = new HashSet<>();
		
		private HashSet<String> functionsWComplexConflictingChangesSet = new HashSet<>();
		private HashSet<String> functionsWComplexConflictingChangesOnSameVarSet = new HashSet<>();
		private HashSet<String> variablesAppearingInConflictingChanges = new HashSet<>();
		private HashSet<String> definitionsImpactingOnChangedUses = new HashSet<>();
		
		
		public HashSet<String> getDefinitionsImpactingOnChangedUses() {
			return definitionsImpactingOnChangedUses;
		}


		public HashSet<String> getVariablesAppearingInConflictingChanges() {
			return variablesAppearingInConflictingChanges;
		}


		public HashSet<String> getFunctionsWComplexConflictingChangesOnSameVarSet() {
			return functionsWComplexConflictingChangesOnSameVarSet;
		}


		public RiskResult( ChangeInfo v1v2, ChangeInfo v0v1, ChangeInfo v0v2 ) {
			
			for ( String file : v1v2.modifiedFunctions.keySet() ){
				Set<String> mfv1v2 = v1v2.modifiedFunctions.get(file);
				Set<String> mfv0v1 = v0v1.modifiedFunctions.get(file);
				Set<String> mfv0v2 = v0v2.modifiedFunctions.get(file);
				
				
				if ( (!v1v2.modifiedFile(file)) && (!v0v2.modifiedFile(file)) && (!v0v1.modifiedFile(file)) ){
					continue; //the file was not modified
				}
				
				if ( mfv1v2 == null ){
					mfv1v2 = new HashSet<>();
				}
				
				if ( mfv0v1 == null ){
					mfv0v1 = new HashSet<>();
				}
				
				if ( mfv0v2 == null ){
					mfv0v2 = new HashSet<>();
				}
				
				updateMaxFunctionsWithChanges(mfv1v2);
				updateMaxFunctionsWithChanges(mfv0v1);
				updateMaxFunctionsWithChanges(mfv0v2);
				
				HashSet<String> changes = new HashSet<>();
				changes.addAll(mfv1v2);
				changes.addAll(mfv0v1);
				changes.addAll(mfv0v2);
				
				totalModifiedFunctions += changes.size();
				
				HashSet<String> concurrentChanges = new HashSet<>();
				concurrentChanges.addAll(mfv1v2);
				concurrentChanges.retainAll(mfv0v1);
				concurrentChanges.retainAll(mfv0v2);
				
				if ( concurrentChanges.size() > maxFunctionsWithConflictingChanges ){
					maxFunctionsWithConflictingChanges = concurrentChanges.size();
				}
				
				functionsWConflictingChanges += concurrentChanges.size();
				
				totalModifiedFiles++;
				if ( concurrentChanges.size() > 0 ){
					totalFilesWithConflictingChanges++;
				}
				
				if ( v1v2.modifiedFile(file) && v0v2.modifiedFile(file) && v0v1.modifiedFile(file) ){
					filesModifiedInAllTheVersions.add( file );
					totalFilesModifiedInAllTheBranches++;
					
					boolean fileHasRealConflictingChanges = false;
					
					HashSet<String> fileChangeSetV1 = new HashSet<String>();
					HashSet<String> fileChangeSetV2 = new HashSet<String>();
					for ( String function : concurrentChanges ){
						fileChangeSetV1.addAll( v0v1.getChanges(file,function) );
						fileChangeSetV2.addAll( v0v2.getChanges(file,function) );
					}
					HashSet<String> fileChangeSetV1Copy = new HashSet<String>();
					fileChangeSetV1Copy.addAll(fileChangeSetV1);

					fileChangeSetV1.removeAll( fileChangeSetV2 );
					fileChangeSetV2.removeAll( fileChangeSetV1Copy );

					if ( fileChangeSetV1.size() > 0  && fileChangeSetV2.size() > 0 ){
						fileHasRealConflictingChanges = true;
					}


					
					if ( fileHasRealConflictingChanges ){

						for ( String function : concurrentChanges ){
							HashSet<String> changeSetV1 = new HashSet<String>();
							changeSetV1.addAll( v0v1.getChanges(file,function) );

							HashSet<String> changeSetV2 = new HashSet<String>();
							changeSetV2.addAll( v0v2.getChanges(file,function) );


							changeSetV1.removeAll(changeSetV2);
							changeSetV2.removeAll( v0v1.getChanges(file,function) );

							if ( changeSetV1.size() > 0 && changeSetV2.size() > 0 ){
								
								
								if ( intersectionEmpty(fileChangeSetV1,changeSetV1) ){
									continue;
								}
								
								if ( intersectionEmpty(fileChangeSetV2,changeSetV2) ){
									continue;
								}
								
								
								functionsWComplexConflictingChanges++;

								System.out.println("!!!Complex changes for "+file+"::"+function);

								System.out.println("!!!In V1");
								Set<String> variablesInChangesSetV1 = new HashSet<>();
								for ( String s : changeSetV1 ){
									System.out.println(s);
									variablesInChangesSetV1.addAll( SourceFileUtil.extractVariablesFromLine(s) );
								}
								
								System.out.println("!!!Assignments in  changes of V1");
								Set<String> variablesAssignedInChangesSetV1 = new HashSet<>();
								for ( String s : changeSetV1 ){
									System.out.println(s);
									variablesAssignedInChangesSetV1.addAll( SourceFileUtil.extractVariablesDefinedInLine(s) );
								}
								
								
								
								
								
								System.out.println("!!!In V2");
								Set<String> variablesInChangesSetV2 = new HashSet<>();
								for ( String s : changeSetV2 ){
									System.out.println(s);
									variablesInChangesSetV2.addAll( SourceFileUtil.extractVariablesFromLine(s) );
								}
								
								System.out.println("!!!Assignments in  changes of V2");
								Set<String> variablesAssignedInChangesSetV2 = new HashSet<>();
								for ( String s : changeSetV2 ){
									System.out.println(s);
									variablesAssignedInChangesSetV2.addAll( SourceFileUtil.extractVariablesDefinedInLine(s) );
								}
								
								String functionKey = file+"::"+function;
								functionsWComplexConflictingChangesSet.add( functionKey );
								
								variablesAssignedInChangesSetV1.retainAll(variablesInChangesSetV2);
								variablesAssignedInChangesSetV2.retainAll(variablesInChangesSetV1);
								
								
								variablesInChangesSetV1.retainAll(variablesInChangesSetV2);
								Set<String> variablesBelongingToBothChanges = variablesInChangesSetV1;
								if ( variablesBelongingToBothChanges.size() > 0 ){
									functionsWComplexConflictingChangesOnSameVarSet.add( functionKey );
									
									System.out.println("!!!Variables belonging to statements modified in both version");
									for ( String s : variablesBelongingToBothChanges ){
										System.out.println(s);
									}
									
									variablesAppearingInConflictingChanges.addAll( variablesBelongingToBothChanges );
								}
								
								
								definitionsImpactingOnChangedUses.addAll( variablesAssignedInChangesSetV1 );
								definitionsImpactingOnChangedUses.addAll( variablesAssignedInChangesSetV2 );
								
								System.out.println("!!!Variables defined in changes of V1 used in changes of V2");
								for ( String s : variablesAssignedInChangesSetV1 ){
									System.out.println(s);
								}
								
								System.out.println("!!!Variables defined in changes of V2 used in changes of V1");
								for ( String s : variablesAssignedInChangesSetV2 ){
									System.out.println(s);
								}
								
								
								variablesAssignedInChangesSetV1.retainAll(variablesInChangesSetV2);
								variablesAssignedInChangesSetV2.retainAll(variablesInChangesSetV1);
								
							}
						}

					}
					
					
				}
				
				if ( mfv1v2.size() > 0 && mfv0v1.size() > 0 && mfv0v2.size() > 0 ){
					totalFilesWithFunctionsModifiedInAllTheBranches++;
				}
				
				this.modifiedFunctions.addAll( changes );
				this.functionsModifiedInAll.addAll( concurrentChanges );
				
				
				this.modifiedFunctionsV0V1.addAll( mfv0v1 );
				this.modifiedFunctionsV0V2.addAll( mfv0v2 );
				this.modifiedFunctionsV1V2.addAll( mfv1v2 );
			}
			
			if ( totalModifiedFunctions == 0 ){
				riskRate = 0;
			} else {
				this.riskRate = (double)functionsWConflictingChanges / (double)totalModifiedFunctions;	
			}
			
			if ( totalFilesModifiedInAllTheBranches == 0 ){
				riskRateForFile = 0;
			} else {
				riskRateForFile = (double)totalFilesModifiedInAllTheBranches / (double)totalModifiedFiles;	
			}
			
			
			
		}


		public boolean intersectionEmpty(HashSet<String> fileChangeSetV1,
				HashSet<String> changeSetV1) {
			for ( String entry : changeSetV1 ){
				if ( fileChangeSetV1.contains(entry) ){
					return false;
				}
			}
			
			return true;
		}

		
		public boolean hasChangesInBothBranches() {
			return modifiedFunctionsV0V1.size() > 0 && 
					modifiedFunctionsV0V2.size() > 0 ;
		}


		public int getTotalFilesWithConflictingChanges() {
			return totalFilesWithConflictingChanges;
		}
		
		
		public int getMaxFunctionsWithChanges() {
			return maxFunctionsWithChanges;
		}

		public String getName() {
			return name;
		}

		public void setName(String arg) {
			this.name = arg;
		}

		public double getRiskRateForFile() {
			return riskRateForFile;
		}

		public void updateMaxFunctionsWithChanges(Set<String> mfv1v2) {
			if ( mfv1v2.size() > maxFunctionsWithChanges ){
				maxFunctionsWithChanges = mfv1v2.size();
			}
		}

		

		public int getFunctionsWConflictingChanges() {
			return functionsWConflictingChanges;
		}

		public int getTotalModifiedFunctions() {
			return totalModifiedFunctions;
		}

		public double getRiskRate() {
			return riskRate;
		}

		public int getTotalModifiedFiles() {
			return totalModifiedFiles;
		}

		public int getMaxFunctionsWithConflictingChanges() {
			return maxFunctionsWithConflictingChanges;
		}


		public int getTotalFilesModifiedInAllTheBranches() {
			return totalFilesModifiedInAllTheBranches;
		}


		public int getTotalFilesWithFunctionsModifiedInAllTheBranches() {
			return totalFilesWithFunctionsModifiedInAllTheBranches;
		}


		public int getFunctionsWComplexConflictingChanges() {
			return functionsWComplexConflictingChanges;
		}
		


	}

	private static boolean noCommentNext(String line) {
		line = line.substring(1).trim();
		
		if ( line.startsWith("*") ){
			return false;
		}
		
		if ( line.startsWith("/*") ){
			return false;
		}
		
		if ( line.startsWith("//") ){
			return false;
		}
		
		return true;
	}

}
