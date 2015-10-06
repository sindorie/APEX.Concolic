package apex;

import java.util.ArrayList;
import java.util.List;

import components.EventSummaryManager;
import components.EventSummaryPair;
import apex.staticFamily.StaticAppBuilder;
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
		Common.esManager = new EventSummaryManager();
		Common.model = new UIModel();
		
		driver = new ExuectionDriver();
		driver.prepare();
	}
	void check(){
		//TODO 
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
		for(EventSummaryPair esPair : Common.esManager.getAllConcreteSegmentalSummary()){
			System.out.println(esPair);
		}
		System.out.println("SymbolicSegmentalSummary");
		for(EventSummaryPair esPair : Common.esManager.getAllSymbolicSegmentalSummary()){
			System.out.println(esPair);
		}
		System.out.println("ConcreteSummary");
		for(EventSummaryPair esPair : Common.esManager.getAllConcreteSummary()){
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
			try{
				loop();
			}catch(Exception e){
				e.printStackTrace();
			}
			if(enableDefaultStatistic)statistic();
		}
	}
}
