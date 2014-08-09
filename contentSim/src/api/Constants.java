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
		public static final int HIGH_MIN = 6;
	}
	
	public static enum Method 
	{
		GLOBAL_AS (Group.STATIC) { public String toString(){return "GLOBAL::AS";}},
		GLOBAL_COS (Group.STATIC){ public String toString(){return "GLOBAL::COS";}}, 
		LOCAL_AS (Group.STATIC){ public String toString(){return "LOCAL::AS";}}, 
		LOCAL_COS (Group.STATIC){ public String toString(){return "LOCAL::COS";}},
		P_GLOBAL_AS (Group.PERSONALZIED){ public String toString(){return "P::GLOBAL::AS";}},
		P_GLOBAL_COS (Group.PERSONALZIED){ public String toString(){return "P::GLOBAL::COS";}}, 
		P_LOCAL_AS (Group.PERSONALZIED){ public String toString(){return "P::LOCAL::AS";}}, 
		P_LOCAL_COS (Group.PERSONALZIED){ public String toString(){return "P::LOCAL::COS";}};
		
		private Group group;
		Method(Group group) {
	        this.group = group;
	    }
		public enum Group {
		        STATIC,
		        PERSONALZIED;		       
		}
		 public boolean isInGroup(Group group) {
		        return this.group == group;
		 }
	};
//	public static enum Static_Method {
//		GLOBAL_AS { public String toString(){return "GLOBAL::AS";}},
//		GLOBAL_COS { public String toString(){return "GLOBAL::COS";}}, 
//		LOCAL_AS { public String toString(){return "LOCAL::AS";}}, 
//		LOCAL_COS { public String toString(){return "LOCAL::COS";}},		
//	}
//	public static enum P_Method {
//		P_GLOBAL_AS { public String toString(){return "P::GLOBAL::AS";}},
//		P_GLOBAL_COS { public String toString(){return "P::GLOBAL::COS";}}, 
//		P_LOCAL_AS { public String toString(){return "P::LOCAL::AS";}}, 
//		P_LOCAL_COS { public String toString(){return "P::LOCAL::COS";}},	
//	}

	public static final int nDCG_LOG_BASE = 2;
	public static final double RELEVANEC_THRESHOLD = +1;
	
	public static final int NOT_HELPFUL_AT_ALL_GAIN = 0;
	public static final int NOT_HELPFUL_GAIN = 0;
	public static final int HELPFUL_GAIN = 1;
	public static final int VERY_HELPFUL_GAIN = 2;
}
