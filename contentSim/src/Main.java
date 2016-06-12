
public class Main {

	public static void main(String[] args) {
		ContentSim cs = new ContentSim();
		String[] contentversion = {"","pp","s14","f14"};

		/* 0: means aggregate the ratings of pretest groups separately; 
		   1: means consider all users to have the same level of pretest and aggregate all ratings on a question no matter of the pretest score of user.
		   -1: means treat each username as a pretest category
		*/
		
		int all = 0; 
		//int all = 1;
		String all_rating = "ratings.csv";
		String outlier_rem_rating = "ratings.outlier.removed.csv";
		String expert_rating = "expert-rating-content-sim-input.csv";

//		EvaluationSim.evaluate(all_rating,all);
		String[] filterUsers = {"","user_weighted_kappa_threshold_gt_0.6.csv","user_weighted_kappa_threshold_high_gt_0.6.csv"};
	    EvaluationSim.evaluate("",all_rating,all,filterUsers[0]);	
	  //  EvaluationSim.getTop3EachApproach("",outlier_rem_rating,all,filterUsers[0]);	
	//	  EvaluationSim.getTop3NaivePersonalizedApproach(outlier_rem_rating,all,contentversion[0],filterUsers[0]);	

//		EvaluationSim.propRatingForMethods("",outlier_rem_rating,all,filterUsers[0]);
//		EvaluationSim.propRatingForMethods("",outlier_rem_rating,all,filterUsers[0]);
		System.out.println("###################################################");

		//cs.calculateSim("",outlier_rem_rating,contentversion[0]);		
		
		//for the correlation analysis
//		EvaluationSim.getInputCorrelationAnalysis(all_rating,all,contentversion[0],filterUsers[0]);	
//		EvaluationSim.getLearningAnalysis("summary_qe.csv",all,filterUsers[0]);	
//		PostProcessLearning.processTimeEngagementInOverlappingExamples("outputLearning_0_summary_qe.csv");
	}
}
