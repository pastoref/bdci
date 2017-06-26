package it.unimib.disco.lta.bdci.jgit;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class JGitMultipleVersions {
	
	/**
	 * Clone repository from Github
	 * @param args 0=local path 1=name of the project 2=link for clone project 3=branch0 3=branch1 3=branch2
	 * @throws IOException
	 * @throws InvalidRemoteException
	 * @throws TransportException
	 * @throws GitAPIException
	 */
	public static void main(String[] args) throws IOException, InvalidRemoteException, TransportException, GitAPIException {
	    
		String localPath = args[0];
		String programName = args[1];
		String remotePath = args[2];
		String branch0 = args[3];
		String branch1 = args[4];
		String branch2 = args[5];
	
	    Repository newRepo = new FileRepository(localPath + programName +".git");
	    newRepo.create();
	
	    System.out.println("Start cloning");
	    CloneCommand cc = Git.cloneRepository();
	    cc.setURI(remotePath).setDirectory(new File(localPath + programName + "V0"));
	    cc.setBranch(branch0);
	    cc.call();
	    System.out.println("V0 completed");
	    
	    cc = Git.cloneRepository();
	    cc.setURI(remotePath).setDirectory(new File(localPath + programName + "V1"));
	    cc.setBranch(branch1);
	    cc.call();
	    System.out.println("V1 completed");
	    
	    cc = Git.cloneRepository();
	    cc.setURI(remotePath).setDirectory(new File(localPath + programName + "V2"));
	    cc.setBranch(branch2);
	    cc.call();
	    System.out.println("V2 completed");
	    
	    System.out.println("End cloning");
	    
	}
    
}
