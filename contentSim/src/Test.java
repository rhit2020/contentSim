import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;


public class Test extends TestCase{
	private EvaluationSim es;
	Data d;
	protected void setUp(){
		es = new EvaluationSim();
		d = Data.getInstance();
	};
	protected void tearDown(){
		es = null;
		d = null; 
	};
//	public void testcg(){
//		Map<Integer, Map<String, Integer>> map = new HashMap<Integer,Map<String,Integer>>();
//		Map<String, Integer> ratingMap1 = new HashMap<String,Integer>();
//		ratingMap1.put("a", 2);
//		Map<String, Integer> ratingMap2 = new HashMap<String,Integer>();
//		ratingMap2.put("b", -1);
//		Map<String, Integer> ratingMap3 = new HashMap<String,Integer>();
//		ratingMap3.put("c", 2);
//		map.put(1,ratingMap1);
//		map.put(2, ratingMap2);
//		map.put(3, ratingMap3);		
//		assertEquals(es.cg(map),3);
//	};	
//	public void testFormat(){
//		DecimalFormat df = new DecimalFormat();
//		df.setMaximumFractionDigits(2);
//		System.out.println("3.445--->"+df.format(3.445));
//		System.out.println("3.444--->"+df.format(3.444));
//		System.out.println("3.446--->"+df.format(3.446));
//	}
//	
//	public void testAggregateJudges()
//	{
//		ArrayList<Integer> list = new ArrayList<Integer>();
//		list.add(-1);
//		list.add(-1);
//		list.add(0);
//		list.add(0);
//		assertEquals(d.aggregateJudges(list),-1);
//	}
	
	public void testTreeMap()
	{
		Map<String,Double> tmp = new HashMap<String,Double>();
		ValueComparatorDouble vc = new ValueComparatorDouble(tmp);
		TreeMap<String,Double> sortedTreeMap = new TreeMap<String,Double>(vc);		
		
		HashMap<String,Double> testM = new HashMap<String,Double>();
		testM.put("A",1.0);
		testM.put("B", 1.5);
		testM.put("C",2.0);
		testM.put("Z",0.0);		
		tmp.putAll(testM);		
		sortedTreeMap.putAll(tmp);
		
		for (Entry<String,Double> entry : sortedTreeMap.entrySet())
		{
			System.out.println(entry.getKey()+" "+entry.getValue());
		}
		System.out.println("-----");
		List<String> list = new ArrayList<String>(sortedTreeMap.keySet());
		for (String s : list)
			System.out.println(s);
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
