package it.unimib.disco.lta.bdci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.JavaRunner;
import daikon.Daikon;

public class TestDaikon {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String directory="D:\\uni\\stage\\Esempi\\new\\Analysis_v2";
		String directory0="D:\\uni\\stage\\Esempi\\esempi_davide_2\\Analysis_v1\\BCT\\BCT_DATA\\BCT\\Preprocessing\\";
		List<String> daikonArgs = new ArrayList<String>();
		daikonArgs.add("--format");
		daikonArgs.add("smt");
		daikonArgs.add(directory0+"decls\\2.decls");
		daikonArgs.add(directory0+"dtrace\\2.dtrace");
		daikonArgs.add("--conf_limit");
		daikonArgs.add("0");
		//daikonArgs.add("--config");
		//daikonArgs.add("/mnt/old_ubuntu/home/fabrizio/Dui/BctSimpleJUnitExample_CommandLine/./BctAnalysis/BCT/BCT_DATA/BCT/conf/files/essentials.txt");
		Appendable out = new StringBuffer();
		try {
			JavaRunner.runMainInClass(Daikon.class, new ArrayList<String>(), daikonArgs, 0, new ArrayList<String>(), true, out, out );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(out);
		System.out.println(out);

	}

}
