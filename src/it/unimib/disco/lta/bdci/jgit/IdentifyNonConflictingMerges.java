package it.unimib.disco.lta.bdci.jgit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.tools.FileObject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import org.jgrapht.alg.NaiveLcaFinder;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import util.ProcessRunner;

public class IdentifyNonConflictingMerges {

	private static BufferedWriter bw;
	private static BufferedWriter bwc;
	private File gitDir;
	private File outDir;
	public IdentifyNonConflictingMerges(File outDir, File gitDir) {
		this.outDir = outDir;
		this.gitDir = gitDir;
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File outDir = new File(args[0]);
		File gitDir = new File(args[1]);
		
		bw = new BufferedWriter ( new FileWriter( new File(outDir,"noTextualConflicts_merges.csv") ));
		bwc = new BufferedWriter ( new FileWriter( new File(outDir,"textualConflicts_merges.csv") ));
		
		BufferedReader br = new BufferedReader(new FileReader(new File(outDir,"merges.csv")));
		int c=0;
		String line = null;
		while ( ( line = br.readLine() ) != null ){
			c++;
			
			System.out.println("Cycle "+c);
			
			String[] splitted = line.split(",");
			String v1Hash = splitted[1];
			String v2Hash = splitted[2];
			File v1 = new File( outDir, "V1" );
			
			checkoutGitVersion(gitDir, v1Hash, v1);
			
			Process p = Runtime.getRuntime().exec("git merge -m \"test\" "+v2Hash,null,v1);
			try {
				int res = p.waitFor();
				
				if ( res == 0 ){
					bw.append(line);
					bw.newLine();
					bw.flush();
					System.out.println("No textual conflict");
				} else {
					bwc.append(line);
					bwc.newLine();
					bwc.flush();
					System.out.println("Textual conflict");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			p = Runtime.getRuntime().exec("rm -rf "+v1.getAbsolutePath());
			try {
				int res = p.waitFor();
				
				if ( res == 0 ){
					System.out.println("Deleted");
				} else {
					System.out.println("Not Deleted");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

		}
		
		bw.close();
		bwc.close();
	}
	public static int checkoutGitVersion(File srcDir, String versionHash,
			File destFolder) throws IOException {
		
		
//		Process p = Runtime.getRuntime().exec("cp -r "+srcDir.getAbsolutePath()+" "+destFolder.getAbsolutePath());
//		try {
//			p.waitFor();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		{	
			List<String> command = Arrays.asList(new String[]{"cp","-r",srcDir.getAbsolutePath(),destFolder.getAbsolutePath()});

			Appendable ob = new StringBuffer();
			Appendable eb = new StringBuffer();

			int exitCode = ProcessRunner.run(command, ob, eb, 0);
			if ( exitCode != 0 ){

				System.out.println(ob.toString());
				System.out.println(eb.toString());

				return exitCode;
			}

		}
		
		List<String> command = Arrays.asList(new String[]{"git","checkout",versionHash});
		
		Appendable ob = new StringBuffer();
		Appendable eb = new StringBuffer();
		
		int exitCode = ProcessRunner.run(command, ob, eb, 0, destFolder);
		if ( exitCode != 0 ){
			
			System.out.println(ob.toString());
			System.out.println(eb.toString());
			
			return exitCode;
		}
		
		return 0;
	}
	
}
