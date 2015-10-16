package apex;

import java.util.HashSet;
import java.util.Set;

import components.EventSummaryManager;
import apex.staticFamily.StaticAppBuilder;
import apex.symbolic.SymbolicExecution;

public class IncrementalProcedure {
	long startTime, maxDuration = -1;
	boolean checkPrevious = false;
	int iterationCount = 0;
	
	
	public IncrementalProcedure(String apkPath, String[] targets, String serial){
		Common.apkPath = apkPath;
		Common.serial = serial;
		if(targets != null){
			Set<String> ta = new HashSet<String>();
			for(String e : targets){ ta.add(e); }
			Common.targets = ta;
			Common.remaining = new HashSet<String>(ta);
		}
	}
	
	public IncrementalProcedure(String apkPath, Iterable<String> targets, String serial){
		Common.apkPath = apkPath;
		Common.serial = serial;
		if(targets != null){
			Set<String> ta = new HashSet<String>();
			for(String e : targets){ ta.add(e); }
			Common.targets = ta;
			Common.remaining = new HashSet<String>(ta);
		}
	}
	
	void setup(){
		Common.app = StaticAppBuilder.fromAPK(Common.apkPath);
		Common.app.instrument();
		Common.symbolic = new SymbolicExecution(Common.app);
		Common.summaryManager = new EventSummaryManager();
		Common.model = new UIModel();
		
		Common.driver = new ExuectionDriver();
		Common.eExecution = new EventExecution();
	}
	void check(){
		Common.println("Basic info checking");
		Common.println("APK Path: "+Common.app.getApkPath());
		Common.println("Instrumented APK Path: "+Common.app.getInstrumentedApkPath());
		Common.println("Package name: "+Common.app.getPackageName());
		Common.println("Main Activity existence: "+(Common.app.getMainActivity() != null));
		Common.println("Main Activity name: "+Common.app.getMainActivity().getJavaName());
		
		//TODO environment checking, tool checking
	}
	void loop(){
		startTime = System.currentTimeMillis();
		while(  !isAllreached() && !isLimitReached()  ){
			boolean status = Common.driver.kick();
			iterationCount += 1;
			if(status == false) break;
		}
	}
	
	boolean isLimitReached(){  return (iterationCount > 0xFFFF) || (this.maxDuration > 0 && System.currentTimeMillis() - this.startTime > this.maxDuration) ;  }
	boolean isAllreached(){ if(Common.targets == null || Common.targets.isEmpty()) return false; return Common.remaining.isEmpty(); }
	public int getCurrentLoopCount(){ return this.iterationCount; }
	public void setMaxDuation(long time){ maxDuration = time; }
	
	public void go(){
		if(checkPrevious){
			//TODO
			
			
		}else{
			setup();
			check();
			Common.driver.prepare();
			try{ loop(); }catch(Exception e){ e.printStackTrace(); }
			Common.driver.finish();
			Common.saveData("generated/"+Common.apkPath.replaceAll("[^\\w]", ""));
		}
		
	}
}
