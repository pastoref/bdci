package it.unimib.disco.lta.bdci;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StructuresTree {

	private Map<String,StructuresTree> children = new HashMap<String,StructuresTree>();
	private String name = "";
	private String type = "";
	private boolean isRoot;
	private StructuresTree parent;
	
	public StructuresTree(StructuresTree parent, String child) {
		name = child;
		this.parent = parent;
	}

	public StructuresTree() {
		isRoot = true;
	}

	public void addSubtree(String subtree, String type ){
		
		
		int pos = subtree.indexOf('.');
		String child;
		String suffix;
		if ( pos > 0 ){
			child = subtree.substring(0,pos);
			suffix = subtree.substring(pos+1);
		} else {
			suffix = null;
			child = subtree;
		}
		
		
		StructuresTree childTree = getChild( child );
		
		if ( suffix == null ){
			childTree.setType( type  );
		} else {
			childTree.addSubtree(suffix, type);
		}
	}
	
	private void setType(String type) {
		this.type = type;
	}

	private StructuresTree getChild(String child) {
		StructuresTree childTree = children.get(child);
		if ( childTree == null ){
			childTree = new StructuresTree(this,child);
			children.put(child, childTree);
		}
		return childTree;
	}
	
	public boolean isStructure() {
		return ! children.isEmpty();
	}
	
	public String toString(){
		String res = name + " " + type ;
		for ( StructuresTree child : children.values() ){
			res += "["+child.toString()+"], ";
		}
		
		return res;
	}

	private static Map<String,String> structNames = new HashMap<String,String>();
	private static Map<String,String> varNames = new HashMap<String,String>();
	private static Map<String, String> nilOfStruct = new HashMap<String, String>();
	public String getStructName(){
		
		String sn = "Struct"+name;
		
		/*int c = 0;
		while ( structNames.containsKey(sn) ){
			sn = "Struct"+name+c;
			c++;
		}
		if(structNames.containsKey(sn)){
			structNames.remove(sn);
			System.out.println("removed");
		}*/
		structNames.put(sn, name);
		varNames.put(getPathName(), sn);
		
		return sn;
	}
	/**
	 * varName = "a.c"
	 */
	public static String getStructNameFromVarName( String varName ){
		return varNames.get(varName);
	}
	
	private String getPathName() {
		StructuresTree node = this;
		
		String pathName = this.name;
		node = this.parent;
		while ( node != null && ! node.isRoot ){
			pathName = node.getName() + "." + pathName;
			node = node.parent;
		}
		
		return pathName;
	}

	public String getStructNil(){
		nilOfStruct.put(getStructName(), "NIL"+getStructName());
		return "NIL"+getStructName();
		
	}
	
	public static String getNilNameFromVarName(String varName){
		String struct=varNames.get(varName);
		return nilOfStruct.get(struct);
	}
	
	
	public List<String> getDeclarations(){
		String decl = getDeclaration();
		
		ArrayList<String> childDecls = new ArrayList<String>();
		for ( StructuresTree child : children.values() ){
			if ( child.isStructure() ){
				childDecls.addAll(child.getDeclarations());
			}
		}
		
		if ( ! isRoot ){
			childDecls.add(getDeclaration());
		}
		return childDecls;
	}

	private String getDeclaration() {
		String decl = "(declare-datatypes () ((";
		
		decl += getStructName()+" "+getStructNil();
		decl += "( cons ";
		for ( StructuresTree child : children.values() ){
			decl += "("+child.getName()+" "+child.getDeclType()+")";
		}
		
		decl += " ) )))";
		
		return decl;
	}
	
	private String getDeclType() {
		if ( ! type.isEmpty() ){
			return type;
		}
		return getStructName();
	}

	private String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	public Map<String, StructuresTree> getChildren() {
		return children;
	}

	public void setChildren(Map<String, StructuresTree> children) {
		this.children = children;
	}

	public static void main(String[] args) {
		StructuresTree s = new StructuresTree();
		
		//s.addSubtree("a.c.d.b.n","int");
		//s.addSubtree("c.d","int");
		//s.addSubtree("c.e","float");
		//s.addSubtree("a.c.e","float");
		s.addSubtree("*product.out_of_catalogue", "int");
		s.addSubtree("*product.items", "int");
		s.addSubtree("ciao.items", "int");
		System.out.println(s);
		
		for ( String decl : s.getDeclarations() ){
			System.out.println(decl);
			//System.out.println(StructuresTree.getStructNameFromVarName(decl));
		}
		
		System.out.println("tipo di a: "+StructuresTree.getStructNameFromVarName("*product"));
		//System.out.println("tipo di a.c: "+StructuresTree.getStructNameFromVarName("c.d"));
		//System.out.println("nil di a: "+StructuresTree.getNilNameFromVarName("a"));

	}

}
