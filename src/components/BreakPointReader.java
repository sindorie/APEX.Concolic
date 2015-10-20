package components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import support.Utility;

public class BreakPointReader {
	private String serial, packName, adb;
	private int localPort = 15975;
	private List<String> lines;
	private Set<String> hits = new HashSet<String>();
	private boolean crash = false;
	private final Object simpleLock = new Object();
	
	public boolean isJustCrashed(){
		boolean result = false;
		synchronized(simpleLock){
			result = crash;
			crash = false;
		}
		return result;
	}
	
	public BreakPointReader(String adb, String serial, String packName, List<String> lines){
		this.serial = serial; this.packName = packName;
		this.lines = new ArrayList<String>(lines);
		this.adb =adb;
	}

	private InternalReader internal;
	
	public void signal(){
		if(internal == null || internal.stopped){
			internal = new InternalReader();
			new Thread(internal).start();
		}else{
			//do nothing as the reader is still running
		}
	}
	
	public Set<String> getHits(){
		return this.hits;
	}
	
	private class InternalReader implements Runnable{
		boolean stopped = false, err = false;;
		Process process; 
		OutputStream out;
		BufferedReader stdReader, errReader;
		InputStream stdin, stderr;
		
		public InternalReader(){
			err = setup();
		}
		
		@Override
		public void run() {
			if(err){ stopped = true; return;}
			System.out.println("Inner thread started");
			String p = "\"thread=[\\w\\s#$@!<>]+\", \\w*(.[\\w\\$<>]+)*\\([\\w\\s,\\$]*\\), line=[\\d,]+ bci=[\\d,]+" ;
			Pattern r = Pattern.compile(p);
			
//			Exception occurred: java.lang.VerifyError (uncaught)"thread=<1> main", com.example.rfdrop.Messages.onCreate(), line=51 bci=22
			try{
				long lastTime = System.currentTimeMillis();
				while(true){
					long current = System.currentTimeMillis();
					String line = stdReader.readLine().trim();
					if(line.startsWith("Exception occurred") && line.contains("(uncaught)")){
						synchronized(simpleLock){ crash = true; }
					}
					
					Matcher m = null;
					try{ m = r.matcher(line);
					}catch(Exception e){
						e.printStackTrace();
						System.out.println(line);
						cont(); continue;
					}
					
					if(m.find()){
						line = m.group().trim();//should be only one group
						String[] chunks = line.split(", ");
						String threadInfo = chunks[0].substring(chunks[0].indexOf("\""), chunks[0].lastIndexOf("\""));
						String className = chunks[1].substring(0, chunks[1].lastIndexOf("."));
						String lineNumber = chunks[2].substring(chunks[2].indexOf("=")+1, chunks[2].indexOf(" "));
						hits.add(className+":"+lineNumber);
						lastTime = current;
						cont();
					}else if(line.contains("thread")){
						lastTime = current;
						cont();
					}else{ 
						Thread.sleep(10);
						if(current - lastTime > 10*1000/*10s*/){
							cont();
							lastTime = current;
						}
					}
//					System.out.println("JDB Thread: "+line);
				}
			}catch(Exception e){}//e.printStackTrace(); }
			
			try {
				this.write("exit\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Inner thread stopped");
			stopped = true;
		}
		
		boolean setup(){
			String pid = Utility.getPID(packName, serial);
			int count = 0;
			while(count < 3 && pid == null){
				try { Thread.sleep(100); } catch (InterruptedException e) { }
				pid = Utility.getPID(packName, serial);	
			}
			if(pid != null){
				try {
					Process p = Runtime.getRuntime().exec(adb+" -s "+ serial+
							" forward tcp:" + localPort + " jdwp:" + pid);
					process = Runtime.getRuntime().exec("jdb -sourcepath src -attach localhost:" + localPort);
					Thread.sleep(100);
					stdin = process.getInputStream();
					stderr = process.getErrorStream();
					out = process.getOutputStream();
					stdReader = new BufferedReader(new InputStreamReader(stdin));
					errReader = new BufferedReader(new InputStreamReader(stderr));
					
					for(String line : lines){ out.write(("stop at "+line+"\n").getBytes()); }
					cont();
					
					Thread.sleep(100);
					int len = stdin.available();
					if(len > 0){
						byte[] b = new byte[len];
						stdin.read(b);
						System.out.println(new String(b).trim());
					}
					
					return false;
				} catch (IOException e) {
//					e.printStackTrace();
				} catch (InterruptedException e) {
//					e.printStackTrace();
				}
			}
			return true;
		}
		
		private void write(String mes) throws IOException{
//			System.out.println("Write to JDB:"+mes);
			out.write(mes.getBytes());
			out.flush();
		}
		
		private void cont() throws IOException{
			this.write("cont\n");
		}
		
//		List<String> lineBuffer = new ArrayList<String>();
//		StringBuilder sb = new StringBuilder();
//		private String nextReading() throws IOException{
//			if(lineBuffer.size() > 0) return lineBuffer.remove(0);
//			
//			while(true){
//				int size = stdin.available();
//				if(size > 0){
//					byte[] b = new byte[size];
//					stdin.read(b);
//					String line = new String(b);
//					if(line.contains("\n")){
//						sb.append(line);
//					}else{
//						
//					}
//				}
//			}
//		}
	}
}
