package it.unimib.disco.lta.bdci.experiments.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import util.FileUtil;
import util.StringUtils;

public class ProcessTestLogAndRepairTest {

	
	
	public ProcessTestLogAndRepairTest(){
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		File testLog = new File( args[0] );
		File testFolder = new File( args[1] );
		
		List<String> lines = FileUtil.getLines(testLog);
		
		ProcessTestLogAndRepairTest t = new ProcessTestLogAndRepairTest();
		t.repairTest(testFolder, lines);
	}

	public void repairTest(File testFolder, List<String> testLogLines)
			throws IOException {
		String currentTest=null;
		
		HashMap<String, TreeSet<String>> failingTests = new HashMap<String,TreeSet<String>>();
		TreeSet<String> failed = new TreeSet<>();
		for ( String line : testLogLines ){
			if ( line.startsWith("***") ){
				
				saveFailed(currentTest, failingTests, failed);
					
				
				String[] elements = line.split(" ");
				currentTest = elements[1];
			} else if ( line.startsWith("* FAIL ") ){
				String[] els = line.split(" ");
				String tn = els[2];
				tn = tn.substring(0,tn.length()-1);
				failed.add( tn );
			}
		}
		
		saveFailed(currentTest, failingTests, failed);
		
		if ( failingTests.size() == 0 ){
			System.out.println("!!! No failing tests???");
			System.out.println("Log lines:");
			for ( String line : testLogLines ){
				System.out.println(line);
			}
			System.out.println("\n");
		}
		
		for( Entry<String, TreeSet<String>> fe : failingTests.entrySet() ){
			String testFile = fe.getKey();
			Set<String> tests = fe.getValue();
			
			System.out.println("To fix "+testFile+" "+tests);
			fixTest(testFolder,testFile,tests);
		}
	}

	boolean noOpenQuote = true;
	
	protected void fixTest(File testFolder, String testFileName, Set<String> tests) throws IOException {
		File testFile = new File( testFolder, testFileName );
		List<String> lines = FileUtil.getLines(testFile);
		List<String> disabled = new ArrayList<>();
		
		List<String> newLines = fixTest( testFolder, tests, lines, disabled );
		
		FileUtil.writeToTextFile(newLines, testFile);
		
		if ( disabled.size() > 0 ){
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File ( testFile.getAbsolutePath()+".disabled.txt") ));
			for ( String dt : disabled ){
				bw.append(dt);
				bw.newLine();
			}
			bw.close();
		}
	}
	
	public List<String> fixTest(File testFolder, Set<String> tests, List<String> lines, List<String> disabled ){
		int testCount = 0;
		
		
		
		boolean commentOut = false;
		List<String> newLines = new ArrayList<>();
		for ( String line : lines ){
			boolean added = false;
			
			
			
			if ( testLine ( line ) ){
				noOpenQuote=true;
				testCount++;
				
				if ( tests.contains(String.valueOf(testCount)) ){
					System.out.println("Fixing: "+line);
					commentOut=true;
					newLines.add( "###"+line);
					added=true;
					
					disabled.add(testCount+": "+line);
				} 
			} else {
				if ( testCommand(line) || ( line.isEmpty() && noOpenQuote ) ){
					commentOut=false;
				}
			}
			
			checkOpenQuote(line);
			
			if ( ! added ){
				String pre = "";
				if ( commentOut ){
					pre="####";
				}
				newLines.add(pre+line);
			}
		}
		
		return newLines;
	}

	private void checkOpenQuote( String line ){
		int occurrencies = StringUtils.countOccurrences(line,'\'');
		if ( occurrencies % 2 == 1 ){ //odd
			noOpenQuote = ! noOpenQuote;
		}
	}
	
	

	
	private static boolean testCommand(String line) {
		if ( line.startsWith("test_") )
			return true;
		
		return false;
	}

	private static boolean testLine(String line) {
		if ( line.startsWith("test_output") )
			return true;
		
		if ( line.startsWith("test_expected_output") )
			return true;
		
		if ( line.startsWith("test_expect_") )
			return true;
		
		return false;
	}

	private static void saveFailed(String currentTest,
			HashMap<String, TreeSet<String>> failingTests, TreeSet<String> failed) {
		if ( failed.size() > 0 ){
			failingTests.put(currentTest, new TreeSet<String>(failed));
			failed.clear();
		}
	}

}
