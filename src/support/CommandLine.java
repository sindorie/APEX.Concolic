package support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Stack;

import apex.Configuration;

public class CommandLine {
	public static boolean DEBUG = true;
	public static String unlockScreenShellCommand = "input keyevent 82";
	public static String clickPowerButtonShellCommand = "input keyevent KEYCODE_POWER";
	
	public static boolean autoClean = true;
	public static int MaxCount = 100;

	private static StringBuilder stdoutSB = new StringBuilder();
	private static StringBuilder stderrSB = new StringBuilder();
	private static Scanner sc;
	
	public static boolean hasNextLine(){
		try {
			return System.in.available() > 0;
		} catch (IOException e) {
			return false;
		}
	}
	
	public static int executeCommand(String command){
		return executeCommand(command,0);
	}
	
	public static int executeADBCommand(String command, String serial){
		return executeCommand(Configuration.getValue(Configuration.attADB)
				+" -s "+serial+" "+command);
	}
	
	public static int executeADBCommand(String command, String serial, int timeout_ms){
		return executeCommand(Configuration.getValue(Configuration.attADB)
				+" -s "+serial+" "+command,timeout_ms);
	}
	
	public static int executeShellCommand(String command, String serial){
		return executeCommand(Configuration.getValue(Configuration.attADB)
				+ " -s "+serial+" shell "+command);
	}
	
	public static int executeShellCommand(String command, String serial, int timeout_ms){
		return executeCommand(Configuration.getValue(Configuration.attADB)
				+ " -s "+serial+" shell "+command, timeout_ms);
	}
	
	public static int executeShellCommand(String command){
		return executeCommand(Configuration.getValue(Configuration.attADB)
				+ " shell "+command);
	}

	public static int executeCommand(String command , int timeout_ms){
		
		Process task = null;
		InputStream stderrStream = null, stdoutStream = null;
		clear();
		Logger.debug(command);
		try {
			task = Runtime.getRuntime().exec(command);
			if(timeout_ms>0){ 
				synchronized(task){
					task.wait(timeout_ms);
				}
//				while(task.isAlive()){
//					task.destroy();
//					Thread.sleep(200);
//				}
			}else{ task.waitFor(); }
			
			stderrStream = task.getErrorStream();
			int count = stderrStream.available();
			if(count > 0){
				byte[] buffer = new byte[count];
				stderrStream.read(buffer);
				String reading = new String(buffer);

				stderrSB.append(reading);
			}
			stdoutStream = task.getInputStream();
			count = stdoutStream.available();
			if(count > 0){
				byte[] buffer = new byte[count];
				stdoutStream.read(buffer);
				String reading = new String(buffer);
	
				stdoutSB.append(reading);
			}
			if(DEBUG)Logger.trace("Stdout: "+stdoutSB.toString().replace("\r|\n", ""));
			if(DEBUG)Logger.trace("Stderr: "+stderrSB.toString().replace("\r|\n", ""));
			return task.exitValue();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(DEBUG)Logger.trace("Stdout: "+stdoutSB.toString().replace("\r|\n", ""));
		if(DEBUG)Logger.trace("Stderr: "+stderrSB.toString().replace("\r|\n", ""));
		return -1;
	}

	public static String getLatestStdoutMessage(){
		return stdoutSB!=null?stdoutSB.toString():null;
	}
	public static String getLatestStderrMessage(){
		return stderrSB!=null?stderrSB.toString():null;
	}
	public static void clear(){
		stdoutSB = new StringBuilder();
		stderrSB = new StringBuilder();
	}
	
	
	public static String requestInput(){
		if(sc == null){
			 sc = new Scanner(System.in);
		}
		
		String line = sc.nextLine();
		return line;
	}
}
