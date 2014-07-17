
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DB {

	private Connection labstudyConn;
	private boolean isConnLabstudyValid;
	
	public void connect() {
		connectToLabstudy();		
	}
	
	public void disconnect() {
		disconnectFromLabstudy();
	}
	
	public void connectToLabstudy()
	{
		  String url = api.Constants.DB.LABSTUDY_URL;
		  String driver = api.Constants.DB.DRIVER;
		  String userName = api.Constants.DB.USER;
		  String password = api.Constants.DB.PASSWORD;
		  
		  try {
		  Class.forName(driver).newInstance();
		  labstudyConn = DriverManager.getConnection(url,userName,password);
		  isConnLabstudyValid = true;
		  System.out.println("Connected to the database labstudy");
		  } catch (Exception e) {
		  e.printStackTrace();
		  }	
	}	
		
	public boolean isConnectedToLabstudy()
	{
		if (labstudyConn != null) {
			try {
				if (labstudyConn.isClosed() == false & isConnLabstudyValid)
					return true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
		
	public void disconnectFromLabstudy()
	{
		if (labstudyConn != null)
			try {
				labstudyConn.close();
			    System.out.println("Database labstudy Connection Closed");
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	public String[] getExamples() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			String sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'example' and domain = 'java' order by content_name;";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				list.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}	
		return (String[]) list.toArray();
	}
	
	public List<String> getConcepts(String content) {
		List<String> conceptList = new ArrayList<String>();
		try
		{
			String sqlCommand = "select distinct concept from rel_content_concept where title ='"+content+"';";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				conceptList.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}				
		return conceptList;	
	}

	public String[] getQuestions() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			String sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'question' and domain = 'java' order by content_name;";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				list.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (SQLException e1) {
			e1.printStackTrace();			
		}			
		return (String[]) list.toArray();
	}
	
	public void insertContentSim(String question, String example, double sim,String method) {
//		try
//		{
//			String sqlCommand = "insert into rel_content_content_sim (question_content_name,example_content_name,sim,method) values ('"+question+"','"+example+"',"+sim+",'"+method+"')";
//			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
//			ps.executeUpdate();
//			ps.close();			
//		}catch (SQLException e) {
//			 e.printStackTrace();
//		}
		System.out.println(question+" "+example+" "+method+" "+sim);
	}
	
	public Map<String,Double> getTFIDF(String content) {
		Map<String,Double> weightMap = new HashMap<String,Double>();
		try {
			String sqlCommand = "SELECT distinct concept,`tfidf` FROM temp2_ent_jcontent_tfidf where title = '" + content + "';";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				weightMap.put(rs.getString(1),rs.getDouble(2));
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return weightMap;
	}	

	public List<String> getAdjacentConcept(String content, int sLine,int eline) {
		List<String> conceptList = new ArrayList<String>();
		try
		{		
			String sqlCommand = "select distinct concept from rel_content_concept where title ='"+content+"' and sline >= "+sLine+" and eline <= "+eline+";" ;
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				if (conceptList.contains(rs.getString(1)) == false)
					conceptList.add(rs.getString(1));
			}
		}catch (SQLException e) {
			 e.printStackTrace();
		}				
		return conceptList;	
	}

	public List<Integer> getStartEndLine(String content) {
		List<Integer> lines = new ArrayList<Integer>();
		try
		{
			String sqlCommand = "select min(sline),max(eline) from rel_content_concept where title ='"+content+"';";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				int s = rs.getInt(1);
				int e = rs.getInt(2);
				lines.add(s);
				lines.add(e);
			}			
		}catch (SQLException e) {
			 e.printStackTrace();
		}			
		return lines;	
	}

	public List<Integer> getConceptEndLines(String content, int line) {
		List<Integer> endLines = new ArrayList<Integer>();
		try
		{								
			String sqlCommand = "select distinct eline from rel_content_concept where title ='"+content+"' and sline = "+line+";";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				endLines.add(rs.getInt(1));
		}catch (SQLException e) {
			 e.printStackTrace();
		}			
		return endLines;	
	}
}
