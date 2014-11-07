package api;

public class Constants {
	
	
	public static final class DB{
		public static final String DRIVER = "com.mysql.jdbc.Driver";
		public static final String LABSTUDY_URL = "jdbc:mysql://localhost:3306/labstudy";
		public static final String USER = "student";
		public static final String PASSWORD = "student";		
	}
	
	public static final String server = "http://localhost:8080";//local instance of the modified cbum project
	public static final String conceptLevelsServiceURL = server + "/cbum/ReportManager";
	
	public static final class Pretest{
		public static final int LOW_MIN = 0;
		public static final int AVE_MIN = 3;
		public static final int HIGH_MIN = 5;
	}
	
	public static enum Method 
	{
		RANDOM_BASELINE (Group.BASELINE) { public String toString(){return "RANDOM::BASELINE";}},
		GLOBAL_AS (Group.STATIC) { public String toString(){return "GLOBAL::AS";}},
		GLOBAL_COS (Group.STATIC){ public String toString(){return "GLOBAL::COS";}}, 
		LOCAL_AS (Group.STATIC){ public String toString(){return "LOCAL::AS";}}, 
		LOCAL_COS (Group.STATIC){ public String toString(){return "LOCAL::COS";}},
		P_GLOBAL_AS (Group.PERSONALZIED){ public String toString(){return "P::GLOBAL::AS";}},
		P_GLOBAL_COS (Group.PERSONALZIED){ public String toString(){return "P::GLOBAL::COS";}}, 
		P_LOCAL_AS (Group.PERSONALZIED){ public String toString(){return "P::LOCAL::AS";}}, 
		P_LOCAL_COS (Group.PERSONALZIED){ public String toString(){return "P::LOCAL::COS";}},
		P_GLOBAL_AS_GOAL (Group.PERSONALZIED){ public String toString(){return "P::GLOBAL::AS::GOAL";}},
		P_GLOBAL_COS_GOAL (Group.PERSONALZIED){ public String toString(){return "P::GLOBAL::COS::GOAL";}},
		P_LOCAL_AS_GOAL (Group.PERSONALZIED){ public String toString(){return "P::LOCAL::AS::GOAL";}},
		P_LOCAL_COS_GOAL (Group.PERSONALZIED){ public String toString(){return "P::LOCAL::COS::GOAL";}};
		
		
		private Group group;
		Method(Group group) {
	        this.group = group;
	    }
		public enum Group {
			    BASELINE,
		        STATIC,
		        PERSONALZIED;		       
		}
		 public boolean isInGroup(Group group) {
		        return this.group == group;
		 }
	};
	
	public static final Method[] GOAL_BASED_METHODS = {Method.P_LOCAL_AS_GOAL,Method.P_LOCAL_COS_GOAL,Method.P_GLOBAL_AS_GOAL,Method.P_GLOBAL_COS_GOAL};
	public static final Method[] NORMALIZED_METHODS = {
														Method.RANDOM_BASELINE, //always 0.5 so btw 0-1
														Method.GLOBAL_COS,Method.P_GLOBAL_COS,Method.P_GLOBAL_COS_GOAL,
														Method.LOCAL_COS,Method.P_LOCAL_COS,Method.P_LOCAL_COS_GOAL
	                                                   }; //the list of methods that has similarity value between 0 and 1. Others are between -1 and 1.
	public static final int nDCG_LOG_BASE = 2;
	public static final double RELEVANEC_THRESHOLD = +2;
	
	public static final int NOT_HELPFUL_AT_ALL_GAIN = 0;
	public static final int NOT_HELPFUL_GAIN = 0;
	public static final int HELPFUL_GAIN = 1;
	public static final int VERY_HELPFUL_GAIN = 2;
	public static final int ASSOCIATION_COEFF_SIM_MAX_VALUE = +1;
	public static final int ASSOCIATION_COEFF_SIM_MIN_VALUE = -1;
	
	public static final int[] GAINS = {NOT_HELPFUL_AT_ALL_GAIN,NOT_HELPFUL_GAIN,HELPFUL_GAIN,VERY_HELPFUL_GAIN};
	public static final int[] RATINGS = {0,1,2,3};

}
