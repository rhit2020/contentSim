
public class Main {

	public static void main(String[] args) {
		int all = 1;
		String all_rating = "ratings.csv";
		String outlier_rem_rating = "ratings.outlier.removed.csv";
		EvaluationSim.evaluate(all_rating,all);
		EvaluationSim.evaluate(outlier_rem_rating,all);		
	}
}
