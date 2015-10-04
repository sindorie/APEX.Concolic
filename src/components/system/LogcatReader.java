package components.system;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import support.CommandLine;


public class LogcatReader {
	
	String serial, adbLocation;
	public LogcatReader(String serial){
		this.serial = serial; 
		this.adbLocation = Configuration.getValue(Configuration.attADB);
	}
	
	public void clearLogcat(){
		CommandLine.executeADBCommand("logcat -c", serial);
	}
	
	/**
	 * Read the feedback from logcat according to tag
	 * @return
	 */
	public List<String> readLogcatFeedBack(){
		ArrayList<String> result = new ArrayList<String>();
		try{
//			I( 3134: 3134) Method_Starting,Lcom/example/zhenxu/myapplication/MainActivity;->showSharedPreference(Landroid/view/View;)V
			String command = adbLocation + " -s "+serial+" logcat -v thread -d  -s System.out";
			final Process pc = Runtime.getRuntime().exec(command);
			InputStream in = pc.getInputStream();
			InputStream err = pc.getErrorStream();
			
			StringBuilder sb = new StringBuilder();
			Thread.sleep(300);
			//in order to avoid infinite loop
			long point1 = System.currentTimeMillis();
			while(true){
				int count = in.available();
				if(count <= 0) break;
				byte[] buf = new byte[count];
				in.read(buf);
				sb.append(new String(buf));
				long point2 = System.currentTimeMillis();
				if(point2 - point1 > 300) break;
			}
			String errMsg = null;
			if(err.available()>0){
				int count = in.available();
				byte[] buf = new byte[count];
				in.read(buf);
				errMsg = new String(buf);
			}
			if(errMsg!=null) System.out.println("DEBUG:errMsg,  "+errMsg);
			
			String tmp = sb.toString().trim();
			if(tmp.equals("")) return result;//empty
			String[] parts = tmp.split("\n");
			for(String part: parts){
				if(part == null) continue;
				if(part.contains(") Method_Starting,") 
						|| part.contains(") Method_Returning,") 
						|| part.startsWith(") execLog,")){
					result.add(part);
				}
			}
			pc.destroy();
			CommandLine.executeADBCommand("logcat -c", serial);
		}catch(Exception e){
			System.out.println(e.getLocalizedMessage());
		}
		return result;
	}
	
	public List<List<String>> extractMethodSequence(List<String> input){
		List<List<String>> result = new ArrayList<List<String>>();
		
		
		
		
		
		
		//TODO
		
		
		
		
		
		
		
		
		
		return result;
	}
	

}
