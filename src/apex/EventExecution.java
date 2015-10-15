package apex;

import java.util.List;

import support.CommandLine;
import android.view.KeyEvent;
import apex.staticFamily.StaticApp;
import components.Event;
import components.EventExecutor;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.LayoutNode;
import components.ViewDeviceInfo;
import components.system.InformationCollector;
import components.system.InputMethodOverview;
import components.system.LogcatReader;
import components.system.WindowInformation;
import components.system.WindowOverview;

public class EventExecution {
		
	Event closeKeyboard = EventFactory.createCloseKeyboardEvent();
	EventExecutor ex;
	InformationCollector sysInfo;
	ViewDeviceInfo viewInfoView;
	LogcatReader logcatReader;
	UIModel model;
	StaticApp app; 
	
	public EventExecution(String serial, UIModel model, StaticApp app){
		ex = new EventExecutor(serial);
		sysInfo = new InformationCollector(serial);
		viewInfoView = new ViewDeviceInfo(serial);
		logcatReader = new LogcatReader(serial);
		//maxTime, minTime, duration, sleepTime, startSleep
		logcatReader.setTime(5000, 100, 200, 50, 100);
		this.model = model;
		this.app = app;
	}
	
	public void reinstall(){
		String pkgName = this.app.getPackageName();
		ex.applyEvent(EventFactory.createReinstallEvent(pkgName, app.getInstrumentedApkPath()));
		Common.TRACE("Stdout:"+CommandLine.getLatestStdoutMessage());
		Common.TRACE("Stderr:"+CommandLine.getLatestStdoutMessage());
		model.hasReinstalled();
		ex.applyEvent(EventFactory.CreatePressEvent(null, KeyEvent.KEYCODE_HOME));	
	}
	
	public void clearup(){
		String pkgName = this.app.getPackageName();
		ex.applyEvent(EventFactory.createUninstallEvent(pkgName));
	}
	
	public boolean reposition(GraphicalLayout current, GraphicalLayout target){
		Common.TRACE();
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
			if(repositionSequence != null){
				reinstall();
				EventExecutionResult midResult = this.doSequence(repositionSequence, true);
				GraphicalLayout focuedWin 
					= midResult.predefinedUI != null ? midResult.predefinedUI
					: Common.model.findOrConstructUI(midResult.focusedWin.actName, midResult.node);
				if(target == focuedWin) return true;
			}
		}
		Common.TRACE("failure");
		return false; // failure
	}
	
	EventExecutionResult carrayout(Event event){
		Common.TRACE();
		logcatReader.clearLogcat();
		ex.applyEvent(event, false);
		EventExecutionResult result = new EventExecutionResult();
		
		logcatReader.readFeedBack(); // waiting is taken into consideration
		result.logcatReading = logcatReader.getExeLog();
//		if(Common.DEBUG){ for(String line : result.logcatReading){ Common.TRACE("EXELOG: "+line); } }
		result.sequences = logcatReader.getMethodLog();
		//the UI and device internal might now stabilize after the logcat finishes reading
		try { Thread.sleep(300); } catch (InterruptedException e1) {   }
		
		if( logcatReader.isCrashed() ){
			result.predefinedUI = GraphicalLayout.ErrorScene;
			result.isCrashed = true;
			return result;
		}

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
		
		result.scope = result.focusedWin.isWithinApplciation(app);
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
	
	public EventExecutionResult doSequence(List<EventSummaryPair> eList, boolean check){
		if(check == false){
			Common.TRACE();
			for(int i = 0 ; i<eList.size() ; i++){
				ex.applyEvent(eList.get(i).getEvent());
				if( sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				if(model != null) model.record(eList.get(i));
			}
			return null;
		}else{
			Common.TRACE();
			for(int i = 0 ; i<eList.size() -1 ; i++){
				ex.applyEvent(eList.get(i).getEvent());	
				if( sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				if(model != null) model.record(eList.get(i));
			}
			return carrayout(eList.get(eList.size()-1).getEvent());
		}
	}
}
