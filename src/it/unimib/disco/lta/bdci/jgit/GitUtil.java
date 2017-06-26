package it.unimib.disco.lta.bdci.jgit;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitUtil {

	public static Repository createRepositoryInstance( File gitDir ) throws IOException{
		FileRepositoryBuilder b = new FileRepositoryBuilder();
		b.setGitDir(gitDir);
		
		return b.build();
	}
}
