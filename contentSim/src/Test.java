import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
//	public void testcg(){
//		Map<Integer, Map<String, Double>> map = new HashMap<Integer,Map<String,Double>>();
//		Map<String, Double> ratingMap1 = new HashMap<String,Double>();
//		ratingMap1.put("a", 2.0);
//		Map<String, Double> ratingMap2 = new HashMap<String,Double>();
//		ratingMap2.put("b", -1.0);
//		Map<String, Double> ratingMap3 = new HashMap<String,Double>();
//		ratingMap3.put("c", 2.0);
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
