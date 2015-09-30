package support;

import java.awt.BorderLayout;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.StringEscapeUtils;

public class Logger {
	
	/*Fields for Information Panel*/
	private static JFrame informationFrame = new JFrame("Information");
	private static JTabbedPane tabbedPane = new JTabbedPane();
	
	/*Global*/
	public final static int LEVEl_ERROR = 4, LEVEl_WARN = 3, LEVEl_INFO = 2, LEVEl_DEBUG = 1, LEVEl_TRACE = 0;
	public final static Logger ConsoleLogger = new Logger(System.out);
	public final static MessageConstructor StandardConstructor;
	private static List<InformationFilter> gFilters = new ArrayList<InformationFilter>();
	private static List<Logger> gLoggers = new ArrayList<Logger>();
	private static boolean gEnabled = true;
	private static int tabCount = 0;

	/*Class initialization*/
	static{
		StandardConstructor = new MessageConstructor(){
			final static String defaultSubChunLength = "(?<=\\G.{200})";
			@Override
			public String[] construct(CurrentThreadInfo info, String tag,
					String message, int level) {
				StringBuilder sb = new StringBuilder();
				StackTraceElement element = info.getCallerRecord();
				sb
				.append(formate(20, info.time))
				.append(formate(10 , info.threadName))
				.append(formate(10 , info.threadId))
				.append(formate(40 , element.getClassName()))
				.append(formate(20 , element.getMethodName()))
				.append(formate(5 , element.getLineNumber()))
				.append(formate(20 , tag))
				.append(formate(5 , level))
				;

				String prefix = sb.toString();
				if(message == null) return new String[]{prefix+"null\n"};
				else{
					List<String> result = new ArrayList<String>();
					for(String chunk: message.split("\n")){
						String[] lines = chunk.split(defaultSubChunLength);
						for(String line : lines){
							if(line == null || line.trim().isEmpty()) continue;
							result.add(prefix+line+"\n");
						}
					}
					return result.toArray(new String[0]);
				}
			}
		};
		gLoggers.add(ConsoleLogger); 
		ConsoleLogger.setMessageConstructor(StandardConstructor);
		
		informationFrame.setLayout(new BorderLayout());
		informationFrame.add(tabbedPane, BorderLayout.CENTER);
		informationFrame.setSize(800, 600);
		informationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//informationFrame will only show up if there is at least one registered panel
	}
	
	public static void removeAllUI(){
		tabbedPane.removeAll();
	}
	
	public static void addWorker(Logger worker){
		gLoggers.add(worker);
	}
	public static void remove(Logger worker){
		gLoggers.remove(worker);
	}
	public static void remove(OutputStream outStream){
		int i = gLoggers.size() -1;
		for(;i>=0;i--){
			if(gLoggers.get(i).outStream.equals(outStream)){
				gLoggers.remove(i);
			}	
		}
	}
	public static int getGlobalWokerAmount(){
		return gLoggers.size();
	}
	
	public static void setEnableGUI(boolean enable){
		if(enable){ informationFrame.setVisible(true);
		}else{ informationFrame.setVisible(false); }
	}
	public static void setEnableGlobal(boolean enable){
		gEnabled = enable;
	}
	
	public static void setGlobalFilter(InformationFilter filter){
		gFilters.add(filter);
	}
	public static void removeGlobalFilter(InformationFilter filter){
		gFilters.remove(filter);
	}
	
	public static void registerJPanel(final String tag, final JComponent panel){
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	tabbedPane.add(tag, panel);
            	tabCount += 1;
            	informationFrame.setVisible(true);
            }
        });
	}
	
	public static void info(String tag, Object message){
		record(tag, message, LEVEl_INFO);
	}
	public static void info(Object message){
		record(null, message, LEVEl_INFO);
	}
	public static void debug(String tag, Object message){
		record(tag, message, LEVEl_DEBUG);
	}
	public static void debug(Object message){
		record( null, message, LEVEl_DEBUG);
	}
	public static void error(String tag, Object message){
		record(tag, message, LEVEl_ERROR);
	}
	public static void error(Object message){
		record(null, message, LEVEl_ERROR);
	}
	public static void warn(String tag, Object message){
		record(tag, message, LEVEl_WARN);
	}
	public static void warn(Object message){
		record(null, message, LEVEl_WARN);
	}
	public static void trace(String tag, Object message){
		record(tag, message, LEVEl_TRACE);
	}
	public static void trace(Object message){
		record(null, message, LEVEl_TRACE);
	}
	public static void trace(){
		record(null, "mark", LEVEl_TRACE);
	}
		
	private static void record(String tag, Object input, int level){
		if(gEnabled == false) return ;
		CurrentThreadInfo info = new CurrentThreadInfo();
		String message = input+"";
		for(InformationFilter filter : gFilters){
			if(filter.filtered(info, tag, message, level)) return;
		}
		for(Logger worker : gLoggers){
			worker.log(info, tag, message, level);
		}
	}
	private static String formate(int length, Object o){
		return String.format( "%-" +length+"s",o == null?"":o.toString()).substring(0, length);
	}
	
	private static int gIndex = 0;
	private int index = gIndex++;
	protected boolean enabled = true;
	protected PrintWriter writer;
	protected boolean flushOnWrite = true;
	protected OutputStream outStream;
	protected List<InformationFilter> localFilters = new ArrayList<InformationFilter>();
	protected MessageConstructor mesCon = null;

	public void removeFilters(InformationFilter filter){
		localFilters.remove(filter);
	}
	
	public Logger(OutputStream outStream){
		this.outStream = outStream;
		if(this.outStream != null) writer = new PrintWriter(outStream);
	}
	
	public void log(CurrentThreadInfo info, String tag, String message, int level){
		if(enabled == false) return;
		for(InformationFilter filter : localFilters){
			if(filter.filtered(info, tag, message, level))return;
		}
		if(writer == null){
			return;
		}
		if(mesCon == null){
			for(String line : StandardConstructor.construct(info, tag, message, level)){
				writer.print(line);
			}
		}else{
			for(String line : mesCon.construct(info, tag, message, level)){
				writer.print(line);
			}
		}
		if(flushOnWrite) writer.flush();
	}
	public void setEnableLocal(boolean enable){
		this.enabled = enable;
	}
	public void setFlushOnLog(boolean flush){
		flushOnWrite = flush;
	}
	public void setMessageConstructor(MessageConstructor mesCon){
		this.mesCon = mesCon;
	}
	public boolean hasTheSameOutStream(OutputStream other){
		return this.outStream==null?false:this.outStream.equals(other);
	}
	public void addLocalFilter(InformationFilter filter){
		this.localFilters.add(filter);
	}
	
	
	public static class CurrentThreadInfo{
		public String threadName;
		public long threadId;
		public StackTraceElement[] traces;
		public long time; // not precise but should be sufficient
		public CurrentThreadInfo(){
			Thread t = Thread.currentThread();
			time = System.currentTimeMillis();
			this.threadName = t.getName();
			this.threadId = t.getId();
			this.traces = t.getStackTrace();
		}
		
		public StackTraceElement getCallerRecord(){
			return traces[4];//TODO pay attention to; this only works in predefined context
		}
	}
	
	public static interface MessageConstructor{
		public String[] construct(CurrentThreadInfo info, String tag, String message, int level);
	}
	
	public static interface InformationFilter{
		public boolean filtered(CurrentThreadInfo info, String tag, String message, int level);
	}
}
