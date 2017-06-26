package it.unimib.disco.lta.bdci;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDemo {

	public static void main(String [] args){
		//String var="return % orig(r) == 0";
		//String regex="orig\\([a-zA-Z]\\)";
		//String regex="[a-zA-Z]+ % orig\\(([a-zA-Z_0-9])\\) == [a-zA-Z_0-9]";
		//String var="(>= ( items *product) 0)(= ( out_of_catalogue *product) 0)(not (product null))";
		//String regex="\\(not \\(([a-zA-Z_0-9]+) null\\)\\)";
		//String regex="\\*";
		//String regex="\\(([a-zA-Z_0-9]+) (null)\\)";
		//var=var.replaceAll("object", "Product");
		//System.out.println(var);
		//System.out.println(var.split("\\.").length);
		/*String var="6.5E45";
		if(var.contains("-")){*/
			String var="(not (*y 0))";
			String regex="\\(((\\*)*[a-zA-Z_0-9]+) \\(([a-zA-Z_0-9]+) ([0-9]+)\\)\\)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(var);
			//System.out.println(matcher.groupCount());
			if (matcher.find()){
			     String s=matcher.group();
			     System.out.println("trovato");
			     s=s.replaceAll(regex, "($1 (= $2 $3))");
			     System.out.println(s);
			}/*
		}
		else{
			String regex="([0-9]+).([0-9]+)E([0-9]+)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(var);
			System.out.println(matcher.groupCount());
			if (matcher.find()){
			      String s=matcher.group();
			      s=s.replaceAll(regex, "^ $1.$2 $3");
			      System.out.println(s);
			}
		}*/
	}
}
