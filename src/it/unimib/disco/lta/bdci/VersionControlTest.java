package it.unimib.disco.lta.bdci;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.FileUtil;

import cpp.gdb.FunctionMonitoringData;
import cpp.gdb.FunctionMonitoringDataSerializer;

public class VersionControlTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testContainsPointerOperation() {
		
		ArrayList<String> pointerParameters = new ArrayList<>();
		pointerParameters.add("prefix");
		pointerParameters.add("argc");
		
		{ 
			boolean res = VersionControl.containsPointerOperation("(or (= prefix 0) (= prefix 34664))", pointerParameters);
			assertTrue(res);
		}
		
		{ 
			boolean res = VersionControl.containsPointerOperation("(not ( = prefix 0))", pointerParameters);
			assertFalse(res);
		}
		
		{ 
			boolean res = VersionControl.containsPointerOperation("(= prefix 0)", pointerParameters);
			assertFalse(res);
		}
		
		{ 
			boolean res = VersionControl.containsPointerOperation("(>= prefix 0)", pointerParameters);
			assertFalse(res);
		}
		
		if ( VersionControl.POINTERS_COMPARISON_ENABLED )
		{ 
			boolean res = VersionControl.containsPointerOperation("(> prefix argc)", pointerParameters);
			assertFalse(res);
		} else {
			boolean res = VersionControl.containsPointerOperation("(> prefix argc)", pointerParameters);
			assertTrue(res);			
		}
		
//		{ 
//			boolean res = VersionControl.containsPointerOperation("(> prefix 93)", pointerParameters);
//			assertFalse(res);
//		}
		
	}
	
	@Test
	public void testInvariantsInDaikonOutput() throws FileNotFoundException, ClassNotFoundException, IOException{
		String daikonOutputFile = "test/data/edit_patch.cache.smt.txt";
		String monitoredFunctionsFile = "test/data/edit_patch.cache.monitoredFunctions.original.ser";
		
		
		Map<String, FunctionMonitoringData> functionData = FunctionMonitoringDataSerializer.load(new File( monitoredFunctionsFile ) );
		FunctionMonitoringData function = functionData.get("edit_patch");
		
		String output = FileUtil.getContent( new File(daikonOutputFile) );
		
		VersionControl.keyword = new NotToBeConsidered();
		String invariants = VersionControl.extractInvariantsInDaikonOutput("EXIT", function, output);
		
		
		System.out.println(invariants);
	}
	
	@Test
	public void testProcessHasNullEquality(){
		assertEquals( "(= prefix 0)", VersionControl.processHashNullEquality("(= (hash prefix) null)") );
		assertEquals( "(not (= prefix 0))", VersionControl.processHashNullEquality("(not (= (hash prefix) null))") );
		assertEquals( "(= (hash prefix) 234245)", VersionControl.processHashNullEquality("(= (hash prefix) 234245)") );
	}

}
