package apex;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import support.Utility;
import components.Event;
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
	public static Map<String, List<List<EventSummaryPair>>> foundSequences = new HashMap<>();
	public static ExuectionDriver driver;
	
	public static StaticApp app;
	public static boolean DEBUG = true;
	
	public final static List<String> buffer = new ArrayList<>();
	public static void addErrorInfo(String msg){ buffer.add(msg); }
	
	public final static PrintStream NULL_STREAM = new PrintStream(new OutputStream(){ @Override public void write(int b) throws IOException {} });
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
	
	public static void saveData(String filePath){
		try{
			ArrayList<Serializable> list = new ArrayList<>();
			list.add(Common.model);
			list.add(Common.summaryManager);
			list.add(Common.apkPath);
			list.add(new HashSet<String>(Common.targets));
			list.add(new HashSet<String>(Common.remaining));
			list.add(new HashMap<String, List<List<EventSummaryPair>>>(Common.foundSequences));
			list.add((ArrayList<String>)buffer);
			list.add(Common.driver.getNewEventList());
			Utility.writeToDisk(list, filePath);
		}catch(Exception e){
			Common.TRACE("Event dump failure");
			e.printStackTrace();
		}catch(Error er){
			er.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	public static void restorFromData(String filePath){
		try{
			List<Serializable> list = (List<Serializable>) Utility.readFromDisk(filePath);
			Common.model = (UIModel) list.remove(0);
			Common.summaryManager = (EventSummaryManager) list.remove(0);
			Common.apkPath = (String) list.remove(0);
			Common.targets = (Set<String>) list.remove(0);
			Common.remaining = (Set<String>) list.remove(0);
			Common.foundSequences = (Map<String, List<List<EventSummaryPair>>>) list.remove(0);
			List<String> buf = (List<String>) list.remove(0);
			Common.buffer.addAll(buf);
			if(Common.driver!=null){Common.driver.setNewEventList((Stack<Event>)list.remove(0));}
		}catch(Exception e){
			Common.TRACE("Restore failure");
			e.printStackTrace();
		}catch(Error er){
			er.printStackTrace();
		}
	}
}
