import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import api.Constants.Method;

public class EvaluationSim {

	private static Data db;
	
	public static void getLearningAnalysis(String ratingFileName, int all, String filterUsers)
	{
		db = Data.getInstance();
		if (db.isReady())
        {
			db.setup("",ratingFileName, all, "",filterUsers);

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			boolean isHeader = true;
			try {
				br = new BufferedReader(new FileReader("./resources/"+ ratingFileName));
				String[] clmn;
				String user;
				String group;
				String datentime;
				String question;
				String exampleString;
				Map<String, Double> simMap;
				Map<String, Double> tmp;
				ValueComparatorDouble vc;
				TreeMap<String, Double> sortedTreeMap;
				String[] exampleList = db.getExamples();
				Map<String, Double> conceptLevelMap;
				String[] clickExString;
				ArrayList<String> clickedExampleList;
				ArrayList<String> temp;
				List<String> baselineSimShuffledList;

				while ((line = br.readLine()) != null) {
					if (isHeader) {
						isHeader = false;
						continue;
					}
					clmn = line.split(cvsSplitBy);
					user = clmn[0];
					group = clmn[1];
					datentime = clmn[7];
					question = clmn[3];
					exampleString = clmn[4];
					if (exampleString.equals(""))
						continue;
					clickExString = exampleString.split(";");
					clickedExampleList = new ArrayList<String>();
					for (String e : clickExString){
//						if (Double.parseDouble(e.split(":")[1])==0)
//							System.out.println("~~~~~zero percentage seen: e"+e);
						//TODO: uncomment following check for computing the learning case
						//if (Double.parseDouble(e.split(":")[1])>0)
								clickedExampleList.add(e.split(":")[0]);  
					}
					for (Method method : Method.values()) {
						if (method.isInGroup(Method.Group.BASELINE)) {
							simMap = ContentSim.calculateSim(question,exampleList, method, null,null,null);
							baselineSimShuffledList = new ArrayList<String>(simMap.keySet());
							Collections.shuffle(baselineSimShuffledList);

							temp = new ArrayList<String>(baselineSimShuffledList);
							int common = getOverlap(clickedExampleList,temp);
							if (common != 0)
								db.writeLearing(line,method,common,temp.subList(0, clickedExampleList.size()));
							//db.writeLearing(line,method,common,temp);
						}
						if (method.isInGroup(Method.Group.STATIC)) {
							simMap = ContentSim.calculateSim(question,exampleList, method, null,null,null);
							// sorting the simMap
							tmp = new HashMap<String, Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String, Double>(vc);
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							temp = new ArrayList<String>(sortedTreeMap.keySet());
							int common = getOverlap(clickedExampleList,temp);
							if (common != 0)
								db.writeLearing(line,method,common,temp.subList(0, clickedExampleList.size()));
							//db.writeLearing(line,method,common,temp);
						} else if (method.isInGroup(Method.Group.PERSONALZIED)) {
							conceptLevelMap = db.getConceptLevelAtTime(group,user, datentime);
							if (conceptLevelMap == null)
								System.out.println("~~~~~no concepts for:"+user+"  "+group+"  "+datentime);
							simMap = ContentSim.calculateSim(question,exampleList, method, conceptLevelMap,null,null);
							// sorting the simMap
							tmp = new HashMap<String, Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String, Double>(vc);
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							temp = new ArrayList<String>(sortedTreeMap.keySet());
							int common = getOverlap(clickedExampleList,temp);
							if (common != 0)
								db.writeLearing(line,method,common,temp.subList(0, clickedExampleList.size()));
							//db.writeLearing(line,method,common,temp);
						}
					}
					//
					clickExString = null;
					temp = null;
					clickedExampleList.clear();
					clickedExampleList = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("System is not ready!");
		}
		db.close();
	}
	
	
	private static void printSim(Map<String, Double> simMap) {
		System.out.println("************************************");
		for (String s : simMap.keySet())
			System.out.print(s+":"+simMap.get(s));
		System.out.println("************************************");
	}


	private static int getOverlap(ArrayList<String> clickedExampleList,
			List<String> rankedExampleList) {
		int overlap = 0;
		for (int i = 0; i < clickedExampleList.size(); i++)
		{
			if (clickedExampleList.contains(rankedExampleList.get(i)))
				overlap++; 
		}
		return overlap;
	}


	public static void getInputCorrelationAnalysis(String ratingFileName, int all,String contentversion, String filterUsers)
	{
		db = Data.getInstance();
		if (db.isReady())
        {
			db.setup("",ratingFileName, all, contentversion,filterUsers);
			//HashMap<user,HashMap<datentime,HashMap<question, questionarray[]>>>
			HashMap<String,HashMap<String,HashMap<String, String[]>>> qact = new HashMap<String,HashMap<String,HashMap<String, String[]>>>();
			HashMap<String,HashMap<String, String[]>> dmap;
			//prepare the map of user-question-activities
			HashMap<String,ArrayList<String>> userTime = db.getUserDatentime();
			GetUserQuestionActivity.openConnection(); //open the db connection
			for (String u : userTime.keySet()){
				for (String d : userTime.get(u))
				{
					dmap = qact.get(u);
					if (dmap != null)
					{
						dmap.put(d, GetUserQuestionActivity.getUserQuestionsActivity(u, d));
					}else{
						dmap = new HashMap<String,HashMap<String, String[]>>();
						dmap.put(d, GetUserQuestionActivity.getUserQuestionsActivity(u, d));
						qact.put(u,dmap);
					}
				}
			}
			GetUserQuestionActivity.closeConnection(); //close the db connection

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			boolean isHeader = true;
			try {
				br = new BufferedReader(new FileReader("./resources/"+ ratingFileName));
				String[] clmn;
				String user;
				String group;
				String datentime;
				String question;
				Map<String, Double> simMap;
				Map<String, Double> tmp;
				ValueComparatorDouble vc;
				TreeMap<String, Double> sortedTreeMap;
				String[] exampleList = db.getExamplesStudy();
				Map<String, Double> conceptLevelMap;
				Map<String, Map<String, Double>> kcByContent = db.getConceptMap();
				List<String> baselineSimShuffledList;
				while ((line = br.readLine()) != null) {
					if (isHeader) {
						isHeader = false;
						continue;
					}
					clmn = line.split(cvsSplitBy);
					user = clmn[0];
					group = clmn[1];
					datentime = clmn[2];
					question = clmn[3];
					question = Data.getInstance().getQuestion(question);
					//Method[] mm = {Method.NAIVE_LOCAL};
					for (Method method : Method.values()) {
					//for (Method method : mm) {
						if (method.isInGroup(Method.Group.BASELINE)) {
							simMap = ContentSim.calculateSim(question,exampleList, method, null,null,null);
							baselineSimShuffledList = new ArrayList<String>(simMap.keySet());
							Collections.shuffle(baselineSimShuffledList);

							db.writeRankedExample(question,"",method,new ArrayList<String>(baselineSimShuffledList));
							//db.writeLearing(line,method,common,temp);
						}
						if (method.isInGroup(Method.Group.STATIC)) {
							simMap = ContentSim.calculateSim(question,exampleList, method, null,null,null);
							// sorting the simMap
							tmp = new HashMap<String, Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String, Double>(vc);
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							db.writeRankedExample(question,"",method,new ArrayList<String>(sortedTreeMap.keySet()));
						} else if (method.isInGroup(Method.Group.PERSONALZIED)) {
							conceptLevelMap = db.getConceptLevelAtTime(group,user, datentime);
							simMap = ContentSim.calculateSim(question,exampleList, method, conceptLevelMap,null,null);
							// sorting the simMap
							tmp = new HashMap<String, Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String, Double>(vc);
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							db.writeRankedExample(question,"",method,new ArrayList<String>(sortedTreeMap.keySet()));
						}
						else if (method.isInGroup(Method.Group.NAIVE_PERSONALIZED))
						{
							conceptLevelMap = db.getConceptLevelAtTime(group,user, datentime);
							simMap = ContentSim.calculateSim(question,exampleList, method, conceptLevelMap,qact.get(user).get(datentime),kcByContent);
							// sorting the simMap
							tmp = new HashMap<String, Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String, Double>(vc);
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							db.writeRankedExample(question,"",method,new ArrayList<String>(sortedTreeMap.keySet()));

						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("System is not ready!");
		}
		db.close();
	}
	
	public static void getTop3NaivePersonalizedApproach(String ratingFileName, int all,String contentversion, String filterUsers)
	{
		db = Data.getInstance();
		if (db.isReady())
        {
			db.setup("",ratingFileName, all, contentversion,filterUsers);
			//HashMap<user,HashMap<datentime,HashMap<question, questionarray[]>>>
			HashMap<String,HashMap<String,HashMap<String, String[]>>> qact = new HashMap<String,HashMap<String,HashMap<String, String[]>>>();
			HashMap<String,HashMap<String, String[]>> dmap;
			//prepare the map of user-question-activities
			HashMap<String,ArrayList<String>> userTime = db.getUserDatentime();
			GetUserQuestionActivity.openConnection(); //open the db connection
			for (String u : userTime.keySet()){
				for (String d : userTime.get(u))
				{
					dmap = qact.get(u);
					if (dmap != null)
					{
						dmap.put(d, GetUserQuestionActivity.getUserQuestionsActivity(u, d));
					}else{
						dmap = new HashMap<String,HashMap<String, String[]>>();
						dmap.put(d, GetUserQuestionActivity.getUserQuestionsActivity(u, d));
						qact.put(u,dmap);
					}
				}
			}
			GetUserQuestionActivity.closeConnection(); //close the db connection

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			boolean isHeader = true;
			try {
				br = new BufferedReader(new FileReader("./resources/"+ ratingFileName));
				String[] clmn;
				String user;
				String group;
				String datentime;
				String question;
				Map<String, Double> simMap;
				Map<String, Double> tmp;
				ValueComparatorDouble vc;
				TreeMap<String, Double> sortedTreeMap;
				String[] exampleList = db.getExamplesStudy();
				Map<String, Double> conceptLevelMap;
				Map<String, Map<String, Double>> kcByContent = db.getConceptMap();
				Map<String, Double> condensedSysRankMap;	//Map<example,SimilarityValue>
				int num = 0;

				while ((line = br.readLine()) != null) {
					if (isHeader) {
						isHeader = false;
						continue;
					}
					clmn = line.split(cvsSplitBy);
					user = clmn[0];
					group = clmn[1];
					datentime = clmn[2];
					question = clmn[3];
					question = Data.getInstance().getQuestion(question);
					Method[] method_list = {Method.P_AVERAGE,Method.P_RE_RANK};
					for (Method method : method_list) {
						num++;
						conceptLevelMap = db.getConceptLevelAtTime(group,user, datentime);
						simMap = ContentSim.calculateSim(question,exampleList, method, conceptLevelMap,qact.get(user).get(datentime),kcByContent);
						// sorting the simMap
						tmp = new HashMap<String, Double>();
						vc = new ValueComparatorDouble(tmp);
						sortedTreeMap = new TreeMap<String, Double>(vc);
						tmp.putAll(simMap);
						sortedTreeMap.putAll(tmp);
						condensedSysRankMap = getTop3ExamplesInfo(question,simMap,new ArrayList<String>(sortedTreeMap.keySet()));
						db.writeTop3SimScore(num,question,method,condensedSysRankMap);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("System is not ready!");
		}
		db.close();
	}

	public static void getTop3EachApproach(String domain, String ratingFileName, int all, String filterUsers){
		db = Data.getInstance();
		if (db.isReady())
        {	
			db.setup("",ratingFileName,all,"",filterUsers); 
			Set<String> pretestList = db.getPretestCategories();
			Map<String, Double> condensedSysRankMap;	//Map<example,SimilarityValue>

			Map<String,Double> simMap;
			Set<String> ratedQList;
			Set<Map<String, Double>> conceptLevelMap;
			//params for sorting the map
			Map<String,Double> tmp;
			ValueComparatorDouble vc;
			TreeMap<String,Double> sortedTreeMap;
			String[] exampleList = db.getExamplesStudy();
			Collections.shuffle(Arrays.asList(exampleList)); //list is shuffled 
			int num = 0;
			ArrayList<String> baselineSimShuffledList;
			//evaluate non-personalized methods
			for (Method method : Method.values())
			{
				ratedQList = db.getUniqueRatedQuestions(new ArrayList<String> (pretestList));
				if (ratedQList.size() != 24)
					System.out.println("~~~~~SIZE UNEXPECTED: "+ ratedQList.size() +"!=24");
				for (String question : ratedQList)
				{
					if ( method.isInGroup(Method.Group.BASELINE))
					{
						num++;
						simMap = ContentSim.calculateSim(question,exampleList,method,null,null,null);
						//randomizing the examples
						baselineSimShuffledList = new ArrayList<String>(simMap.keySet());
						Collections.shuffle(baselineSimShuffledList);
						condensedSysRankMap = getTop3ExamplesInfo(question,simMap,new ArrayList<String>(baselineSimShuffledList));
						db.writeTop3SimScore(num,question,method,condensedSysRankMap);
					}
					if (method.isInGroup(Method.Group.STATIC))
					{
						num++;
						simMap = ContentSim.calculateSim(question,exampleList,method,null,null,null);
						//sorting the simMap
						tmp = new HashMap<String,Double>();
						vc = new ValueComparatorDouble(tmp);
						sortedTreeMap = new TreeMap<String,Double>(vc);		
						tmp.putAll(simMap);
						sortedTreeMap.putAll(tmp);
						//sortedTreeMap.keySet()  preserves the order
						condensedSysRankMap = getTop3ExamplesInfo(question,simMap,new ArrayList<String>(sortedTreeMap.keySet()));
						db.writeTop3SimScore(num,question,method,condensedSysRankMap);
					}
					else if (method.isInGroup(Method.Group.PERSONALZIED))
					{
						for (String pretest : pretestList)
						{
							conceptLevelMap = db.getKnowledgeLevels(pretest,question);	
							if (conceptLevelMap != null)
							{
								for (Map<String,Double> kmap : conceptLevelMap)
								{
									num++;
									simMap = ContentSim.calculateSim(question,exampleList,method,kmap,null,null);
									//sorting the simMap
									tmp = new HashMap<String,Double>();
									vc = new ValueComparatorDouble(tmp);
									sortedTreeMap = new TreeMap<String,Double>(vc);		
									tmp.putAll(simMap);
									sortedTreeMap.putAll(tmp);
									//sortedTreeMap.keySet()  preserves the order
									condensedSysRankMap = getTop3ExamplesInfo(question,simMap,new ArrayList<String>(sortedTreeMap.keySet()));
									db.writeTop3SimScore(num,question,method,condensedSysRankMap);
							     }
							}
						}
					}
				}										
			}			
		}
		else
		{
			System.out.println("System is not ready!");
		}
		db.close();
	}
	
	
	public static void evaluate(String domain, String ratingFileName, int all, String filterUsers){
		db = Data.getInstance();
		if (db.isReady())
        {	
			db.setup("",ratingFileName,all,"",filterUsers); 
			Set<String> pretestList = new HashSet<String>();
		    if (filterUsers.equals("user_weighted_kappa_threshold_high_gt_0.4.csv"))
		    	pretestList.add("High");
		    else
		    	pretestList = db.getPretestCategories();
			Map<Integer, Map<String, Integer>> condensedSysRankMap;	//Map<rank,Map<example,MajorityVotingRating>>
			Map<String, Double> condensedSimScoreMap;	//Map<example,similarityScore>

			Map<Integer, Map<String, Integer>> IdealRankMap;
			Map<String,Double> simMap;
			Set<String> ratedQList;
			Set<Map<String, Double>> conceptLevelMap;
			int totalRelevantExample;
			double AP,nDCG,QMeasure,RMSE;
			//params for sorting the map
			Map<String,Double> tmp;
			ValueComparatorDouble vc;
			TreeMap<String,Double> sortedTreeMap;
			String[] exampleList = db.getExamplesStudy();
			Collections.shuffle(Arrays.asList(exampleList)); //list is shuffled 
			ArrayList<String> baselineSimShuffledList;
			//evaluate non-personalized methods
			for (Method method : Method.values())
			{
				for (String pretest : pretestList)
				{	
					ratedQList = db.getRatedQuestions(pretest);
					for (String question : ratedQList)
					{
						if ( method.isInGroup(Method.Group.BASELINE))
						{
							simMap = ContentSim.calculateSim(question,exampleList,method,null,null,null);
							//randomizing the examples
							baselineSimShuffledList = new ArrayList<String>(simMap.keySet());
							Collections.shuffle(baselineSimShuffledList);
							totalRelevantExample = db.getRelevantExampleList(pretest, question).size();
							condensedSysRankMap = getCondensedListTop3(question,pretest,new ArrayList<String>(baselineSimShuffledList));
							IdealRankMap = getIdealRanking(condensedSysRankMap);
							AP = getAP(condensedSysRankMap,totalRelevantExample);
							nDCG = getNDCG(condensedSysRankMap, IdealRankMap);
							QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap, totalRelevantExample);
							RMSE = getRMSEBaseline(baselineSimShuffledList,condensedSysRankMap,method);//sim for all examples is considered as 0.5
							db.writeToFile(question,pretest,method,AP,nDCG,QMeasure,RMSE);
						}
						if (method.isInGroup(Method.Group.STATIC))
						{
							simMap = ContentSim.calculateSim(question,exampleList,method,null,null,null);
							//sorting the simMap
							tmp = new HashMap<String,Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String,Double>(vc);		
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							//sortedTreeMap.keySet()  preserves the order
							condensedSysRankMap = getCondensedListTop3(question,pretest,new ArrayList<String>(sortedTreeMap.keySet()));
							condensedSimScoreMap = getcondensedSimScoreList(question,pretest, simMap);
							IdealRankMap = getIdealRanking(condensedSysRankMap);
							totalRelevantExample = db.getRelevantExampleList(pretest, question).size();
							AP = getAP(condensedSysRankMap,totalRelevantExample);
							nDCG = getNDCG(condensedSysRankMap, IdealRankMap);
							QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap, totalRelevantExample);
							RMSE = getRMSE(condensedSimScoreMap,condensedSysRankMap,method);
							db.writeToFile(question,pretest,method,AP,nDCG,QMeasure,RMSE);
						}
						else if (method.isInGroup(Method.Group.PERSONALZIED) )
						{
							conceptLevelMap = db.getKnowledgeLevels(pretest,question);	
							for (Map<String,Double> kmap : conceptLevelMap)
							{
								simMap = ContentSim.calculateSim(question,exampleList,method,kmap,null,null);
								//sorting the simMap
								tmp = new HashMap<String,Double>();
								vc = new ValueComparatorDouble(tmp);
								sortedTreeMap = new TreeMap<String,Double>(vc);		
								tmp.putAll(simMap);
								sortedTreeMap.putAll(tmp);
								//sortedTreeMap.keySet()  preserves the order
								condensedSysRankMap = getCondensedListTop3(question,pretest,new ArrayList<String>(sortedTreeMap.keySet()));
								condensedSimScoreMap = getcondensedSimScoreList(question,pretest, simMap);
								IdealRankMap = getIdealRanking(condensedSysRankMap);
								totalRelevantExample = db.getRelevantExampleList(pretest,question).size();
								AP = getAP(condensedSysRankMap,totalRelevantExample);
								nDCG = getNDCG(condensedSysRankMap,IdealRankMap);
								QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap,totalRelevantExample);
								RMSE = getRMSE(condensedSimScoreMap,condensedSysRankMap,method);
								db.writeToFile(question,pretest,method,AP,nDCG,QMeasure,RMSE);						
							}
						}
					}										
				}
			}			
		}
		else
		{
			System.out.println("System is not ready!");
		}
		db.close();
	}
	

	private static double getRMSE(Map<String, Double> condensedSimScoreMap,
			Map<Integer, Map<String, Integer>> condensedSysRankMap, Method method) {
		double ss = 0.0;
		double sim,majorityVotingRate;
		double normalizedSim = 0;
		double normalizedMajorityVotingRate = 0.0;
		for (String example : condensedSimScoreMap.keySet())
		{
			sim = condensedSimScoreMap.get(example);
			majorityVotingRate = getMajorityVoting(example,condensedSysRankMap);
			if (majorityVotingRate == -1)
			{
				//System.out.println("UNEXPECTED ERROR : NO MAJORITY VOTING FOR THE EXAMPLE: "+example);
				continue;
			}
			normalizedMajorityVotingRate = normalizeVoting(majorityVotingRate);
			if (Arrays.asList(api.Constants.NORMALIZED_METHODS).contains(method) == false){//you should normalize the similarity score, it is between -1 and 1.
				normalizedSim = normalizedSim(sim);
			}
			else {
				normalizedSim = sim; //the similarity is already between 0,1.
			}
			ss += Math.pow((normalizedSim-normalizedMajorityVotingRate),2);
		}
		double mse = ss/condensedSimScoreMap.size();
		double rmse = Math.sqrt(mse);
		return rmse;
	}

	
	private static double getRMSEBaseline(List<String> baselineSimShuffledList,Map<Integer, Map<String, Integer>> condensedSysRankMap,
			                              Method method) {
		double ss = 0.0;
		double sim,majorityVotingRate;
		double normalizedSim = 0;
		double normalizedMajorityVotingRate = 0.0;
		int itemsToCountInRMSE = 0;
		for (String example : baselineSimShuffledList)
		{
			sim = 0.5;
			majorityVotingRate = getMajorityVoting(example,condensedSysRankMap);
			if (majorityVotingRate == -1)
			{
				//System.out.println("UNEXPECTED ERROR : NO MAJORITY VOTING FOR THE EXAMPLE: "+example);
				continue;
			}
			normalizedMajorityVotingRate = normalizeVoting(majorityVotingRate);
			if (Arrays.asList(api.Constants.NORMALIZED_METHODS).contains(method) == false){//you should normalize the similarity score, it is between -1 and 1.
				normalizedSim = normalizedSim(sim);
			}
			else {
				normalizedSim = sim; //the similarity is already between 0,1.
			}
			ss += Math.pow((normalizedSim-normalizedMajorityVotingRate),2);
			itemsToCountInRMSE ++;
		}
		double mse = ss/itemsToCountInRMSE;
		double rmse = Math.sqrt(mse);
		return rmse;
	}

	
	/*
	 * returns the normalized value for the majority voting based on the gains
	 */
	private static double normalizeVoting(double majorityVotingRate) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i : api.Constants.RATINGS)
		{
			if (i < min)
				min = i;
			if (i > max)
				max = i;
		}		
		return (majorityVotingRate-min)/(max-min);
	}

	private static int getMajorityVoting(String example,
			Map<Integer, Map<String, Integer>> condensedSysRankMap) {
		for (Map<String,Integer> map : condensedSysRankMap.values())
		{
			if (map.containsKey(example))
			{
				return map.get(example);
			}
		}
		return -1;
	}

	/*
	 * returns a normalized score for the given similarity value
	 */
	private static double normalizedSim(double sim) {
		double max = api.Constants.ASSOCIATION_COEFF_SIM_MAX_VALUE;
		double min = api.Constants.ASSOCIATION_COEFF_SIM_MIN_VALUE;
        double n = (sim - min)/(max-min);
		return n;
	}

	/*
	 * returns a condensed map for examples being rated with example as the key and the similarity score as the value
	 */
	private static Map<String, Double> getcondensedSimScoreList(
			String question, String pretest, Map<String, Double> simMap) {
		
		Map<String,Double> condensedSimScoreMap = new HashMap<String,Double>();
		for (String example : simMap.keySet())
		{
			if (db.isJudged(question,example,pretest) == true)
			{				
				condensedSimScoreMap.put(example,simMap.get(example));
			}			
		}
		return condensedSimScoreMap;
	}

	private static double getQMeasure(
			Map<Integer, Map<String, Integer>> condensedSysRankMap,
			Map<Integer, Map<String, Integer>> idealRankMap, int totalRelevantExample) {
		double BR = 0,QMeasure;
		if (totalRelevantExample == 0)
			QMeasure = 0;
		else{
			Map<Integer, Map<String, Integer>> cSub, ISub;		
			for (int rank : condensedSysRankMap.keySet())
			{
				if (isRelevant(rank,condensedSysRankMap) == false)
					BR += 0;
				else
				{
					cSub = getSubMap(condensedSysRankMap,rank);
					ISub = getSubMap(idealRankMap,rank);
					BR = BR + (double)(cg(cSub)+ getCountRelevantExamples(rank,cSub))/(double)(cg(ISub)+rank);
				}
			}
			QMeasure = BR/totalRelevantExample;
		}		
		return QMeasure;
	}

	//test
	public static int cg(Map<Integer, Map<String, Integer>> subMap) {
	    int cg = 0;
		for (int i : subMap.keySet())
			cg += getGain((Integer) subMap.get(i).values().toArray()[0]);
		return cg;
	}

	public static Map<Integer, Map<String, Integer>> getSubMap(
			Map<Integer, Map<String, Integer>> map, int rank) {
		Map<Integer, Map<String, Integer>> subMap = new HashMap<Integer, Map<String, Integer>>();
		for (int i : map.keySet())
		{
			if (i <= rank)
				subMap.put(i, map.get(i));	// we should iterate through all elements in the map, because HashMap is not sorted			
		}
		return subMap;
	}

	private static double getNDCG(
			Map<Integer, Map<String, Integer>> condensedSysRankMap,
			Map<Integer, Map<String, Integer>> idealRankMap) {
		double dg = 0, dgI = 0, nDCG;
		int rate = 0, rateI = 0;
		if (condensedSysRankMap.size()!=idealRankMap.size())
			System.out.println("~~~~~~ ERROR in getNDCG(): size condensedSysRankMap != idealRankMap");
		//this part is changed in second revision of ijaied to due to bug in ndcg method
		for (int rank : condensedSysRankMap.keySet())
		{
			rate = (Integer) condensedSysRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
			dg += dg(rank,rate);
		}
		for (int rank : idealRankMap.keySet())
		{
			rateI = (Integer) idealRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
			dgI += dg(rank,rateI);
		}
		if (dg == 0.0 && dgI == 0.0) //all docs are not helpful
			nDCG = 0;
		else
			nDCG = dg/dgI;
		return nDCG;
	}

	private static double dg(int rank, int rate) {
		int gain = getGain(rate);
		double dg = 0.0;
		if (rank <= api.Constants.nDCG_LOG_BASE)
			dg = gain;
		else 
		{
			double log2 = Math.log10(rank)/Math.log10(2);
			dg = gain/log2;
		}
		return dg;
	}

	private static int getGain(int rating) {
		int Nrating = 0;
		switch (rating)
        {
		  case 0:
			  	Nrating = api.Constants.NOT_HELPFUL_AT_ALL_GAIN; //not helpful at all
			  	break;
		  case 1:
				Nrating = api.Constants.NOT_HELPFUL_GAIN; //not helpful 
				break;
		  case 2:
				Nrating = api.Constants.HELPFUL_GAIN; //helpful 
				break;
		  case 3:
				Nrating = api.Constants.VERY_HELPFUL_GAIN; //very helpful 
				break;
		  default: 
			    break;
		}		
		return Nrating;
	}
	
	private static double getAP(Map<Integer, Map<String, Integer>> condensedSysRankMap,int totalRelevantExamples) {
		double sum = 0;
		double AP;
//		if (totalRelevantExamples >condensedSysRankMap.size())
//			System.out.println("~~~~~~AP: totalRelevantExamples >condensedSysRankMap.size()");
		if (totalRelevantExamples != 0)
		{
			for (int rank : condensedSysRankMap.keySet())  //we need to go over all rank to check if our ranking is good
			{
				if (isRelevant(rank,condensedSysRankMap) == false)
					sum += 0;
				else
					sum = sum + (double)getCountRelevantExamples(rank,condensedSysRankMap)/(double)rank;
			}
			AP = sum/totalRelevantExamples;		
		}
		else
			AP = 0;
		
		return AP;
	}

	private static boolean isRelevant(int rank,Map<Integer, Map<String, Integer>> condensedSysRankMap) {
		Map<String, Integer> rMap = condensedSysRankMap.get(rank);
		//rMap has only one <key,value>
		for (Integer rate : rMap.values())
		{
			if (rate >= api.Constants.RELEVANEC_THRESHOLD)
				return true;
		}
		return false;
	}

	private static int getCountRelevantExamples(int rank,Map<Integer, Map<String, Integer>> condensedSysRankMap) {
		int count = 0;
		for (int i : condensedSysRankMap.keySet()) // we need for loop because HashMap is not sorted.
		{
			if (i <= rank)
			{
				if (isRelevant(i,condensedSysRankMap) == true)
					count++;
			}
		}
		return count;
	}
	
	/*
	 * idealList is a map with key:rank and value:map<key:example;value:MajorityVotingRate>. 
	 * Example,MajorityRating pairs are sorted descendingly with the example with highest ratings at the top.
	 * rankings are then determined based on the sorted example,rating pairs. 
	 */
	private static Map<Integer, Map<String, Integer>> getIdealRanking(Map<Integer, Map<String, Integer>> condensedSysRankMap) {
		Map<String,Integer> tmp = new HashMap<String,Integer>();
		ValueComparatorInteger vc = new ValueComparatorInteger(tmp);
		TreeMap<String,Integer> sortedTreeMap = new TreeMap<String,Integer>(vc);
		for (Map<String,Integer> rMap : condensedSysRankMap.values())
			tmp.putAll(rMap);
		sortedTreeMap.putAll(tmp);
		Map<Integer,Map<String,Integer>> sortedRankMap = new HashMap<Integer,Map<String,Integer>>();
		int irank = 0;
		for (Entry<String,Integer> entry : sortedTreeMap.entrySet())
		{
			irank++;
			Map<String,Integer> ratingMap = new HashMap<String,Integer>();
			ratingMap.put(entry.getKey(), entry.getValue());
			sortedRankMap.put(irank,ratingMap);
		}
		return sortedRankMap;
	}
	
	/*
	 * returns a map with top-3 examples ranked by approach. map<key:example;value:similarityValue>
	 */
	private static Map<String, Double> getTop3ExamplesInfo(String question,Map<String, Double> simMap, List<String> orderedSet) {
		int rank = 0;
		Map<String,Double> sortedRankMap = new HashMap<String,Double>();
		for (String example : orderedSet)
        {
			rank++;// gosh this was misplaced for the prelim/first revision
					// ijaied inside if and it always had ranks 1,2,3,4,5,... no
					// matter what was the order of the example in the list
			if (rank > 3)
				break;
			sortedRankMap.put(example, simMap.get(example));
		}
		return sortedRankMap;
	}


	/*
	 * condensedlist is a map with key:rank and value:map<key:example;value:MajorityVoting>
	 */
	private static Map<Integer, Map<String, Integer>> getCondensedList(String question,String pretest, List<String> orderedSet) {
		int rank = 0;
		Map<Integer,Map<String,Integer>> sortedRankMap = new HashMap<Integer,Map<String,Integer>>();
		List<Integer> ratingList;
		for (String example : orderedSet)
        {
			rank++;// gosh this was misplaced for the prelim/first revision
					// ijaied inside if and it always had ranks 1,2,3,4,5,... no
					// matter what was the order of the example in the list
			if (db.isJudged(question, example, pretest) == true) {
				Map<String, Integer> ratingMap = new HashMap<String, Integer>();
				ratingList = db.getRatingList(question, example, pretest);
				ratingMap.put(example, db.aggregateJudges(ratingList));
				sortedRankMap.put(rank, ratingMap);
			}

		}
		return sortedRankMap;
	}

	/*
	 * condensedlist is a map with key:rank and value:map<key:example;value:MajorityVoting>
	 */
	private static Map<Integer, Map<String, Integer>> getCondensedListTop3(String question,String pretest, List<String> orderedSet) {
		int rank = 0;
		Map<Integer,Map<String,Integer>> sortedRankMap = new HashMap<Integer,Map<String,Integer>>();
		List<Integer> ratingList;
		for (String example : orderedSet)
        {
			rank++;// gosh this was misplaced for the prelim/first revision
					// ijaied inside if and it always had ranks 1,2,3,4,5,... no
					// matter what was the order of the example in the list
			if (rank <= 3){
				if (db.isJudged(question, example, pretest) == true) {
					Map<String, Integer> ratingMap = new HashMap<String, Integer>();
					ratingList = db.getRatingList(question, example, pretest);
					ratingMap.put(example, db.aggregateJudges(ratingList));
					sortedRankMap.put(rank, ratingMap);
				}
			}
		}
		return sortedRankMap;
	}

	private static class ValueComparatorInteger implements Comparator<String> {
	    Map<String, Integer> base;
	    public ValueComparatorInteger(Map<String, Integer> base) {
	        this.base = base;
	    }
	    // Note: this comparator sorts the values descendingly, so that the best activity is in the first element.
	    public int compare(String a, String b) {
	    	if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // 
	    } // returning 0 would merge keys	   
	}
	
	private static class ValueComparatorDouble implements Comparator<String> {

	    Map<String, Double> base;
	    public ValueComparatorDouble(Map<String, Double> base) {
	        this.base = base;
	    }

	    // Note: this comparator sorts the values descendingly, so that the best activity is in the first element.
	    public int compare(String a, String b) {
	    	if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // 
	    } // returning 0 would merge keys	   
	}
	
	
	public static void propRatingForMethods(String domain, String ratingFileName, int all, String filterUsers){
		db = Data.getInstance();
		if (db.isReady())
        {	
			db.setup("",ratingFileName,all,"",filterUsers); 
			Set<String> pretestList = db.getPretestCategories();
			Map<String, Integer> judgedTop3ExList;	

			Map<String,Double> simMap;
			Set<String> ratedQList;
			Set<Map<String, Double>> conceptLevelMap;
			String[] exampleList = db.getExamplesStudy();
			Collections.shuffle(Arrays.asList(exampleList)); //list is shuffled 
			//params for sorting the map
			Map<String,Double> tmp;
			ValueComparatorDouble vc;
			TreeMap<String,Double> sortedTreeMap;
			List<String> top3ex;
			List<String> shuffletmp;
			//evaluate non-personalized methods
			for (Method method : Method.values())
			{
				for (String pretest : pretestList)
				{	
					ratedQList = db.getRatedQuestions(pretest);
					for (String question : ratedQList)
					{
						if ( method.isInGroup(Method.Group.BASELINE))
						{
							simMap = ContentSim.calculateSim(question,exampleList,method,null,null,null);
							shuffletmp = new ArrayList<String>(simMap.keySet());
							Collections.shuffle(shuffletmp);
							top3ex = getTop3Ex(shuffletmp);
							judgedTop3ExList = getJudgedTop3Examples(question,pretest,top3ex);
							db.writePropRatingsForMethods(question,judgedTop3ExList,pretest,method);
						}
						if (method.isInGroup(Method.Group.STATIC))
						{
							simMap = ContentSim.calculateSim(question,exampleList,method,null,null,null);
							//sorting the simMap
							tmp = new HashMap<String,Double>();
							vc = new ValueComparatorDouble(tmp);
							sortedTreeMap = new TreeMap<String,Double>(vc);		
							tmp.putAll(simMap);
							sortedTreeMap.putAll(tmp);
							//sortedTreeMap.keySet()  preserves the order
							top3ex = getTop3Ex(new ArrayList<String>(sortedTreeMap.keySet()));
							judgedTop3ExList = getJudgedTop3Examples(question,pretest,top3ex);
							db.writePropRatingsForMethods(question,judgedTop3ExList,pretest,method);
						}
						else if (method.isInGroup(Method.Group.PERSONALZIED))
						{
							conceptLevelMap = db.getKnowledgeLevels(pretest,question);	
							for (Map<String,Double> kmap : conceptLevelMap)
							{
								simMap = ContentSim.calculateSim(question,exampleList,method,kmap,null,null);
								//sorting the simMap
								tmp = new HashMap<String,Double>();
								vc = new ValueComparatorDouble(tmp);
								sortedTreeMap = new TreeMap<String,Double>(vc);		
								tmp.putAll(simMap);
								sortedTreeMap.putAll(tmp);
								//sortedTreeMap.keySet()  preserves the order
								top3ex = getTop3Ex(new ArrayList<String>(sortedTreeMap.keySet()));
								judgedTop3ExList = getJudgedTop3Examples(question,pretest,top3ex);
								db.writePropRatingsForMethods(question,judgedTop3ExList,pretest,method);
							}
						}
					}										
				}
			}			
		}
		else
		{
			System.out.println("System is not ready!");
		}
		db.close();
	}


	private static List<String> getTop3Ex(List<String> input) {
		List<String> list = new ArrayList<String>();
		int i = 0;
		for (String s : input)
		{
			if (i >= 3)
				break;
			else{
				list.add(s);
				i++;
			}
		}
		return list;
	}


	private static Map<String,Integer> getJudgedTop3Examples(String question,String pretest, List<String> orderedSet) {
		Map<String,Integer> ratedExamplesMap = new HashMap<String,Integer>();
		List<Integer> ratingList;
		for (String example : orderedSet)
		{
			if (db.isJudged(question,example,pretest) == true)
			{
				ratingList = db.getRatingList(question, example, pretest);
				ratedExamplesMap.put(example,db.aggregateJudges(ratingList));
			}			
		}
		return ratedExamplesMap;
	}

	
	
	
}
