package apex;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import support.Utility;
import components.EventSummaryManager;
import components.EventSummaryPair;
import components.ExpressionTranfomator;
import apex.staticFamily.StaticAppBuilder;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
import apex.symbolic.SymbolicExecution;

public class IncrementalProcedure {
	ExuectionDriver driver;
	long startTime, maxDuration = -1;
	boolean checkPrevious = false;
	int iterationCount = 0;
	boolean enableDefaultStatistic = true;
	boolean showGraphUI = true;
	PrintWriter[] writers = null;
	
	
	public IncrementalProcedure(String apkPath, String[] targets, String serial){
		Common.apkPath = apkPath;
		Common.serial = serial;
		if(targets != null){
			List<String> ta = new ArrayList<String>();
			for(String e : targets){ ta.add(e); }
			Common.targets = ta;
			Common.remaining = new ArrayList<String>(ta);
		}
	}
	
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
		println("Basic info checking");
		println("APK Path: "+Common.app.getApkPath());
		println("Instrumented APK Path: "+Common.app.getInstrumentedApkPath());
		println("Package name: "+Common.app.getPackageName());
		println("Main Activity existence: "+(Common.app.getMainActivity() != null));
		println("Main Activity name: "+Common.app.getMainActivity().getJavaName());
		
		
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
		println("Total edges: "+Common.model.getAllEdges().size());
		if(Common.summaryManager.getAllConcreteSegmentalSummary().isEmpty()){
			println(Utility.format_UnderLineSeperator("Concrete segmental PathSummary is Empty"));
		}else{
			println(Utility.format_UnderLineSeperator("Concrete segmental PathSummary:"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSegmentalSummary()){
				println(Utility.format_spaceLevel(esPair, 1));
			}
		}
		if(Common.summaryManager.getAllSymbolicSegmentalSummary().isEmpty()){
			println(Utility.format_UnderLineSeperator("Symbolic segmental PathSummary is Empty"));
		}else{
			println(Utility.format_UnderLineSeperator("Symbolic segmental PathSummary:"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllSymbolicSegmentalSummary()){
				println(Utility.format_spaceLevel(esPair, 1));
			}
		}
		if(Common.summaryManager.getAllConcreteSummary().isEmpty()){
			println(Utility.format_UnderLineSeperator("Concrete PathSummary is Empty"));
		}else{
			println(Utility.format_UnderLineSeperator("Concrete PathSummary"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSummary()){
				println(Utility.format_spaceLevel(esPair, 1));
			}
		}
		if(Common.summaryManager.getAllInvalidSegmentalSummary().isEmpty()){
			println(Utility.format_UnderLineSeperator("Invalid Segmental PathSummary is Empty"));
		}else{
			println(Utility.format_UnderLineSeperator("Invalid Segmental Path Summary"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllInvalidSegmentalSummary()){
				println(Utility.format_spaceLevel(esPair, 1));
				PathSummary summary = esPair.getPathSummary();
				List<Expression> constraints = summary.getPathConditions();
				List<Expression> symbolices = summary.getSymbolicStates();
				List<Expression>[] transformed = ExpressionTranfomator.transform(constraints, symbolices);
				List<Expression> transformedCon = transformed[ExpressionTranfomator.CONSTRAINT_INDEX];
				
				if(constraints.isEmpty()){
					println(Utility.format_spaceLevel("Path Summary Constraints is Empty",1));
				}else{
					println(Utility.format_spaceLevel("Path Summary Constraints:",1));
					for(Expression expre : constraints){
						println(Utility.format_spaceLevel(expre.toYicesStatement(), 2));
					}
				}
				
				if(symbolices.isEmpty()){
					println(Utility.format_spaceLevel("Path Summary Symbolic states is Empty", 1));
				}else{
					println(Utility.format_spaceLevel("Path Summary Symbolic states:", 1));
					for(Expression expre : symbolices){
						println(Utility.format_spaceLevel(expre.toYicesStatement(), 2));
					}
				}
				
				if(transformedCon.isEmpty()){
					println(Utility.format_spaceLevel("Transformed Constraints is Empty", 1));
				}else{
					println(Utility.format_spaceLevel("Transformed Constraints:", 1));
					for(Expression expre : transformedCon){
						println(Utility.format_spaceLevel(expre.toYicesStatement(), 2));
					}
				}
			}
		}
		
		if(Common.summaryManager.getRemainingInQueue().isEmpty()){
			println(Utility.format_UnderLineSeperator("Remaining Event Path Pair in Queue is Empty"));
		}else{
			println(Utility.format_UnderLineSeperator("Remaining Event Path Pair:"));
			for(EventSummaryPair esPair : Common.summaryManager.getRemainingInQueue()){
				println(Utility.format_spaceLevel(esPair, 1));
			}
		}
	}
	boolean isLimitReached(){  return (iterationCount > 0xFFFF) || (this.maxDuration > 0 && System.currentTimeMillis() - this.maxDuration>0) ;  }
	boolean isAllreached(){ if(Common.targets == null || Common.targets.isEmpty()) return false; return Common.remaining.isEmpty(); }
	public int getCurrentLoopCount(){ return this.iterationCount; }
	public void setMaxDuation(long time){ maxDuration = time; }
	
	public void go(){
		if(checkPrevious){
			//TODO
		}else{
			setup();
			check();
			driver.prepare();
			try{ loop(); }catch(Exception e){ e.printStackTrace(); }
			if(showGraphUI) Common.model.showGUI();
			if(enableDefaultStatistic)statistic();
		}
	}
	
	public void setStatisticOutputChannel(OutputStream... streams){
		if(streams == null || streams.length == 0) return ;
		this.writers = new PrintWriter[streams.length];
		for(int i =0 ; i<streams.length ; i++){
			writers[i] = new PrintWriter(streams[i]);
		}
	}
	private void println(String message){
		if(writers == null){System.out.println(message);}
		else{
			for(PrintWriter pw : this.writers){
				pw.println(message);
			}
		}
	}
}
