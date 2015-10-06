package components;

import apex.Common;
import support.CommandLine;

public class EventExecutor { 
	private String serial;
	private boolean noReinstall = false;
	
	public EventExecutor(String serial){
		this.serial = serial;
	}
	
	public void applyEvent(Event event){
		this.applyEvent(event, true);
	}
	
	public void setForceNotReinstall(boolean noReinstall){
		this.noReinstall = noReinstall;
	}
	
	public void applyEvent(Event event, boolean sleep){
		CommandLine.clear();
		if(Common.DEBUG) System.out.println(event);
		int type = event.getEventType();
		switch(type){
		case EventFactory.iLAUNCH:{
			String packageName = (String) event.getAttribute(EventFactory.att_pkgName);
			String actName = (String) event.getAttribute(EventFactory.att_actName);
			String shellCommand = "am start " + packageName + "/" + actName;
			CommandLine.executeShellCommand(shellCommand, serial);
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
			String inputCommand = "input keyevent " + keycode;
			CommandLine.executeShellCommand(inputCommand, serial);
			
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
			String inputCommand = "input tap " + iX + " " + iY;
			CommandLine.executeShellCommand(inputCommand, serial);
		}break;
		case EventFactory.iINPUT:{
			int iX, iY;
			//first click (focus), then input text
			String x = event.getAttribute(EventFactory.att_xCoordinate).toString();
			String y = event.getAttribute(EventFactory.att_yCoordinate).toString();
			iX = Integer.parseInt(x);
			iY = Integer.parseInt(y);
			String inputCommand = "input tap " + iX + " " + iY;
			CommandLine.executeShellCommand(inputCommand, serial);
			
			String text = event.getAttribute(EventFactory.att_textInput).toString();
			String textCommand = "input text "+ text;
			CommandLine.executeShellCommand(textCommand, serial);
			
		}break;
		case EventFactory.iUNDEFINED:
		default: throw new IllegalArgumentException();
		}

		if(sleep){
			try { Thread.sleep(EventFactory.getNeededSleepDuration(type));
			} catch (InterruptedException e) { }
		}
	}
}

