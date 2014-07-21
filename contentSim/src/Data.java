
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {
	
	private Map<String,List<String>> contentMap = null;
	private Map<String,Map<String,Double>> conceptMap = null; //keys are contents, values are the map with concept as key and weight as value
	private Map<String,List<Integer>> startEndLineMap = null; //keys are contents, values: list[0]:start line; list[1]:end line
	private Map<String,Map<Integer,List<Integer>>> contentBlockEndLine = null; //keys are contents, values: a map with key:start line and list of end lines of the concept in that start line


	public void setup() {
		String path = "./resources/";
		readContentData(path+"content.csv");
		readContentConceptData(path+"content_concept.csv");
		readContentStartEndlineData(path+"content_start_end.csv");
		readContentBlockEndLine(path+"content_block_end_line.csv");

	}
	
	private void readContentBlockEndLine(String path) {
		contentBlockEndLine = new HashMap<String,Map<Integer,List<Integer>>>();
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
				elineTXT = clmn[2].split(",");
				eline = new ArrayList<Integer>();
				System.out.println(content);
				for (String s : elineTXT)
					eline.add(Integer.parseInt(s));
				map = new HashMap<Integer,List<Integer>>();
				map.put(sline,eline);
				contentBlockEndLine.put(content, map);
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
		System.out.println("contentBlockEndLine:"+contentBlockEndLine.size());
			
	}

	private void readContentStartEndlineData(String path) {
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

	private void readContentConceptData(String path) {
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
					contentMap.put(name, list);
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

	}
		
	public void disconnectFromLabstudy()
	{
		
	}
	
	public boolean isReady()
	{
		return true;
	}
	
	public String[] getExamples() {
		ArrayList<String> list = new ArrayList<String>();
			
		return list.toArray(new String[list.size()]);
	}
	
	public List<String> getConcepts(String content) {
		List<String> conceptList = new ArrayList<String>();
		
		return conceptList;	
	}

	public String[] getQuestions() {
		ArrayList<String> list = new ArrayList<String>();
				
		return list.toArray(new String[list.size()]);
	}
	
	public void insertContentSim(String question, String example, double sim,String method) {
		//System.out.println(question+" "+example+" "+method+" "+sim);
	}
	
	public Map<String,Double> getTFIDF(String content) {
		Map<String,Double> weightMap = new HashMap<String,Double>();
		
		return weightMap;
	}	

	public List<String> getAdjacentConcept(String content, int sLine,int eline) {
		List<String> conceptList = new ArrayList<String>();
						
		return conceptList;	
	}
	
	public ArrayList<String> getConceptsInSameLine(String content, int sLine) {
		ArrayList<String> conceptList = new ArrayList<String>();
						
		return conceptList;	
	}

	public List<Integer> getStartEndLine(String content) {
		List<Integer> lines = new ArrayList<Integer>();
			
		return lines;	
	}

	public List<Integer> getEndLineBlock(String content, int line) {
		List<Integer> endLines = new ArrayList<Integer>();
					
		return endLines;	
	}

	public void readData() {
		// TODO Auto-generated method stub
		
	}
}
