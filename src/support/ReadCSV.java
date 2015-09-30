package support;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
 
public class ReadCSV {
 
  public static void main(String[] args) {
 
	ReadCSV obj = new ReadCSV();
	obj.run();
 
  }
 
  public void run() {
 
	String csvFile = "/home/zhenxu/Downloads/traces.csv";
	BufferedReader br = null;
	String line = "";
	String cvsSplitBy = ",";
 
	try {
 
		br = new BufferedReader(new FileReader(csvFile));
		while ((line = br.readLine()) != null) {
			String[] country = line.split(cvsSplitBy);

			StringBuilder sb = new StringBuilder(country[7]);
			sb.deleteCharAt(0);
			sb.deleteCharAt(sb.length()-1);
			
		     String k= sb.toString();
		     String ss= k.replace("/", ".");
		     
		   
			if(country[5].equals("0")==true && country[6].equals("0")==false){
			System.out.println("\""+ss+":"+country[6]+"\",");
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
 
	System.out.println("Done");
  }
 
}