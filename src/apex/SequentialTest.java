package apex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import support.Utility;
 

public class SequentialTest {

	public static void main(String[] args) throws FileNotFoundException {
		String scriptLocation = "script.txt";
		String serial = Utility.getDeviceSerial();
		if(serial == null) throw new IllegalArgumentException();
		else System.out.println("Serial: "+serial);
		
		File folder = new File("generated");
		folder.mkdirs();
		Paths.AppDataDir = "generated";
		Paths.apktoolPath = "libs/apktool_2.0.1.jar";
		
		File script = new File(scriptLocation);
		Scanner sc = new Scanner(script);
		List<String> targets = new ArrayList<>();
		String fileLocation = null;
		while(sc.hasNextLine()){
			String line = sc.nextLine().trim();
			if(line.isEmpty()) continue;
			if(line.startsWith("APP:")){
				fileLocation = line.replace("APP:", "").trim();
			}else if(line.startsWith("END")){
				if(fileLocation == null){
					System.out.println("Script Format error");
					targets = new ArrayList<>();
					continue;
				}
				
				final PrintStream stdout = System.out;
				final PrintStream stderr = System.err;
				
				try{
					int index = fileLocation.lastIndexOf("/");
					String appID = fileLocation.substring(index+1, fileLocation.length());//.replaceAll("[^\\w]", "");
					File logFolder = new File("generated/"+appID);
					logFolder.mkdirs();
					File logFile = new File("generated/"+appID+"/"+appID+".log");
					
					FileOutputStream fOut = new FileOutputStream(logFile);
					PrintStream outStream = Utility.createBundleStream(fOut, stdout);
					PrintStream errStream = Utility.createBundleStream(fOut, stderr);
					System.setErr(errStream);
					System.setOut(outStream);
					
					Common.reset();
					
					IncrementalProcedure procedure = new IncrementalProcedure(fileLocation, new ArrayList<String>(targets), serial);
					procedure.go();
					Statistic stat = new Statistic();
					stat.check();
					
					fOut.flush();
					String statReport = stat.toString();
					fOut.write(statReport.getBytes());
					
					Common.saveData("generated/"+appID+"/"+appID+".data");
					fOut.close();
				}catch(Exception e){
					System.setOut(stdout);
					System.setErr(stderr);
					e.printStackTrace();
				}catch(Error e1){
					System.setOut(stdout);
					System.setErr(stderr);
					System.gc();
					e1.printStackTrace();
				}
				
				System.setOut(stdout);
				System.setErr(stderr);
				
				targets = new ArrayList<>();
				fileLocation = null;
				
			}else{
				try{
					String[] checked = Utility.targetLineFormateCheck(line);
					for(String chec : checked){ targets.add(chec); }
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		sc.close();
		
		System.out.println("End sequential Test");
	}

}
