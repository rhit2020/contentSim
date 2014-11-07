package headliner.treedistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SIM_V2 {
	
	public static void main (String[] args)
	{
		DB db  = new DB();
		db.connect();
		if (db.isConnectedToLabstudy())
        {
//			String[] eList = db.getExamplesName();
//			List<String> list = db.getQuestionNames();
//			String[] qList = list.toArray(new String[list.size()]);
//			
			// **** for test ****//
			String[] qList = {"jarraylist1"};			
			String[] eList = {"Point_v2","arraylist2_v2"};
//			String[] eList = {"poly_v2","inheritance_casting_1","inheritance_polymorphism_1",
//					"inheritance_polymorphism_2","inheritance_constructors_1","simple_inheritance_1"};
			// **** for test ****//
			
			calculateStructuralSim(db, qList, eList);
			//calculateConceptSim(db, qList, eList);
			
			db.disconnect();
		}

	}

	private static void calculateConceptSim(DB db, String[] qList, String[] eList) {
		for (String q : qList)
		{
			for (String e : eList) {
				calConceptSimilarity(db,q,e);
			}
		}
		
	}

	private static void calculateStructuralSim(DB db, String[] qList, String[] eList) {
		for (String q : qList)
		{
			for (String e : eList) {
				calStructuralContentSim(db, q, e);
			}
		}
		//normalize the distance
//		double maxdistance = db.getMaxDist();
//		double mindistance = db.getMinDist();
//		db.normalizeDistance(maxdistance,mindistance);
		//update the sim (current function is : 1-x)
//		db.updateSim();		
	}

	private static void calConceptSimilarity(DB db, String q, String e) {
		List<String> qConcepts = db.getConcepts(q);
		List<String> eConcepts = db.getConcepts(e);
		List<String> conceptSpaceList = db.getContentConcepts();
		int count = 0;
		double[] qvec = new double[conceptSpaceList.size()];		
		double[] evec = new double[conceptSpaceList.size()];
		for (String c : conceptSpaceList)
		{
			qvec[count] = 0;
			evec[count] = 0;
			if (qConcepts.contains(c))
			{
				qvec[count] = db.getTFIDF(q,c);				
			}				
			if (eConcepts.contains(c))
			{
				evec[count] = db.getTFIDF(e,c);				
			}
			count++;			
		}
		double denom1 = 0.0;
		double denom2 = 0.0;
		double numerator = 0.0;
		for (int i = 0; i < conceptSpaceList.size(); i++)
		{
			numerator += qvec[i]*evec[i];
			denom1 += Math.pow(qvec[i],2);
			denom2 += Math.pow(evec[i],2);
		}
		double denom = Math.sqrt(denom1)*Math.sqrt(denom2);
		if (denom == 0.0)
			denom += Double.MIN_VALUE;
		double sim = numerator / denom;
		db.insertContentSim(q, e,sim,-1,"CONCSIM");
//		System.out.println("q:"+q+" e:"+e+" sim:"+sim);
	}

	private static void calStructuralContentSim(DB db, String q, String e) {
		String[] qsubList;
		String[] esubList;
		String qtree;
		String etree;
		double temp;
		double mincost;
		Map<String, Double> q2edistMap = new HashMap<String,Double>();
		Map<String, Double> e2qdistMap = new HashMap<String,Double>();
		double sim;
		double dist = 0.0;
		qtree = db.getTree(q);
		etree = db.getTree(e);
		qsubList = qtree.split("@");
		esubList = etree.split("@");
		
		double sizeQ = db.getAllConcepts(q).size();
		double sizeE = db.getAllConcepts(e).size();
		
		double size_es;
		double size_qs;
		
		HashMap<String,Double> q_vector = db.getConceptsVector(q); //keys are concept and values are tf-idf value for concepts
        HashMap<String,Double> e_vector = db.getConceptsVector(e);//keys are concept and values are tf-idf value for concepts

		List<String> es_concepts;
        List<String> qs_concepts;
        List<String> concept_space;
        HashMap<String,Double> es_vector;
        HashMap<String,Double> qs_vector;
        
		//find distance of q to e
		for (String qs : qsubList)
		{
			if (qs.equals("ROOT-EqualExpression;ROOT-WhileStatement;"))
				System.out.println();
			qs_concepts = getConceptsInSubtree(qs);
			size_qs = getConceptsInSubtree(qs).size();
			mincost = Double.POSITIVE_INFINITY;
			for (String es : esubList)
			{	
				temp  = calculateDist(es, qs, e, q);
				size_es = getConceptsInSubtree(es).size();
				//calculate size ratio
				temp = temp * Math.pow((size_qs/sizeQ)*(size_es/sizeE), -1);
				//calculate cos(qs,es) for the common concept in both es and qs, we should create weights as follows:
				// for es_concepts: the weight is the log(tf+1)idf of the concept in example,
				//for qs_concepts: the weight is the log(tf+1)idf of the concept in question,
				//the reason for this is that if for instance ++ is an important concept in Q, it should be also an important concept in E to make the example a good candidate
				//if the weight of the concept is not high in example, it means example is not fully descriptive for this concept. 
				
				//create the space of concepts from concepts in es and concepts in qs
				concept_space = new ArrayList<String>();
				es_concepts = getConceptsInSubtree(es);
				//add es concepts
				for (String c : es_concepts)
					if (concept_space.contains(c) == false)
						concept_space.add(c);
				//add qs concepts
				for (String c: qs_concepts)
					if (concept_space.contains(c) == false)
						concept_space.add(c);
				
				es_vector = new HashMap<String,Double>();
				qs_vector = new HashMap<String,Double>(); //concepts in qs vector depends on es..so this is the right place for creating that

				for (String con : concept_space)
				{
					//update vector of es
					if (es_concepts.contains(con))
						es_vector.put(con, e_vector.get(con));// here is where we get the tf-idf value for the example concept
					else
						es_vector.put(con,0.0);
					//update vector of qs
					if (qs_concepts.contains(con))
						qs_vector.put(con, q_vector.get(con));// here is where we get the tf-idf value for the question concept
					else
						qs_vector.put(con,0.0);
				}
				double numerator = 0.0;
				double demoninator_es = 0.0;
				double denominator_qs = 0.0;
				double cosine = 0.0;
				for (String con :  concept_space)
				{
					numerator += qs_vector.get(con) * es_vector.get(con);
					demoninator_es += Math.pow(es_vector.get(con), 2);
					denominator_qs += Math.pow(qs_vector.get(con), 2);
				}
				if ((Math.sqrt(denominator_qs)*Math.sqrt(demoninator_es)) == 0.0)
				{
					cosine = 0; //cosine =0 so temp *1/0 = temp*positive infitiy as follows
					temp = Double.POSITIVE_INFINITY;
				}
				else
				{
					cosine = numerator/(Math.sqrt(denominator_qs)*Math.sqrt(demoninator_es));
					temp = temp * (2.0/Math.pow(cosine+1,-1));					
				}
				
				if (temp < mincost)
				{
					mincost = temp;
				}
			}
			q2edistMap.put(qs, mincost);
//			System.out.println("qs: "+qs);
//			System.out.println("es: "+tmp);
//			System.out.println("mincost: "+mincost);
//			System.out.println("******************");
		}
		
//		//find distance of e to q
//		for (String es : esubList)
//		{
//			mincost = Double.POSITIVE_INFINITY;
//			for (String qs : qsubList)
//			{
//				temp  = calculateDist(es, qs, e, q);
//				
//			}
//			e2qdistMap.put(es, mincost);
//		}
//		
		for (Map.Entry<String, Double> entry : q2edistMap.entrySet())
		{
			dist += entry.getValue();
		}
//		for (Map.Entry<String, Double> entry : e2qdistMap.entrySet())
//		{
//			dist += entry.getValue();
//		}

		if (dist == Double.POSITIVE_INFINITY)
			dist = Double.MAX_VALUE;//just for storing in db
		//sim = 1.0/(Math.exp(0.01*dist));
		
		System.out.println("Question:"+q+", Subtree#:"+q2edistMap.size());
		System.out.println("*****"+e+"*****");
		for (Map.Entry<String, Double> entry : q2edistMap.entrySet())
		{
			System.out.println(entry.getKey()+":"+entry.getValue());
			System.out.println();
			System.out.println();

		} 
		System.out.println("dist:"+dist);
		System.out.println("**********");
		
		
		//db.insertContentSim(q, e,-1,dist,"CSSIM");
	}



	private static List<String> getConceptsInSubtree(String subtree) {
		String[] edges = subtree.split(";");
		List<String> subtreeConcepts = new ArrayList<String>();
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
		return subtreeConcepts;
	}

	private static double calculateDist(String es, String qs,String e, String q) {
		try
		{
			TreeDefinition eTree = CreateTreeHelper.makeTree(es);
			TreeDefinition qTree = CreateTreeHelper.makeTree(qs);
			ComparisonZhangShasha treeCorrector = new ComparisonZhangShasha();
			OpsZhangShasha costs = new OpsZhangShasha();
			Transformation transform = treeCorrector.findDistance(eTree, qTree, costs);
			double dist = transform.getCost();		
			return dist;
		}catch(Exception ex)
		{
			System.out.println(e+" "+es);
			System.out.println("");
			System.out.println(q+" "+qs);
		}
		return 0;
	}
}
