import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import api.Constants.Method;

public class EvaluationSim {

	private static Data db;
	
	
	public static void main(String[] args){
		db = Data.getInstance();
		if (db.isReady())
        {	
			db.setup();
			Set<String> pretestList = db.getPretestCategories();
			Map<Integer, Map<String, Integer>> condensedSysRankMap;	//Map<rank,Map<example,MajorityVotingRating>>
			Map<String, Double> condensedSimScoreMap;	//Map<example,similarityScore>

			Map<Integer, Map<String, Integer>> IdealRankMap;
			Map<String,Double> simMap;
			Set<String> ratedQList;
			Set<Map<String, Double>> conceptLevelMap;
			int totalRelevantExample;
			double AP,nDCG,QMeasure,RMSE;
			//evaluate non-personalized methods
			for (Method method : Method.values())
			{
				for (String pretest : pretestList)
				{	
					ratedQList = db.getRatedQuestions(pretest);
					for (String question : ratedQList)
					{
						if (method.isInGroup(Method.Group.STATIC))
						{
							simMap = ContentSim.calculateStaticSim(question,db.getExamples(),method,null);
							condensedSysRankMap = getCondensedList(question,pretest, simMap);
							condensedSimScoreMap = getcondensedSimScoreList(question,pretest, simMap);
							IdealRankMap = getIdealRanking(condensedSysRankMap);
							totalRelevantExample = db.getRelevantExampleList(pretest, question).size();
							AP = getAP(condensedSysRankMap,totalRelevantExample);
							nDCG = getNDCG(condensedSysRankMap, IdealRankMap);
							QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap, totalRelevantExample);
							RMSE = getRMSE(condensedSimScoreMap,condensedSysRankMap,method);
							db.writeToFile(question,pretest,method.toString(),AP,nDCG,QMeasure,RMSE);
						}
						else if (method.isInGroup(Method.Group.PERSONALZIED))
						{
							conceptLevelMap = db.getKnowledgeLevels(pretest,question);	
							for (Map<String,Double> kmap : conceptLevelMap)
							{
								simMap = ContentSim.calculateStaticSim(question,db.getExamples(),method,kmap);
								condensedSysRankMap = getCondensedList(question,pretest,simMap);
								condensedSimScoreMap = getcondensedSimScoreList(question,pretest, simMap);
								IdealRankMap = getIdealRanking(condensedSysRankMap);
								totalRelevantExample = db.getRelevantExampleList(pretest,question).size();
								AP = getAP(condensedSysRankMap,totalRelevantExample);
								nDCG = getNDCG(condensedSysRankMap,IdealRankMap);
								QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap,totalRelevantExample);
								RMSE = getRMSE(condensedSimScoreMap,condensedSysRankMap,method);
								db.writeToFile(question,pretest,method.toString(),AP,nDCG,QMeasure,RMSE);						
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
				System.out.println("UNEXPECTED ERROR : NO MAJORITY VOTING FOR THE EXAMPLE: "+example);
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

	/*
	 * returns the normalized value for the majority voting based on the gains
	 */
	private static double normalizeVoting(double majorityVotingRate) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i : api.Constants.GAINS)
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
			cg += (Integer) subMap.get(i).values().toArray()[0];
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
		double dg = 0, dgI = 0, gain = 0, gainI = 0,nDCG;
		for (int rank : condensedSysRankMap.keySet())
		{
			gain = (Integer) condensedSysRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
			gainI = (Integer) idealRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
			dg += dg(rank,gain);
			dgI += dg(rank,gainI);
		}
		if (dg == 0.0 && dgI == 0.0) //all docs are not helpful
			nDCG = 0;
		else
			nDCG = dg/dgI;
		return nDCG;
	}

	private static double dg(int rank, double gain) {
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

	private static double getAP(Map<Integer, Map<String, Integer>> condensedSysRankMap,int totalRelevantExamples) {
		double sum = 0;
		double AP;
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
				if (isRelevant(rank,condensedSysRankMap) == true)
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
	 * condensedlist is a map with key:rank and value:map<key:example;value:MajorityVoting>
	 */
	private static Map<Integer, Map<String, Integer>> getCondensedList(String question,String pretest, Map<String, Double> simMap) {
		Map<String,Double> tmp = new HashMap<String,Double>();
		ValueComparatorDouble vc = new ValueComparatorDouble(tmp);
		TreeMap<String,Double> sortedTreeMap = new TreeMap<String,Double>(vc);		
		
		tmp.putAll(simMap);
		sortedTreeMap.putAll(tmp);
		int rank = 0;
		Map<Integer,Map<String,Integer>> sortedRankMap = new HashMap<Integer,Map<String,Integer>>();
		String example;	
		List<Integer> ratingList;
		for (Entry<String,Double> entry : sortedTreeMap.entrySet())
		{
			example = entry.getKey();
			if (db.isJudged(question,example,pretest) == true)
			{
				rank ++;
				Map<String,Integer> ratingMap = new HashMap<String,Integer>();
				ratingList = db.getRatingList(question, example, pretest);
				ratingMap.put(example,db.aggregateJudges(ratingList));
				sortedRankMap.put(rank, ratingMap);
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
}
