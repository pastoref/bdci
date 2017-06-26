package it.unimib.disco.lta.bdci.jgit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

public class FindMerges {

	private File gitDir;
	private File outDir;
	public FindMerges(File outDir, File gitDir) {
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
		
		
		FindMerges fm = new FindMerges(outDir,gitDir);
		fm.process();
		
		
	}
	
	SimpleDirectedGraph<RevCommit,DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
	HashSet<RevCommit> merges = new HashSet<>();
	private BufferedWriter mergesTriplets;
	private BufferedWriter intermediateMergesTriplets;
	private Repository repository;
	private int countLimit = Integer.valueOf(System.getProperty("countLimit", "-1"));
	
	Set<RevCommit> processedParents = new HashSet<RevCommit>();
	private boolean oncePerParent = Boolean.getBoolean("uniqueParents");

	private void process() throws IOException {
		
		createFiles();
		
		populateGraph();
		
		identifyMergeTriplets();
		
		closeFiles();
	}
	
	private void closeFiles() throws IOException {
		mergesTriplets.close();
		intermediateMergesTriplets.close();
	}
	
	private void identifyMergeTriplets() throws MissingObjectException, IncorrectObjectTypeException, IOException {
		NaiveLcaFinder<RevCommit, DefaultEdge> lca = new NaiveLcaFinder<>(graph);
		
		for ( RevCommit merge : merges ){
			int pc = merge.getParentCount();
			
			List<Pair> pairs = identifyAllMergePairs(merge, pc);
			processEachPairAndCreateMergeTriplets(lca, pairs, merge);

			
		}
	}
	private void processEachPairAndCreateMergeTriplets(
			NaiveLcaFinder<RevCommit, DefaultEdge> lca, List<Pair> pairs, RevCommit merge) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		for ( Pair<RevCommit, RevCommit> pair : pairs ){

			RevCommit l = pair.getLeft();
			RevCommit r = pair.getRight();
			RevCommit commonParent = lca.findLca(l, r);
			
			if ( commonParent == null ){
				continue;
			}
			newMergeTriple( commonParent, l, r, merge );
			
			identifyPossibleIntermediateMerges( commonParent, l, r, merge );
		}
	}
	
	private void identifyPossibleIntermediateMerges(RevCommit commonParent,
			RevCommit l, RevCommit r, RevCommit merge) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		
		List<RevCommit> leftList = createListFilledUpToParent(l,commonParent);
		
		List<RevCommit> rightList = createListFilledUpToParent(r,commonParent);
		
		for ( RevCommit leftIntermediate : leftList ){
			for ( RevCommit rightIntermediate : rightList ){
				if ( l!=leftIntermediate || r!=rightIntermediate ){
					addMergeTriple(intermediateMergesTriplets, commonParent, leftIntermediate, rightIntermediate,merge);
				}
			}	
		}
	}
	
	private List<RevCommit> createListFilledUpToParent(RevCommit l, RevCommit commonParent)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		List<RevCommit> leftList = new ArrayList<>();
		
		RevWalk rw = new RevWalk(repository);
		rw.markStart(commonParent);
		
		Iterator<RevCommit> it = rw.iterator();
		
		RevCommit commit = l;
		leftList.add(commit);
		
		while ( it.hasNext() && ( ! l.equals(commit) ) ){	
			commit = it.next();
			leftList.add(commit);
		}
		
		return leftList;
	}
	
	
	
	private List<Pair> identifyAllMergePairs(RevCommit merge, int pc) {
		List<Pair> pairs = new ArrayList<Pair>(pc);
		for( int il=0; il<pc-1;il++){
			for( int ir=il+1; ir<pc;ir++){
				RevCommit l = merge.getParent(il);
				RevCommit r = merge.getParent(ir);
				
				if ( oncePerParent ){
					if ( processedParents.contains(l) || processedParents.contains(r) ){
						continue;
					}
					processedParents.add(r);
					processedParents.add(l);
				}
				
				Pair<RevCommit,RevCommit> pair = new ImmutablePair<RevCommit, RevCommit>(l,r);					
				pairs.add( pair );
			}	
		}
		return pairs;
	}
	private void createFiles() throws IOException {
		outDir.mkdirs();
		mergesTriplets = new BufferedWriter(new FileWriter(new File(outDir,"merges.csv"),false));
		intermediateMergesTriplets = new BufferedWriter(new FileWriter(new File(outDir,"intermediateMerges.csv")));
	}
	
	private void newMergeTriple(RevCommit commonParent, RevCommit l, RevCommit r, RevCommit merge) {
		BufferedWriter buffer = mergesTriplets;
		
		addMergeTriple(buffer, commonParent, l, r, merge);
	}
	private void addMergeTriple(BufferedWriter buffer, RevCommit commonParent,
			RevCommit l, RevCommit r, RevCommit merge) {
		try {
			buffer.append(
					commonParent.getName()+","
					+l.getName()+","
					+r.getName()+","
					+merge.getName()
					);
			buffer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void populateGraph() throws IOException, MissingObjectException,
			IncorrectObjectTypeException, AmbiguousObjectException {
		
		
		repository = GitUtil.createRepositoryInstance(gitDir);
		
		
		System.out.println(repository.getBranch());
		
		RevWalk rw = new RevWalk(repository);
		rw.markStart(rw.parseCommit(repository.resolve("HEAD")));
		
		Iterator<RevCommit> it = rw.iterator();
		
		
		int count=0;
		while ( it.hasNext() ){
			
			RevCommit commit = it.next();
			addCommit(commit);
			
			int pc = commit.getParentCount();
			if (  pc > 1 ){
				merges.add( commit );
			}
			
			for ( RevCommit parent : commit.getParents() ){
				addCommit(parent);
				graph.addEdge(parent,commit);
			}
			
			if ( countLimit > 0 ){
				if ( count > countLimit ){
					break;
				}
			}
			count++;
			
			System.out.println(commit.getCommitTime());
		}
	}

	HashSet<RevCommit> commits = new HashSet<>();
	private void addCommit(
			RevCommit commit) {
		if ( commits.contains(commit) ){
			return;
		}
		
		commits.add( commit );
		graph.addVertex(commit);
		
	}

}
