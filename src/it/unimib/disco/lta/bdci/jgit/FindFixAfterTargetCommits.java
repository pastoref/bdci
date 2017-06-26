package it.unimib.disco.lta.bdci.jgit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.tools.FileObject;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import org.jgrapht.alg.NaiveLcaFinder;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import util.FileUtil;

public class FindFixAfterTargetCommits {

	private File gitDir;
	private File outDir;
	private HashSet<String> targetCommits;
	public FindFixAfterTargetCommits(File outDir, File gitDir, HashSet<String> targetCommits) {
		this.outDir = outDir;
		this.gitDir = gitDir;
		this.targetCommits = targetCommits;
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File outDir = new File(args[0]);
		File gitDir = new File(args[1]);
		File commits = new File(args[2]);

		List<String> lines = FileUtil.getLines(commits);
		HashSet<String> targetCommits = new HashSet<String>();
		targetCommits.addAll(lines);

		FindFixAfterTargetCommits fm = new FindFixAfterTargetCommits(outDir,gitDir,targetCommits);
		fm.process();


	}

	SimpleDirectedGraph<RevCommit,DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
	HashSet<RevCommit> merges = new HashSet<>();
	//	private BufferedWriter mergesTriplets;
	//	private BufferedWriter intermediateMergesTriplets;
	private Repository repository;

	private void process() throws IOException {

		createFiles();

		populateGraph();


		closeFiles();
	}

	private void closeFiles() throws IOException {
		//		mergesTriplets.close();
		//		intermediateMergesTriplets.close();
	}



	private void createFiles() throws IOException {
		//		outDir.mkdirs();
		//		mergesTriplets = new BufferedWriter(new FileWriter(new File(outDir,"merges.csv"),false));
		//		intermediateMergesTriplets = new BufferedWriter(new FileWriter(new File(outDir,"intermediateMerges.csv")));
	}


	private void populateGraph() throws IOException, MissingObjectException,
	IncorrectObjectTypeException, AmbiguousObjectException {


		repository = GitUtil.createRepositoryInstance(gitDir);


		System.out.println(repository.getBranch());

		RevWalk rw = new RevWalk(repository);
		rw.markStart(rw.parseCommit(repository.resolve("HEAD")));

		Iterator<RevCommit> it = rw.iterator();


		int count = 0;

		while ( it.hasNext() ){

			RevCommit commit = it.next();
			addCommit(commit);

			if ( ! isBugFix( commit ) ){
				continue;
			}

			//System.out.println("Bug fix: "+commit.getName() );


			for ( RevCommit mergeCommit : commit.getParents() ){
				String mergeCommitName = mergeCommit.getName();
				if ( targetCommits.contains(mergeCommitName) ){

					HashMap<String, DiffEntry> changedInCommit = getChangedFiles(commit, mergeCommit);

					HashMap<String, EditList> diffEditsForCommit = retrieveAllDiffEdits(changedInCommit);

					HashSet<String> modifiedFiles = new HashSet<>();



					HashSet<String> changedInParent = new HashSet<>();
					for ( RevCommit merged : mergeCommit.getParents() ){

						HashMap<String, DiffEntry> changedInPParent = getChangedFiles(mergeCommit, merged);

						for ( String modifiedFile : changedInPParent.keySet() ){
							if ( changedInCommit.containsKey(modifiedFile) ){
								DiffEntry commitDiff = changedInCommit.get(modifiedFile);
								DiffEntry pparentDiff = changedInPParent.get(modifiedFile);

								EditList editInPParent = calculateDiffToParent(pparentDiff);

								EditList editInCommit = diffEditsForCommit.get(modifiedFile);

								if ( overlappingEdits(editInCommit, editInPParent, commitDiff, pparentDiff) ){
									modifiedFiles.add( modifiedFile );
									System.out.println("PPARENT: "+merged.getName());
								}
							}
						}

					}



					if ( modifiedFiles.size() > 0 ){
						count++;

						String editedFiles = "";
						for ( String f : modifiedFiles ){
							editedFiles += f+",";
						}

						System.out.println("Bug-fix: "+commit.getName()+" Merge-commit: "+mergeCommitName+" "+editedFiles);

						for ( RevCommit pparent : mergeCommit.getParents() ){

							RevCommit[] merged = pparent.getParents();
							if ( merged != null ){
								for ( RevCommit mergeing : merged ){
									HashMap<String, DiffEntry> changedInPParent = getChangedFiles(pparent, mergeing);

									for ( String modifiedFile : changedInPParent.keySet() ){
										if ( editedFiles.contains(modifiedFile) ){
											System.out.println("Possible merged file: "+modifiedFile+" "+mergeing.getName());
											break;
										}
									}
								}
							}


						}

					}
				}
			}


		}

		System.out.println(count);

	}
	private boolean overlappingEdits(EditList editInCommit, EditList editInPParent, DiffEntry diffCommit, DiffEntry diffPParent) throws IOException {

		Iterator<Edit> cIt = editInCommit.iterator();

		while ( cIt.hasNext() ){
			Edit cN = cIt.next();

			Iterator<Edit> pIt = editInPParent.iterator();
			while ( pIt.hasNext() ){
				Edit pN = pIt.next();



				if ( overlapping( cN.getBeginB(), cN.getEndB(), pN.getBeginA(), pN.getEndA() ) ){
					System.out.println( cN.getBeginB()+ " , " + cN.getEndB()+ " , " + pN.getBeginA()+ " , " + pN.getEndA());

					//					{
					//						ObjectReader reader = repository.newObjectReader();	 
					//						RawText oldText = readText(diffCommit.getOldId(), reader); 
					//
					//						String sC = oldText.getString(cN.getBeginB(), cN.getEndB(), false);
					//						System.out.println(sC);
					//					}
					//
					//					{
					//						ObjectReader reader = repository.newObjectReader();	 
					//						RawText oldText = readText(diffPParent.getNewId(), reader); 
					//
					//						String sC = oldText.getString(pN.getBeginA(), pN.getEndA(), false);
					//						System.out.println(sC);
					//					}

					return true;
				}
				//if ( cN.getBeginB() > pN.getBeginA())
			}
		}

		return false;
	}
	private boolean overlapping(int begin1, int end1, int begin2, int end2) {
		// TODO Auto-generated method stub
		return (begin1 <= end2) && (begin2 <= end1);
	}
	public HashMap<String, EditList> retrieveAllDiffEdits(HashMap<String, DiffEntry> changedInCommit)
			throws IOException {
		HashMap<String, EditList> diffEditsForCommit = new HashMap<String, EditList>();
		for ( Entry<String, DiffEntry> e : changedInCommit.entrySet() ){
			diffEditsForCommit.put(e.getKey(), calculateDiffToParent(e.getValue()) );
		}

		return diffEditsForCommit;
	}
	public HashMap<String, DiffEntry> getChangedFiles(RevCommit commit, RevCommit parent)
			throws IOException {
		HashMap<String,DiffEntry> ld = new HashMap<String,DiffEntry>();

		DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
		//		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);




		df.setRepository(repository);
		df.setDiffComparator(RawTextComparator.DEFAULT);
		df.setDetectRenames(true);
		List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
		for (DiffEntry diff : diffs) {
			if ( diff.getOldPath().endsWith(".c") || diff.getOldPath().endsWith(".h") ){

				if ( diff.getChangeType() == ChangeType.MODIFY ){



					ld.put( diff.getOldPath(), diff );


				}
			}
		}

		return ld;
	}

	private EditList calculateDiffToParent(DiffEntry diffEntry) throws IOException { 
		ObjectReader reader = repository.newObjectReader();	 

		RawText oldText = readText(diffEntry.getOldId(), reader); 
		RawText newText = readText(diffEntry.getNewId(), reader); 

		StoredConfig config = repository.getConfig(); 
		DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(config 
				.getEnum(ConfigConstants.CONFIG_DIFF_SECTION, null, 
						ConfigConstants.CONFIG_KEY_ALGORITHM, 
						SupportedAlgorithm.HISTOGRAM)); 

		EditList editList = diffAlgorithm.diff(RawTextComparator.DEFAULT, 
				oldText, newText); 

		return editList;
	} 

	private static RawText readText(AbbreviatedObjectId blobId, 
			ObjectReader reader) throws IOException { 
		ObjectLoader oldLoader = reader.open(blobId.toObjectId(), 
				Constants.OBJ_BLOB); 
		return new RawText(oldLoader.getCachedBytes()); 
	} 

	private boolean isBugFix(RevCommit commit) {
		String msg = commit.getFullMessage().toLowerCase();



		if ( msg.startsWith("merge") ){
			return false;
		}

		if ( msg.contains("fix") ){
			return true;
		}

		return false;
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
