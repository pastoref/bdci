package it.unimib.disco.lta.bdci;

public class dividetimpera {

	public static String unisce(String a){
		String [] temp = a.split("\n");
		if(temp.length==2) return "(and "+temp[0]+temp[1]+")";
		if(temp.length%2!=0){
			String b="";
			String c="";
			for(int i=0; i<temp.length/2;i++){
				b+=temp[i]+"\n";
			}
			for(int i=temp.length/2; i<temp.length-1;i++){
				c+=temp[i]+"\n";
			}
			return "(and "+unisce(unisce(b)+"\n"+unisce(c))+ temp[temp.length-1]+")";
		}
		else{
			String b="";
			String c="";
			for(int i=0; i<temp.length/2;i++){
				b+=temp[i]+"\n";
			}
			for(int i=temp.length/2; i<temp.length;i++){
				c+=temp[i]+"\n";
			}
			return unisce(unisce(b)+"\n"+unisce(c));
		}
	}
	
	public static void main(String [] args){
		
		String var="(not (= x 0))\n";
		var+="(>= x 1.0)\n";
		var+="(not (= y 0))\n";
		var+="(>= y 1.89999998)\n";
		var+="(not (= x y))\n";
		System.out.println(unisce(var));


	}
}
