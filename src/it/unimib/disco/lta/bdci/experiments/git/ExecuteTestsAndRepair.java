package it.unimib.disco.lta.bdci.experiments.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;

import util.FileUtil;
import util.ProcessRunner;

public class ExecuteTestsAndRepair {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		File testFolder = new File( args[0] );

		int maxRepairableTests = Integer.getInteger("bdci.maxTestRepair", 100);

		Appendable errorBuffer = new StringBuffer();
		Appendable outputBuffer = new StringBuffer();

		int counter=0;
		int exitCode = 0;
		do {
			
			System.out.println("Repair iteration #"+counter);
			
			List<String> command = Arrays.asList(new String[]{"make"});
			exitCode = ProcessRunner.run(command, outputBuffer, errorBuffer, 0, testFolder);

			System.out.println("Make output:");
			System.out.println(outputBuffer.toString());
			System.out.println("Make error output:");
			System.out.println(errorBuffer.toString());
			
			ProcessTestLogAndRepairTest t = new ProcessTestLogAndRepairTest();
			
			BufferedReader br = new BufferedReader(new StringReader(outputBuffer.toString()));
			List<String> lines = FileUtil.getLines(br);
			FileUtil.writeToTextFile(lines, new File(testFolder,"test."+counter+".log") );
			
			t.repairTest(testFolder, lines);
			
			counter++;
			
			if ( counter > maxRepairableTests ){
				throw new IllegalStateException("Max number of repaired test reached: "+maxRepairableTests);
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while ( exitCode != 0 );
	}
}
