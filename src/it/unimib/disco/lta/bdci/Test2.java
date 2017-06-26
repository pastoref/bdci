package it.unimib.disco.lta.bdci;

import java.util.regex.Pattern;

public class Test2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Pattern uninitVariable = Pattern.compile(" \\d\\d\\d\\d\\d\\d\\d\\)");
		String temp = " (= size 4218515)";
		System.out.println(uninitVariable.matcher(temp).find());
	}

}
