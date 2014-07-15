import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
			String[] qList = {"jarraylist1"};			
			String[] eList = {"Point_v2"};
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
				//weight of concepts in question
				Map<String,Double> qConceptWeight = db.getTFIDF(q);
				//calculate global similarity 
				double sim = simConceptCount(qConcepts,eConcepts); //variant 1: global tree - count concept
				db.insertContentSim(q, e, sim, "GLOBAL-C");
				sim = simConceptWeight(db,q,qConcepts,eConcepts,qConceptWeight); //variant 2: global tree - weight concept
				db.insertContentSim(q, e, sim, "GLOBAL-W");
				//calculate local similarity
				List<ArrayList<String>> qtree = getSubtrees(db,q);
				List<ArrayList<String>> etree = getSubtrees(db,e);
				sim = localSim(null,null,qtree,etree,"C",null); //variant 1: local subtree - count concept
				db.insertContentSim(q, e, sim, "LOCAL-C");
				sim = localSim(db,q,qtree,etree,"W",qConceptWeight); //variant 2: local subtree - weight concept
				db.insertContentSim(q, e, sim, "LOCAL-W"); 
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

	private static double localSim(DB db, String question, List<ArrayList<String>> qtree, List<ArrayList<String>> etree, String variant, Map<String,Double> qConceptWeight) {
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
				if (variant.equals("C"))
					s[i][j] = simConceptCount(qtree.get(i),etree.get(j));
				else if (variant.equals("W"))
				{
					s[i][j] = simConceptWeight(db,question,qtree.get(i),etree.get(j),qConceptWeight);
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
		sim /= (qtree.size()*etree.size());
		return sim;
	}

	private static double simConceptWeight(DB db, String q, List<String> qConcepts, List<String> eConcepts, Map<String, Double> qConceptWeight) {
		//sim by weight of common and not common concepts
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		List<String> alist = new ArrayList<String>(intersection(qConceptSet, eConceptSet));
		List<String> blist = new ArrayList<String>(symDifference(qConceptSet, eConceptSet));
		double aw = 0.0;
		double bw = 0.0;
		for (String a : alist)
		{
			aw += qConceptWeight.get(a);
		}
		for (String b : blist)
		{
			bw += (qConceptWeight.get(b) == null?0:qConceptWeight.get(b));
		}
		double sim = (2*aw-bw)/(2*aw+bw);
		return sim;	
	}

	public static double simConceptCount(List<String> qConcepts, List<String> eConcepts){
		//sim by count of the concept
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
