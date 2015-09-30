package apex;

import apex.staticFamily.StaticApp;
import apex.staticFamily.StaticAppBuilder;

public class mainProcedure {
	String apkPath;
	//setup
	void setup(){
		StaticApp app = StaticAppBuilder.fromAPK(apkPath);
		app.instrument();
	}
	
	void check(){
		
	}

	void loop(){
		
	}
	
	static void go(){}
}
