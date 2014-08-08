import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;


public class Test extends TestCase{
	private EvaluationSim es;
	protected void setUp(){
		EvaluationSim es = new EvaluationSim();
	};
	protected void tearDown(){
		es = null;
	};
	public void testcg(){
		Map<Integer, Map<String, Double>> map = new HashMap<Integer,Map<String,Double>>();
		Map<String, Double> ratingMap1 = new HashMap<String,Double>();
		ratingMap1.put("a", 2.0);
		Map<String, Double> ratingMap2 = new HashMap<String,Double>();
		ratingMap2.put("b", -1.0);
		Map<String, Double> ratingMap3 = new HashMap<String,Double>();
		ratingMap3.put("c", 2.0);
		map.put(1,ratingMap1);
		map.put(2, ratingMap2);
		map.put(3, ratingMap3);
		EvaluationSim es = new EvaluationSim();
		assertEquals(es.cg(map),3);
	};
	
	public void testdg(){
		
	}
}
