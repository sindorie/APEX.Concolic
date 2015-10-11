package apex;

public class Main {

	static String apkPath = 
			"/home/zhenxu/workspace/ArraySolvingApk/app/build/outputs/apk/ArraySolvingAPK.apk";
//			"/home/zhenxu/workspace/AndroidTestAPKPlayGround/Jensen/net.mandaria.tippytipper.apk";
	public static void main(String[] args) {
		readInput();
		environmentSetup();
		IncrementalProcedure procedure = new IncrementalProcedure(apkPath, null, "emulator-5554");
		procedure.go();
	}
	
	static void environmentSetup(){
		//over write path 
		Paths.AppDataDir = "generated";
		Paths.apktoolPath = "libs/apktool_2.0.1.jar";
	}
	
	static void readInput(){
		//TODO
	}
}
