package apex;

import java.util.List;
import components.EventSummaryDeposit;
import components.ViewDeviceInfo;
import components.system.InformationCollector;
import components.system.LogcatReader;
import apex.staticFamily.StaticApp;
import apex.symbolic.SymbolicExecution;

public class Common {
	public static UIModel model;
	public static SymbolicExecution symbolic;
	public static EventSummaryDeposit esDeposit;
	
	public static String apkPath;
	public static String serial;
	public static List<String> targets;
	public static List<String> remaining;
	
	public static StaticApp app;
	public static InformationCollector sysInfo;
	public static ViewDeviceInfo viewInfoView;
	public static LogcatReader logcatReader;
	
}
