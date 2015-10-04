package apex;

import java.util.List;

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
		this.model = model;
		this.app = app;
	}
	
	EventExecutionResult carrayout(Event event){
		logcatReader.clearLogcat();
		ex.applyEvent(event);
		try { Thread.sleep(100);
		} catch (InterruptedException e1) { }
		
		//read lines
		List<String> lines = logcatReader.readLogcatFeedBack();
		List<List<String>> sequences = logcatReader.extractMethodSequence(lines);
		try { Thread.sleep(300); } catch (InterruptedException e1) { }

		//check if the UI within the app		
		InputMethodOverview iInfo = sysInfo.getInputMethodOverview();
		WindowOverview wInfo = sysInfo.getWindowOverview();
		boolean keyboardVisible = wInfo.isKeyboardVisible();
		if (keyboardVisible) {ex.applyEvent(closeKeyboard);}
		WindowInformation focusedWin = wInfo.getFocusedWindow();
		int count = 0;
		while(focusedWin == null){
			if(count > 3){
				System.out.println("Fail to retrieve layout");
				return null;
			}
			try { Thread.sleep(20); } catch (InterruptedException e1) { }
			focusedWin = sysInfo.getWindowOverview().getFocusedWindow();
			count += 1;
		}	
		
		LayoutNode node = null;
		int scope = focusedWin.isWithinApplciation(app);
		if( scope == WindowInformation.SCOPE_WITHIN ){
			node = viewInfoView.loadWindowData();
		}
		EventExecutionResult result = new EventExecutionResult();
		if( scope == WindowInformation.SCOPE_LAUNCHER ){
			result.predefinedUI = GraphicalLayout.Launcher;
		}
		result.node = node;
		result.scope = scope;
		result.iInfo = iInfo;
		result.wInfo = wInfo;
		result.keyboardVisible = keyboardVisible;
		result.sequences = sequences;
		result.log = lines;
		return result;
	}
	
	public EventExecutionResult doSequence(List<EventSummaryPair> eList, boolean check){
		if(check){
			for(int i = 0 ; i<eList.size() ; i++){
				ex.applyEvent(eList.get(i).getEvent());
				if( sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				if(model != null) model.record(eList.get(i));
			}
			return null;
		}else{
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
