import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PostProcessLearning {

	public static void processTimeEngagementInOverlappingExamples(String path)
	{
		//knw21	IS17Spring2015	3ADFB	jWhile3	JavaTutorial_4_6_9:1.0:11.58;while_v2:0.5:10.34;	0	0	29:52.9	
		//GLOBAL::COS	1	ae_while_v2;while_v2;
		BufferedReader br = null;
		BufferedWriter bw = null;
		BufferedWriter bw2 = null;

		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader("./resources/"+path));
			File file = new File("./resources/engagement_time_overlapping_cases_summary_qe.csv");
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			
			File file2 = new File("./resources/overalp_case_for_finding_rating.csv");
			FileWriter fw2 = new FileWriter(file2.getAbsoluteFile());
			bw2 = new BufferedWriter(fw2);

			String[] clmn,clickExString,commString;
			String sequence,common;
			ArrayList<String> clickedExampleList,comList;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				sequence = clmn[4];
				common = clmn[10];	
				clickExString = sequence.split(";");
				commString = common.split(";");
				
				clickedExampleList = new ArrayList<String>();
				for (String e : clickExString)
					clickedExampleList.add(e.split(":")[0]);  
				
				comList = new ArrayList<String>();
				for (String e : commString){
					comList.add(e);  
				}
				
				int count = getOverlap(clickedExampleList,comList);
				if (count != Integer.parseInt(clmn[9]))
					System.out.println("~~~~~incosistency: "+count+","+clmn[9]);
				if (count > 0)
				{
					for (int i = 0; i < clickedExampleList.size(); i++)
					{
						String best = null;
						if (i < comList.size())
							best = maxMatch(comList.get(i),clickExString);
						if (best != null)
						{
							bw.write(clmn[8]+","+clmn[9]+","+best.split(":")[1]+","+best.split(":")[2]+","+best);
							bw.newLine();
							bw.flush();
							
							bw2.write(clmn[0]+","+clmn[1]+","+clmn[2]+","+clmn[3]+","+clmn[7]+","+best.split(":")[0]+","+clmn[8]);
							bw2.newLine();
							bw2.flush();
						}									
					}				
				}
				
			}	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	private static String maxMatch(String commonE, String[] clickExString) {
		double max = -1.0;
		String best = null;
		for(int i = 0; i < clickExString.length; i++){
		    if(clickExString[i].split(":")[0].equals(commonE)){
		       if (Double.parseDouble(clickExString[i].split(":")[1])> max)
		       {
		    	   best = clickExString[i];
		    	   max = Double.parseDouble(clickExString[i].split(":")[1]);
		       }
		    }
		}
		return best;
	}

	private static int getOverlap(ArrayList<String> clickedExampleList,
			List<String> rankedExampleList) {
		int overlap = 0;
		for (int i = 0; i < clickedExampleList.size(); i++)
		{
			if (i < rankedExampleList.size())
				if (clickedExampleList.contains(rankedExampleList.get(i)))
				overlap++; 
		}
		return overlap;
	}
}
