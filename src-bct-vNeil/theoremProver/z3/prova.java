package theoremProver.z3;

public class prova {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		for (int i = 1; i<30;i++){
			System.out.println("UPDATE `bct`.`datum` SET dataDefinition = Replace(dataDefinition, '\"', '') where idDatum=" + i + ";");
//			System.out.println("C:c12:c12=65 I:c7:c7=5 X:c1:c1="+i);
//			System.out.println("P:c9:c9=1 I:c7:c7="+i +" X:c1:c1=2");
//			System.out.println("V:c12:c12=6 I:c7:c7=" + i);
		}
//		System.out.println("SELECT * FROM `bct`.`datum`;");
	}

}
