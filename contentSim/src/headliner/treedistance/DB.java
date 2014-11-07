package headliner.treedistance;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DB {

	private Connection labstudyConn;
	private boolean isConnLabstudyValid;
		
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
	
	public List<String> getContentsRdfs() {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> rdfs = new ArrayList<String>();
		try
		{
			sqlCommand = "SELECT distinct content_name FROM ent_content";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				rdfs.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}	
		return rdfs;	
	}
	
	public String[] getExamplesName() {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		String[] list = null;
		int count = -1;
		sqlCommand = "SELECT count(distinct content_name) FROM ent_content where content_type = 'example' and domain = 'java'";
		try {
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				count = rs.getInt(1);
			list = new String[count];
			rs.close();
			ps.close();
			sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'example' and domain = 'java'";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			int i = 0;
			while (rs.next())
			{
				list[i] = rs.getString(1);
				i++;
			}
			rs.close();
			ps.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
		return list;
	}

	public List<String> getAdjacentConcept(String content, String concept) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> conceptList = new ArrayList<String>();
		String title = "";
		boolean isExample = false;
		sqlCommand = "select content_type from ent_content where content_name ='"+content+"'";
		try {
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				 if (rs.getString(1).equals("example"))
					 isExample = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String conceptTable = "ent_jquiz_concept";
		title = content;
		

		try
		{
			if (isExample)
			{
				conceptTable = "ent_jexample_concept";
				sqlCommand = "select name from ent_dissection where rdfID ='"+content+"'";
				ps = labstudyConn.prepareStatement(sqlCommand);
				rs = ps.executeQuery();
				while (rs.next())
					title = rs.getString(1);
				rs.close();
				ps.close();
			}
			
			int s=-1,e=-1 ;
			sqlCommand = "select sline,eline from "+conceptTable+" where title ='"+title+"' and concept = '"+concept+"'";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			rs.close();
			ps.close();
			ResultSet rs2;
			while (rs.next())
			{
				s = rs.getInt(1);
				e = rs.getInt(2);
				sqlCommand = "select distinct concept from "+conceptTable+" where title ='"+title+"' and sline >= "+s+" and eline <= "+e +" and concept != '"+concept+"'";
				ps = labstudyConn.prepareStatement(sqlCommand);
				rs2 = ps.executeQuery();
				while (rs2.next())
				{
					if (conceptList.contains(rs2.getString(1)) == false)
						conceptList.add(rs2.getString(1));
				}
				rs.close();
				ps.close();
			}
			
		}catch (SQLException e) {
			 e.printStackTrace();
		}
			
		return conceptList;	
	}

	public List<String> getConcepts(String content) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> conceptList = new ArrayList<String>();
		boolean isExample = false;
		sqlCommand = "select content_type from ent_content where content_name ='"+content+"'";
		try {
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				 if (rs.getString(1).equals("example"))
					 isExample = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String conceptTable = "ent_jquiz_concept";
		if (isExample)
			conceptTable = "ent_jexample_concept";					
		try
		{				
			sqlCommand = "select distinct concept from "+conceptTable+" where title ='"+content+"'";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				conceptList.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
				
		return conceptList;	
	}

	public ArrayList<String> getQuestionNames() {

		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		ArrayList<String> list = null;
		int count = -1;
//		sqlCommand = "SELECT count(distinct content_name) FROM ent_content where content_type = 'question' and domain = 'java' order by content_name";
		try {
//			ps = guanjieConn.prepareStatement(sqlCommand);
//			rs = ps.executeQuery();
//			while (rs.next())
//				count = rs.getInt(1);
			list = new ArrayList<String>();
			//TODO you should change the limit
			sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'question' and domain = 'java' order by content_name";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				list.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			try {
				ps.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
		return list;
	
	}
	
	public String getTree(String c) {
        PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		String tree = "";
		
		try {
			sqlCommand = "SELECT tree  FROM ent_content_tree where content_name = '"+c+"'";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				 tree = rs.getString(1);
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
			
		return tree;
	}

	public void insertContentSim(String question, String example, double sim,double distance, String method) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		try
		{
			sqlCommand = "insert into rel_con_con_sim (question_content_name,example_content_name,sim,method,distance) values ('"+question+"','"+example+"',"+sim+",'"+method+"',"+distance+")";
			ps = labstudyConn.prepareStatement(sqlCommand);
			ps.executeUpdate();
			ps.close();
			
		}catch (SQLException e) {
			 e.printStackTrace();
		}			
	}

	public List<String> getAllConcepts(String content) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> conceptList = new ArrayList<String>();
		boolean isExample = false;
		sqlCommand = "select content_type from ent_content where content_name ='"+content+"'";
		try {
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				 if (rs.getString(1).equals("example"))
					 isExample = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String conceptTable = "ent_jquiz_concept";
		if (isExample)
			conceptTable = "ent_jexample_concept";					

		try
		{				
			sqlCommand = "select distinct concept from "+conceptTable+" where title ='"+content+"'";
			ps = labstudyConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				conceptList.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
			
		return conceptList;	
	

	}


	public List<String> getContentConcepts() {
		List<String> list = new ArrayList<String>();

		try
		{				
			String sqlCommand = "(SELECT distinct concept FROM ent_jexample_concept) union (SELECT distinct concept FROM ent_jquiz_concept) ";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				list.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
	
		return list;
	}

		public double getTFIDF(String q, String c) {
		double val = 0.0;

		try
		{				
			String sqlCommand = "SELECT `log(tf+1)idf` FROM temp2_ent_jcontent_tfidf where title = '"+q+"' and concept = '"+c+"';";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				val = rs.getDouble(1);
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}	
		return val;
	}

	public double getWeightInSubtree(String subtree, String content, DB db) {

		String[] edges = subtree.split(";");
		ArrayList<String> subtreeConcepts = new ArrayList<String>();
		String[] temp;
		for (String edge : edges)
		{
			temp = edge.split("-");
			if (temp[0].equals("ROOT"))
				subtreeConcepts.add(temp[1]);
			else 
			{
				subtreeConcepts.add(temp[0]);	
				subtreeConcepts.add(temp[1]);	
			}
		}
		String concepts = ""; 
		for (int i = 0; i < subtreeConcepts.size(); i++)
		{
			concepts += "'"+subtreeConcepts.get(i)+"'";
			if (i < subtreeConcepts.size() - 1)
				concepts += ",";
		}
		double weight = 0.0;
		try
		{				
			String sqlCommand = "SELECT `log(tf+1)idf` FROM temp2_ent_jcontent_tfidf where title = '"+content+"' and concept in ("+concepts+");";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				weight += rs.getDouble(1);
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
			
		return weight;
	}

	public void connect() {
		connectToLabstudy();		
	}
	public void disconnect() {
		disconnectFromLabstudy();
	}

	public HashMap<String, Double> getConceptsVector(String content) {
		HashMap<String,Double> map = new HashMap<String,Double>();
		List<String> list = getAllConcepts(content);
		String concepts = ""; 
		for (int i = 0; i < list.size(); i++)
		{
			concepts += "'"+list.get(i)+"'";
			if (i < list.size() - 1)
				concepts += ",";
		}
		try
		{				
			String sqlCommand = "SELECT concept,`log(tf+1)idf` FROM temp2_ent_jcontent_tfidf where title = '"+content+"' and concept in ("+concepts+");";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				map.put(rs.getString(1),rs.getDouble(2));
			}
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
		return map;	
	}
	
	public double getMaxDist() {
		double max = Double.NEGATIVE_INFINITY;
		try
		{				
			String sqlCommand = "SELECT max(distance) from rel_con_con_sim where distance != "+Double.MAX_VALUE+";";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				max = rs.getDouble(1);
			}
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
		return max;	
	}
	
	public double getMinDist() {
		double min = Double.POSITIVE_INFINITY;
		try
		{				
			String sqlCommand = "SELECT min(distance) from rel_con_con_sim;";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				min = rs.getDouble(1);
			}
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
		return min;	
	}

	public void updateSim() {
		try
		{				
			String sqlCommand = "update rel_con_con_sim set sim = (1-normalized_distance)";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ps.executeUpdate();	
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
		
	}

	public void normalizeDistance(double maxdistance, double mindistance) {
		try
		{				
			//update distances != Double.Maxvalue
			String sqlCommand = "update rel_con_con_sim set normalized_distance = (distance-"+mindistance+")/("+(maxdistance-mindistance)+") where distance !="+Double.MAX_VALUE+";";
			PreparedStatement ps = labstudyConn.prepareStatement(sqlCommand);
			ps.executeUpdate();
			//set normalized_distance = 1 when distance = Double.Maxvalue
			sqlCommand = "update rel_con_con_sim set normalized_distance = 1 where distance ="+Double.MAX_VALUE+";";
			ps = labstudyConn.prepareStatement(sqlCommand);
			ps.executeUpdate();		
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}		
	}

}
