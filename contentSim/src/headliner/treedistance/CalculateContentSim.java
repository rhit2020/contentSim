package headliner.treedistance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CalculateContentSim {
	
	public static void main (String[] args)
	{
		DB db  = new DB();
		db.connect();
		if (db.isConnectedToGuanjie())
        {
			//storeLDAOutputToDB(db);			
//			String[] eList = db.getExamplesName();
			List<String> list = db.getQuestionNames();
			String[] qList = list.toArray(new String[list.size()]);
//			String[] qList = {"jArrayList5"};			
//			String[] eList = {"inheritance1_v2"};
			String[] eList = {"poly_v2","inheritance_casting_1","inheritance_polymorphism_1",
					"inheritance_polymorphism_2","inheritance_constructors_1","simple_inheritance_1"};
			for (String q : qList)
			{
				for (String e : eList) {
					calStructuralContentSim(db, q, e);
					calConceptSimilarity(db,q,e);
					//calLDASimilarity(db,q,e);
				}
			}
			db.disconnect();
		}

	}

	private static void calLDASimilarity(DB db, String q, String e) {

		double distance = 0.0;
		double distq2e = 0.0;
		double dise2q = 0.0;
		double tmp;
		double topicvalq;
		double topicvale;
		for (int i = 0; i < api.Constants.LDA_TOPIC_COUNT; i++)
		{
			topicvalq = db.getTopicVal(q,i);
			topicvale = db.getTopicVal(e,i);
			tmp = Math.log10(topicvalq/topicvale)/Math.log10(2);
			distq2e += topicvalq*tmp;
			
			tmp = Math.log10(topicvale/topicvalq)/Math.log10(2);
			dise2q += topicvale*tmp;
		}
		distance = 0.75*distq2e + 0.25*dise2q;
		double sim = (1.0/Math.exp(1000*distance));

		sim = Math.round(sim*100000.0)/100000.0;
		db.insertContentSim(q, e, sim, "LDA");
//		System.out.println("q:"+q+" e:"+e+" sim:"+sim);
	}

	private static void storeLDAOutputToDB(DB db) {
		BufferedReader br = null;	 
		try {
 
			String line;
			String content;
			String[] lineContents;	
			String val;
			String topic;
			br = new BufferedReader(new FileReader("resource/topic_docs.txt")); 

				while ((line = br.readLine()) != null) {
					lineContents = line.split("\t");
					content = lineContents[1];
					for (int i = 2; i < lineContents.length - 1 ; i+=2)
					{
						topic = lineContents[i];
						val = lineContents[i+1];
						db.insertContentTopicVal(content,topic,val);
//						System.out.println("content:"+content+" topic:"+topic+" val:"+val);
					}					
				}				
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

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
		db.insertContentSim(q, e,sim,"CONCSIM");
//		System.out.println("q:"+q+" e:"+e+" sim:"+sim);
	}

	private static void calStructuralContentSim(DB db, String q, String e) {
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
				weight = db.getWeightInSubtree(es,q,db);
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
				weight = db.getWeightInSubtree(qs,e,db);
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
		db.insertContentSim(q, e,sim,"CSSIM");
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
	
	private static int getCountSubtreeOutcomeConcepts(String subtree, List<String> outcomeList)
	{
		String[] edges = subtree.split(";");
		ArrayList<String> subtreeConcepts = new ArrayList<String>();
		String[] temp;
		for (String edge : edges)
		{
			temp = edge.split("-");
			if (temp[0].equals("JAVA"))
				subtreeConcepts.add(temp[1]);
			else 
				subtreeConcepts.add(temp[0]);			
		}
		int count = 0;
		for (String s : subtreeConcepts)
			if (outcomeList.contains(s))
				count++;
		return count;
	}
}
