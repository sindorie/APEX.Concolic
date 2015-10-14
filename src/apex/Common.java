package apex;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import components.EventSummaryManager;
import components.EventSummaryPair;
import components.ViewDeviceInfo;
import components.system.InformationCollector;
import components.system.LogcatReader;
import apex.staticFamily.StaticApp;
import apex.symbolic.SymbolicExecution;

public class Common {
	public static UIModel model;
	public static SymbolicExecution symbolic;
	public static EventSummaryManager summaryManager;
	
	public static String apkPath;
	public static String serial;
	public static Set<String> targets;
	public static Set<String> remaining;
	public final static Map<String, List<List<EventSummaryPair>>> foundSequences = 
			new HashMap<String, List<List<EventSummaryPair>>>();
	
	public static StaticApp app;
	public static boolean DEBUG = true;
	
	public final static List<String> buffer = new ArrayList<>();
	public static void addErrorInfo(String msg){ buffer.add(msg); }
	private static PrintWriter[] writers = null;
	
	/**
	 * Print the caller method information
	 */
	public static void TRACE(){
		if(DEBUG){
			StackTraceElement[] info = Thread.currentThread().getStackTrace();
			StackTraceElement element = info[2];
			String context = element.getClassName()+" "+element.getMethodName()+" "+element.getLineNumber();
			System.out.println(context);
		}
	}
	
	/**
	 * Print the caller method information with the message
	 * @param msg
	 */
	public static void TRACE(String msg){
		if(DEBUG){
			StackTraceElement[] info = Thread.currentThread().getStackTrace();
			StackTraceElement element = info[2];
			String context = element.getClassName()+" "+element.getMethodName()+" "+element.getLineNumber();
			if(msg != null){
				System.out.println(context+" "+msg);
			}else{
				System.out.println(context);
			}
		}
	}
	
	public static void setChannel(OutputStream... streams){
		if(streams == null || streams.length == 0) return ;
		writers = new PrintWriter[streams.length];
		for(int i =0 ; i<streams.length ; i++){
			writers[i] = new PrintWriter(streams[i]);
		}
	}
	public static void println(){
		println("");
	}
	public static void println(Object message){
		if(writers == null){System.out.println(message);}
		else{
			for(PrintWriter pw : writers){
				pw.println(message);pw.flush();
			}
		}
	}
}
