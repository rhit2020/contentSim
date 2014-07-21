import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ContentSim {

	private static Data db;
	//private static DB db;
	public static void main(String[] args){
		
		db = new Data();
//		db  = new DB();
		db.setup();
		// **** for test ****//
//		String[] qList = {"jArray2"};	
//		String[] eList = {"inheritance1_v2"};
		// **** for test ****//
		if (db.isReady())
        {			
			String[] eList = db.getExamples();
			String[] qList = db.getQuestions();				
			calculateSim(qList, eList);
		}
		else
		{
			System.out.println("No connection to database!");
		}
		db.close();
	}

	private static void calculateSim(String[] qList, String[] eList) {
		List<String> qConcepts = null;
		List<String> eConcepts = null;
		Map<String,Double> qConceptWeight = null;
		Map<String,Double> eConceptWeight = null;
		List<ArrayList<String>> qtree = null;
		List<ArrayList<String>> etree = null;
		double sim = 0.0;
		for (String q : qList)
		{
			//creating list of concepts in question
			qConcepts = db.getConcepts(q);
			//TFIDF values used as weight of concepts in question
			qConceptWeight = db.getTFIDF(q);
			//subtrees in question
			qtree = getSubtrees(q);
			for (String e : eList) {
				//creating list of concepts in example
				eConcepts = db.getConcepts(e);
				//TFIDF values used as weight of concepts in example
				eConceptWeight = db.getTFIDF(e);
				//subtrees in example
				etree = getSubtrees(e);
				//calculate global similarity 				
				sim = simAssociationCoefficient(qConcepts,eConcepts); //variant 1: global tree - count concept
				db.insertContentSim(q, e, sim, "GLOBAL:AS");
				sim = simCosine(q,qConcepts,eConcepts,qConceptWeight,eConceptWeight); //variant 2: global tree - weight concept
				db.insertContentSim(q, e, sim, "GLOBAL:COS");
				//calculate local similarity
				sim = localSim(null,qtree,etree,"AS",null,null); //variant 1: local subtree - count concept
				db.insertContentSim(q, e, sim, "LOCAL:AS");
				sim = localSim(q,qtree,etree,"COS",qConceptWeight,eConceptWeight); //variant 2: local subtree - weight concept
				db.insertContentSim(q, e, sim, "LOCAL:COS");
			}
		}		
	}
	
	private static List<ArrayList<String>> getSubtrees(String content) {
		List<ArrayList<String>> subtreeList = new ArrayList<ArrayList<String>>();
		List<Integer> lines = db.getStartEndLine(content);
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
			List<Integer> endLines = db.getEndLineBlock(content,line);			
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
	private static double localSim(String question, List<ArrayList<String>> qtree, List<ArrayList<String>> etree, 
			                       String variant, Map<String,Double> qConceptWeight, Map<String,Double> eConceptWeight)
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
					s[i][j] = simAssociationCoefficient(qtree.get(i),etree.get(j));
				}
				else if (variant.equals("COS"))
				{
					s[i][j] = simCosine(question,qtree.get(i),etree.get(j),qConceptWeight,eConceptWeight);
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
	private static double simCosine(String q, List<String> qConcepts, List<String> eConcepts, Map<String, Double> qConceptWeight, Map<String, Double> eConceptWeight) {
		//create concept space by union of two sets. Set drops repeated elements and contains unique values
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		List<String> conceptSpace = new ArrayList<String>(union(qConceptSet, eConceptSet));
		HashMap<String,Double> evector = new HashMap<String,Double>();// concept vector for example
		HashMap<String,Double> qvector = new HashMap<String,Double>(); // concept vector for question
		for (String c : conceptSpace)
		{
			evector.put(c, eConceptSet.contains(c)?eConceptWeight.get(c):0);
			qvector.put(c, qConceptSet.contains(c)?qConceptWeight.get(c):0);			
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
		double sim = numerator/(Math.sqrt(qDenominator)*Math.sqrt(eDemoninator)); //square root of the qDenominator/eDenominator
		return sim;	
	}

	/*
	 * Return value ranges from -1 to 1.
	 */
	public static double simAssociationCoefficient(List<String> qConcepts, List<String> eConcepts){
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		double a = intersection(qConceptSet, eConceptSet).size();
		double b = symDifference(qConceptSet, eConceptSet).size();
		double sim = (2*a-b)/(2*a+b);
		return sim;
	}	

	public static <T> Set<T> union(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.addAll(setB);
		return tmp;
	}

	public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>();
		for (T x : setA)
			if (setB.contains(x))
				tmp.add(x);
		return tmp;
	}

	public static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.removeAll(setB);
		return tmp;
	}

	public static <T> Set<T> symDifference(Set<T> setA, Set<T> setB) {
		Set<T> tmpA;
		Set<T> tmpB;
		tmpA = union(setA, setB);
		tmpB = intersection(setA, setB);
		return difference(tmpA, tmpB);
	}
	
	public static class SortByName implements Comparator<String> {
	    public int compare(String s1, String s2) {
	        return s1.compareTo(s2);
	    }
	}
}
