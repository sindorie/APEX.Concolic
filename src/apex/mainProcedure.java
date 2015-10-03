package apex;

import components.EventSummaryDeposit;
import apex.staticFamily.StaticAppBuilder;
import apex.symbolic.SymbolicExecution;

public class mainProcedure {
	String apkPath;
	ExuectionDriver driver; 
	String serial;
	//setup
	void setup(){
		Common.app = StaticAppBuilder.fromAPK(apkPath);
		Common.app.instrument();
		Common.apkPath = apkPath;
		Common.symbolic = new SymbolicExecution(Common.app);
		Common.esDeposit = new EventSummaryDeposit();
		//TODO
		Common.model = new UIModel();
		driver = new ExuectionDriver();
	}
	int iterationCount = 0;
	
//	static UIModel model;
//	static SymbolicExecution symbolic;
//	static EventDeposit eDeposit;
//	static EventSummaryDeposit esDeposit;
//	static EventExecutor ex;
//	
//	static String apkPath;
//	static String serial;
//	static List<String> targets;
//	static List<String> remaining;
//	
//	static StaticApp app;
//	static InformationCollector sysInfo;
//	static ViewDeviceInfo viewInfoView;
//	static LogcatReader logcatReader;
	
	void check(){
		
	}

	void loop(){
		while(  !isAllreached() && !isLimitReached()  ){
			driver.kick();
			iterationCount += 1;
		}
	}
	
	void postStatic(){
		
	}
	
	boolean isLimitReached(){
		return false;
	}
	
	
	
	boolean isAllreached(){
		if(Common.targets == null || Common.targets.isEmpty()) return false;
		return Common.remaining.isEmpty();
	}
	
	static void go(){}
}
