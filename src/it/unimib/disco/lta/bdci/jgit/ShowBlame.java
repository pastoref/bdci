package it.unimib.disco.lta.bdci.jgit;

import java.io.ByteArrayInputStream; 
import java.io.ByteArrayOutputStream; 
import java.io.File;
import java.io.IOException; 
 
import org.apache.commons.io.IOUtils; 
import org.eclipse.jgit.api.BlameCommand; 
import org.eclipse.jgit.api.errors.GitAPIException; 
import org.eclipse.jgit.blame.BlameResult; 
import org.eclipse.jgit.lib.ObjectId; 
import org.eclipse.jgit.lib.ObjectLoader; 
import org.eclipse.jgit.lib.Repository; 
import org.eclipse.jgit.revwalk.RevCommit; 
import org.eclipse.jgit.revwalk.RevTree; 
import org.eclipse.jgit.revwalk.RevWalk; 
import org.eclipse.jgit.treewalk.TreeWalk; 
import org.eclipse.jgit.treewalk.filter.PathFilter; 
 
 
 
/**
 * Simple snippet which shows how to get a diff showing who 
 * changed which line in a file 
 *  
 * @author dominik.stadler at gmx.at 
 */ 
public class ShowBlame { 
 
    public static void main(String[] args) throws IOException, GitAPIException { 
        // prepare a new test-repository 
        
    	File gitDir = new File ( args[0] );
    	String filePath = args[1];
    	
		Repository repository = GitUtil.createRepositoryInstance(gitDir);
    	{ 
    		ObjectId commitID = repository.resolve("HEAD"); 
    		
            BlameCommand blamer = new BlameCommand(repository); 
            blamer.setStartCommit(commitID); 
            blamer.setFilePath( filePath ); 
            blamer.setFollowFileRenames(true);
            
            BlameResult blame = blamer.call(); 
     
            // read the number of lines from the commit to not look at changes in the working copy 
            int lines = countFiles(repository, commitID, filePath ); 
            for (int i = 0; i < lines; i++) { 
                RevCommit commit = blame.getSourceCommit(i); 
                ObjectId newCommitId = commit.getId();
                int blameLine = blame.getSourceLine(i);
               
                System.out.println("Line: " + i + ": " + blameLine + ":" + newCommitId ); 
                String newfilePath = blame.getSourcePath(blameLine);
                
                extractBugFixCommits(0,newfilePath, repository, newCommitId, blameLine);
            } 
     
            System.out.println("Displayed commits responsible for " + lines + " lines of README.md"); 
        } 
    }

	public static void extractBugFixCommits(
			int c,
			String filePath,
			Repository repository, 
			ObjectId newCommitId, 
			int blameLine)
			throws GitAPIException {
		
		BlameCommand bc = new BlameCommand(repository); 
		bc.setStartCommit(newCommitId); 
		bc.setFilePath( filePath ); 
		bc.setFollowFileRenames(true);
		BlameResult b = bc.call();
		
		RevCommit srcCommit = b.getSourceCommit(blameLine); 
		if ( srcCommit == null  ){
			return;
		}
		
		
		
		ObjectId commitId = srcCommit.getId();
		if ( commitId.equals(newCommitId) ){
			System.out.println("same");
			return;
		}
		
		int newBlameLine = b.getSourceLine(blameLine);
		filePath = b.getSourcePath(blameLine);
		
		for ( int i = 0; i < c; i++ ){
			System.out.print("\t");
		}
		System.out.println("Line: " + blameLine + ": " + newBlameLine + ":" + commitId); 
		
		extractBugFixCommits( c+1, filePath, repository, commitId, newBlameLine);
	} 
 
    private static int countFiles(Repository repository, ObjectId commitID, String name) throws IOException { 
        try (RevWalk revWalk = new RevWalk(repository)) { 
            RevCommit commit = revWalk.parseCommit(commitID); 
            RevTree tree = commit.getTree(); 
            System.out.println("Having tree: " + tree); 
     
            // now try to find a specific file 
            try (TreeWalk treeWalk = new TreeWalk(repository)) { 
                treeWalk.addTree(tree); 
                treeWalk.setRecursive(true); 
                treeWalk.setFilter(PathFilter.create(name)); 
                if (!treeWalk.next()) { 
                    throw new IllegalStateException("Did not find expected file "+name); 
                } 
         
                ObjectId objectId = treeWalk.getObjectId(0); 
                ObjectLoader loader = repository.open(objectId); 
         
                ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
                // and then one can the loader to read the file 
                loader.copyTo(stream); 
                 
                revWalk.dispose(); 
                 
                return IOUtils.readLines(new ByteArrayInputStream(stream.toByteArray())).size(); 
            } 
        } 
    } 
}
