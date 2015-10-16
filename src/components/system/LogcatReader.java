package components.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import apex.Configuration;
import support.CommandLine;
import support.Utility;


public class LogcatReader {
	
	private String serial, adbLocation, errMsg;
	private List<String> exeLog, tagLog, threadOrder;
	private List<List<String>> methodLog;
	private boolean isCrashed, reachMaxTime, hasException, readAll = false;
	private long maxTime, minTime, duration ,lastUpdated, startSleep, sleepTime = 10;
	public final static String formatMatcher = "^\\w\\(\\s*\\d*:\\s*\\d*\\).*"; //beginningLabel = "-*.*";;
	public final static String instrumentationMatcher = "^\\w\\(\\s*\\d*:\\s*\\d*\\) (Method_Starting,|Method_Returning,|execLog,).*";
	
	public LogcatReader(){ this(null);}
	public LogcatReader(String serial){
		this.serial = serial; 
		this.adbLocation = Configuration.getValue(Configuration.attADB); 
		setTime( 5*1000, 1*1000, 100, 50, 300 );
	}
	
	/**
	 * Set the time attributes, -1 means does not use
	 * @param maxTime - the max time which the execution time should exceed 
	 * @param minTime - the min time which the execution should process unless exception
	 * @param duration - the min duration between two effect reading
	 * @param sleepTime - the sleep time for each loop
	 * @param startSleep - the sleep time right after the process is started
	 */
	public void setTime(long maxTime, long minTime, long duration, long sleepTime, long startSleep){
		this.maxTime = maxTime; this.minTime = minTime; this.duration = duration; 
		this.sleepTime = sleepTime; this.startSleep = startSleep;
	}
	/*Getters*/
	public long getLastUpdated() { return lastUpdated;}
	public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
	public List<String> getExeLog() { return exeLog; }
	public List<String> getTagLog() { return tagLog; }
	public boolean isCrashed() { return isCrashed; }
	public List<List<String>> getMethodLog() { return methodLog; }
	public boolean hasException() { return hasException;}
	public String getErrMsg() { return errMsg; }
	public boolean hasReachedMaxTime(){ return this.reachMaxTime; }
	public List<String> getThreadOrder(){return this.threadOrder;}
	
	/**
	 * Will not group method. Only group with thread
	 * @param readAll
	 */
	public void setReadAll(boolean readAll){ this.readAll = readAll; }
	public boolean isReadAll(){return this.readAll; }
	
	/**
	 * Clear the logcat buffer
	 */
	public void clearLogcat(){
		String id = Utility.getPID("logcat", serial);
		if(id != null && !id.isEmpty()){
			CommandLine.executeADBCommand("shell kill "+id, serial);
		}
		CommandLine.executeADBCommand("logcat -c", serial); 
	}
	
	/**
	 * Read the logcat output
	 * Samples:
	 * I( 3134: 3134) Method_Starting,Lcom/example/zhenxu/myapplication/MainActivity;->showSharedPreference(Landroid/view/View;)V
	 * --------- beginning of system
	 * --------- beginning of crash
	 * 
	 * 
	 * Does not use -d (which indicates No Block) as the ouput might be constant
	 * Needs a max time out and min time limit. 
	 * 
	 * Take thread into consideration, group exelog by Thread and method root
	 */
	public void readFeedBack(){
		try {
			String command = null;
			if(serial == null){ command = adbLocation +" logcat -v thread -s System.out";
			}else{ command = adbLocation + " -s "+serial+" logcat -v thread -s System.out"; }
			Process readProcess = Runtime.getRuntime().exec(command);
			InputStream in = null, err = null;

			in = readProcess.getInputStream();
			err = readProcess.getErrorStream();
			
			resetInternalData();
			readData(readProcess, in, err);
			this.methodLog = process();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read and process feedback from a given stream
	 * @param in
	 */
	public void readFromStream(InputStream in){
		resetInternalData();
		readData(null, in, null);
		this.methodLog = process();
	}
	
	private void resetInternalData(){
		lastUpdated = -1;
		threadOrder = new ArrayList<String>();
		exeLog = new ArrayList<>();
		tagLog = new ArrayList<>();
		methodLog = new ArrayList<>();
		isCrashed = false;
		reachMaxTime = false;
		hasException = false;
		errMsg = "";
	}
	
	private void readData(Process readProcess, InputStream in, InputStream err){
		StringBuilder sb = new StringBuilder();
		try{
			if(startSleep > 0)Thread.sleep(startSleep); 
			long startTime = System.currentTimeMillis();
			while(true){
				int len = in.available();
				boolean updated = false;
				if(len > 0){
					byte[] buf = new byte[len];
					in.read(buf);
					String bufStr = new String(buf);
					for(int i = 0;i <bufStr.length(); i++){
						char c = bufStr.charAt(i);
						if(c == '\n'){
							String line = sb.toString();
							updated = processline(line);
							sb = new StringBuilder();
						}else{ sb.append(c);}
					}
				}
				
				//cannot breach max time limit
				long currentTime = System.currentTimeMillis();
				if(maxTime>0 && currentTime - startTime > maxTime){ reachMaxTime = true; break; }
				//reading must be within certain limit
				if(updated){ lastUpdated = currentTime;
				}else{
					if(minTime > 0 && currentTime - startTime <= minTime){}
					else if(duration > 0 && currentTime - lastUpdated > duration){ break;}
				}
				if(sleepTime>0)Thread.sleep(sleepTime);
			}
			
			if(err != null){
				int len = err.available();
				if(len > 0){
					byte[] buf = new byte[len];
					in.read(buf);
					String msg = new String(buf).trim();
					if(msg.isEmpty() == false) errMsg = msg;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			hasException = true;
			errMsg += e.getMessage();
		}finally{
			String line = sb.toString();
			if(line.isEmpty() == false) processline(line);
			if(readProcess != null){ readProcess.destroy();; }
		}
	}

	
	/**
	 * I( 3134: 3134) Method_Starting,Lcom/example/zhenxu/myapplication/MainActivity;->showSharedPreference(Landroid/view/View;)V
	 * --------- beginning of system
	 * --------- beginning of crash
	 * 
	 * elements in tagLog and exeLog are pairs
	 * @param line
	 */
	private boolean processline(String line){
		line = line.trim();
		if(readAll){
			if(line.matches(formatMatcher)){
				int index = line.indexOf(')');
				String tag = line.substring(0, index+1);
				String content = line.substring(index+2, line.length());
				this.exeLog.add(content);
				this.tagLog.add(tag);
			}else{
				this.exeLog.add(line);
				this.tagLog.add("");
			}
			return true;
		}else if(line.trim().equals("--------- beginning of crash")){
			isCrashed = true;
			return true;
		}else if(line.matches(instrumentationMatcher)){
			int index = line.indexOf(')');
			String tag = line.substring(0, index+1);
			String content = line.substring(index+2, line.length());
			this.exeLog.add(content);
			this.tagLog.add(tag);
			return true;
		}
		return false;
	}
	
	private List<List<String>> process(){
		Map<String,List<String>> threadGroup = new HashMap<>();
		for(int i =0 ;i < this.exeLog.size(); i++){
			String content = exeLog.get(i);
			String tag = tagLog.get(i);
			List<String> seq = null;
			String threadId = "";
			if(!tag.isEmpty()){
				threadId = tag.replace(")", "").split(":")[1].trim();
				seq = threadGroup.get(threadId);
			}
			if(seq == null){
				seq = new ArrayList<>();
				threadGroup.put(threadId, seq);
				if(threadOrder.contains(threadId) == false){ threadOrder.add(threadId); }
			}
			seq.add(content);
		}
		List<List<String>> resultMethodLog = new ArrayList<>();
		if(readAll){
			for(String threadId : threadOrder){
				List<String> value = threadGroup.get(threadId);
				resultMethodLog.add(value);
			}
		}else{
			for(String threadId : threadOrder){
				List<String> value = threadGroup.get(threadId);
				List<List<String>> threadMethodLog = extractMethodSequence(value);
				resultMethodLog.addAll(threadMethodLog);
			}
		}
		return resultMethodLog;
	}
	
	private List<List<String>> extractMethodSequence(List<String> input){
		List<List<String>> result = new ArrayList<List<String>>();
		Stack<String> methodStack = new Stack<String>();

		List<String> currentTrack = new ArrayList<>();
		for(String line : input){
			if(line.startsWith("Method_Starting,")){
				currentTrack.add(line);
				String content = line.replace("Method_Starting,", "");
				content = content.substring(0, content.indexOf(')')+1);
				methodStack.push(content);
			}else if(line.startsWith("Method_Returning,")){
				currentTrack.add(line);
				String content = line.replace("Method_Returning,", "");
				content = content.substring(0, content.indexOf(')')+1);
				while(!methodStack.isEmpty()){
					String poped = methodStack.pop();
					if(poped.equals(content)){
						break;
					}
				}
				if(methodStack.isEmpty()){
					result.add(currentTrack);
					currentTrack = new ArrayList<>();
				}
			}else{
				currentTrack.add(line);
			}
		}
		if(!currentTrack.isEmpty()) result.add(currentTrack);

		return result;
	}
	

}
