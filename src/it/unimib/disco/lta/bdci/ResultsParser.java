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

public class ResultsParser {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		List<String> lines = FileUtil.getLines(new File(args[0]));
		Iterator<String> it = lines.iterator();
		while(  it.hasNext() ){
			String line = it.next();
			if ( line.startsWith("Model 1<->2") || line.startsWith("Model 1<->0") || line.startsWith("Model 2<->0") ){
				System.out.println(line);
				process( it.next() );
			} else {
				System.out.println(line);
			}
		}
	}

	private static void process(String next) {
		String[] splitted = next.split("<->");
		System.out.println("NEXT "+next);
		List<String> extracted1 = extract( splitted[0] );

		if ( splitted.length == 1 ){
			return;
		}
		List<String> extracted2 = extract( splitted[1] );
		int s1 = extracted1.size();
		int s2 = extracted2.size();

		int max = s1 > s2 ? s1 : s2;

		for ( int i = 0; i < max; i++){
			if ( i < s1 ){
				System.out.println(extracted1.get(i));
			} else {
				System.out.println("\t");
			}



			if ( i < s2 ){
				System.out.print(extracted2.get(i));
			} else {
				System.out.print("\t");
			}

			System.out.println("\n");
		}
	}

	private static List<String> extract(String string) {
		String[] l = string.split(" ");


		List<String> coll = new LinkedList();



		int start = 0;
		int end = 80; 
		int p = 0;
		while( string.length() > p ){
			if ( string.length() < end ){
				end = string.length();
			}
			
			//System.out.println(start +" "+end);
			String ll = string.substring(start, end);
			start = end;
			end += 80;

			coll.add(ll);
			p+=ll.length();
			
			//System.out.println(ll);
		}





		return coll;
	}

}
