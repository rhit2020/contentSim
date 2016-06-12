import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExpertStudy_analysis2 {
	Map<String,List<String>> methodItem = new HashMap<String,List<String>>();//method,<item>
	Map<String,String> methodFeature = new HashMap<String,String>();//method,feature
    static Map<String,String> mname = new HashMap<String,String>();
    Map<String,String> itemdiff = new HashMap<String,String>();

	Map<String,Integer> emv = new HashMap<String,Integer>();//item,mv

	public static void main(String[] args){

		mname.put("RANDOM::BASELINE","B");
		mname.put("GLOBAL::AS","A");
		mname.put("GLOBAL::COS","C");
		mname.put("LOCAL::AS","SA");
		mname.put("LOCAL::COS","SC");
		mname.put("P::GLOBAL::AS","PA");
		mname.put("P::GLOBAL::COS","PC");
		mname.put("P::LOCAL::AS","PSA");
		mname.put("P::LOCAL::COS","PSC");
		mname.put("P::GLOBAL::AS::GOAL","PGA");
		mname.put("P::GLOBAL::COS::GOAL","PGC");
		mname.put("P::LOCAL::AS::GOAL","PGSA");
		mname.put("P::LOCAL::COS::GOAL","PGSC");
		mname.put("NAIVE::LOCAL","ST");
		
		
		ExpertStudy_analysis2 es = new ExpertStudy_analysis2();
		es.readMethodItem();
		es.readItemDiff();
		es.readExpertMV();
		es.propMethodToEMV();
	}

	private void readItemDiff() {

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		String que,diff;
		try {
			br = new BufferedReader(new FileReader("./resources/new-item-diff.csv"));
			String[] clmn;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				que = clmn[0].replaceAll("\"", "");
				diff= clmn[2].replaceAll("\"", ""); 
				itemdiff.put(que.replaceAll(" ", ""),diff.replaceAll(" ", ""));
			}
			
			System.out.println("itemdiff: "+itemdiff.size());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}				
	}

	private void propMethodToEMV() {
		BufferedWriter bw = null;
		File file = new File("./resources/expert-majority-votes-with-methods.csv");
		FileWriter fw = null;
		try {
			fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			bw.write("item,mv,method,isstatic,isPersonalized,isStructural,isgoal,diff");
			bw.newLine();
			bw.flush();		
			List<String> tmp;	
			//for the lowRating map
			for (String i : emv.keySet())
			{
				tmp = getMethodForItem(i);
				for (String m : tmp)
				{
					bw.write(i+","+emv.get(i)+","+mname.get(m)+","+methodFeature.get(m)+","+getdiff(i));
					bw.newLine();
					bw.flush();
				}
			}
			bw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}		
	}

	private String getdiff(String i) {
		for (String que : itemdiff.keySet())
			if (i.startsWith(que.replaceAll(" ", "")))
				return  itemdiff.get(que);
		return null;
	}

	private List<String> getMethodForItem(String i) {
		List<String> list = new ArrayList<String>();
		for (String s : methodItem.keySet())
			if (methodItem.get(s).contains(i))
				list.add(s);
		return list;
	}

	private void readExpertMV() {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader("./resources/expert-majority-votes.csv"));
			String[] clmn;
			String item,test;
			int mv;
			//item	maxf	majority_vote	test
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				item = clmn[0];
				mv = Integer.parseInt(clmn[2]);
				test = clmn[3];
				if (test.equals("NULL"))
					continue;
				else{
					emv.put(item, mv);
				}			
			}	
			System.out.println("emv: "+emv.size());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}	
		
	}

	private void readMethodItem() {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader("./resources/method-items-for-expert-analysis.csv"));
			String[] clmn;
			String method,item;
			int rating;
			List<String> list;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				method = clmn[0];
				item = clmn[1];
				methodFeature.put(method, clmn[2]+","+clmn[3]+","+clmn[4]+","+getGoalValue(mname.get(method)));
				list = methodItem.get(method);
				if (list == null)
				{
					list = new ArrayList<String>();
					list.add(item);
					methodItem.put(method, list);
				}
				else{
					list.add(item);
				}
			}
			int count = 0;
			for (String s:methodItem.keySet())
				count+=methodItem.get(s).size();
			System.out.println("methodItem: "+count);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
		
	}

	private String getGoalValue(String method) {
		if (method.equals("B") |method.equals("C") | method.equals("A") | method.equals("SA") | method.equals("SC")|method.equals("ST") )
		return "NA";
		else if (method.equals("PA") | method.equals("PC") | method.equals("PSA") | method.equals("PSC") )
		return "false";
		else if (method.equals("PGSA") | method.equals("PGSC") | method.equals("PGC") | method.equals("PGA"))
		return "true";
		return null;
	}

}
