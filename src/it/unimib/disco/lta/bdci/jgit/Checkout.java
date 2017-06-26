package it.unimib.disco.lta.bdci.jgit;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import util.FileUtil;
import util.ProcessRunner;

/**
 * Checkout the four versions of a repository indicated in a given csv file.
 * 
 * 
 */
public class Checkout {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		File merges = new File( args[0] );
		int lineNo = Integer.valueOf(args[1]);
		File src = new File( args[2] );
		
		File cleanUpScript = new File( args[3] );
		
		String mergeLine = FileUtil.getLines(merges).get(lineNo);
		
		String[] splitted = mergeLine.split(",");
		
		String V0 = splitted[0];
		String V1 = splitted[1];
		String V2 = splitted[2];
		String V3 = splitted[3];
		
		File workspace = new File( "BDCI_"+lineNo );
		File dirV0 = new File ( workspace, "V0" );
		File dirV1 = new File ( workspace, "V1" );
		File dirV2 = new File ( workspace, "V2" );
		File dirV3 = new File ( workspace, "V3" );
		
		workspace.mkdirs();
		
		int exit = 0;
		
		exit+=IdentifyNonConflictingMerges.checkoutGitVersion(src, V0, dirV0);
		
		exit+=IdentifyNonConflictingMerges.checkoutGitVersion(src, V1, dirV1);
		
		exit+=IdentifyNonConflictingMerges.checkoutGitVersion(src, V2, dirV2);
		
		exit+=		IdentifyNonConflictingMerges.checkoutGitVersion(src, V3, dirV3);
		
		
		if ( exit > 0 ){
			workspace.renameTo(new File("BDCI_"+lineNo +"_error"));
		}
		
		Process p = Runtime.getRuntime().exec("bash "+cleanUpScript.getAbsolutePath());
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		boolean changesV0V1 = false;
		boolean changesV0V2 = false;

		{
			List<String> lines = identifyFilesWithDiffs(dirV0, dirV1);
			ArrayList<String> diffFiles = new ArrayList<String>();
			ArrayList<String> v0Files = new ArrayList<String>();
			ArrayList<String> v1Files = new ArrayList<String>();
			extractDiffsV0V1(dirV0, dirV1, lines, diffFiles, v0Files, v1Files);
			
			if ( diffFiles.size() > 0 || v0Files.size() > 0 || v1Files.size() > 0 ){
				changesV0V1 = true;
				
				System.out.println("Changes V0 V1: ");
				for( String f : diffFiles ){
					System.out.println(f);
				}
				for( String f : v0Files ){
					System.out.println(f);
				}
				for( String f : v1Files ){
					System.out.println(f);
				}
			}
		}
		
		{
			List<String> lines = identifyFilesWithDiffs(dirV0, dirV2);
			ArrayList<String> diffFiles = new ArrayList<String>();
			ArrayList<String> v0Files = new ArrayList<String>();
			ArrayList<String> v2Files = new ArrayList<String>();
			extractDiffsV0V1(dirV0, dirV2, lines, diffFiles, v0Files, v2Files);
			
			if ( diffFiles.size() > 0 || v0Files.size() > 0 || v2Files.size() > 0 ){
				changesV0V2 = true;
			}
			
			System.out.println("Changes V0 V2: ");
			for( String f : diffFiles ){
				System.out.println(f);
			}
			for( String f : v0Files ){
				System.out.println(f);
			}
			for( String f : v2Files ){
				System.out.println(f);
			}
		}
		
		if ( changesV0V1==false && changesV0V2==false ){
			workspace.renameTo( new File(workspace.getName()+"_noChange") );
			System.exit(1);
		}
		
//		ArrayList<String> v2Files = new ArrayList<String>();
//		extractDiffsV0V1(dirV0, dirV2, lines, diffFiles, v0Files, v2Files);
	}

	public static List<String> identifyFilesWithDiffs(File dirV0, File dirV1)
			throws IOException {
		List<String> command = new ArrayList<>();
		command.add("diff");
		command.add("-q");
		command.add("-r");
		command.add(dirV0.getAbsolutePath());
		command.add(dirV1.getAbsolutePath());
		
		Appendable outputBuffer = new StringBuffer();
		Appendable errorBuffer = new StringBuffer();
		int maxExecutionTime = 0;
		
		ProcessRunner.run(command, outputBuffer, errorBuffer, maxExecutionTime);
		
		BufferedReader r = new BufferedReader(new StringReader(outputBuffer.toString()));
		
		List<String> lines = FileUtil.getLines(r);
		return lines;
	}

	private static void extractDiffsV0V1(File dirV0, File dirV1,
			List<String> lines, ArrayList<String> diffFiles,
			ArrayList<String> v0Files, ArrayList<String> v1Files) {
		for ( String line : lines ){
			if ( line.startsWith("Files") ){
				String[] spl = line.split(" ");
				String file = spl[1].trim();
				if ( file.endsWith(".c") || file.endsWith(".h") || file.endsWith(".cpp") ){
					diffFiles.add(file.substring(dirV0.getAbsolutePath().length()+1));
				}
				
			} else if ( line.startsWith("Only in "+dirV0.getAbsolutePath()) ){
				int idx = line.indexOf(':');
				String file = line.substring(idx).trim();
				
				if ( file.endsWith(".c") || file.endsWith(".h") || file.endsWith(".cpp") ){
					v0Files.add(file);
				}
				
				
			} else if ( line.startsWith("Only in "+dirV1.getAbsolutePath()) ){
				int idx = line.indexOf(':');
				String file = line.substring(idx).trim();
				
				if ( file.endsWith(".c") || file.endsWith(".h") || file.endsWith(".cpp") ){
					v1Files.add(file);
				}
			}
		}
	}

}
