package apex;

public class Main {

	static String apkPath = "/home/zhenxu/workspace/ArraySolvingApk/app/build/outputs/apk/ArraySolvingAPK.apk";
	public static void main(String[] args) {
		readInput();
		IncrementalProcedure procedure = new IncrementalProcedure(apkPath, null, "emulator-5554");
		procedure.go();
	}
	
	static void readInput(){
		//TODO
	}
}
