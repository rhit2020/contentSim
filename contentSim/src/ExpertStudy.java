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


public class ExpertStudy {
	Map<String,Map<String,Integer>> expertRating = new HashMap<String,Map<String,Integer>>();//item,<expert,rating>
	Map<String,Map<String,Integer>> lowRating = new HashMap<String,Map<String,Integer>>();//method,<item,rating>
	Map<String,Map<String,Integer>> highRating = new HashMap<String,Map<String,Integer>>();//method,<item,rating>
	List<String> expertList = new ArrayList<String>();

	public static void main(String[] args){
		ExpertStudy es = new ExpertStudy();
		es.readExpertRating();
		es.readUserRating();
		es.calculateDistance();
	}

	private void calculateDistance() {
		Map<String,Integer> lmap,hmap;
		int eRate,uRate;
		BufferedWriter bw = null;
		File file = new File("./resources/user-method-distance-with-experts.csv");
		FileWriter fw = null;
		try {
			fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			bw.write("user,method,d1,d2,d3,e1,e2,e3");
			bw.newLine();
			bw.flush();		
						
			//for the lowRating map
			for (String m : lowRating.keySet())
			{
				lmap =lowRating.get(m);
				//normalized euclidean distance with expert 1
				double d1 = 0;
				for (String i : lmap.keySet())
				{
					uRate = lmap.get(i);
					eRate = expertRating.get(i).get(expertList.get(0));//expert 1
					d1 += Math.pow(uRate-eRate, 2);
				}
				d1 = Math.sqrt(d1/lmap.size());

				//normalized euclidean distance with expert 2
				double d2 = 0;
				for (String i : lmap.keySet())
				{
					uRate = lmap.get(i);
					eRate = expertRating.get(i).get(expertList.get(1));//expert 2
					d2 += Math.pow(uRate-eRate, 2);
				}
				d2 = Math.sqrt(d2/lmap.size());
			
				
				//normalized euclidean distance with expert 3
				double d3 = 0;
				for (String i : lmap.keySet())
				{
					uRate = lmap.get(i);
					eRate = expertRating.get(i).get(expertList.get(2));//expert 3
					d3 += Math.pow(uRate-eRate, 2);
				}
				d3 = Math.sqrt(d3/lmap.size());

				//write distance for the method
				bw.write("Low,"+m+","+d1+","+d2+","+d3+","+expertList.get(0)+","+expertList.get(1)+","+expertList.get(2));
				bw.newLine();
				bw.flush();
					
			}
			
			
			//for the highRating map
			for (String m : highRating.keySet())
			{
				hmap =highRating.get(m);
				//normalized euclidean distance with expert 1
				double d1 = 0;
				for (String i : hmap.keySet())
				{
					uRate = hmap.get(i);
					eRate = expertRating.get(i).get(expertList.get(0));//expert 1
					d1 += Math.pow(uRate-eRate, 2);
				}
				d1 = Math.sqrt(d1/hmap.size());

				//normalized euclidean distance with expert 2
				double d2 = 0;
				for (String i : hmap.keySet())
				{
					uRate = hmap.get(i);
					eRate = expertRating.get(i).get(expertList.get(1));//expert 2
					d2 += Math.pow(uRate-eRate, 2);
				}
				d2 = Math.sqrt(d2/hmap.size());
			
				
				//normalized euclidean distance with expert 3
				double d3 = 0;
				for (String i : hmap.keySet())
				{
					uRate = hmap.get(i);
					eRate = expertRating.get(i).get(expertList.get(2));//expert 3
					d3 += Math.pow(uRate-eRate, 2);
				}
				d3 = Math.sqrt(d3/hmap.size());

				//write distance for the method
				bw.write("High,"+m+","+d1+","+d2+","+d3+","+expertList.get(0)+","+expertList.get(1)+","+expertList.get(2));
				bw.newLine();
				bw.flush();
					
			}
			bw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}		
	}

	private void readUserRating() {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader("./resources/user-method-item-rating.csv"));
			String[] clmn;
			String pretest,item,method;
			int rating;
			Map<String,Integer> map;
			int j = 0,k=0;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				pretest = clmn[0];
				method = clmn[1];
				item = clmn[2];	
				rating = Integer.parseInt(clmn[3]);
				if(pretest.trim().equals("Low"))
				{
					map = lowRating.get(method);	
					if (map == null)
					{
						map = new HashMap<String,Integer>();
						map.put(item, rating);
						lowRating.put(method,map);k++;
					}else{
						map.put(item, rating);k++;
					}

				}else if (pretest.trim().equals("High")){
					map = highRating.get(method);	
					if (map == null)
					{
						map = new HashMap<String,Integer>();
						map.put(item, rating);
						highRating.put(method,map);
						j++;
					}else{
						map.put(item, rating);j++;
					}
				}else{
					System.out.println("ERROR:     Invalid Pretest!");
				}
			
			}	
			System.out.println("k: "+k);
			System.out.println("j: "+j);
			int count = 0;
			for (String s:highRating.keySet())
				count+=highRating.get(s).size();
			System.out.println("highRating: "+count);
			count = 0;
			for (String s:lowRating.keySet())
				count+=lowRating.get(s).size();
			System.out.println("lowRating: "+count);
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

	private void readExpertRating() {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader("./resources/expert-item-rating.csv"));
			String[] clmn;
			String expert,item;
			int rating;
			Map<String,Integer> map;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				expert = clmn[0];
				if (expertList.contains(expert) == false)
					expertList.add(expert);
				item = clmn[1];	
				rating = Integer.parseInt(clmn[2]);
				map = expertRating.get(item);	
				if (map == null)
				{
					map = new HashMap<String,Integer>();
					map.put(expert, rating);
					expertRating.put(item,map);
				}else{
					map.put(expert, rating);
				}
			}	 
			int count = 0;
			for (String s:expertRating.keySet())
				count+=expertRating.get(s).size();
			System.out.println("expertRating: "+count);
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
		
	};
}
