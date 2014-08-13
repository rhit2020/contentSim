import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	public void testcg(){
		Map<Integer, Map<String, Integer>> map = new HashMap<Integer,Map<String,Integer>>();
		Map<String, Integer> ratingMap1 = new HashMap<String,Integer>();
		ratingMap1.put("a", 2);
		Map<String, Integer> ratingMap2 = new HashMap<String,Integer>();
		ratingMap2.put("b", -1);
		Map<String, Integer> ratingMap3 = new HashMap<String,Integer>();
		ratingMap3.put("c", 2);
		map.put(1,ratingMap1);
		map.put(2, ratingMap2);
		map.put(3, ratingMap3);		
		assertEquals(es.cg(map),3);
	};	
	public void testFormat(){
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		System.out.println("3.445--->"+df.format(3.445));
		System.out.println("3.444--->"+df.format(3.444));
		System.out.println("3.446--->"+df.format(3.446));
	}
	
	public void testAggregateJudges()
	{
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(-1);
		list.add(-1);
		list.add(0);
		list.add(0);
		assertEquals(d.aggregateJudges(list),-1);
	}
}
