import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;


public class Test extends TestCase{

	protected void setUp(){};
	protected void tearDown(){};
	public void testdg(){
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
}
