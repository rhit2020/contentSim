
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {
	
	private Map<String,List<String>> contentMap = null; //keys are content type (example,question) and values are the list of the contents 
	private Map<String,Map<String,Double>> conceptMap = null; //keys are contents, values are the map with concept as key and weight as value
	private Map<String,List<Integer>> startEndLineMap = null; //keys are contents, values: list[0]:start line; list[1]:end line
	private Map<String,Map<Integer,List<Integer>>> blockEndLineMap = null; //keys are contents, values: a map with key:start line and list of end lines of the concept in that start line
	private Map<String,Map<Integer,Map<Integer,List<String>>>> adjacentConceptMap = null; //keys are contents, values: a map with key:start line and a map as value(key:end line, value, List of concepts in that start and end line)
	private File file; //output file where similarity results are stored
	private FileWriter fw;
	BufferedWriter bw;
	public void setup() {
		String path = "./resources/";
		file = new File(path+"output.txt");
		try {
			if (!file.exists())
				file.createNewFile();
			fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
				e.printStackTrace();
			}
		readContentData(path+"content.csv");
		readConceptData(path+"content_concept.csv");
		readStartEndlineData(path+"content_start_end.csv");
		readBlockEndLine(path+"block_end_line.csv");
		readAdjacentConcept(path+"adjacent_concept.csv");

	}
	
	private void readAdjacentConcept(String path) {
		adjacentConceptMap = new HashMap<String,Map<Integer,Map<Integer,List<String>>>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		Map<Integer,Map<Integer,List<String>>> map;
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			int sline,eline;
			List<String> concept;
			Map<Integer,List<String>> elineMap;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				sline = Integer.parseInt(clmn[1]);
				eline = Integer.parseInt(clmn[2]);
				clmn[3] = clmn[3].replace("\"", "");
				concept = new ArrayList<String>(Arrays.asList(clmn[3].split(",")));
				if (adjacentConceptMap.containsKey(content))
				{
					map = adjacentConceptMap.get(content);
					if (map.containsKey(sline))
					{
						elineMap = map.get(sline);
						elineMap.put(eline, concept);
					}
					else
					{
						elineMap = new HashMap<Integer,List<String>>();
						elineMap.put(eline,concept);
						map.put(sline, elineMap);
					}
				}				
				else
				{
					map = new HashMap<Integer,Map<Integer,List<String>>>();
					elineMap = new HashMap<Integer,List<String>>();
					elineMap.put(eline,concept);
					map.put(sline,elineMap);
					adjacentConceptMap.put(content, map);
				}				
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// print size
		int count = 0;
		for (Map<Integer, Map<Integer, List<String>>> val : adjacentConceptMap.values())
			for (Map<Integer,List<String>> val2 : val.values())
				count += val2.size();
		System.out.println("adjacentConceptMap:" + count);
    
		
	}

	private void readBlockEndLine(String path) {
		blockEndLineMap = new HashMap<String,Map<Integer,List<Integer>>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		Map<Integer,List<Integer>> map;
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			int sline;
			String[] elineTXT;
			List<Integer> eline;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				sline = Integer.parseInt(clmn[1]);
				clmn[2] = clmn[2].replace("\"", "");
				elineTXT = clmn[2].split(",");
				eline = new ArrayList<Integer>();
				for (String s : elineTXT)
					eline.add(Integer.parseInt(s));
				if (blockEndLineMap.containsKey(content))
				{
					map = blockEndLineMap.get(content);
					map.put(sline, eline);
				}
				else
				{
					map = new HashMap<Integer,List<Integer>>();
					map.put(sline,eline);
					blockEndLineMap.put(content, map);
				}				
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// print size
		int count = 0;
		for (Map<Integer,List<Integer>> val : blockEndLineMap.values())
			count += val.size();
		System.out.println("contentBlockEndLine:" + count);
    }

	private void readStartEndlineData(String path) {
		startEndLineMap = new HashMap<String,List<Integer>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		List<Integer> list;
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			int sline,eline;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				sline = Integer.parseInt(clmn[1]);
				eline = Integer.parseInt(clmn[2]);
				list = new ArrayList<Integer>();
				list.add(sline);
				list.add(eline);
				startEndLineMap.put(content,list);
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("startEndLineMap:"+startEndLineMap.size());			
	}

	private void readConceptData(String path) {
		conceptMap = new HashMap<String,Map<String,Double>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		Map<String,Double> map;
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			String concept;
			double weight;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				concept = clmn[1];
				weight = Double.parseDouble(clmn[2]);
				if (conceptMap.containsKey(content))
				{
					map = conceptMap.get(content);
					map.put(concept,weight);
				}
				else
				{
					map = new HashMap<String,Double>();
					map.put(concept,weight);
					conceptMap.put(content, map);
				}
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//print size 
		int count = 0;
		for (Map<String, Double> val: conceptMap.values())
			count+=val.size();
		System.out.println("conceptMap:"+count);
	}

	private void readContentData(String path) {
		contentMap = new HashMap<String,List<String>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		List<String> list;
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String type;
			String name;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				type = clmn[0];
				name = clmn[1];
				if (contentMap.containsKey(type))
				{
					list = contentMap.get(type);
					list.add(name);
				}
				else
				{
					list = new ArrayList<String>();
					list.add(name);
					contentMap.put(type, list);
				}
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("contentMap:"+contentMap.values().size());
	}

	public void close() {
		try {
			file = null; // destroy the file for writing the output
			if (fw != null) {
				fw.close();
				fw = null;
			}
			if (bw != null) {
				bw.close();
				bw = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (contentMap != null)
		{
			for (List<String> list : contentMap.values()) //destroy the lists in this map
				destroy(list);				
			destroy(contentMap); //destroy the map
		}
		if (startEndLineMap != null)
		{
			for (List<Integer> list : startEndLineMap.values()) //destroy the lists in this map
				destroy(list); 
			destroy(startEndLineMap);//destroy the map
		}
		if (conceptMap != null)
		{
			for (Map<String,Double> map : conceptMap.values()) //destroy the maps in this map
				destroy(map);
			destroy(conceptMap);//destroy the map
		}
		if (blockEndLineMap != null)
		{
			for (Map<Integer, List<Integer>> map : blockEndLineMap.values())
			{
				if (map != null)
				{
					for (List<Integer> list : map.values()) //destroy lists in map
						destroy(list);					
					destroy(map); //destroy the map
				}
			}
			destroy(blockEndLineMap); //destroy the map
		}
		if (adjacentConceptMap != null)
		{
			for (Map<Integer, Map<Integer, List<String>>> map : adjacentConceptMap.values())
			{
				if (map != null)
				{
					for (Map<Integer, List<String>> map2 : map.values())
					{
						if (map2 != null)
						{
							for (List<String> list : map2.values()) //destroy the list in map
								destroy(list);							
						}
						destroy(map2); //destroy the map2
					}
					destroy(map); //destroy map
				}
			}
			destroy(adjacentConceptMap);
		}
	}	
	
	private void destroy(Map map) {
		if (map != null)
		{
			for (Object obj : map.keySet())
			{
				obj = null;
			}
			for (Object obj : map.values())
			{
				obj = null;
			}
			map.clear();
			map = null;
		}		
	}

	private void destroy(List list) {
		if (list != null)
		{
			for (Object obj : list)
				obj = null;	
			list.clear();
			list = null;
		}		
	}

	public boolean isReady()
	{
		return true;
	}
	
	//String sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'example' and domain = 'java' order by content_name;";
	public String[] getExamples() {
		List<String> list = contentMap.get("example");			
		return list.toArray(new String[list.size()]);
	}
	
	//String sqlCommand = "select distinct concept from rel_content_concept where title ='"+content+"';";
	public List<String> getConcepts(String content) {
		Map<String,Double> weightMap = conceptMap.get(content);
		List<String> conceptList = new ArrayList<String>();
		for (String c : weightMap.keySet())
			conceptList.add(c);
		return conceptList;	
	}
	
	//String sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'question' and domain = 'java' order by content_name;";
	public String[] getQuestions() {
		List<String> list = contentMap.get("question");	
		return list.toArray(new String[list.size()]);
	}
	
	public void insertContentSim(String question, String example, double sim, String method) {
		try {
			bw.write(question+"\t"+example+"\t"+sim+"\t"+method);
			bw.newLine();
		    bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println(question+" "+example+" "+method+" "+sim);
	}
	
	//String sqlCommand = "SELECT distinct concept,`tfidf` FROM temp2_ent_jcontent_tfidf where title = '" + content + "';";
	public Map<String,Double> getTFIDF(String content) {
		Map<String,Double> weightMap = conceptMap.get(content);		
		return weightMap;
	}	

	//String sqlCommand = "select distinct concept from rel_content_concept where title ='"+content+"' and sline >= "+sLine+" and eline <= "+eline+";" ;
	public List<String> getAdjacentConcept(String content, int sline,int eline) {
		List<String> conceptList = new ArrayList<String>();
		Map<Integer,Map<Integer,List<String>>> map = adjacentConceptMap.get(content);	
		Map<Integer,List<String>> elineMap;
		List<String> concepts;
		for (Integer s : map.keySet())
		{
			if (s >= sline)
			{
				elineMap = map.get(s);
				for (Integer e : elineMap.keySet())
				{
					if (e <= eline)			
					{
						concepts = elineMap.get(e);
						for (String c : concepts)
							if (conceptList.contains(c) == false)	
								conceptList.add(c);
					}
				}
			}
		}
		return conceptList;	
	}
	//String sqlCommand = "select distinct concept from rel_content_concept where title ='"+content+"' and sline = "+sLine+";";
	public ArrayList<String> getConceptsInSameLine(String content, int sline) {
		ArrayList<String> conceptList = new ArrayList<String>();
		Map<Integer,Map<Integer,List<String>>> map = adjacentConceptMap.get(content);	
		Map<Integer, List<String>> elineMap;
		for (Integer s : map.keySet())
		{
			if (s == sline)
			{
				elineMap = map.get(s);
				for (List<String> concepts : elineMap.values())
				{
					for (String c : concepts)
						if (conceptList.contains(c) == false)	
							conceptList.add(c);					
				}
			}
		}
		return conceptList;		
	}
	
	//String sqlCommand = "select min(sline),max(eline) from rel_content_concept where title ='"+content+"';";
	public List<Integer> getStartEndLine(String content) {
		List<Integer> lines = startEndLineMap.get(content);
		return lines;
	}
    
	//String sqlCommand = "select distinct eline from rel_content_concept where title ='"+content+"' and sline = "+line+" and eline !="+line+";";
	public List<Integer> getEndLineBlock(String content, int sline) {
        List<Integer> endLines = new ArrayList<Integer>();
		Map<Integer,List<Integer>> map = blockEndLineMap.get(content);
		List<Integer> tmp = map.get(sline);
        if (tmp != null)
        {
        	for (Integer t : tmp)
            	if ( t != sline)
            		endLines.add(t);
        }		
		return endLines;	
	}	
}
