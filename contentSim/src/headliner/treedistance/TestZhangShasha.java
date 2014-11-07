package headliner.treedistance;

import java.util.Hashtable;
import java.util.ArrayList;

/* 
 * INSERT-LICENCE-INFO
 */
public class TestZhangShasha {

    public static void main 
	(String argv []) throws java.io.IOException  {
	
//	if (argv.length != 2) {
//	    System.out.println("Usage TestZhangShasha <tree1> <tree2>");
//	    return;
//	}


//    String main = "a-b;a-c";
//    String[] test = {"a-b;a-d;","a-b;","a-b;a-g;a-e","a-b;a-c"};//one replace, one insert, one delete, same 
	String main = "1-2;1-3;2-4;2-5;";
	String[] test = {"1-2;1-3;1-4;1-5"};
    TreeDefinition aTree = CreateTreeHelper.makeTree(main);
	//System.out.println("The tree is: \n"+aTree);
	for (String t : test)
    {
	    TreeDefinition bTree = CreateTreeHelper.makeTree(t);
	    //System.out.println("The tree is: \n"+bTree);
	    ComparisonZhangShasha treeCorrector = new ComparisonZhangShasha();
		OpsZhangShasha costs = new OpsZhangShasha();
		Transformation transform = treeCorrector.findDistance(aTree, bTree, costs);
		System.out.println("Tree: "+t);
		System.out.println("Distance: "+transform.getCost());
		System.out.println("******************");
    }
  }	
}
