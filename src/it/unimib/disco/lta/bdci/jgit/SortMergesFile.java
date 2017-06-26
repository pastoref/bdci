package it.unimib.disco.lta.bdci.jgit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

import util.FileUtil;

public class SortMergesFile {

	private File gitDir;
	private File mergesFile;
	public SortMergesFile(File mergesFile, File gitDir) {
		this.mergesFile = mergesFile;
		this.gitDir = gitDir;
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File mergesFile = new File(args[0]);
		File gitDir = new File(args[1]);
		
		
		SortMergesFile fm = new SortMergesFile(mergesFile,gitDir);
		fm.process();
		
		
	}
	
	SimpleDirectedGraph<RevCommit,DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
	HashSet<RevCommit> merges = new HashSet<>();
	private BufferedWriter mergesTriplets;
	private BufferedWriter intermediateMergesTriplets;
	private Repository repository;
	private HashMap<String, Integer> timings;

	private void process() throws IOException {
		
		
		populateTimingsMap();
		
		
		List<String> content = FileUtil.getLines(mergesFile);
		
		Comparator<? super String> c = new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				String k1 = o1.split(",")[3];
				String k2 = o2.split(",")[3];
				return timings.get(k2)-timings.get(k1);
			}
		};
		Collections.sort(content, c);
		
		File sorted = new File( mergesFile.getParentFile(), mergesFile.getName()+".sorted.txt" );
		FileUtil.writeToTextFile(content, sorted);
	}
	

	


	private void populateTimingsMap() throws IOException, MissingObjectException,
			IncorrectObjectTypeException, AmbiguousObjectException {
		FileRepositoryBuilder b = new FileRepositoryBuilder();
		b.setGitDir(gitDir);
		
		repository = b.build();
		
		
		System.out.println(repository.getBranch());
		
		RevWalk rw = new RevWalk(repository);
		rw.markStart(rw.parseCommit(repository.resolve("HEAD")));
		
		Iterator<RevCommit> it = rw.iterator();
		
		timings = new HashMap<String,Integer>();
		
		while ( it.hasNext() ){
			
			RevCommit commit = it.next();
			
			timings.put( commit.getName(), commit.getCommitTime() );
			
		}
	}



}
