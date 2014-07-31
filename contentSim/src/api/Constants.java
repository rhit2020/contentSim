package api;

public class Constants {
	
	
	public static final class DB{
		public static final String DRIVER = "com.mysql.jdbc.Driver";
		public static final String LABSTUDY_URL = "jdbc:mysql://localhost:3306/labstudy";
		public static final String USER = "student";
		public static final String PASSWORD = "student";		
	}
	
	public static final String server = "http://adapt2.sis.pitt.edu";
	public static final String conceptLevelsServiceURL = server + "/cbum/ReportManager";
	
	public static final class Pretest{
		public static final int LOW_MIN = 0;
		public static final int AVE_MIN = 3;
		public static final int HIGH_MIN = 6;
	}
}
