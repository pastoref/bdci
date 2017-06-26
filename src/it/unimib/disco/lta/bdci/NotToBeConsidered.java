package it.unimib.disco.lta.bdci;

import java.util.HashSet;
import java.util.Set;

public class NotToBeConsidered {
	
	private Set<String> keyword=null;
	
	public NotToBeConsidered(){
		keyword= new HashSet<String>();
		this.setKeyword();
		
	}
	
	public void setKeyword(){
		this.keyword.add("orig");
		//this.keyword.add("argv");
		//this.keyword.add("Unknown");
		//this.keyword.add("\tat");
		this.keyword.add("hash");
	}
	
	public void setKeyword(String key){
		if(key.contains(".")){
			String [] tmp=key.split("\\.");
			key=tmp[1];
		}
		
		this.keyword.add(key);
		
	}
	
	public Set<String> getKeyword(){
		return this.keyword;
		
	}
	
	public void to_String(){
		for(String key : keyword ){
			System.out.println(key);
		}
	}

	public boolean contains(String string) {
		return keyword.contains(string);
	}
	
}
