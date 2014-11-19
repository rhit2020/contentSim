
public class Main {

	public static void main(String[] args) {
		int all = 0;
		String all_rating = "ratings.csv";
		String outlier_rem_rating = "ratings.outlier.removed.csv";
//		EvaluationSim.evaluate(all_rating,all);
//		EvaluationSim.evaluate(outlier_rem_rating,all);	
		ContentSim cs = new ContentSim();
		String[] contentversion = {"pp","s14","f14"};
		cs.calculateSim("",contentversion[0]);		
	}
}
