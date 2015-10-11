package apex;

import java.util.ArrayList;
import java.util.List;

import components.EventSummaryManager;
import components.EventSummaryPair;
import components.ExpressionTranfomator;
import apex.staticFamily.StaticAppBuilder;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
import apex.symbolic.SymbolicExecution;

public class IncrementalProcedure {
	ExuectionDriver driver;
	long startTime;
	boolean checkPrevious = false;
	int iterationCount = 0;
	boolean enableDefaultStatistic = true;
	
	public IncrementalProcedure(String apkPath, Iterable<String> targets, String serial){
		Common.apkPath = apkPath;
		Common.serial = serial;
		if(targets != null){
			List<String> ta = new ArrayList<String>();
			for(String e : targets){ ta.add(e); }
			Common.targets = ta;
			Common.remaining = new ArrayList<String>(ta);
		}
	}
	
	void setup(){
		Common.app = StaticAppBuilder.fromAPK(Common.apkPath);
		Common.app.instrument();
		Common.symbolic = new SymbolicExecution(Common.app);
		Common.summaryManager = new EventSummaryManager();
		Common.model = new UIModel();
		
		driver = new ExuectionDriver();
	}
	void check(){
		System.out.println("Basic info checking");
		System.out.println("APK Path: "+Common.app.getApkPath());
		System.out.println("Instrumented APK Path: "+Common.app.getInstrumentedApkPath());
		System.out.println("Package name: "+Common.app.getPackageName());
		System.out.println("Main Activity existence: "+(Common.app.getMainActivity() != null));
		System.out.println("Main Activity name: "+Common.app.getMainActivity().getJavaName());
		
		
		//TODO environment checking, tool checking
	}
	void loop(){
		startTime = System.currentTimeMillis();
		while(  !isAllreached() && !isLimitReached()  ){
			boolean status = driver.kick();
			iterationCount += 1;
			if(status == false) break;
		}
	}
	void statistic(){ 
		Common.model.showGUI();
		System.out.println("Total edges: "+Common.model.getAllEdges().size());
		System.out.println("ConcreteSegmentalSummary");
		for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSegmentalSummary()){
			System.out.println(esPair);
		}
		System.out.println("SymbolicSegmentalSummary");
		for(EventSummaryPair esPair : Common.summaryManager.getAllSymbolicSegmentalSummary()){
			System.out.println(esPair);
		}
		System.out.println("ConcreteSummary");
		for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSummary()){
			System.out.println(esPair);
		}
		System.out.println("InvalidSummary");
		for(EventSummaryPair esPair : Common.summaryManager.getAllInvalidSegmentalSummary()){
			System.out.println(esPair);
			PathSummary summary = esPair.getPathSummary();
			List<Expression> constraints = summary.getPathConditions();
			List<Expression> symbolices = summary.getSymbolicStates();
			List<Expression>[] transformed = ExpressionTranfomator.transform(constraints, symbolices);
			List<Expression> transformedCon = transformed[ExpressionTranfomator.CONSTRAINT_INDEX];
			System.out.println("constraints");
			for(Expression expre : constraints){
				System.out.println(expre.toYicesStatement());
			}
			System.out.println("symbolices");
			for(Expression expre : symbolices){
				System.out.println(expre.toYicesStatement());
			}
			System.out.println("transformedCon");
			for(Expression expre : transformedCon){
				System.out.println(expre.toYicesStatement());
			}
			
		}
		System.out.println("Queue Remaining");
		for(EventSummaryPair esPair : Common.summaryManager.getRemainingInQueue()){
			System.out.println(esPair);
		}
	}
	boolean isLimitReached(){
		return iterationCount > 0xFFFF;
	}
	boolean isAllreached(){
		if(Common.targets == null || Common.targets.isEmpty()) return false;
		return Common.remaining.isEmpty();
	}
	public int getCurrentLoopCount(){ return this.iterationCount; }
	
	void go(){
		if(checkPrevious){
			
		}else{
			setup();
			check();
			driver.prepare();
			try{
				loop();
			}catch(Exception e){
				e.printStackTrace();
			}
			if(enableDefaultStatistic)statistic();
		}
	}
}
