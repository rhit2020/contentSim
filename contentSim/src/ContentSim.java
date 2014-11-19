import headliner.treedistance.ComparisonZhangShasha;
import headliner.treedistance.CreateTreeHelper;
import headliner.treedistance.OpsZhangShasha;
import headliner.treedistance.Transformation;
import headliner.treedistance.TreeDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import api.Constants.Method;;

public class ContentSim {

	static List<String>  contentNotInStartEndLineFile = new ArrayList<String>();
	private static Data db;
	public static void calculateSim(String ratingFileName, String contentversion){
		
		db = Data.getInstance();
		// **** for test **** START
//		String[] qList = {"jArray2"};	
//		String[] eList = {"inheritance1_v2"};
		// **** for test **** END
		if (db.isReady())
        {	
			db.setup(ratingFileName,0,contentversion);
			String[] eList = db.getExamples();
			String[] qList = db.getQuestions();
			HashMap<String,Double> rankMap;

			for (Method method : Method.values())
			{
				if (method.isInGroup(Method.Group.STATIC))
				{
					for (String q : qList)
					{					
						rankMap = calculateSim(q, eList,method,null);
						for (String e : rankMap.keySet())
							db.insertContentSim(q, e, rankMap.get(e), method.toString());
					}
				}
				
			}
		}
		else
		{
			System.out.println("System is not ready!");
		}
		System.out.println("Finished! #contentNotInStartEndLineFile:"+contentNotInStartEndLineFile.size());
		db.close();
	}

	/*
	 * returns a map, with keys as example and values as the calculated similarity value
	 */
	public static HashMap<String, Double> calculateSim(String q, String[] eList, Method method, Map<String, Double> kmap) {
		HashMap<String,Double> rankMap = new HashMap<String,Double>();
		List<String> qConcepts = null;
		List<String> eConcepts = null;
		Map<String,Double> qConceptWeight = null;
		Map<String,Double> eConceptWeight = null;
		List<ArrayList<String>> qtree = null;
		List<ArrayList<String>> etree = null;
		double sim = 0.0;
		if (db == null)
			db = Data.getInstance(); //the singleton instance
		//creating list of concepts in question
		qConcepts = db.getConcepts(q);
		//TFIDF values used as weight of concepts in question
		qConceptWeight = db.getTFIDF(q);
		//subtrees in question
		qtree = getSubtrees(q);
		List<String> qTopicList = db.getTopic(q); //topic(s) of the question
		for (String e : eList) {
			sim = 0.0;
			//creating list of concepts in example
			eConcepts = db.getConcepts(e);
			//TFIDF values used as weight of concepts in example
			eConceptWeight = db.getTFIDF(e);
			//subtrees in example
			etree = getSubtrees(e);
			//calculate global similarity 				
			switch(method){
			//baseline method
			case RANDOM_BASELINE:
			{
				sim = 0.5; // the similarity of random model for each example is same random number 0.5.
				break;
			}
			case NAIVE_LOCAL:
			{
				sim = calStructuralContentSim(q, e);
				break;
			}
			//static methods
			case GLOBAL_AS:
			{
				sim = simAssociationCoefficient(qConcepts,eConcepts,method,kmap,null); 
				break;
			}
			case GLOBAL_COS:
			{
				sim = simCosine(qConcepts,eConcepts,qConceptWeight,eConceptWeight,method,kmap,null,q); 
				break;
			}
			case LOCAL_AS:
			{
				sim = localSim(qtree,etree,"AS",null,null,method,kmap,null,q); 
				break;
			}
			case LOCAL_COS:
			{
				sim = localSim(qtree,etree,"COS",qConceptWeight,eConceptWeight,method,kmap,null,q); 
				break;
			}
			//personalized methods
			case P_GLOBAL_AS:
			{
				sim = simAssociationCoefficient(qConcepts,eConcepts,method,kmap,null); 	
				break;
			}
			case P_GLOBAL_COS:
			{
				sim = simCosine(qConcepts,eConcepts,qConceptWeight,eConceptWeight,method,kmap,null,q); 
				break;
			}
			case P_LOCAL_AS:
			{
				sim = localSim(qtree,etree,"AS",null,null,method,kmap,null,q); 
				break;
			}
			case P_LOCAL_COS:
			{
				sim = localSim(qtree,etree,"COS",qConceptWeight,eConceptWeight,method,kmap,null,q); 
				break;
			}
			//personalized with focus on current goal
			case P_LOCAL_AS_GOAL:
			{
				sim = localSim(qtree,etree,"AS",null,null,method,kmap,qTopicList,q); 
				break;
			}
			case P_LOCAL_COS_GOAL:
			{
				sim = localSim(qtree,etree,"COS",qConceptWeight,eConceptWeight,method,kmap,qTopicList,q); 
				break;
			}
			case P_GLOBAL_AS_GOAL:
			{
				sim = simAssociationCoefficient(qConcepts,eConcepts,method,kmap,qTopicList);
				break;
			}
			case P_GLOBAL_COS_GOAL:
			{
				sim = simCosine(qConcepts,eConcepts,qConceptWeight,eConceptWeight,method,kmap,qTopicList,q); 
				break;
			}
			default:
				break;								
			}
			rankMap.put(e, sim);
		}				
		return rankMap;
	}
	
	private static List<ArrayList<String>> getSubtrees(String content) {
		List<ArrayList<String>> subtreeList = new ArrayList<ArrayList<String>>();
		List<Integer> lines = db.getStartEndLine(content);
		if(lines == null)
		{
			if (contentNotInStartEndLineFile.contains(content)==false)
				contentNotInStartEndLineFile.add(content);			
		}
		else
		{
			int start = lines.get(0);
			int end = lines.get(1);
			ArrayList<String> subtree = null;
			List<String> adjucentConceptsList = null;
			for (int line = start; line <= end; line++)
			{
				//create subtree for concepts that are in the current line
				subtree = db.getConceptsInSameLine(content, line);
				if (updateSubtreeList(subtree,subtreeList) == true)
					subtreeList.add(subtree);	
				List<Integer> endLines = db.getEndLineBlock(content,line); //in case that line has a concept that has an end line different than the start line, endlines would be nonempty			
				for (int e : endLines)
				{
					//create subtree for the block
					subtree = new ArrayList<String>();
					adjucentConceptsList = db.getAdjacentConcept(content,line,e);
					Collections.sort(adjucentConceptsList, new SortByName());
					for (String adjcon : adjucentConceptsList)
						subtree.add(adjcon);				
					if (updateSubtreeList(subtree,subtreeList) == true)
						subtreeList.add(subtree);	
				}			
			}
		}			
		return subtreeList;
	}	

	private static boolean updateSubtreeList(ArrayList<String> subtree, List<ArrayList<String>> subtreeList) {
		if (subtree.isEmpty() == false && subtreeList.contains(subtree) == false)
			return true;
		return false;		
	}

	/*
	 * Return value ranges from -1 to 1. 
	 */
	private static double localSim(List<ArrayList<String>> qtree, List<ArrayList<String>> etree, 
			                       String variant, Map<String,Double> qConceptWeight, Map<String,Double> eConceptWeight, Method method, Map<String, Double> kmap, List<String> qTopicList, String q2)
	{
		double [][] s = new double[qtree.size()][etree.size()]; 
		int [][] alpha = new int[qtree.size()][etree.size()];	
		//initialize all elements of alpha to be 1
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
				alpha[i][j] = 1;
				
		//fill s
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				if (variant.equals("AS"))
				{
					s[i][j] = simAssociationCoefficient(qtree.get(i),etree.get(j),method,kmap,qTopicList);
				}
				else if (variant.equals("COS"))
				{
					s[i][j] = simCosine(qtree.get(i),etree.get(j),qConceptWeight,eConceptWeight,method,kmap,qTopicList,q2);
				}
			}
		//print(s);//print s[i][j]
		//fill alpha
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				if (alpha[i][j] != 0)
				{
					//set alpha 1 for this element
					alpha[i][j] = 1;
					//if s[i][j] is one, set alpha of other elements in the same row and column to 0. 
					if (s[i][j] == 1)
					{
						//set alpha 0 for other elements in the same row
						for (int e = 0; e < etree.size(); e++)
						{
							if (e!=j)
								alpha[i][e] = 0;
						}
						//set alpha 0 for other elements in the same column
						for (int q = 0; q < qtree.size(); q++)
						{
							if (q!=i)
								alpha[q][j] = 0;
						}
					}								
				}				
			}
		//print(alpha);//print alpha[i][j]
		double sim = 0.0;
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				sim += (alpha[i][j]*s[i][j]);
			}
		
		/*
		 * divide the sim by sum of weights to calculate weighted average
		 * sumOfWeights = qtree.size()*etree.size() when no alpha is 0
		 */
		double sumOfWeights = 0.0;
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)			
				sumOfWeights += alpha[i][j];
		
		sim = sim / sumOfWeights;		
		//release space
		s = null;
		alpha = null;
		return sim;
	}

	private static void print(int[][] alpha) {
		for (int i = 0; i < alpha.length; i++)
		{
			for(int j = 0; j < alpha[0].length; j++)
				System.out.print(String.format("%s ", alpha[i][j]));
			System.out.println();
		}			
	}

	private static void print(double[][] s) {
		for (int i = 0; i < s.length; i++)
		{
			for(int j = 0; j < s[0].length; j++)
				System.out.print(String.format("%.2f ", s[i][j]));
			System.out.println();
		}		
	}

	/* 
	 * Return value (cosine similarity) ranges between 0-1 since tfidf values are not negative.
	 */
	private static double simCosine(List<String> qConcepts, List<String> eConcepts, Map<String, Double> qConceptWeight, Map<String, Double> eConceptWeight, Method method, Map<String, Double> kmap, List<String> qTopicList,String q) {
		//create concept space by union of two sets. Set drops repeated elements and contains unique values
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		List<String> conceptSpace = new ArrayList<String>(union(qConceptSet, eConceptSet));
		HashMap<String,Double> evector = new HashMap<String,Double>();// concept vector for example
		HashMap<String,Double> qvector = new HashMap<String,Double>(); // concept vector for question
		Set<String> qTopicSet = (qTopicList!=null?new HashSet<String>(qTopicList):null);
		Set<String> conceptTopicSet;
		boolean isTargetConcept;
		double lackKnowledge;
		for (String c : conceptSpace)
		{
			if (method.isInGroup(api.Constants.Method.Group.STATIC) == true)
			{
				evector.put(c, eConceptSet.contains(c)?eConceptWeight.get(c):0);
				qvector.put(c, qConceptSet.contains(c)?qConceptWeight.get(c):0);				
			}
			else
			{				
				if (Arrays.asList(api.Constants.GOAL_BASED_METHODS).contains(method) == true)
				{
					if (kmap.get(c) == null)
						lackKnowledge = 0;
					else 
						lackKnowledge = 1-kmap.get(c);	
					//if the concept is in the target concepts of the topic weight of concept is non-zero, otherwise it is 0.
					conceptTopicSet = new HashSet<String>(db.getConceptTopic(c));
					isTargetConcept = (intersection(conceptTopicSet,qTopicSet).size()>0);
					evector.put(c, isTargetConcept && eConceptSet.contains(c)?lackKnowledge:0);
					qvector.put(c, isTargetConcept && qConceptSet.contains(c)?lackKnowledge:0);
				}
				else
				{
					if (kmap.get(c) == null)
						lackKnowledge = 0;
					else 
						lackKnowledge = 1-kmap.get(c);					
					evector.put(c, eConceptSet.contains(c)?lackKnowledge:0);
					qvector.put(c, qConceptSet.contains(c)?lackKnowledge:0);
				}	
			}				
		}
		double numerator = 0.0;
		double eDemoninator = 0.0;
		double qDenominator = 0.0;
		for (String c :  conceptSpace)
		{
			numerator += qvector.get(c) * evector.get(c);
			eDemoninator += Math.pow(evector.get(c), 2); //each element in the example vector is raised to the power of 2 
			qDenominator += Math.pow(qvector.get(c), 2); //each element in the example vector is raised to the power of 2 
		}
		//this is check for not getting NaN as sim
		//possible NaN cases: when for one vector all weights are 0 for the static method
		//for the goal based method, all concepts are not within target in both vector
		//for personalized method, user already knows all the concepts in either question vector
		//or the example vector, so this example-question pair has no gain for user.
		double sim;
		if (Math.sqrt(qDenominator)*Math.sqrt(eDemoninator) == 0.0)
			sim = 0.0;
		else 
			sim = numerator/(Math.sqrt(qDenominator)*Math.sqrt(eDemoninator)); //square root of the qDenominator/eDenominator
		return sim;	
	}

	/*
	 * Return value ranges from -1 to 1.
	 */
	private static double simAssociationCoefficient(List<String> qConcepts, List<String> eConcepts, Method method, Map<String, Double> kmap, List<String> qTopicList){
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		double a = 0.0,b = 0.0;
		if (method.isInGroup(api.Constants.Method.Group.STATIC) == true)
		{
			a = intersection(qConceptSet, eConceptSet).size();
			b = symDifference(qConceptSet, eConceptSet).size();
		}
		else
		{
			Set<String> intersectionSet = intersection(qConceptSet, eConceptSet);
			Set<String> symDifferenceSet = symDifference(qConceptSet, eConceptSet);
			Set<String> qTopicSet = (qTopicList!=null?new HashSet<String>(qTopicList):null);
			double lackKnowledge = 0;
			if (Arrays.asList(api.Constants.GOAL_BASED_METHODS).contains(method) == true)
			{
				
				//step 1: create the 'a' set:concepts in intersection that are also target concept for the topic
				Set<String> intersectionTargetset = new HashSet<String>();
				Set<String> conceptTopicSet;
				for (String concept : intersectionSet)
				{
					conceptTopicSet = new HashSet<String>(db.getConceptTopic(concept));
					if (intersection(qTopicSet,conceptTopicSet).size() > 0)
						intersectionTargetset.add(concept);
				}
				//step 2: calculate concepts in intersectionSet that are not target concept for the topic
				Set<String> notIntersectionTargetSet = symDifference(intersectionSet,intersectionTargetset);
				//step 3: update b set, adding the concepts in step2 to the set that contains concepts that are not in common 
				symDifferenceSet = union(symDifferenceSet,notIntersectionTargetSet);
				for (String concept : intersectionTargetset)
				{
					if (kmap.get(concept) == null)
						lackKnowledge = 0;
					else
						lackKnowledge = 1-kmap.get(concept);
					a += lackKnowledge;					
				}
				for (String concept : symDifferenceSet)
				{
					if (kmap.get(concept) == null)
						lackKnowledge = 0;
					else
						lackKnowledge = 1-kmap.get(concept);
					b += lackKnowledge;
				}
					
			}
			else
			{
				for (String concept : intersectionSet)
				{					
					if (kmap.get(concept) == null)
						lackKnowledge = 0;
					else
						lackKnowledge = 1-kmap.get(concept);
					a += lackKnowledge;
				}
				for (String concept : symDifferenceSet)
				{
					if (kmap.get(concept) == null)
						lackKnowledge = 0;
					else
						lackKnowledge = 1-kmap.get(concept);
					b += lackKnowledge;
				}
			}
		
		}
		//this is check for not getting NaN as sim
		//possible NaN cases: when user already knows all the concepts in both common concepts and not common concepts, or all of such concepts are not among
		//reported concepts in knowledge report
		//so this example-question pair has no gain for user.
		double sim;
		if (a==0 & b==0)
			sim = 0.0;
		else 
			sim = (2*a-b)/(2*a+b);
		return sim;
	}	
	
	/*
	 * old structural similarity used in the labstudy
	 */
	private static double calStructuralContentSim(String q, String e) {
		String[] qsubList;
		String[] esubList;
		String qtree;
		String etree;
		double temp;
		double mincost;
		Map<String, Double> q2edistMap;
		Map<String, Double> e2qdistMap;
		double weight = 0.0;
		double sim;
		double dist = 0.0;
		q2edistMap = new HashMap<String,Double>();
		e2qdistMap = new HashMap<String,Double>();
		qtree = db.getTree(q);
		etree = db.getTree(e);
		if (qtree == null | etree == null)
		{
			//System.out.println("TREE not available! sim = 0 for "+q+" "+e);
			return 0;
		}
		qsubList = qtree.split("@");
		esubList = etree.split("@");
		String tmp = "";

		//find distance of q to e
		for (String qs : qsubList)
		{
			mincost = Double.POSITIVE_INFINITY;
			for (String es : esubList)
			{	
				temp  = calculateDist(es, qs, e, q);
				weight = db.getWeightInSubtree(es,q);
				if (weight <= 0)
					temp = Double.POSITIVE_INFINITY;
				else
					temp = temp/weight;
				if (temp < mincost)
				{
					mincost = temp;
					tmp = es;
				}
			}
			q2edistMap.put(qs, mincost);
//			System.out.println("qs: "+qs);
//			System.out.println("es: "+tmp);
//			System.out.println("mincost: "+mincost);
//			System.out.println("******************");
		}
		//find distance of e to q
		for (String es : esubList)
		{
			mincost = Double.POSITIVE_INFINITY;
			for (String qs : qsubList)
			{
				temp  = calculateDist(es, qs, e, q);
				weight = db.getWeightInSubtree(qs,e);
				if (weight <= 0)
					temp = Double.POSITIVE_INFINITY;
				else
					temp = temp/weight;
				if (temp < mincost)
				{
					mincost = temp;
					tmp = qs;
				}
			}
			e2qdistMap.put(es, mincost);
//			System.out.println("es: "+es);
//			System.out.println("qs: "+tmp);
//			System.out.println("mincost: "+mincost);
//			System.out.println("******************");
		}

		for (Map.Entry<String, Double> entry : q2edistMap.entrySet())
		{
			dist += entry.getValue();
		}
		for (Map.Entry<String, Double> entry : e2qdistMap.entrySet())
		{
			dist += entry.getValue();
		}

		sim = 1.0/(Math.exp(0.01*dist));
		return sim;
	}
	
	
	
	private static double calculateDist(String es, String qs,String e, String q) {
		TreeDefinition eTree = CreateTreeHelper.makeTree(es);
		TreeDefinition qTree = CreateTreeHelper.makeTree(qs);
		ComparisonZhangShasha treeCorrector = new ComparisonZhangShasha();
		OpsZhangShasha costs = new OpsZhangShasha();
		Transformation transform = treeCorrector.findDistance(eTree, qTree, costs);
		double dist = transform.getCost();		
		return dist;
	}

	private static <T> Set<T> union(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.addAll(setB);
		return tmp;
	}

	private static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>();
		for (T x : setA)
			if (setB.contains(x))
				tmp.add(x);
		return tmp;
	}

	private static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.removeAll(setB);
		return tmp;
	}

	private static <T> Set<T> symDifference(Set<T> setA, Set<T> setB) {
		Set<T> tmpA;
		Set<T> tmpB;
		tmpA = union(setA, setB);
		tmpB = intersection(setA, setB);
		return difference(tmpA, tmpB);
	}
	
	private static class SortByName implements Comparator<String> {
	    public int compare(String s1, String s2) {
	        return s1.compareTo(s2);
	    }
	}
}
