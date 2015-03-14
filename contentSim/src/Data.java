
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import api.Constants.Method;

public class Data {
	
	private Map<String,List<String>> contentMap = null; //keys are content type (example,question) and values are the list of the contents 
	private Map<String,Map<String,Double>> conceptMap = null; //keys are contents, values are the map with concept as key and weight as value
	private Map<String,List<Integer>> startEndLineMap = null; //keys are contents, values: list[0]:start line; list[1]:end line
	private Map<String,Map<Integer,List<Integer>>> blockEndLineMap = null; //keys are contents, values: a map with key:start line and list of end lines of the concept in that start line
	private Map<String,Map<Integer,Map<Integer,List<String>>>> adjacentConceptMap = null; //keys are contents, values: a map with key:start line and a map as value(key:end line, value, List of concepts in that start and end line)
	private File fileSim,fileConceptLevels,fileMeasures,fileRankedExample; //output file where similarity results are stored
	private FileWriter fwSim,fwConceptLevels,fwMeasures,fwRankedExample;
	private BufferedWriter bwSim,bwConceptLevels,bwMeasures,bwRankedExample;	
	private DecimalFormat df;	
    //maps for using in the evaluation process
	private Map<String,String> difficultyMap; //content_name,difficulty
	private Map<String,List<String>> topicQuestionMap; //there is one content currently that has two topics.
	private Map<String,Double> pretestMap; //userid, pretest
	private Map<String,Map<String,Map<String,Map<String,Double>>>> conceptLevelMap; //Map<group,Map<user,Map<datentime,Map<concept,knowledge>>>>
	private Map<String,Map<String,Map<Map<String,Double>,Map<String,List<Integer>>>>> ratingMap;//Map<pretest_level,Map<question,Map<Map<concept,knowledge>,Map<example,List<rating>>>>>
	private Map<String,List<String>> topicConceptMap;
	private Map<String,List<Integer>> userMinMaxRatingList; //Map<user,List<min,max>>  keys are users, values are a list of length 2 with the first elem as min rating and second elem as max rating
	private Map<String,String> contentTreeMap; //Map<content,tree> this is for local similarity using TED approach used in study 
	private Map<String,String> rdfTitleMap; //Map<rdfid,title>
	private static Data data = null;
	
	private Data() {
		// Exists only to defeat instantiation.
		//it can only be accessed in the class
    }
	
	public static Data getInstance() {
		if(data == null) {
			data = new Data();
	    }
		return data;
	}
	
	/*
	 * contentversion is mainly used for the contentSim for getting static methods similarity over different times.
	 * for the purpose of my prelim evaluation I use the "" which are the f14 version (after adding new contents, and also changing existing one)
	 * note that content files for f14 has no prefix, e.g. content_start_end or block_end_line 
	 */
	public void setup(String ratingFileName, int all, String contentversion) {
		df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		
		String path = "./resources/";		
		fileSim = new File(path+(contentversion==""?"":contentversion+"_")+"outputSim.txt");
		try {
			if (!fileSim.exists())
				fileSim.createNewFile();
			fwSim = new FileWriter(fileSim.getAbsoluteFile());
			bwSim = new BufferedWriter(fwSim);
		} catch (IOException e) {
				e.printStackTrace();
		}	
		fileMeasures = new File(path+"outputMeasures_"+all+"_"+ratingFileName);
		try {
			if (!fileMeasures.exists())
				fileMeasures.createNewFile();
			fwMeasures = new FileWriter(fileMeasures.getAbsoluteFile());
			bwMeasures = new BufferedWriter(fwMeasures);
		} catch (IOException e) {
				e.printStackTrace();
		}	
		
		fileRankedExample = new File(path+"outputRankedExample_"+all+"_"+ratingFileName);
		try {
			if (!fileRankedExample.exists())
				fileRankedExample.createNewFile();
			fwRankedExample = new FileWriter(fileRankedExample.getAbsoluteFile());
			bwRankedExample = new BufferedWriter(fwRankedExample);
		} catch (IOException e) {
				e.printStackTrace();
		}
//		fileConceptLevels = new File(path+"outputConceptLevels.csv");
//		try {
//			if (!fileConceptLevels.exists())
//				fileConceptLevels.createNewFile();
//			fwConceptLevels = new FileWriter(fileConceptLevels.getAbsoluteFile());
//			bwConceptLevels = new BufferedWriter(fwConceptLevels);
//		} catch (IOException e) {
//				e.printStackTrace();
//		}		
		readContentData(path+(contentversion==""?"":contentversion+"_")+"content.csv");
		readConceptData(path+(contentversion==""?"":contentversion+"_")+"content_concept.csv");
		readStartEndlineData(path+(contentversion==""?"":contentversion+"_")+"content_start_end.csv");
		readBlockEndLine(path+(contentversion==""?"":contentversion+"_")+"block_end_line.csv");
		readAdjacentConcept(path+(contentversion==""?"":contentversion+"_")+"adjacent_concept.csv");
		readDifficulty(path+"difficulty.csv"); //content,difficulty
		readTopic(path+"topic.csv");//content, topic
		readPretest(path+"pretest_Q5_removed.csv");//user,pretest
//    	createConceptLevelFile(path+"ratings.csv"); //create the conceptLevel file
		readConceptLevels(path+"outputConceptLevels.csv");//group,user,datentime,concept,knowledge
		readRatings(path+ratingFileName,all);//user,group,datentime,question,example,rating (0,1,2,3)
		readTopicConcept(path+"topicOutcomeConcepts.csv");
//		readUserMinMaxRating(path+"user_min_max_rating.csv"); 
		/*I do not use it because normalization has no meaning here with ordinal ratings.
		E.g. if user rates always 0 or 1. Then normalizing 0 will result in 0 and 1 to 1.
		 Means when rated 1 it is good because later we want to aggregate judges, we should say first 1/3, is no gain, 1/30-2/3 is gain 1, and 2/3-3/3 is gain 2. should if normalized
		 score of all users was 1 (assume all of them are users with ratings either 0 or 1, this means that they think it is useful example. It is totally screwing things up
		 */
		//for the old local similarity based on TED used in lasbtudy
		readContentTree(path+(contentversion==""?"":contentversion+"_")+"tree.csv");
		readRdfTitle(path+"rdfid_title.csv");
	}
	
	private void readRdfTitle(String path) {
		rdfTitleMap = new HashMap<String,String>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String title;
			String rdfid;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				rdfid =clmn[0];	
				title = clmn[1];
				rdfTitleMap.put(rdfid,title);
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
		System.out.println("rdfTitleMap:"+rdfTitleMap.size());		
	}

	private void readContentConceptTFIDF(String string) {
		// TODO Auto-generated method stub
		
	}

	private void readContentTree(String path) {
		contentTreeMap = new HashMap<String,String>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String content;
			String tree;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				content = clmn[0];
				if (clmn.length == 1)
				{
					System.out.println("No tree for content: "+content);
					continue;
				}
				else
				{
					tree = clmn[1];
					contentTreeMap.put(content,tree);
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
		System.out.println("contentTreeMap: "+contentTreeMap.size());			
	}

	private void readUserMinMaxRating(String path) {
		userMinMaxRatingList = new HashMap<String,List<Integer>>();
		List<Integer> list;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String user;
			int min,max;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				user = clmn[0];
				min = Integer.parseInt(clmn[1]);
				max = Integer.parseInt(clmn[2]);
				list = new ArrayList<Integer>();
				list.add(min,max);
				userMinMaxRatingList.put(user, list);
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
		System.out.println("userMinMaxRatingList: "+userMinMaxRatingList.size());		
	}

	private void readTopicConcept(String path) {
		topicConceptMap = new HashMap<String,List<String>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String topic;
			String[] conceptList;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				topic = clmn[0];
				clmn[1] = clmn[1].replace("\"", "");
				conceptList = clmn[1].split(";");
				if (topicConceptMap.containsKey(topic) != false)
				{
					List<String> list = topicConceptMap.get(topic);
					for (String c : conceptList)
						if (list.contains(c) == false)
							list.add(c);					
				}
				else
				{
					List<String> list = new ArrayList<String>();
					for (String c : conceptList)
						list.add(c);
					topicConceptMap.put(topic,list);
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
		int count = 0,countTopic = 0;
		for (String t : topicConceptMap.keySet())
		{
			countTopic++;
			count += topicConceptMap.get(t).size();
		}
		System.out.println("topicConceptMap: topic:"+countTopic+" topic_concept:"+count);		
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
		boolean isHeader = false; //the file has no header @see createConceptLevelFile
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
		Map<String, Map<String, Double>> map;
		for (String group : conceptLevelMap.keySet())
			for (String user : conceptLevelMap.get(group).keySet())
			{
			    map = conceptLevelMap.get(group).get(user);
				for (String datentime : map.keySet())					
					count+=map.get(datentime).size();				
			}
		System.out.println("conceptLevelMap:"+count);								
	}

	private void readRatings(String path, int all) {
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
//			int gain;
			Map<String,Map<Map<String,Double>,Map<String,List<Integer>>>> pretestMap;
			Map<Map<String,Double>,Map<String,List<Integer>>> questionMap;
			Map<String,Double> knowledgeMap;
			Map<String,List<Integer>> exampleMap;
			List<Integer> list;
			String pretest = "";
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
				//map ratings to gains 
//				gain = getGain(rating);
				if (all == 0)
					pretest = getPretestLevel(user);
				else if (all == 1)
					pretest = "-";
				else if (all == -1)//means treat pretest as user, number of pretest will be the same as users
					pretest = ""+user;
				knowledgeMap = getKnowledgeMap(user,group,datentime);
				if (ratingMap.containsKey(pretest) == false)
				{
					list = new ArrayList<Integer>();
//					list.add(gain);
					list.add(rating);
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
//						list.add(gain);
						list.add(rating);
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
//							list.add(gain);
							list.add(rating);
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
//								list.add(gain);
								list.add(rating);
								exampleMap.put(example, list);
							}
							else
							{
								list = exampleMap.get(example);
//								list.add(gain);
								list.add(rating);
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
	
	private String getPretestLevel(String user) {
		String level = "";
		double pretest = pretestMap.get(user);
//		if( pretest < api.Constants.Pretest.AVE_MIN)
//			level = "Low";
//		else if (pretest < api.Constants.Pretest.HIGH_MIN)
//			level = "Average";
		if( pretest < api.Constants.Pretest.HIGH_MIN)
			level = "Low";		
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
		topicQuestionMap = new HashMap<String,List<String>>();
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
				if (topicQuestionMap.containsKey(content) != false)
				{
					List<String> list = topicQuestionMap.get(content);
					if (list.contains(topic) == false)
						list.add(topic);
				}
				else
				{
					List<String> list = new ArrayList<String>();
					list.add(topic);
					topicQuestionMap.put(content,list);
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
		for (String t : topicQuestionMap.keySet())
			count += topicQuestionMap.get(t).size();
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
		df = null;
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
		if (topicQuestionMap != null)
		{
			for (List<String> list : topicQuestionMap.values())
				destroy(list);
			destroy(topicQuestionMap);
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
		if (topicConceptMap != null)
		{
			for (List<String> list : topicConceptMap.values())
				destroy(list);
			destroy(topicConceptMap);
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
		if (weightMap!=null)		
		{
			for (String c : weightMap.keySet())
				conceptList.add(c);
		}	
		return conceptList;	
	}
	
	//String sqlCommand = "SELECT distinct content_name FROM ent_content where content_type = 'question' and domain = 'java' order by content_name;";
	public String[] getQuestions() {
		List<String> list = contentMap.get("question");	
		return list.toArray(new String[list.size()]);
	}
	
	public void insertContentSim(String question, String example, double sim, String method) {
		try {
			bwSim.write(question+"\t"+example+"\t"+sim+"\t"+method+"\t"+rdfTitleMap.get(question));
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
		return topicQuestionMap.get(content);
	}
	
	public String getTopicText(String content){
		String topics = "";
		if (topicQuestionMap.get(content).size() == 1)
		{
			topics = topicQuestionMap.get(content).get(0);
		}
		else
		{
			for (String t : topicQuestionMap.get(content))
			{
				topics += t + " ";
			}
		}		
		return topics;
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
                        + URLEncoder.encode(grp, "UTF-8") + "&datentime=" + URLEncoder.encode(datentime, "UTF-8"));

            }
            if (domain.equalsIgnoreCase("sql")) {
                url = new URL(api.Constants.conceptLevelsServiceURL
                        + "?typ=con&dir=out&frm=xml&app=23&dom=sql_ontology"
                        + "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
                        + URLEncoder.encode(grp, "UTF-8") + "&datentime=" + URLEncoder.encode(datentime, "UTF-8"));

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
	
	public Map<String, Double> getConceptLevelAtTime(String group, String user,String datentime) {
		return conceptLevelMap.get(group).get(user).get(datentime);
	}

	public Set<String> getPretestCategories() {
		return ratingMap.keySet();
	}

	public void writeToFile(String question,String pretest,Method method, double AP, double nDCG, double QMeasure, double RMSE) {
		try {
			String topicText = getTopicText(question);
			String difficulty = getDifficulty(question);
			if (difficulty.equals("null"))
				System.out.println("null diff");
			bwMeasures.write(question+","+topicText+","+difficulty+","+pretest+","+method.toString()+","+"AP"+","+df.format(AP));
			bwMeasures.newLine();
			bwMeasures.flush();
			//
			bwMeasures.write(question+","+topicText+","+difficulty+","+pretest+","+method.toString()+","+"nDCG"+","+df.format(nDCG));
			bwMeasures.newLine();
			bwMeasures.flush();
			//
			bwMeasures.write(question+","+topicText+","+difficulty+","+pretest+","+method.toString()+","+"QMeasure"+","+df.format(QMeasure));
			bwMeasures.newLine();
			bwMeasures.flush();
		    //
			bwMeasures.write(question+","+topicText+","+difficulty+","+pretest+","+method.toString()+","+"RMSE"+","+df.format(RMSE));
		    bwMeasures.newLine();
		    bwMeasures.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	//TODO this is changed to gather ratings from all users
	public boolean isJudged(String question, String example, String pretest) {
		boolean isJudged = false;
		Map<Map<String, Double>, Map<String, List<Integer>>> qMap = ratingMap.get(pretest).get(question);
		for (Map<String, List<Integer>> exampleMap : qMap.values())
			if (exampleMap.containsKey(example))
			{
				isJudged = true;
				break;
			}
		return isJudged;

//		for (Map<String, Map<Map<String, Double>, Map<String, List<Integer>>>> pretestMap : ratingMap.values())
//		{
//			Map<Map<String, Double>, Map<String, List<Integer>>> qMap = pretestMap.get(question);
//			if (qMap != null)
//			{
//				for (Map<String, List<Integer>> exampleMap : qMap.values())
//					if (exampleMap.containsKey(example))			
//						return true;
//			}			
//		}	
//		return false;
	}
	
	//TODO this is changed to gather ratings from all users
	public List<Integer> getRatingList(String question, String example, String pretest)
	{
		List<Integer> rateList = new ArrayList<Integer>();
		Map<Map<String, Double>, Map<String, List<Integer>>> qMap = ratingMap.get(pretest).get(question);
		for (Map<String, List<Integer>> eMap : qMap.values())
			if (eMap.containsKey(example))			
				rateList.addAll(eMap.get(example));
		return rateList;
		
//		List<Integer> rateList = new ArrayList<Integer>();
//		for (Map<String, Map<Map<String, Double>, Map<String, List<Integer>>>> pretestMap : ratingMap.values())
//		{
//			Map<Map<String, Double>, Map<String, List<Integer>>> qMap = pretestMap.get(question);
//			if (qMap != null)
//			{
//				for (Map<String, List<Integer>> eMap : qMap.values())
//					if (eMap.containsKey(example))			
//						rateList.addAll(eMap.get(example)); // it keeps the repetitive numbers. It is necessary to know frequency for aggragation of judges.
//			}
//		}
//		return rateList;
	}
	
	/*
	 * this method implements majority voting, where in case of ties lower relevance is selected
	 */
	public int aggregateJudges(List<Integer> values) {
		HashMap<Integer, Integer> freqs = new HashMap<Integer, Integer>();
		for (int val : values) {
			Integer freq = freqs.get(val);
			freqs.put(val, (freq == null ? 1 : freq + 1));
		}
		int mode = 0;
		int maxFreq = 0;
		int tieVal;
		for (Map.Entry<Integer, Integer> entry : freqs.entrySet())
		{
			int freq = entry.getValue();
			if (freq > maxFreq) {
				maxFreq = freq;
				mode = entry.getKey();
			}
			//handling the ties, take the lower values
			if (freq == maxFreq)
			{
				tieVal = entry.getKey();
				if (tieVal < mode)
					mode = tieVal;				
			}
		}
		return mode;
	}

	/*
	 * this method adds all the relevant example for the question to the list
	 * relevant examples have a majority voting gain that is either +1:helpful or is +2:very helpful.
	 * AVERAGE gain is obtained by summing all the available gains for the example and then dividing them by the total number of the gains.
	 */	 
	public List<String> getRelevantExampleList(String pretest, String question) {
		Map<Map<String, Double>, Map<String, List<Integer>>> qMap = ratingMap.get(pretest).get(question);
		List<String> relList = new ArrayList<String>();
		List<Integer> ratingList;
		for (Map<String, List<Integer>> exampleMap : qMap.values())
			for (String example : exampleMap.keySet())
			{
				ratingList = exampleMap.get(example);
				if (aggregateJudges(ratingList) >= api.Constants.RELEVANEC_THRESHOLD)
				{
					if (relList.contains(example) == false)
						relList.add(example);
				}
			}			
		return relList;
	}
	
	public List<String> getConceptTopic(String concept)
	{
		List<String> topicList = new ArrayList<String>();
		for (Entry<String, List<String>> entry : topicConceptMap.entrySet())
		{	
			if (entry.getValue().contains(concept))
			{
				if (topicList.contains(entry.getKey()) == false)
					topicList.add(entry.getKey());
			}
		}
		return topicList;
	}

	public String getTree(String content) {
		return contentTreeMap.get(content);
	}

	public double getWeightInSubtree(String subtree, String content) {
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
		double weight = 0.0;
		for (String concept : subtreeConcepts)
		{
			if (conceptMap.get(content).containsKey(concept))
			weight += conceptMap.get(content).get(concept);
		}
		return weight;
	}

	public void writeRankedExample(String question, String pretest, Method method,ArrayList<String> orderedList) {
		try {
			String topicText = getTopicText(question);
			String difficulty = getDifficulty(question);
			if (difficulty.equals("null"))
				System.out.println("null diff");
			String rankedExampleTxt = "";
			for (String example : orderedList)
			{
				rankedExampleTxt += example + "@";
			}
			if (orderedList.isEmpty() == false)
				rankedExampleTxt = rankedExampleTxt.substring(0,rankedExampleTxt.length()-1); //ignoring the last comma
			bwRankedExample.write(question+","+topicText+","+difficulty+","+pretest+","+method.toString()+","+rankedExampleTxt);
			bwRankedExample.newLine();
			bwRankedExample.flush();			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}	
}
