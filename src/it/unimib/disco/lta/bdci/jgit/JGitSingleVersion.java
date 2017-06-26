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

public class JGitSingleVersion {
	
	/**
	 * Clone repository from Github
	 * @param args 0=local path 1=name of the project 2=link for clone project 3=branch
	 * @throws IOException
	 * @throws InvalidRemoteException
	 * @throws TransportException
	 * @throws GitAPIException
	 */
	public static void main(String[] args) throws IOException, InvalidRemoteException, TransportException, GitAPIException {
	    
		String localPath = args[0];
		String programName = args[1];
		String remotePath = args[2];
		String branch = args[3];
	
	    Repository newRepo = new FileRepository(localPath + programName +".git");
	    newRepo.create();
	
	    System.out.println("Start cloning");
	    CloneCommand cc = Git.cloneRepository();
	    cc.setURI(remotePath).setDirectory(new File(localPath + programName));
	    cc.setBranch(branch);
	    cc.call();
	    System.out.println("End cloning");
	    
	}
    
}