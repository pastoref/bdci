package it.unimib.disco.lta.bdci;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Collections2;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.LinkedList;

import util.FileUtil;

public class HighOrderWarningsCounter {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		List<String> lines = FileUtil.getLines(new File(args[0]));
		Iterator<String> it = lines.iterator();
		
		String current = null;
		String status = null;
		while(  it.hasNext() ){
			String line = it.next();
			if ( line.startsWith("BDCI-") ){
				if ( current != null ){
					System.out.println(current+" : "+status);
				}
				status="COMPATIBLE";
				current=line;
			} else if ( line.startsWith("HIGH-ORDER") ){
				status="INCOMPATIBLE";
			} else {
				//System.out.println(line);
			}
		}
		
		if ( current != null ){
			System.out.println(current+" : "+status);
		}
	}


}
