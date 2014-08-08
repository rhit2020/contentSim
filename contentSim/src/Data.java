
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Data {
	
	private Map<String,List<String>> contentMap = null; //keys are content type (example,question) and values are the list of the contents 
	private Map<String,Map<String,Double>> conceptMap = null; //keys are contents, values are the map with concept as key and weight as value
	private Map<String,List<Integer>> startEndLineMap = null; //keys are contents, values: list[0]:start line; list[1]:end line
	private Map<String,Map<Integer,List<Integer>>> blockEndLineMap = null; //keys are contents, values: a map with key:start line and list of end lines of the concept in that start line
	private Map<String,Map<Integer,Map<Integer,List<String>>>> adjacentConceptMap = null; //keys are contents, values: a map with key:start line and a map as value(key:end line, value, List of concepts in that start and end line)
	private File fileSim,fileConceptLevels,fileMeasures; //output file where similarity results are stored
	private FileWriter fwSim,fwConceptLevels,fwMeasures;
	BufferedWriter bwSim,bwConceptLevels,bwMeasures;	
    //maps for using in the evaluation process
	private Map<String,String> difficultyMap; //content_name,difficulty
	private Map<String,List<String>> topicMap; //there is one content currently that has two topics.
	private Map<String,Double> pretestMap; //userid, pretest
	private Map<String,Map<String,Map<String,Map<String,Double>>>> conceptLevelMap; //Map<group,Map<user,Map<datentime,Map<concept,knowledge>>>>
	private Map<String,Map<String,Map<Map<String,Double>,Map<String,List<Integer>>>>> ratingMap;//Map<pretest_level,Map<question,Map<Map<concept,knowledge>,Map<example,List<rating>>>>>
	
	public void setup() {
		String path = "./resources/";
		fileSim = new File(path+"outputSim.txt");
		try {
			if (!fileSim.exists())
				fileSim.createNewFile();
			fwSim = new FileWriter(fileSim.getAbsoluteFile());
			bwSim = new BufferedWriter(fwSim);
		} catch (IOException e) {
				e.printStackTrace();
		}	
		fileMeasures = new File(path+"outputMeasures.txt");
		try {
			if (!fileMeasures.exists())
				fileMeasures.createNewFile();
			fwMeasures = new FileWriter(fileMeasures.getAbsoluteFile());
			bwMeasures = new BufferedWriter(fwMeasures);
		} catch (IOException e) {
				e.printStackTrace();
		}	
		fileConceptLevels = new File(path+"outputConceptLevels.csv");
		try {
			if (!fileConceptLevels.exists())
				fileConceptLevels.createNewFile();
			fwConceptLevels = new FileWriter(fileConceptLevels.getAbsoluteFile());
			bwConceptLevels = new BufferedWriter(fwConceptLevels);
		} catch (IOException e) {
				e.printStackTrace();
		}		
		readContentData(path+"content.csv");
		readConceptData(path+"content_concept.csv");
		readStartEndlineData(path+"content_start_end.csv");
		readBlockEndLine(path+"block_end_line.csv");
		readAdjacentConcept(path+"adjacent_concept.csv");
		readDifficulty(path+"difficulty.csv"); //content,difficulty
		readTopic(path+"topic.csv");//content, topic
		readPretest(path+"pretest_Q5_removed.csv");//user,pretest
		createConceptLevelFile(path+"ratings.csv"); //create the conceptLevel file
		readConceptLevels(path+"outputConceptLevels.csv");//group,user,datentime,concept,knowledge
		readRatings(path+"ratings.csv");//user,group,datentime,question,example,rating (0,1,2,3)
	}
	
	private void createConceptLevelFile(String path) {
		//Step1: save distinct group,user,datentime 
		Map<String, Map<String,List<String>>> gud = new HashMap<String, Map<String, List<String>>>(); //g:group u: user d:datentime  // Map<group, Map<user,List<datentime>>>
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String group;
			String user;
			String datentime;
			Map<String, List<String>> ud;
			List<String> dList;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				user = clmn[0];
				group = clmn[1];
				datentime = clmn[2];
				if (gud.containsKey(group) == false)
				{
					ud = new HashMap<String, List<String>>();
					dList = new ArrayList<String>();
					dList.add(datentime);
					ud.put(user,dList);
					gud.put(group, ud);
				}
				else{
					ud = gud.get(group);
					if (ud.containsKey(user) == false)
					{
						dList = new ArrayList<String>();
						dList.add(datentime);
						ud.put(user,dList);
					}
					else
					{
						dList = ud.get(user);
						if (dList.contains(datentime) == false)
							dList.add(datentime);
					}
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
		int count = 0;
		for (Map<String, List<String>> group : gud.values())
			for ( List<String> dl : group.values())
				count += dl.size();
		System.out.println("gud:"+count);
		//write the user knowledge for each group,user,datentime to the output file
		List<String> dList;
		Map<String, Double> conceptLeveles;
		count = 0;
		int count2 = 0;
		//write to the file	
		try {
			for (String group : gud.keySet())
				for (String user : gud.get(group).keySet())
				{
					dList = gud.get(group).get(user);
					for (String datentime : dList)
					{
						conceptLeveles = getConceptLevels(user,"java",group,datentime);
						count2 += 1;
						for (String concept : conceptLeveles.keySet())
						{
								bwConceptLevels.write(group+","+user+","+datentime+","+concept+","+conceptLeveles.get(concept));
								bwConceptLevels.newLine();
								bwConceptLevels.flush();
								count += 1;
						} 
					}
				}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("size of group-user-datentime:"+count2);
		System.out.println("total line in outputConceptLevels:"+count);
		//destroy the local map
		for (Map<String,List<String>> map : gud.values())
		{
			if (map != null)
			{
				for (List<String> list : map.values())
				{
					if (list != null)
						destroy(list);
				}
				destroy(map);
			}
		}
		destroy(gud);		
	}

	private void readConceptLevels(String path) {
		//Map<group,Map<user,Map<datentime,Map<concept,knowledge>>>>
		//group,user,datentime,concept,knowledge
		conceptLevelMap = new HashMap<String,Map<String,Map<String,Map<String,Double>>>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String group;
			String user;
			String datentime;
			String concept;
			double knowledge;
			Map<String,Map<String,Map<String,Double>>> groupMap;
			Map<String,Map<String,Double>> userMap;
			Map<String,Double> knowledgeMap;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				group = clmn[0];
				user = clmn[1];
				datentime = clmn[2];
				concept = clmn[3];
				knowledge = Double.parseDouble(clmn[4]);
				if (conceptLevelMap.containsKey(group) == false)
				{
					knowledgeMap = new HashMap<String,Double>();
					knowledgeMap.put(concept,knowledge);
					userMap = new HashMap<String,Map<String,Double>>();
					userMap.put(datentime,knowledgeMap);
					groupMap = new HashMap<String,Map<String,Map<String,Double>>>();
					groupMap.put(user,userMap);
					conceptLevelMap.put(group, groupMap);
				}
				else
				{
					groupMap = conceptLevelMap.get(group);
					if (groupMap.containsKey(user) == false)
					{
						knowledgeMap = new HashMap<String,Double>();
						knowledgeMap.put(concept,knowledge);
						userMap = new HashMap<String,Map<String,Double>>();
						userMap.put(datentime,knowledgeMap);
						groupMap.put(user,userMap);
					}
					else{
						userMap = groupMap.get(user);
						if (userMap.containsKey(datentime) == false)
						{
							knowledgeMap = new HashMap<String,Double>();
							knowledgeMap.put(concept,knowledge);
							userMap.put(datentime,knowledgeMap);
						}
						else
						{
							knowledgeMap = userMap.get(datentime);
							knowledgeMap.put(concept,knowledge);							
						}
					}
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
		int count = 0;
		for (String group : conceptLevelMap.keySet())
			for (String user: conceptLevelMap.get(group).keySet())
				for (String datentime : conceptLevelMap.get(group).get(user).keySet())
					count += conceptLevelMap.get(group).get(user).get(datentime).keySet().size();
		System.out.println("conceptLevelMap:"+count);								
	}

	private void readRatings(String path) {
		//user,group,datentime,question,example,rating (0,1,2,3)
		ratingMap = new HashMap<String,Map<String,Map<Map<String,Double>,Map<String,List<Integer>>>>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String user;
			String group;
			String datentime;
			String question;
			String example;
			int rating;
			int Nrating;
			Map<String,Map<Map<String,Double>,Map<String,List<Integer>>>> pretestMap;
			Map<Map<String,Double>,Map<String,List<Integer>>> questionMap;
			Map<String,Double> knowledgeMap;
			Map<String,List<Integer>> exampleMap;
			List<Integer> list;
			String pretest;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				user = clmn[0];
				group = clmn[1];
				datentime = clmn[2];
				question = clmn[3];
				example = clmn[4];
				rating = Integer.parseInt(clmn[5]);
				//map ratings (0,1,2,3) to (-2,-1,0,+1,+2) to penalize examples that are not helpful 
				Nrating = getNrating(rating);
				pretest = getPretestLevel(user);
				knowledgeMap = getKnowledgeMap(user,group,datentime);
				if (ratingMap.containsKey(pretest) == false)
				{
					list = new ArrayList<Integer>();
					list.add(Nrating);
					exampleMap = new HashMap<String,List<Integer>>();
					exampleMap.put(example, list);
					questionMap = new HashMap<Map<String,Double>,Map<String,List<Integer>>>();
					questionMap.put(knowledgeMap, exampleMap);
					pretestMap = new HashMap<String,Map<Map<String,Double>,Map<String,List<Integer>>>>();
					pretestMap.put(question, questionMap);
					ratingMap.put(pretest, pretestMap);
				}
				else
				{
					pretestMap = ratingMap.get(pretest);
					if (pretestMap.containsKey(question) == false)
					{
						list = new ArrayList<Integer>();
						list.add(Nrating);
						exampleMap = new HashMap<String,List<Integer>>();
						exampleMap.put(example, list);
						questionMap = new HashMap<Map<String,Double>,Map<String,List<Integer>>>();
						questionMap.put(knowledgeMap, exampleMap);
						pretestMap.put(question, questionMap);
					}
					else{
						questionMap = pretestMap.get(question);
						if (questionMap.containsKey(knowledgeMap) == false)
						{
							list = new ArrayList<Integer>();
							list.add(Nrating);
							exampleMap = new HashMap<String,List<Integer>>();
							exampleMap.put(example, list);
							questionMap.put(knowledgeMap, exampleMap);
						}
						else
						{
							exampleMap = questionMap.get(knowledgeMap);
							if (exampleMap.containsKey(example) == false)
							{
								list = new ArrayList<Integer>();
								list.add(Nrating);
								exampleMap.put(example, list);
							}
							else
							{
								list = exampleMap.get(example);
								list.add(Nrating);
							}
						}
					}
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
		int count = 0;
		for (String pretest : ratingMap.keySet())
			for (String question : ratingMap.get(pretest).keySet())
				for (Map<String,List<Integer>> exampleMap : ratingMap.get(pretest).get(question).values())
					for (String example : exampleMap.keySet())
						count += exampleMap.get(example).size();						
		System.out.println("ratingMap:"+count);						
	}

	private Map<String, Double> getKnowledgeMap(String user, String group, String datentime) {
		return conceptLevelMap.get(group).get(user).get(datentime);
	}

	private int getNrating(int rating) {
		int Nrating = 0;
		switch (rating)
        {
		  case 0:
			  	Nrating = -2; // not helpful at all
			  	break;
		  case 1:
				Nrating = -1; // not helpful 
				break;
		  case 2:
				Nrating = +1; // helpful 
				break;
		  case 3:
				Nrating = +2; // very helpful 
				break;
		  default: 
			    break;
		}		
		return Nrating;
	}

	private String getPretestLevel(String user) {
		String level = "";
		double pretest = pretestMap.get(user);
		if( pretest < api.Constants.Pretest.AVE_MIN)
			level = "Low";
		else if (pretest < api.Constants.Pretest.HIGH_MIN)
			level = "Average";
		else
			level = "High";
		return level;
	}

	private void readPretest(String path) {
		pretestMap = new HashMap<String,Double>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String user;
			double pretest;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				user = clmn[0];
				pretest = Double.parseDouble(clmn[1]);				
				pretestMap.put(user,pretest);
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
		System.out.println("pretestMap:"+pretestMap.size());						
	}

	private void readTopic(String path) {
		topicMap = new HashMap<String,List<String>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			String topic;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				topic = clmn[1];
				if (topicMap.containsKey(content) != false)
				{
					List<String> list = topicMap.get(content);
					if (list.contains(topic) == false)
						list.add(topic);
				}
				else
				{
					List<String> list = new ArrayList<String>();
					list.add(topic);
					topicMap.put(content,list);
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
		int count = 0;
		for (String t : topicMap.keySet())
			count += topicMap.get(t).size();
		System.out.println("topicMap:"+count);					
	}

	private void readDifficulty(String path) {
		difficultyMap = new HashMap<String,String>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			String difficulty;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				difficulty = clmn[1];				
				difficultyMap.put(content,difficulty);
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
		System.out.println("difficultyMap:"+difficultyMap.size());			
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
				concept = new ArrayList<String>(Arrays.asList(clmn[3].split(";")));
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
				elineTXT = clmn[2].split(";");
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
					contentMap.put(type,list);
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
		int count = 0;
		for (List<String> l : contentMap.values())
			count += l.size();
		System.out.println("contentMap:"+count);
	}

	public void close() {
		try {
			fileSim = null; // destroy the file for writing the output
			if (fwSim != null) {
				fwSim.close();
				fwSim = null;
			}
			if (bwSim != null) {
				bwSim.close();
				bwSim = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fileMeasures = null; // destroy the file for writing the output
			if (fwMeasures != null) {
				fwMeasures.close();
				fwMeasures = null;
			}
			if (bwMeasures != null) {
				bwMeasures.close();
				bwMeasures = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fileConceptLevels = null; // destroy the file for writing the output
			if (fwConceptLevels != null) {
				fwConceptLevels.close();
				fwConceptLevels = null;
			}
			if (bwConceptLevels != null) {
				bwConceptLevels.close();
				bwConceptLevels = null;
			}
		} catch (IOException e) {
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
							destroy(map2); //destroy the map2
						}
					}
					destroy(map); //destroy map
				}
			}
			destroy(adjacentConceptMap);
		}
		if (difficultyMap != null)			
			destroy(difficultyMap); //destroy the map
		if (topicMap != null)
		{
			for (List<String> list : topicMap.values())
				destroy(list);
			destroy(topicMap);
		}
		if (pretestMap != null)
			destroy(pretestMap);
		
		if (conceptLevelMap != null)
		{
			for (Map<String,Map<String,Map<String,Double>>> groupMap : conceptLevelMap.values())
			{
				if (groupMap != null)
				{
					for (Map<String,Map<String,Double>> userMap : groupMap.values())
					{
						if (userMap != null)
						{
							for (Map<String,Double> kMap : userMap.values())
								destroy(kMap);
							destroy(userMap);
						}
					}
					destroy(groupMap);
				}
			}
			destroy(conceptLevelMap);
		}
		if (ratingMap != null)
		{
			for (Map<String,Map<Map<String,Double>,Map<String,List<Integer>>>> pretestMap : ratingMap.values())
			{
				if (pretestMap != null)
				{
					for (Map<Map<String,Double>,Map<String,List<Integer>>> questionMap : pretestMap.values())
					{
						if (questionMap != null)
						{
							for (Map<String,Double> kMap : questionMap.keySet())
								destroy(kMap);
							for (Map<String,List<Integer>> exampleMap : questionMap.values())
							{
								if (exampleMap != null)
								{
									for (List<Integer> list : exampleMap.values())
										destroy(list);
								}
								destroy(exampleMap);
							}
							destroy(questionMap);
						}
					}
					destroy(pretestMap);
				}				
			}
			destroy(ratingMap);
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
			bwSim.write(question+"\t"+example+"\t"+sim+"\t"+method);
			bwSim.newLine();
		    bwSim.flush();
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
	
	public String getDifficulty(String content){
		return difficultyMap.get(content);
	}
	
	public List<String> getTopic(String content){
		return topicMap.get(content);
	}
	
	public double getPretest(String user)
	{
		return pretestMap.get(user);
	}
	
	//--- methods for processing user knowledge ---//
	// CALLING A UM SERVICE
    public HashMap<String, Double> getConceptLevels(String usr, String domain,String grp, String datentime) {
        HashMap<String, Double> user_concept_knowledge_levels = new HashMap<String, Double>();
        try {
            URL url = null;
            if (domain.equalsIgnoreCase("java")) {
                url = new URL(api.Constants.conceptLevelsServiceURL
                        + "?typ=con&dir=out&frm=xml&app=25&dom=java_ontology"
                        + "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
                        + URLEncoder.encode(grp, "UTF-8"));

            }
            if (domain.equalsIgnoreCase("sql")) {
                url = new URL(api.Constants.conceptLevelsServiceURL
                        + "?typ=con&dir=out&frm=xml&app=23&dom=sql_ontology"
                        + "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
                        + URLEncoder.encode(grp, "UTF-8"));

            }
            if (url != null)
                user_concept_knowledge_levels = processUserKnowledgeReport(url);
            // System.out.println(url.toString());
        } catch (Exception e) {
            user_concept_knowledge_levels = null;
            e.printStackTrace();
        }
        return user_concept_knowledge_levels;
    }
    
    private static HashMap<String, Double> processUserKnowledgeReport(URL url) {
        HashMap<String, Double> userKnowledgeMap = new HashMap<String, Double>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(url.openStream());
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("concept");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    NodeList cogLevels = eElement.getElementsByTagName("cog_level");
                    for (int i = 0; i < cogLevels.getLength(); i++) {
                        Node cogLevelNode = cogLevels.item(i);
                        if (cogLevelNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element cogLevel = (Element) cogLevelNode;
                            if (getTagValue("name", cogLevel).trim().equals("application")) {
                                // System.out.println(getTagValue("name",
                                // eElement));
                                double level = Double.parseDouble(getTagValue("value",cogLevel).trim());
                                userKnowledgeMap.put(getTagValue("name", eElement),level);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
        return userKnowledgeMap;
    }
    
    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue.getNodeValue();
    }

	public Set<String> getRatedQuestions(String pretest) {
		return ratingMap.get(pretest).keySet();
	}

	public Set<Map<String, Double>> getKnowledgeLevels(String pretest,String question) {
		return ratingMap.get(pretest).get(question).keySet();
	}

	public Set<String> getPretestCategories() {
		return ratingMap.keySet();
	}

	public void writeToFile(String question,String pretest,String method, double AP, double nDCG, double QMeasure) {
		try {
			bwMeasures.write(question+"\t"+topicMap.get(question)+"\t"+difficultyMap.get(question)+"\t"+pretest+"\t"+method+"\t"+"AP"+"\t"+AP);
			bwMeasures.newLine();
			bwMeasures.flush();
			//
			bwMeasures.write(question+"\t"+topicMap.get(question)+"\t"+difficultyMap.get(question)+"\t"+pretest+"\t"+method+"\t"+"nDCG"+"\t"+nDCG);
			bwMeasures.newLine();
			bwMeasures.flush();
			//
			bwMeasures.write(question+"\t"+topicMap.get(question)+"\t"+difficultyMap.get(question)+"\t"+pretest+"\t"+method+"\t"+"QMeasure"+"\t"+QMeasure);
			bwMeasures.newLine();
			bwMeasures.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public boolean isJudged(String question, String example, String pretest) {
		boolean isJudged = ((Map<String,List<Integer>>)(ratingMap.get(pretest).get(question).values())).containsKey(example);
		return isJudged;
	}
	
	public double getAvgRate(String question, String example, String pretest)
	{
		List<Integer> rateList = ((Map<String,List<Integer>>)(ratingMap.get(pretest).get(question).values())).get(example);
		double sum = 0.0;
		for (int r : rateList)
			sum += r;
		double avg = sum/rateList.size();
		return avg;
	}

	public Set<String> getRelevantExampleList(String pretest, String question) {
		Set<String> relList = ((Map<String,List<Integer>>)(ratingMap.get(pretest).get(question).values())).keySet();
		return relList;
	}
}
