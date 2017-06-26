package it.unimib.disco.lta.bdci;

import it.unimib.disco.lta.bdci.ProcessResultsLog.PrjData;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.FileUtil;

public class ProcessResultsLog {

	public static class PrjData{
		HashMap<String, HashSet<String>> v0 = new HashMap<String,HashSet<String>>();
		HashMap<String, HashSet<String>> v1 = new HashMap<String,HashSet<String>>();
		HashMap<String, HashSet<String>> v2 = new HashMap<String,HashSet<String>>();
		private String name;
		
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

		private void add(HashMap<String, HashSet<String>> v, String method,
				String property) {
			HashSet<String> set = v.get(method);
			if ( set == null ){
				set = new HashSet<>();
				v.put(method,set);
			}
			set.add(property);
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
			
			
			System.out.println(name+"\t"+allV0+"\t"+allV1+"\t"+allV2+"\t"+newV1+"\t"+newV2+"\t"+delV1+"\t"+delV2+"\t"+constV1+"\t"+constV2);
			
		}

		private int countNewProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<Object> cp1 = new HashSet<>();
			cp1.addAll(m1);
			cp1.removeAll(m0);
			
			return cp1.size();
		}
		
		private int countRemovedProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<Object> cp0 = new HashSet<>();
			cp0.addAll(m0);
			cp0.removeAll(m1);
			return cp0.size();
		}
		
		private int countConstantProperties(HashSet<String> m0, HashSet<String> m1) {
			HashSet<Object> cp0 = new HashSet<>();
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
	}
	
	HashMap<String,PrjData> prjs = new HashMap<String,PrjData>();
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
		File log = new File( args[0] );
		
		ProcessResultsLog m = new ProcessResultsLog();
		m.process(log);
		
		m.printAll();
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
			prj.printStats();
		}
	}

	private void process(File log) throws FileNotFoundException {
		List<String> lines = FileUtil.getLines(log);
		
		String currentPrj = null;
		String currentVersion = null;
		String currentMethod = null;
		
		for ( String line : lines ){
			if ( line.startsWith("INFO: Executing") && line.contains("Daikon")){
				System.out.println(line);
				String[] splitted = line.split(" ");
				String traceFile = splitted[9];
				System.out.println(traceFile);
				
				String[] names = traceFile.split("/");
				currentPrj = names[6];
				currentVersion = names[7];
				currentMethod = names[8];
				
				System.out.println(currentPrj +" "+currentVersion+" "+currentMethod);
			} else if ( line.startsWith("Including property:") ){
				String pr = line.substring(19).trim();
				
				addProperty( currentPrj, currentVersion, currentMethod, pr );
			}
		}
	}

	private void addProperty(String currentPrj, String currentVersion,
			String currentMethod, String pr) {
		System.out.println("Adding "+currentPrj +" "+currentVersion+" "+currentMethod+" "+pr);
		PrjData prj = getPrj(currentPrj);
		prj.add(currentVersion, currentMethod, pr);
	}

}
