package it.unimib.disco.lta.bdci.jgit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import util.FileUtil;

public class FindSmallDiffs {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		File fileNumberOfModifiedFunctions = new File( args[0] );
		
		List<String> lines = FileUtil.getLines(fileNumberOfModifiedFunctions);
		
		
		for ( int i=0, m=lines.size(); i < m; i++ ){
			Integer value = Integer.valueOf(lines.get(i));
			
			
			if ( value >= 10  && value < 30){
				System.out.println(i);
			}
		}
		
		
	}

}
