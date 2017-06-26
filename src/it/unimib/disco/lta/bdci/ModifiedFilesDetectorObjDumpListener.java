package it.unimib.disco.lta.bdci;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import cpp.gdb.FileChangeInfo;
import cpp.gdb.ModifiedFunctionsAnalysisResult;
import cpp.gdb.ObjDumpParserListener;

import util.componentsDeclaration.Component;
import util.componentsDeclaration.MatchingRule;
import util.componentsDeclaration.MatchingRuleInclude;

public class ModifiedFilesDetectorObjDumpListener implements
		ObjDumpParserListener {

	public static final String DELETED_FUNCTIONS_COMPONENT_NAME = "DeletedFunctions";
	public static final String ADDED_FUNCTIONS_COMPONENT_NAME = "AddedFunctions";
	public static final String MODIFIED_FUNCTIONS_COMPONENT_NAME = "ModifiedFunctions";
	



	private String curFunc;
	private HashMap<File, FileChangeInfo> fileChangesMap = new HashMap<File,FileChangeInfo>();
	private HashSet<String> modifiedFunctions = new HashSet<String>();
	private HashSet<String> allFunctions = new HashSet<String>();


	
	private boolean useDemangledNames = false;
	public boolean isUseDemangledNames() {
		return useDemangledNames;
	}

	public HashSet<String> getAllFunctions() {
		return allFunctions;
	}
	
	public void setUseDemangledNames(boolean useDemangledNames) {
		this.useDemangledNames = useDemangledNames;
	}

	private int lastPositionInCurrentFunction = -1;
	private int begin;
	private int end;
	private String curFuncLocation;
	private HashSet<String> addedFunctions;
	private HashSet<String> deletedFunctions;
	private boolean coverChanges;
	



	public void setDeletedFunctions(HashSet<String> deletedFunctions) {
		this.deletedFunctions = deletedFunctions;
	}


	public ModifiedFilesDetectorObjDumpListener(
			List<FileChangeInfo> fileChanges) {
		for ( FileChangeInfo fileChange : fileChanges ){
			fileChangesMap.put(fileChange.getFile(),fileChange);
		}
	}

	@Override
	public void callInstruction(String address, String calleeName) {

	}

	@Override
	public void newFunction(String curFunc, String address) {
		
	}

	

	@Override
	public void newSourceLocation(String string, int position) {
		curFuncLocation=string;
		if ( begin == -1 || position < begin ){
			begin=position;
		}
		if ( end == -1 || position > end ){
			end = position;
		}
		
		// TODO Auto-generated method stub
		File srcFile;
		try {
			srcFile = new File( new File(string).getCanonicalPath() );
		} catch (IOException e) {
			return;
		}
		FileChangeInfo changeInfo = fileChangesMap.get( srcFile );
		if ( changeInfo == null ){
			return;
		}
		coverChanges=true;
		throw new ChangeFound();
	}


	@Override
	public void returnInstruction(String address) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newFunctionName(String substring) {
//		curFuncName = substring;
	}

	@Override
	public void instruction(String address, String string) {
		
	}

	@Override
	public void objdumpEnd() {
		
	}



	@Override
	public void leaveInstruction(String address) {
		
	}







	public void setAddedFunctions(HashSet<String> addedFunctions) {
		this.addedFunctions = addedFunctions;
	}

	@Override
	public void jmpInstruction(String address, String jmpToAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newLine(String line) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void popEbp(String address) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fldlInstruction(String address, String jmpToAddress) {
		// TODO Auto-generated method stub
		
	}

	public boolean coverChanges() {
		// TODO Auto-generated method stub
		return coverChanges;
	}

}
