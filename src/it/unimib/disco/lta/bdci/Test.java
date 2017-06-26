package it.unimib.disco.lta.bdci;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Pattern returnPattern = Pattern.compile("\\( eax returnValue\\) \\d\\d\\d\\d\\d\\d\\d\\)");
		Matcher m = returnPattern.matcher("(= ( eax returnValue) 638489)");
		System.out.println(m.find());
	}

}
