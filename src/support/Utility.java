package support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import apex.Configuration;

public class Utility {
	
	public static String[] targetLineFormateCheck(String line){
		List<String> result = new ArrayList<>();
		if(line.startsWith("L")){
			line = line.substring(1, line.length());
		}
		
		if(line.startsWith("/")){
			line = line.substring(1, line.length());
		}
		if(line.endsWith(",")){
			line = line.substring(0, line.length()-1);
		}
		
		line = line.replace(",", "|");
		line = line.replace("/", ".");
		line = line.replace(";", ":");
		line = line.replaceAll("[\"\\s]", "");
		line = line.replace(".java", "");
		if(line.contains("src/")){
			line = line.split("src/")[1];
		}
		
		
		String parts[] = line.split(":");
		String name = parts[0];
		if(parts.length != 2) return null;
		String lineNum = parts[1];
		if(lineNum == null) return null;
		lineNum = lineNum.replace("~", "-");
		if(lineNum.contains("-")){
			String[] nums = lineNum.split("-");
			try{
				int num1 = Integer.parseInt(nums[0]);
				int num2 = Integer.parseInt(nums[1]);
				for( int i = Math.min(num1, num2); i<=Math.max(num1, num2); i++){
					result.add(name+":"+i);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}else if(lineNum.contains("|")){
			String[] nums = lineNum.split("\\|");
			for(String num : nums){
				try{
					int i = Integer.parseInt(num);
					result.add(name+":"+i);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}else{
			result.add(line.trim());
		}
		return result.toArray(new String[0]);
	}
	
	
	public static java.io.PrintStream createBundleStream(final OutputStream... streams){
		return new PrintStream(new OutputStream(){
			@Override
			public void write(int b) throws IOException {
				for(OutputStream stream : streams){
					stream.write(b);
				}
			}
		});
	}
	
	public static List<String> fileToLines(String name) throws FileNotFoundException{
		File f = new File(name);
		Scanner sc = new Scanner(f);
		List<String> arr = new ArrayList<>();
		while(sc.hasNextLine()){
			String line = sc.nextLine().trim();
			if(line.isEmpty() ==false) arr.add(line);
		}
		sc.close();
		return arr;
	}
	
	/**
	 * Dump a Serializable object to a file
	 * @param object
	 * @param filePath
	 * @return if successful
	 */
	public static boolean writeToDisk(Serializable object, String filePath){
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
	
	/**
	 * Read object from a file
	 * @param filePath
	 * @return object or null if failure
	 */
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
	
	/**
	 * Join a collection of string with the joiner
	 * a,b,d,e where ',' is the joiner
	 * @param joiner
	 * @param collection
	 * @return the joined string
	 */
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
	
	
	/**
	 * Turn the input object into string and append tab before it
	 * @param in - input object
	 * @param level - the amount of tab needs to be appended
	 * @return
	 */
	public static String format_TabLevel(Object in, int level){
		String input = (in == null ? "null" : in.toString());
		for(int i = 0;i<level;i++){ input = "\t"+input; }
		return input;
	}
	
	/**
	 * Append prefix and postfix of hyphen to a input String
	 * The resulted string should have a length of 80  
	 * @param input
	 * @return
	 */
	public static String format_HyphenWrapper(String input){
		if(input.length() > 80) return input;
		int length = input.length();
		int remaining = 80 - length;
		int half = remaining/2;
		int postfix = half;
		int prefix = 80 - length - postfix;
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
	
	/**
	 * Retrieve the process id identified by a package name
	 * @param packageName
	 * @param serial
	 * @return
	 */
	public static String getPID(String packageName, String serial) {
		
//		try {
//			Process p = Runtime.getRuntime().exec(
//					Configuration.getValue(Configuration.attADB) +
//							" -s "+serial+" shell ps | grep " + packageName );
//			Thread.sleep(100);
//			InputStream in = p.getInputStream();
//			int len = 0;
//			while((len = in.available()) <= 0){
//				Thread.sleep(100); }
//			
//			byte[] b = new byte[len];
//			String reading = new String(b).trim();
//			for(String line : reading.split("\n")){
//				line = line.trim();
//				if (!line.endsWith(packageName)){continue;}
//				String[] parts = line.split(" ");
//				for (int i = 1; i < parts.length; i++) {
//					if (parts[i].equals("")){continue;}
//					return parts[i].trim();
//				}
//			}
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
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
	
	/**
	 * Turn a CSV file into a list of string
	 * @param fileLocation
	 * @return
	 */
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
	
	public static String getDeviceSerial(){
		CommandLine.clear();
		CommandLine.executeCommand("adb devices");
		String lines = CommandLine.getLatestStdoutMessage();
		String part[] = lines.split("\n");
		if(part.length > 1){
			return part[1].trim().split("[ \t]")[0];
		}
		return null;
	}

}
