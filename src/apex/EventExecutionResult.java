package apex;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import components.GraphicalLayout;
import components.LayoutNode;
import components.system.InputMethodOverview;
import components.system.WindowInformation;
import components.system.WindowOverview;

public class EventExecutionResult {
	
	public final static int 
		NO_ERROR = 0,
		ERROR_READ_LOGCAT = 1,
		ERROR_RETRIEVE_WIN_INFO = 2,
		ERROR_FOCUS_WDINDOW = 3,
		ERROR_LOAD_LAYOUT = 4,
		ERROR_CHECK_KEYBOARD = 5;
	
	public static String codeToString(int code){
		switch(code){
		case ERROR_READ_LOGCAT: return "Logcat reading error";
		case ERROR_RETRIEVE_WIN_INFO: return "Failure to retrieve window info";
		case ERROR_FOCUS_WDINDOW: return "Failure to retrieve focused window";
		case ERROR_LOAD_LAYOUT: return "Failure to load layout tree";
		case ERROR_CHECK_KEYBOARD: return "Failure to check keyboard";
		}
		return "No Error";
	}
	
	List<String> logcatReading;
	List<List<String>> sequences;
	InputMethodOverview iInfo;
	WindowOverview wInfo;
	WindowInformation focusedWin;
	LayoutNode node;
	boolean keyboardVisible = false, isCrashed = false;
	GraphicalLayout predefinedUI;
	int scope, errorCode = NO_ERROR;
}