package components;

import java.util.Arrays;
import java.util.List;

import support.CommandLine;
import support.Logger;

public class Executer { 
	private String serial;
	private EventDeposit deposit;
	private boolean recordEvent = true;
	private double[] corrdinatesRatio;
	private boolean noReinstall = false;
	
	public Executer(String serial){
		this(serial,null);
	}
	
	public Executer(String serial,EventDeposit deposit){
		this.serial = serial;
		this.deposit = deposit;
	}

	public void applyEvent(Event event){
		this.applyEvent(event, true);
	}
	
	public void setForceNotReinstall(boolean noReinstall){
		this.noReinstall = noReinstall;
	}
	
	public void applyEvent(Event event, boolean sleep){ 
		Logger.trace(""+event);
		CommandLine.clear();
		int type = event.getEventType();
		switch(type){
		case EventFactory.iLAUNCH:{
			String packageName = (String) event.getAttribute(EventFactory.att_pkgName);
			String actName = (String) event.getAttribute(EventFactory.att_actName);
			String shellCommand = "am start " + packageName + "/" + actName;
			CommandLine.executeShellCommand(shellCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			if(deposit != null){ deposit.addEvent(event); }
		}break;
		case EventFactory.iREINSTALL:{
			String packageName = (String) event.getAttribute(EventFactory.att_pkgName);
			String path = (String)event.getAttribute(EventFactory.att_pkgPath);
			if(noReinstall){
				CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
				CommandLine.executeADBCommand("shell am force-stop "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
//				CommandLine.executeADBCommand(" input keyevent "+KeyEvent.KEYCODE_HO1ME , serial);
			}else{
				//clear the data first 
				CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
				CommandLine.executeADBCommand("uninstall "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
				String installCommand = "install "+ path;
				CommandLine.executeADBCommand(installCommand, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
			}
			if(deposit != null){ deposit.hasReinstalled(); }
		}break;
		case EventFactory.iPRESS:{
			String keycode = (String)event.getAttribute(EventFactory.att_keyCode);
			String inputCommand = "input keyevent " + keycode;
			CommandLine.executeShellCommand(inputCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if(deposit != null){ deposit.addEvent(event); }
		}break;
		case EventFactory.iONCLICK:{
			int iX, iY;
			if(event.poly){
				//it is currently sufficient 
				//but what if the device is changed before continuing procedure
				//in short serial number is not an ideal id
				List<Integer> xy = (List<Integer>) event.getAttribute(serial);
				if(corrdinatesRatio != null){
					iX = (int) (corrdinatesRatio[0] * xy.get(0));
					iY = (int) (corrdinatesRatio[1] * xy.get(1));
				}else{
					iX = xy.get(0); iY = xy.get(1);
				}
			}else{
				String x = event.getAttribute(EventFactory.att_xCoordinate).toString();
				String y = event.getAttribute(EventFactory.att_yCoordinate).toString();
				iX = Integer.parseInt(x);
				iY = Integer.parseInt(y);
				if(corrdinatesRatio != null){
					iX = (int) (corrdinatesRatio[0] * iX);
					iY = (int) (corrdinatesRatio[1] * iY);
				}
			}
			String inputCommand = "input tap " + iX + " " + iY;
			CommandLine.executeShellCommand(inputCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			if(deposit != null){  deposit.addEvent(event); }
		}break;
		case EventFactory.iINPUT:{
			int iX, iY;
			if(event.poly){
				List<Integer> xy = (List<Integer>) event.getAttribute(serial);
				if(corrdinatesRatio != null){
					iX = (int) (corrdinatesRatio[0] * xy.get(0));
					iY = (int) (corrdinatesRatio[1] * xy.get(1));
				}else{
					iX = xy.get(0); iY = xy.get(1);
				}
			}else{
				//first click (focus), then input text
				String x = event.getAttribute(EventFactory.att_xCoordinate).toString();
				String y = event.getAttribute(EventFactory.att_yCoordinate).toString();
				iX = Integer.parseInt(x);
				iY = Integer.parseInt(y);
				if(corrdinatesRatio != null){
					iX = (int) (corrdinatesRatio[0] * iX);
					iY = (int) (corrdinatesRatio[1] * iY);
				}
			}
			
			String inputCommand = "input tap " + iX + " " + iY;
			CommandLine.executeShellCommand(inputCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			
			String text = event.getAttribute(EventFactory.att_textInput).toString();
			String textCommand = "input text "+ text;
			CommandLine.executeShellCommand(textCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			
		}break;
		case EventFactory.iUNDEFINED:
		default: throw new IllegalArgumentException();
		}

		if(sleep){
			try { Thread.sleep(EventFactory.getNeededSleepDuration(type));
			} catch (InterruptedException e) { }
		}
		Logger.trace();
	}

	public void setRatio(double[] corrdinatesRatio){
		this.corrdinatesRatio = Arrays.copyOf(corrdinatesRatio, 2);
	}
	
	public String getSerial() {
		return serial;
	}
	
	public boolean isRecordingEvent() {
		return recordEvent;
	}

	public void enableRecordingEvent(boolean recordEvent) {
		this.recordEvent = recordEvent;
	}	
	public void setEventDeposit(EventDeposit deposit){
		this.deposit = deposit;
	}
}

