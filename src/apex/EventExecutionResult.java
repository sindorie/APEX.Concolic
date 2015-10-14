package apex;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import components.GraphicalLayout;
import components.LayoutNode;
import components.system.InputMethodOverview;
import components.system.WindowInformation;
import components.system.WindowOverview;

public class EventExecutionResult {
	List<String> logcatReading;
	List<List<String>> sequences;
	InputMethodOverview iInfo;
	WindowOverview wInfo;
	WindowInformation focusedWin;
	LayoutNode node;
	boolean keyboardVisible = false, isCrashed = false;
	GraphicalLayout predefinedUI;
	int scope;
}