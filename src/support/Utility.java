package support;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utility {
	
	public static boolean writeToDisk(Object object, String filePath){
		try{
			FileOutputStream fout = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fout);   
			oos.writeObject(object);
			oos.close();
			fout.close();
		   }catch(Exception ex){
			   ex.printStackTrace();
			   return false;
		   }
		return true;
	}
	
	public static Object readFromDisk(String filePath){
		try {
			FileInputStream fin = new FileInputStream(filePath);
			ObjectInputStream oin = new ObjectInputStream(fin);
			Object o = oin.readObject();
			oin.close();
			fin.close();
			return o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String join(String joiner,List<String> collection){
		if(collection != null && collection.size() > 0){
			StringBuilder sb = new StringBuilder();
			sb.append(collection.get(0));
			for(int i = 1; i< collection.size() ; i++){
				sb.append(joiner).append(collection.get(i));
			}
			return sb.toString();
		}
		return "";
	}
	
	
	public static String format_spaceLevel(Object in, int level){
		String input = (in == null ? "null" : in.toString());
		for(int i = 0;i<level;i++){ input = " "+input; }
		return input;
	}
	
	public static String format_UnderLineSeperator(String input){
		if(input.length() > 80) return input;
		int length = input.length();
		int remaining = 80 - length;
		int half = remaining/2;
		int postfix = half;
		int prefix = 80 - half - postfix;
		StringBuilder sb = new StringBuilder();
		for(int i =0;i<prefix;i++){
			sb.append("-");
		}
		sb.append(input);
		for(int i =0;i<postfix;i++){
			sb.append("-");
		}
		return sb.toString();
	}
	
	
	public static String getPID(String packageName, String serial) {
		CommandLine.executeADBCommand(" shell ps | grep " + packageName, serial);
		String message = CommandLine.getLatestStdoutMessage();
		if(message!= null){
			String[] lines = message.split("\n");
			for(String line : lines){
				line = line.trim();
				if (!line.endsWith(packageName)){continue;}
				String[] parts = line.split(" ");
				for (int i = 1; i < parts.length; i++) {
					if (parts[i].equals("")){continue;}
					return parts[i].trim();
				}
			}
		}
		return null;
	}
	
	public List<String> processCSV(String fileLocation){
		List<String> result = new ArrayList<>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		try {
			br = new BufferedReader(new FileReader(fileLocation));
			while ((line = br.readLine()) != null) {
				String[] country = line.split(cvsSplitBy);
				StringBuilder sb = new StringBuilder(country[7]);
				sb.deleteCharAt(0);
				sb.deleteCharAt(sb.length()-1);
			    String k= sb.toString();
			    String ss= k.replace("/", ".");
				if(country[5].equals("0")==true && country[6].equals("0")==false){
					result.add("\""+ss+":"+country[6]+"\",");
				}
		
			}
		} catch (IOException e ) { e.printStackTrace();
		} finally {
			if (br != null) {
				try { br.close(); } catch (IOException e) { e.printStackTrace(); }
			}
		}
		return result;
	}
}
