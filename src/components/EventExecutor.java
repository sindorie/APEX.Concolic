package components;

import java.util.ArrayList;
import java.util.List;

import apex.Common;
import support.CommandLine;

public class EventExecutor { 
	private String serial;
	private boolean noReinstall = false;
	private List<ActionListener> listener = new ArrayList<>();
	
	public EventExecutor(String serial){
		this.serial = serial;
	}
	
	public void applyEvent(Event event){
		this.applyEvent(event, true);
	}
	
	public void setForceNotReinstall(boolean noReinstall){
		this.noReinstall = noReinstall;
	}
	
	public void addListner(ActionListener lis){
		listener.add(lis);
	}
	
	public void applyEvent(Event event, boolean sleep){
		Common.TRACE(event.toString());
		CommandLine.clear();
		for(ActionListener lis : listener){lis.beforeEventExecution(event);}
		
		int type = event.getEventType();
		switch(type){
		case EventFactory.iLAUNCH:{
			String packageName = (String) event.getAttribute(EventFactory.att_pkgName);
			String actName = (String) event.getAttribute(EventFactory.att_actName);
			String shellCommand = "shell am start " + packageName + "/" + actName;
			CommandLine.executeADBCommand(shellCommand, serial);
		}break;
		case EventFactory.iUNINSTALL:{
			String packageName = (String) event.getAttribute(EventFactory.att_pkgName);
			CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
			CommandLine.executeADBCommand("uninstall "+packageName, serial);
		}break;
		case EventFactory.iREINSTALL:{
			String packageName = (String) event.getAttribute(EventFactory.att_pkgName);
			String path = (String)event.getAttribute(EventFactory.att_pkgPath);
			if(noReinstall){
				CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
				CommandLine.executeADBCommand("shell am force-stop "+packageName, serial);
//				CommandLine.executeADBCommand(" input keyevent "+KeyEvent.KEYCODE_HO1ME , serial);
			}else{
				//clear the data first 
				CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
				CommandLine.executeADBCommand("uninstall "+packageName, serial);
				String installCommand = "install "+ path;
				CommandLine.executeADBCommand(installCommand, serial);
			}
		}break;
		case EventFactory.iPRESS:{
			String keycode = (String)event.getAttribute(EventFactory.att_keyCode);
			String inputCommand = "shell input keyevent " + keycode;
			CommandLine.executeADBCommand(inputCommand, serial);
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}break;
		case EventFactory.iONCLICK:{
			int iX, iY;
			String x = event.getAttribute(EventFactory.att_xCoordinate).toString();
			String y = event.getAttribute(EventFactory.att_yCoordinate).toString();
			iX = Integer.parseInt(x);
			iY = Integer.parseInt(y);
			String inputCommand = "shell input tap " + iX + " " + iY;
			CommandLine.executeADBCommand(inputCommand, serial);
		}break;
		case EventFactory.iINPUT:{
			int iX, iY;
			//first click (focus), then input text
			String x = event.getAttribute(EventFactory.att_xCoordinate).toString();
			String y = event.getAttribute(EventFactory.att_yCoordinate).toString();
			iX = Integer.parseInt(x);
			iY = Integer.parseInt(y);
			String inputCommand = "shell input tap " + iX + " " + iY;
			CommandLine.executeADBCommand(inputCommand, serial);
			
			String text = event.getAttribute(EventFactory.att_textInput).toString();
			String textCommand = "shell input text "+ text;
			CommandLine.executeADBCommand(textCommand, serial);
			
		}break;
		case EventFactory.iUNDEFINED:
		default: throw new IllegalArgumentException();
		}

		if(sleep){
			try { Thread.sleep(EventFactory.getNeededSleepDuration(type));
			} catch (InterruptedException e) { }
		}

		for(ActionListener lis : listener){lis.afterEventExecution(event);}
	}
	
	public interface ActionListener{
		public void beforeEventExecution(Event event);
		public void afterEventExecution(Event event);
	}
}

