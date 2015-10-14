package apex;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
	//the apk path
	static String apkPath = 
//			"/home/zhenxu/workspace/ArraySolvingApk/app/build/outputs/apk/ArraySolvingAPK.apk";
			"/home/zhenxu/workspace/AndroidTestAPKPlayGround/Jensen/net.mandaria.tippytipper.apk";
//			"/home/zhenxu/workspace/StringSymbolicTestingApp/app/build/outputs/apk/app-debug.apk";
//			"/home/zhenxu/workspace/ThreadTest/app/build/outputs/apk/ThreadAndExceptionTest.apk";
	static String[] targets = null;
	
	public static void main(String[] args) {
//		targets = readInput("targets.txt");
		environmentSetup();
		IncrementalProcedure procedure = new IncrementalProcedure(apkPath, targets, "emulator-5554");
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
}
