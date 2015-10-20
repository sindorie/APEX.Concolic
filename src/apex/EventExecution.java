package apex;

import java.util.List;
import java.util.Set;

import support.CommandLine;
import android.view.KeyEvent;
import apex.staticFamily.StaticApp;
import components.Event;
import components.EventExecutor;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.ViewDeviceInfo;
import components.system.InformationCollector;
import components.system.LogcatReader;
import components.system.WindowInformation;

public class EventExecution {
	private static Event closeKeyboard = EventFactory.createCloseKeyboardEvent();
	private EventExecutor ex;
	private InformationCollector sysInfo;
	private ViewDeviceInfo viewInfoView;
	private LogcatReader logcatReader;
	
	public EventExecution(){
		ex = new EventExecutor(Common.serial);
		sysInfo = new InformationCollector(Common.serial);
		viewInfoView = new ViewDeviceInfo(Common.serial);
		logcatReader = new LogcatReader(Common.serial);
		//maxTime, minTime, duration, sleepTime, startSleep
		logcatReader.setTime(5000, 100, 300, 50, 100);
		
		if(Common.useJDB){
			ex.setInitJDBOnLaunch(true, Common.targets);
		}
	}
	
	public Set<String> getLineHitFromJDB(){
		return ex.getBreakPointReader()!=null?this.ex.getBreakPointReader().getHits():null; 
	}
	
	/**
	 * Ask the program to reintall the application
	 */
	public void reinstall(){
		String pkgName = Common.app.getPackageName();
		ex.applyEvent(EventFactory.createReinstallEvent(pkgName, Common.app.getInstrumentedApkPath()));
		Common.TRACE("Stdout:"+CommandLine.getLatestStdoutMessage());
		Common.TRACE("Stderr:"+CommandLine.getLatestStdoutMessage());
		Common.model.hasReinstalled();
		ex.applyEvent(EventFactory.CreatePressEvent(null, KeyEvent.KEYCODE_HOME));	
	}
	
	/**
	 * Should be called before the termination of concolic execution if normal execution
	 */
	public void clearup(){
		String pkgName = Common.app.getPackageName();
		ex.applyEvent(EventFactory.createUninstallEvent(pkgName));
	}
	
	/**
	 * reposition from current layout to the target layout
	 * @param current
	 * @param target
	 * @return
	 */
	public boolean reposition(GraphicalLayout current, GraphicalLayout target){
		Common.TRACE(current.toString()+" to "+target.toString());
		List<EventSummaryPair> repositionSequence = null;
		{ //try find a pat from current to target
			repositionSequence = Common.model.findSequence(current, target);
			if(repositionSequence != null){
				EventExecutionResult midResult = this.doSequence(repositionSequence, true);
				GraphicalLayout focuedWin 
					= midResult.predefinedUI != null ? midResult.predefinedUI
					: Common.model.findOrConstructUI(midResult.focusedWin.actName, midResult.node);
				if(target == focuedWin) return true;
			}
		}
		Common.TRACE("Seoncd phase");
		
		{ //try to find a known sequence from launcher
			repositionSequence = Common.model.findKownSequence(target);
			if(repositionSequence != null && repositionSequence.isEmpty() == false){
				reinstall();
				EventExecutionResult midResult = this.doSequence(repositionSequence, true);
				GraphicalLayout focuedWin 
					= midResult.predefinedUI != null ? midResult.predefinedUI
					: Common.model.findOrConstructUI(midResult.focusedWin.actName, midResult.node);
				if(target == focuedWin) return true;
			}
		}
		Common.TRACE("failure to "+target.toString());
		return false; // failure
	}
	
	/**
	 * do an event; read logcat, window info, focused window info, keyboard information
	 * will check if the application has crashed
	 * @param event
	 * @return
	 */
	EventExecutionResult carrayout(Event event){
		Common.TRACE();
		logcatReader.clearLogcat();
		ex.applyEvent(event, false);
		EventExecutionResult result = new EventExecutionResult();
		
		logcatReader.readFeedBack(); // waiting is taken into consideration
		result.logcatReading = logcatReader.getExeLog();
		result.sequences = logcatReader.getMethodLog();
		//the UI and device internal might now stabilize after the logcat finishes reading
		try { Thread.sleep(300); } catch (InterruptedException e1) {   }
		
		Common.TRACE(logcatReader.getThreadOrder().toString());
		if( logcatReader.isCrashed() ){
			Common.TRACE("Logcat detects crashing");
			result.predefinedUI = GraphicalLayout.ErrorScene;
			result.isCrashed = true;
			return result;
		}
		
		if(Common.useJDB){
			try { Thread.sleep(100);
			} catch (InterruptedException e) {}
			if(this.ex.getBreakPointReader().isJustCrashed()){
				Common.TRACE("JDB indicates crashing");
				result.predefinedUI = GraphicalLayout.ErrorScene;
				result.isCrashed = true;
				return result;
			}
		}
		
		Common.TRACE("No Crashing detected");

		//check if the UI within the app		
		result.iInfo = sysInfo.getInputMethodOverview();
		result.wInfo = sysInfo.getWindowOverview();
		result.keyboardVisible = result.wInfo.isKeyboardVisible();
		if (result.keyboardVisible) {ex.applyEvent(closeKeyboard);}
		result.focusedWin = result.wInfo.getFocusedWindow();
		
		int tryCount = 0;
		while(result.focusedWin == null && tryCount < 3){ 
			result.focusedWin = result.wInfo.getFocusedWindow(); 
			try { Thread.sleep(100+100 * tryCount); } catch (InterruptedException e) { }
			tryCount += 1;
		}
		if(result.focusedWin == null){
			result.errorCode = EventExecutionResult.ERROR_FOCUS_WDINDOW;
			return result; 
		}
		
		result.scope = result.focusedWin.isWithinApplciation(Common.app);
		if( result.scope == WindowInformation.SCOPE_LAUNCHER ){
			result.predefinedUI = GraphicalLayout.Launcher;
		}
		if( result.scope == WindowInformation.SCOPE_WITHIN ){
			int i = 0;
			while(result.node == null && i<3){
				try{
					Thread.sleep(200 + 500 * i);
					result.node = viewInfoView.loadWindowData();
				}catch(Exception e){ e.printStackTrace(); }
				i++;
			}
			if(result.node == null){
				result.errorCode = EventExecutionResult.ERROR_LOAD_LAYOUT;
				return result; 
			}
		}   
		return result;
	}
	
	/**
	 * do a list of events. the check flag indicates if the the result of the sequence should be retrieved
	 * @param eList
	 * @param check
	 * @return
	 */
	public EventExecutionResult doSequence(List<EventSummaryPair> eList, boolean check){
		if(check == false){
			Common.TRACE();
			for(int i = 0 ; i<eList.size() ; i++){
				ex.applyEvent(eList.get(i).getEvent());
				if( sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				if(Common.model != null) Common.model.record(eList.get(i));
			}
			return null;
		}else{
			Common.TRACE();
			for(int i = 0 ; i<eList.size() -1 ; i++){
				ex.applyEvent(eList.get(i).getEvent());	
				if( sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				if(Common.model != null) Common.model.record(eList.get(i));
			}
			EventExecutionResult result = carrayout(eList.get(eList.size()-1).getEvent());
			if(Common.model != null) Common.model.record(eList.get(eList.size()-1));
			return result;
		}
	}
}
