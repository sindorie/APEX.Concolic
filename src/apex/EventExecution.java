package apex;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

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
	
	public EventExecution(){
		ex = new EventExecutor(Common.serial);
	}
	
	EventExecutionResult carrayout(Event event){
		Common.logcatReader.clearLogcat();
		ex.applyEvent(event);
		try { Thread.sleep(100);
		} catch (InterruptedException e1) { }
		
		//read lines
		List<String> lines = Common.logcatReader.readLogcatFeedBack();
		List<List<String>> sequences = Common.logcatReader.extractMethodSequence(lines);
		try { Thread.sleep(300); } catch (InterruptedException e1) { }

		//check if the UI within the app		
		InputMethodOverview iInfo = Common.sysInfo.getInputMethodOverview();
		WindowOverview wInfo = Common.sysInfo.getWindowOverview();
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
			focusedWin = Common.sysInfo.getWindowOverview().getFocusedWindow();
			count += 1;
		}	
		
		LayoutNode node = null;
		int scope = focusedWin.isWithinApplciation(Common.app);
		if( scope == WindowInformation.SCOPE_WITHIN ){
			node = Common.viewInfoView.loadWindowData();
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
				if( Common.sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				Common.model.record(eList.get(i));
			}
			return null;
		}else{
			for(int i = 0 ; i<eList.size() -1 ; i++){
				ex.applyEvent(eList.get(i).getEvent());	
				if( Common.sysInfo.getWindowOverview().isKeyboardVisible() ){
					ex.applyEvent(closeKeyboard);
				}
				Common.model.record(eList.get(i));
			}
			return carrayout(eList.get(eList.size()-1).getEvent());
		}
	}
}
