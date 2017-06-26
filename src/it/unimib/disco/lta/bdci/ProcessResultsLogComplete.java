package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bdci.ProcessResultsLogComplete.PrjData;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.FileUtil;

public class ProcessResultsLogComplete {

	public static class PrjData{
		HashMap<String, HashSet<String>> v0 = new HashMap<String,HashSet<String>>();
		HashMap<String, HashSet<String>> v1 = new HashMap<String,HashSet<String>>();
		HashMap<String, HashSet<String>> v2 = new HashMap<String,HashSet<String>>();
		private String name;
		private int changedPrePostV1;
		private int changedPrePostV2;
		private int unchangedPrePostV1;
		private int unchangedPrePostV2;
		private int highOrderWarnings;
		private int lowOrderWarnings;
		private int changedPrePostV1V2;
		private int unchangedPrePostV1V2;
		private int unknownPrePostV1V2;
		private int unknownPrePostV2;
		private int unknownPrePostV1;
		private long bdciExecutionTime;
		private long daikonExecutionTime;
		private int highOrderWarningsMissingCalls;
		
		public PrjData(String name){
			this.name = name;
		}
		
		public void add( String v, String method, String property ){
			if ( v.equals("AnalysisV0")){
				add( v0, method, property );
			} else if ( v.equals("AnalysisV1")){
				add( v1, method, property );
			} else if ( v.equals("AnalysisV2")){
				add( v2, method, property );
			} else {
				System.out.println("NOT ADDING");
			}
		}
		
		public void rem( String v, String method, String property ){
			if ( v.equals("AnalysisV0")){
				rem( v0, method, property );
			} else if ( v.equals("AnalysisV1")){
				rem( v1, method, property );
			} else if ( v.equals("AnalysisV2")){
				rem( v2, method, property );
			} else {
				System.out.println("NOT REMOVING");
			}
		}

		private void add(HashMap<String, HashSet<String>> v, String method,
				String property) {
			HashSet<String> set = v.get(method);
			if ( set == null ){
				set = new HashSet<>();
				v.put(method,set);
			}
			set.add(property);
		}
		
		private void rem(HashMap<String, HashSet<String>> v, String method,
				String property) {
			HashSet<String> set = v.get(method);
			
			set.remove(property);
		}
		
		private void print( HashMap<String, HashSet<String>> v ){
			for ( Entry<String, HashSet<String>> e : v.entrySet() ){
				System.out.println(e.getKey());
				
				for ( String t : e.getValue() ){
					System.out.print(t);
				}
				System.out.println();
			}
		}
		
		
		public void printAddedRemoved(){
			Set<String> methods = new HashSet<String>();
			methods.addAll( v0.keySet() );
			methods.addAll( v1.keySet() );
			methods.addAll( v2.keySet() );

			
			for ( String method : methods ){
				HashSet<String> m0 = v0.get(method);
				HashSet<String> m1 = v1.get(method);
				HashSet<String> m2 = v2.get(method);
				
				if ( m0 == null ){
					continue;
				}
				
				if ( m1 == null ){
					continue;
				}
				
				if ( m2 == null ){
					continue;
				}
				
				
				
				System.out.println("==== Changes in "+name+" method "+method);
				System.out.println("new in V1");
				printNewProperties(m0,m1);
				System.out.println("new in V2");
				printNewProperties(m0,m2);
				
				System.out.println("missing in V1");
				printRemovedProperties(m0,m1);
				System.out.println("missing in V2");
				printRemovedProperties(m0,m2);
								
			}
			
		}
		
		public void printStats(){
			Set<String> methods = new HashSet<String>();
			methods.addAll( v0.keySet() );
			methods.addAll( v1.keySet() );
			methods.addAll( v2.keySet() );
			
			int newV1=0;
			int newV2=0;
			
			int delV1=0;
			int delV2=0;
			
			int constV1=0;
			int constV2=0;
			
			int allV0=0;
			int allV1=0;
			int allV2=0;
			
			for ( String method : methods ){
				HashSet<String> m0 = v0.get(method);
				HashSet<String> m1 = v1.get(method);
				HashSet<String> m2 = v2.get(method);
				
				if ( m0 == null ){
					continue;
				}
				
				if ( m1 == null ){
					continue;
				}
				
				if ( m2 == null ){
					continue;
				}
				
				allV0+=m0.size();
				allV1+=m1.size();
				allV2+=m2.size();
				
				newV1 += countNewProperties(m0,m1);
				newV2 += countNewProperties(m0,m2);
				
				delV1 += countRemovedProperties(m0,m1);
				delV2 += countRemovedProperties(m0,m2);
				
				constV1 += countConstantProperties(m0,m1);
				constV2 += countConstantProperties(m0,m2);
				
			}
			
			int allP = (unchangedPrePostV1+unknownPrePostV1+changedPrePostV1);
			System.out.println(name+"\t"+allV0+"\t"+allV1+"\t"+allV2+"\t"+newV1+"\t"+newV2+"\t"+delV1+"\t"+delV2+"\t"+constV1+"\t"+constV2+
					"\t"+allP
					+"\t"+changedPrePostV1+"\t"+changedPrePostV2+"\t"
					
					+lowOrderWarnings+"\t"+highOrderWarnings+"\t"+highOrderWarningsMissingCalls+
					
					"\t"+bdciExecutionTime+"\t"+daikonExecutionTime+
					
					"\t"+unchangedPrePostV1V2+"\t"+changedPrePostV1V2+"\t"+
					unknownPrePostV1+"\t"+unknownPrePostV2+"\t"+unknownPrePostV1V2);
			
		}

		private int countNewProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<Object> cp1 = new HashSet<>();
			cp1.addAll(m1);
			cp1.removeAll(m0);
			
			return cp1.size();
		}
		
		private void printNewProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<String> cp1 = new HashSet<>();
			cp1.addAll(m1);
			cp1.removeAll(m0);
			
			for ( String p : cp1 ){
				System.out.println(p);
			}
		}
		
		private int countRemovedProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<Object> cp0 = new HashSet<>();
			cp0.addAll(m0);
			cp0.removeAll(m1);
			return cp0.size();
		}
		
		private void printRemovedProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<String> cp0 = new HashSet<>();
			cp0.addAll(m0);
			cp0.removeAll(m1);
			
			for ( String p : cp0 ){
				System.out.println(p);
			}
			
			
		}
		
		private int countConstantProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<String> cp0 = new HashSet<>();
			cp0.addAll(m0);
			cp0.retainAll(m1);
			return cp0.size();
		}

		public void printV0() {
			System.out.println("V0");
			print(v0);
		}
		
		public void printV1() {
			System.out.println("V1");
			print(v1);
		}
		
		public void printV2() {
			System.out.println("V2");
			print(v2);
		}

		public void addChangedPrePostconditionV1() {
			changedPrePostV1++;
		}
		
		public void addChangedPrePostconditionV2() {
			changedPrePostV2++;
		}
		
		public void addUnchangedPrePostconditionV1() {
			unchangedPrePostV1++;
		}
		
		public void addUnchangedPrePostconditionV2() {
			unchangedPrePostV2++;
		}

		public void addLowOrderWarning() {
			lowOrderWarnings++;
		}

		public void addHighOrderWarning() {
			highOrderWarnings++;
		}

		public void addChangedPrePostconditionV1V2() {
			changedPrePostV1V2++;
		}

		public void addUnchangedPrePostconditionV1V2() {
			unchangedPrePostV1V2++;
		}

		public void addUnknownPrePostconditionV1V2() {
			unknownPrePostV1V2++;
		}

		public void addUnknownPrePostconditionV1() {
			unknownPrePostV1++;
		}

		public void addUnknownPrePostconditionV2() {
			unknownPrePostV2++;
		}

		public void setExecutionTimeBDCI(long time) {
			bdciExecutionTime=time;
		}

		public void setExecutionTimeDaikon(long time) {
			daikonExecutionTime=time;
		}

		public void addHighOrderWarningMissingCall() {
			highOrderWarningsMissingCalls++;
		}
	}
	
	HashMap<String,PrjData> prjs = new HashMap<String,PrjData>();
	private String lastpr;
	private boolean removedProperty;
	public PrjData getPrj(String name ){
		PrjData p = prjs.get(name);
		if ( p == null ){
			p = new PrjData(name);
			prjs.put(name, p);
		}
		return p;
	}
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		ProcessResultsLogComplete m = new ProcessResultsLogComplete();
		
		for ( String arg : args ){
			File dir = new File(arg);
			File log = new File( dir, "analyze.log" );
			
			
			m.process(log);
			
			File logBdci = new File( dir, "bdci.log" );
			m.processPreAnalysis(logBdci);
		}
		m.printAll();	
		
	}

	private void processPreAnalysis(File log) throws FileNotFoundException {
		// TODO Auto-generated method stub
		List<String> lines = FileUtil.getLines(log);

		String currentPrj = null;
		String currentVersion = null;
		String currentMethod = null;

		String prjName = log.getParentFile().getName();

		for ( String line : lines ){
			if ( line.startsWith("Tempo Preanalisi:") ){
				String[] splitted = line.split(" ");
				Long time = Long.valueOf(splitted[2].trim());
				getPrj(prjName).setExecutionTimeDaikon(time);
			}

		}
	}

	private void printAll() {
		for ( Entry<String, PrjData>  e : prjs.entrySet() ){
			String name = e.getKey();
			PrjData prj = e.getValue();
			
			System.out.println(name);
			prj.printV0();
			prj.printV1();
			prj.printV2();
		}
		
		
		for ( Entry<String, PrjData>  e : prjs.entrySet() ){
			String name = e.getKey();
			PrjData prj = e.getValue();
			
			//System.out.println(name);
			prj.printAddedRemoved();
		}
		
		for ( Entry<String, PrjData>  e : prjs.entrySet() ){
			String name = e.getKey();
			PrjData prj = e.getValue();
			
			//System.out.println(name);
			prj.printStats();
		}
	}

	private void process(File log) throws FileNotFoundException {
		List<String> lines = FileUtil.getLines(log);
		
		String currentPrj = null;
		String currentVersion = null;
		String currentMethod = null;
		
		String prjName = log.getParentFile().getName();
		
		for ( String line : lines ){
			if ( line.startsWith("INFO: Executing") && line.contains("Daikon")){
				//System.out.println(line);
				String[] splitted = line.split(" ");
				String traceFile = splitted[9];
				//System.out.println(traceFile);
				
				String[] names = traceFile.split("/");
				currentPrj = names[6];
				currentVersion = names[7];
				currentMethod = names[8];
				
				//System.out.println(currentPrj +" "+currentVersion+" "+currentMethod);
//			} else if ( line.startsWith("Lost_property:") ){
//				String info = line.substring(14);
//				int vEnd = info.indexOf(':');
//				String version = info.substring(0, vEnd);
//				String funInfo = info.substring(vEnd+1);
//				int fEnd = funInfo.indexOf(':');
//				String function = funInfo.substring(0, fEnd);
//				String property = funInfo.substring(fEnd+1);
//				addProperty( currentPrj, version, function, property );
//			} else if ( line.startsWith("Including property:") ){
//				lastpr = line.substring(19).trim();
//				removedProperty=false;
//				addProperty( currentPrj, currentVersion, currentMethod, lastpr );
//			} else if ( line.startsWith("Excluding ") ){
//				if ( line.startsWith("Excluding property:") ){
//					
//				} else {
//					if ( removedProperty ){
//						throw new IllegalStateException("Two removal of properties: "+line);
//					}
//					removedProperty=true;
//					remProperty( currentPrj, currentVersion, currentMethod, lastpr );
//				}
			} else if ( line.startsWith("Included property:") ){
				lastpr = line.substring(18).trim();
				
				addProperty( currentPrj, currentVersion, currentMethod, lastpr );
			} else if ( line.contains("_ENTER ->") ){
				processVersionsZ3Comparison(prjName, line);
			} else if ( line.contains("_EXIT ->") ){
				processVersionsZ3Comparison(prjName, line);
			} else if ( line.startsWith("LOW-ORDER WARNING:") ){
				getPrj(prjName).addLowOrderWarning();
			} else if ( line.startsWith("HIGH-ORDER WARNING:") ){
				if ( line.contains("(MISSING CALL")){
					getPrj(prjName).addHighOrderWarningMissingCall();	
				} else {
				getPrj(prjName).addHighOrderWarning();
				}
			} else if ( line.startsWith("Tempo BDCI:") ){
				String[] splitted = line.split(" ");
				Long time = Long.valueOf(splitted[2].trim());
				getPrj(prjName).setExecutionTimeBDCI(time);
			} else if ( line.startsWith("Tempo Preanalisi:") ){
				String[] splitted = line.split(" ");
				Long time = Long.valueOf(splitted[2].trim());
				getPrj(prjName).setExecutionTimeDaikon(time);
			}
		}
	}

	private void processVersionsZ3Comparison(String prjName, String line) {
		String[] els = line.split(" ");
		String changedV1 = els[3];
		String changedV2 = els[5];
		String changedV1V2 = els[7];
		PrjData prj = getPrj(prjName);
		
		if ( changedV1.equals("unsat,") ){
			prj.addChangedPrePostconditionV1();
		} else if ( changedV1.equals("sat,") ){
			prj.addUnchangedPrePostconditionV1();
		} else {
			System.out.println("UNKNOWN : "+changedV1);
			prj.addUnknownPrePostconditionV1();
		}
		
		if ( changedV2.equals("unsat,") ){
			prj.addChangedPrePostconditionV2();
		} else if ( changedV2.equals("sat,") ){
			prj.addUnchangedPrePostconditionV2();
		} else {
			prj.addUnknownPrePostconditionV2();
		}
		
		if ( changedV1V2.equals("unsat") ){
			prj.addChangedPrePostconditionV1V2();
		} else if ( changedV1V2.equals("sat") ){
			prj.addUnchangedPrePostconditionV1V2();
		} else {
			prj.addUnknownPrePostconditionV1V2();
		}
	}

	private void addProperty(String currentPrj, String currentVersion,
			String currentMethod, String pr) {
		//System.out.println("Adding "+currentPrj +" "+currentVersion+" "+currentMethod+" "+pr);
		PrjData prj = getPrj(currentPrj);
		prj.add(currentVersion, currentMethod, pr);
	}
	
	private void remProperty(String currentPrj, String currentVersion,
			String currentMethod, String pr) {
		//System.out.println("Adding "+currentPrj +" "+currentVersion+" "+currentMethod+" "+pr);
		PrjData prj = getPrj(currentPrj);
		prj.rem(currentVersion, currentMethod, pr);
	}

}
