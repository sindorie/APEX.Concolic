package apex;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import support.CommandLine;

public class Main {
	//the apk path
	static String apkPath = 
//			"/home/zhenxu/workspace/ArraySolvingApk/app/build/outputs/apk/ArraySolvingAPK.apk";
			"/home/zhenxu/workspace/AndroidTestAPKPlayGround/Jensen/net.mandaria.tippytipper.apk";
//			"/home/zhenxu/workspace/StringSymbolicTestingApp/app/build/outputs/apk/app-debug.apk";
//			"/home/zhenxu/workspace/ThreadTest/app/build/outputs/apk/ThreadAndExceptionTest.apk";
	static String[] targets = null;
	static String serial = null;
	public static void main(String[] args) {
		targets = readInput("targets.txt");
		environmentSetup();
		if(serial == null) serial = getDeviceSerial();
		System.out.println("Serial: "+serial);
		IncrementalProcedure procedure = new IncrementalProcedure(apkPath, targets, serial);
		procedure.go();
		new Statistic().check();
	}
	
	static void environmentSetup(){
		//over write path
		Paths.AppDataDir = "generated";
		Paths.apktoolPath = "libs/apktool_2.0.1.jar";
	}
	
	static String[] readInput(String name){
		File f = new File(name);
		try {
			List<String> arr = new ArrayList<>();
			Scanner sc = new Scanner(f);
			while(sc.hasNextLine()){
				String line = sc.nextLine();
				line = line.replaceAll(",| |\"", "");
				arr.add(line);
			}
			sc.close();
		return arr.toArray(new String[0]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	
		return null;
	}
	
	static String getDeviceSerial(){
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
