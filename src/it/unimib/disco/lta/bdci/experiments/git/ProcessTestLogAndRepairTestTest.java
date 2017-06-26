package it.unimib.disco.lta.bdci.experiments.git;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessTestLogAndRepairTestTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFixTestFileSetOfStringListOfStringListOfString() {
		List<String> lines = new ArrayList<String>();
		lines.add("test_expect_success \\");
		lines.add("    'DF vs DF/DF case setup.' \\");
		lines.add("    'rm -f .git/index &&");
		lines.add("     echo DF >DF &&");
		lines.add("     git-update-cache --add DF &&");
		lines.add("     treeDF=`git-write-tree` &&");
		lines.add("     echo treeDF $treeDF &&");
		lines.add("     git-ls-tree $treeDF &&");
		lines.add("     git-ls-files --stage >DF.out");
		lines.add("");
		lines.add("     rm -f DF &&");
		lines.add("     mkdir DF &&");
		lines.add("     echo DF/DF >DF/DF &&");
		lines.add("     git-update-cache --add --remove DF DF/DF &&");
		lines.add("     treeDFDF=`git-write-tree` &&");
		lines.add("     echo treeDFDF $treeDFDF &&");
		lines.add("     git-ls-tree $treeDFDF &&");
		lines.add("     git-ls-files --stage >DFDF.out'");
		lines.add("");
		lines.add("     NO COMMENT");
		
		ProcessTestLogAndRepairTest p = new ProcessTestLogAndRepairTest();
		
		List<String> disabled = new ArrayList<>();
		Set<String> tests = new HashSet<>();
		tests.add("1");
		File testFolder = null;
		List<String> fixed = p.fixTest(testFolder, tests, lines, disabled);

		
		System.out.println(fixed);
	}

}
