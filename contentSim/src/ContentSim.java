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

	public static void main(String[] args){
		DB db  = new DB();
		db.connect();
		if (db.isConnectedToLabstudy())
        {
//			String[] eList = db.getExamples();
//			String[] qList = db.getQuestions();			
			// **** for test ****//
			String[] qList = {"jVariables3"};			
			String[] eList = {"JavaTutorial_4_1_5"};
//			String[] eList = {"arraylist2_v2"};

//			String[] eList = {"poly_v2","inheritance_casting_1","inheritance_polymorphism_1","inheritance_polymorphism_2","inheritance_constructors_1","simple_inheritance_1"};
			// **** for test ****//
			calculateSim(db, qList, eList);
			db.disconnect();
		}
		else
		{
			System.out.println("No connection to database!");
		}
	}

	private static void calculateSim(DB db, String[] qList, String[] eList) {
		for (String q : qList)
		{
			for (String e : eList) {
				//creating list of concepts in question and example
				List<String> qConcepts = db.getConcepts(q);
				List<String> eConcepts = db.getConcepts(e);
				/*
				 * sij = (2a-b)/(2a+b); a: common concepts in two trees/subtrees; b = concepts that are not common in two trees/subtrees
				 */
				//TFIDF values used as weight of concepts in question/example
				Map<String,Double> qConceptWeight = db.getTFIDF(q);
				Map<String,Double> eConceptWeight = db.getTFIDF(e);

				//calculate global similarity 
				double sim = 0.0;
				sim = simAssociationCoefficient(qConcepts,eConcepts); //variant 1: global tree - count concept
				db.insertContentSim(q, e, sim, "GLOBAL:AS");
				sim = simCosine(db,q,qConcepts,eConcepts,qConceptWeight,eConceptWeight); //variant 2: global tree - weight concept
				db.insertContentSim(q, e, sim, "GLOBAL:COS");
				//calculate local similarity
				List<ArrayList<String>> qtree = getSubtrees(db,q);
				List<ArrayList<String>> etree = getSubtrees(db,e);
				sim = localSim(null,null,qtree,etree,"AS",null,null); //variant 1: local subtree - count concept
				db.insertContentSim(q, e, sim, "LOCAL:AS");
				sim = localSim(db,q,qtree,etree,"COS",qConceptWeight,eConceptWeight); //variant 2: local subtree - weight concept
				db.insertContentSim(q, e, sim, "LOCAL:COS"); 
			}
		}		
	}
	private static List<ArrayList<String>> getSubtrees(DB db, String content) {
		List<ArrayList<String>> subtreeList = new ArrayList<ArrayList<String>>();
		List<Integer> lines = db.getStartEndLine(content);
		int start = lines.get(0);
		int end = lines.get(1);
		for (int line = start; line <= end; line++)
		{
			List<Integer> endLines = db.getConceptEndLines(content,line);			
			for (int e : endLines)
			{
				ArrayList<String> subtree = new ArrayList<String>();
				List<String> adjucentConceptsList = db.getAdjacentConcept(content,line,e);
				Collections.sort(adjucentConceptsList, new SortByName());
				for (String adjcon : adjucentConceptsList)
					subtree.add(adjcon);				
				if (subtree.isEmpty() == false && subtreeList.contains(subtree) == false)
				{
					subtreeList.add(subtree);	
				}
			}			
		}	
		return subtreeList;
	}

	/*
	 * Return value ranges from -1 to 1. But never reaches one: [-1,1). The min happens when all cells are -1. Max happens when the value in each
	 * cell gets very close to 1 but not 1 (so, all cells will have a value very close to 1)
	 */
	private static double localSim(DB db, String question, List<ArrayList<String>> qtree, List<ArrayList<String>> etree, 
			                       String variant, Map<String,Double> qConceptWeight, Map<String,Double> eConceptWeight)
	{
		//sim by count of the concept
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
					s[i][j] = simAssociationCoefficient(qtree.get(i),etree.get(j));
				else if (variant.equals("COS"))
				{
					s[i][j] = simCosine(db,question,qtree.get(i),etree.get(j),qConceptWeight,eConceptWeight);
				}
			}
		//fill alpha
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				//check 1's in row
				for (int e = 0; e < etree.size(); e++)
				{
					if (s[i][e] == 1 & e!=j)
						alpha[i][j] = 0;
				}
				//check 1's in column
				for (int q = 0; q < qtree.size(); q++)
				{
					if (s[q][j] == 1 & q!=i)
						alpha[i][j] = 0;
				}
			}
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
		return sim;
	}

	/* 
	 * Return value (cosine similarity) ranges between 0-1 since tfidf values are not negative.
	 */
	private static double simCosine(DB db, String q, List<String> qConcepts, List<String> eConcepts, Map<String, Double> qConceptWeight, Map<String, Double> eConceptWeight) {
		//create concept space by union of two sets. Set drops repeated elements and contains unique values
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		List<String> conceptSpace = new ArrayList<String>(union(qConceptSet, eConceptSet));
		HashMap<String,Double> evector = new HashMap<String,Double>();// concept vector for example
		HashMap<String,Double> qvector = new HashMap<String,Double>(); // concept vector for question
		for (String c : conceptSpace)
		{
			evector.put(c, eConceptWeight.get(c)==null?0:eConceptWeight.get(c));
			qvector.put(c, qConceptWeight.get(c)==null?0:qConceptWeight.get(c));			
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
