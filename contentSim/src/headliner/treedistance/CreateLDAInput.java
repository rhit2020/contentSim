package headliner.treedistance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateLDAInput {

	public static void main (String[] args)
	{
		try { 
			File file = new File("resource/allcontent_LDA_input.txt");
 			// if file does not exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);			
			
			DB db = new DB();
			db.connectToWebex21();
			if (db.isConnectedToWebex21())
			{
				db.connectToGuangie();
				if (db.isConnectedToGuanjie())
				{
					List<String> contentList = db.getContentsRdfs();
					List<String> outcomeConceptList;
					List<String> conceptList;
					List<String> outComeRepeatList;
					for (String c : contentList)
					{
						outComeRepeatList  = new ArrayList<String>();
						conceptList = db.getAllConcepts(c);
						outcomeConceptList = db.getOutcomeConcept(c);
						for (String outcome : outcomeConceptList)
						{
							outComeRepeatList.add(outcome);
						}
						bw.write(c+"\t"+c+"\t");

						//write to file
						for (String con : conceptList)
							bw.write(con+"\t");
						for (String repeatedOutcome : outComeRepeatList)
						{
							for (int i = 1 ; i <=3; i++)
								bw.write(repeatedOutcome+"\t");						
						}		
						bw.newLine();
					}	
					db.disconnectFromGuanjie();
				}
				db.disconnectFromWebex21();
			}
			bw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}		
	}
}
