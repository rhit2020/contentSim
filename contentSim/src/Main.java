
public class Main {

	public static void main(String[] args) {
		String all_rating = "ratings.csv";
		String outlier_rem_rating = "ratings.outlier.removed.csv";
		EvaluationSim.evaluate(all_rating);
		EvaluationSim.evaluate(outlier_rem_rating);		
	}
}
