import java.util.Comparator;
import java.util.HashMap;
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
			Map<Integer, Map<String, Double>> condensedSysRankMap;	// Map<rank,Map<example,avgRating>>	
			Map<Integer, Map<String, Double>> IdealRankMap;
			Set<String> ratedQList;
			Set<Map<String, Double>> conceptLevelMap;
			int totalRelevantExample;
			double AP,nDCG,QMeasure;
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
							condensedSysRankMap = getCondensedList(question,pretest, method, null);
							IdealRankMap = getIdealRanking(condensedSysRankMap);
							totalRelevantExample = db.getRelevantExampleList(pretest, question).size();
							AP = getAP(condensedSysRankMap,totalRelevantExample);
							nDCG = getNDCG(condensedSysRankMap, IdealRankMap);
							QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap, totalRelevantExample);
							db.writeToFile(question,pretest,method.toString(),AP,nDCG,QMeasure);
						}
						else if (method.isInGroup(Method.Group.PERSONALZIED))
						{
							conceptLevelMap = db.getKnowledgeLevels(pretest,question);	
							for (Map<String,Double> kmap : conceptLevelMap)
							{
								condensedSysRankMap = getCondensedList(question,pretest,method,kmap);
								IdealRankMap = getIdealRanking(condensedSysRankMap);
								totalRelevantExample = db.getRelevantExampleList(pretest,question).size();
								AP = getAP(condensedSysRankMap,totalRelevantExample);
								nDCG = getNDCG(condensedSysRankMap,IdealRankMap);
								QMeasure = getQMeasure(condensedSysRankMap,IdealRankMap,totalRelevantExample);
								db.writeToFile(question,pretest,method.toString(),AP,nDCG,QMeasure);						
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
	
	private static double getQMeasure(
			Map<Integer, Map<String, Double>> condensedSysRankMap,
			Map<Integer, Map<String, Double>> idealRankMap, int totalRelevantExample) {
		double BR = 0;
		Map<Integer, Map<String, Double>> cSub, ISub;		
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
		double QMeasure = BR/totalRelevantExample;
		return QMeasure;
	}

	//(test
	public static int cg(Map<Integer, Map<String, Double>> subMap) {
	    int cg = 0;
		for (int i : subMap.keySet())
			cg += (Double) subMap.get(i).values().toArray()[0];
		return cg;
	}

	public static Map<Integer, Map<String, Double>> getSubMap(
			Map<Integer, Map<String, Double>> map, int rank) {
		Map<Integer, Map<String, Double>> subMap = new HashMap<Integer, Map<String, Double>>();
		for (int i : map.keySet())
		{
			if (i <= rank)
				subMap.put(i, map.get(i));			
			else
				break;
		}
		return subMap;
	}

	private static double getNDCG(
			Map<Integer, Map<String, Double>> condensedSysRankMap,
			Map<Integer, Map<String, Double>> idealRankMap) {
		double dg = 0, dgI = 0, gain = 0, gainI = 0;
		for (int rank : condensedSysRankMap.keySet())
		{
			gain = (Double) condensedSysRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
			gainI = (Double) idealRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
			dg += dg(rank,gain);
			dgI += dg(rank,gainI);
		}
		double nDCG = dg/dgI;
		return nDCG;
	}

	private static double dg(int rank, double gain) {
		double dg = 0.0;
		if (rank <= api.Constants.nDCG_LOG_BASE)
			dg = gain;
		else 
		{
			double log2 = Math.log10(gain)/Math.log10(2);
			dg = gain/log2;
		}
		return dg;
	}

	private static double getAP(Map<Integer, Map<String, Double>> condensedSysRankMap,int totalRelevantExamples) {
		double sum = 0;
		for (int rank : condensedSysRankMap.keySet())
		{
			if (isRelevant(rank,condensedSysRankMap) == false)
				sum += 0;
			else
				sum = sum + (double)getCountRelevantExamples(rank,condensedSysRankMap)/(double)rank;
		}
		double AP = sum/totalRelevantExamples;
		return AP;
	}

	private static boolean isRelevant(double rank,Map<Integer, Map<String, Double>> condensedSysRankMap) {
		double rate = (Double) condensedSysRankMap.get(rank).values().toArray()[0]; //only 1 element in the map
		return (rate > 0);
	}

	private static int getCountRelevantExamples(int rank,Map<Integer, Map<String, Double>> condensedSysRankMap) {
		int count = 0;
		for (int i : condensedSysRankMap.keySet())
		{
			if (i <= rank)
			{
				if (isRelevant(rank,condensedSysRankMap) == true)
					count++;
			}
			else
				break;
		}
		return count;
	}

	private static Map<Integer, Map<String, Double>> getIdealRanking(Map<Integer, Map<String, Double>> condensedSysRankMap) {
		Map<String,Double> tmp = new HashMap<String,Double>();
		ValueComparator vc = new ValueComparator(tmp);
		TreeMap<String,Double> sortedTreeMap = new TreeMap<String,Double>(vc);
		tmp.putAll((Map<String, Double>)condensedSysRankMap.values());
		sortedTreeMap.putAll(tmp);
		Map<Integer,Map<String,Double>> sortedRankMap = new HashMap<Integer,Map<String,Double>>();
		int irank = 0;
		for (Entry<String,Double> entry : sortedTreeMap.entrySet())
		{
			irank++;
			Map<String,Double> ratingMap = new HashMap<String,Double>();
			ratingMap.put(entry.getKey(), entry.getValue());
			sortedRankMap.put(irank,ratingMap);
		}
		return sortedRankMap;
	}

	private static Map<Integer, Map<String, Double>> getCondensedList(String question,String pretest, Method method, Map<String, Double> kmap) {
		Map<String,Double> tmp = new HashMap<String,Double>();
		ValueComparator vc = new ValueComparator(tmp);
		TreeMap<String,Double> sortedTreeMap = new TreeMap<String,Double>(vc);		
		tmp.putAll(ContentSim.calculateStaticSim(question,db.getExamples(),method,kmap));
		sortedTreeMap.putAll(tmp);
		int rank = 0;
		Map<Integer,Map<String,Double>> sortedRankMap = new HashMap<Integer,Map<String,Double>>();
		String example;
		
		for (Entry<String,Double> entry : sortedTreeMap.entrySet())
		{
			example = entry.getKey();
			if (db.isJudged(question,example,pretest) == true)
			{
				rank ++;
				Map<String,Double> ratingMap = new HashMap<String,Double>();
				ratingMap.put(example, db.getAvgRate(question, example, pretest));
				sortedRankMap.put(rank, ratingMap);
			}			
		}
		return sortedRankMap;
	}

	private static class ValueComparator implements Comparator<String> {

	    Map<String, Double> base;
	    public ValueComparator(Map<String, Double> base) {
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
