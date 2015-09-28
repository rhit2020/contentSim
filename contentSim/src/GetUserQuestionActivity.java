import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;


public class GetUserQuestionActivity{

	protected String dbstring;
	protected String dbuser;
	protected String dbpass;
	
	protected static Connection conn;
	protected static Statement stmt = null; 
	protected static ResultSet rs = null;

   public static HashMap<String, String[]> getUserQuestionsActivity(String usr, String datentime) {
	   HashMap<String, String[]> res = new HashMap<String, String[]>();
        try {
            stmt = conn.createStatement();
            String query = "(select if(UA.appid=25,AC.activity,mid(AC.URI,INSTR(AC.URI,'#')+1)) as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess from um2.ent_user_activity UA, um2.ent_activity AC where (UA.appid=25 OR UA.appid=2) and "
            		+ "UA.datentime < '"+datentime+"'"
            		+ " UA.userid = (select userid from um2.ent_user where login='"
                    + usr
                    + "') and AC.activityid=UA.activityid and UA.Result != -1 group by UA.activityid);";

            // System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[3];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            releaseStatement(stmt, rs);
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            releaseStatement(stmt, rs);
        }
        return res;
    }
   
	public static void releaseStatement(Statement stmt, ResultSet rs){
		if (rs != null) {
			try { 
				rs.close();
			}catch (SQLException sqlEx) { sqlEx.printStackTrace(); } 
			rs = null;
		}
		if (stmt != null) {
			try{
				stmt.close();
			}catch (SQLException sqlEx) { sqlEx.printStackTrace(); } 
			stmt = null;
		}
	}

	public static boolean openConnection(){
		try{
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			//System.out.println(dbstring+"?"+ "user="+dbuser+"&password="+dbpass);
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/um2","root","");
			if (conn!=null){
				return true;
			}
		}catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage()); 
			System.out.println("SQLState: " + ex.getSQLState()); 
			System.out.println("VendorError: " + ex.getErrorCode());
			return false;
		}catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true; 
	}
	
	public static void closeConnection(){
		releaseStatement(stmt, rs);
		if (conn != null){
			try{
				conn.close();
			}catch (SQLException sqlEx) { } 
		}
	}

}
