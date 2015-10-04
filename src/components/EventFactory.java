package components;

import android.view.KeyEvent;

public class EventFactory { 
	public static final String 
			att_pkgPath = "event_path",
			att_actName = "event_actName",
			att_pkgName = "event_packageName",
			att_keyCode = "event_keycode",
			att_xCoordinate = "event_clickx",
			att_yCoordinate = "event_clicky",
			att_textInputType = "event_input_type",
			att_textInput = "event_input";

	public static final long 
			NON_SLEEP = 0, 
			LAUNCH_SLEEP = 2000,
			RESTART_SLEEP = 2000,  
			REINSTALL_SLEEP = 2000,
			PRESS_SLEEP = 1500, 
			ONCLICK_SLEEP = 1500;
	
	private final static String 
			UNDEFINED = "undefined",//, EMPTY = "empty", UPDATE = "update";
			LAUNCH = "launch", REINSTALL = "reinstall", //, RESTART = "restart";
			PRESS = "press", 
			ONCLICK = "android:onClick",
			TEXT_INPUT = "text input";
	
	public final static int 
			iUNDEFINED = -1,//, iEMPTY = -3, iUPDATE = -2;
			iLAUNCH = 0, 
			iREINSTALL = 2, 
			iPRESS = 3, 
			iONCLICK = 4, //, iRESTART = 1;
			iINPUT = 5;
	
	private EventFactory(){}
	
	
	public static Event createReinstallEvent(String pkgName, String path){
		Event e = new Event();
		e.source = null;
		e.eventType = iREINSTALL;
		e.putAttribute(EventFactory.att_pkgName, pkgName);
		e.putAttribute(EventFactory.att_pkgPath, path);
		return e;
	}
	
	public static Event createLaunchEvent(GraphicalLayout source, String pkg, String act){
		Event e = new Event();
		e.source = source;
		e.eventType = iLAUNCH;
		e.putAttribute(EventFactory.att_pkgName, pkg);
		e.putAttribute(EventFactory.att_actName, act);
		return e;
	}
	
	public static Event createTextEvet(GraphicalLayout source, int x, int y, String text){
		Event e = new Event();
		e.source = source;
		e.eventType = iINPUT;
		e.putAttribute(EventFactory.att_xCoordinate, x);
		e.putAttribute(EventFactory.att_yCoordinate, y);
		e.putAttribute(EventFactory.att_textInput, text);
		return e;
	}
	public static Event createTextEvet(Event clickEvent, String text, String inputType){
		Event e = clickEvent.clone();
		e.eventType = iINPUT;
		e.putAttribute(EventFactory.att_textInput, text);
		e.putAttribute(EventFactory.att_textInputType, inputType);
		return e;
	}
	
	
	public static Event createTextEvet(Event clickEvent, String text){
		return createTextEvet(clickEvent, text, "0x20001");
	}
	
	public static Event createClickEvent(GraphicalLayout source, LayoutNode node){
		return createClickEvent(source, (node.startx+node.endx)/2,
				(node.starty+node.endy)/2 );
	}

	public static Event createClickEvent(GraphicalLayout source, int x, int y){
		Event e = new Event();
		e.source = source;
		e.eventType = iONCLICK;
		e.putAttribute(EventFactory.att_xCoordinate, x);
		e.putAttribute(EventFactory.att_yCoordinate, y);
		return e;
	}
	
	public static Event createCloseKeyboardEvent(){
		Event e = new Event();
		e.eventType = iPRESS;
		e.putAttribute(EventFactory.att_keyCode, KeyEvent.KEYCODE_BACK+"");
		e.ignoreByRecorder = true;
		return e;
	}
	
	public static Event CreatePressEvent(GraphicalLayout source, int type){ 
		return createPressEvent(source, type+"");
	}
	
	public static Event createPressEvent(GraphicalLayout source, String type){
		Event e = new Event();
		e.source = source;
		e.eventType = iPRESS;
		e.putAttribute(EventFactory.att_keyCode,type);
		return e;
	}
	
	public static int stringToint(String eventString){
		if(eventString.equals(LAUNCH)){
			return iLAUNCH;
		}else if(eventString.equals(REINSTALL)){
			return iREINSTALL;
		}else if(eventString.equals(PRESS)){
			return iPRESS;
		}else if(eventString.equals(ONCLICK)){
			return iONCLICK;
		}else if(eventString.equals(TEXT_INPUT)){
			return iINPUT;
		}else return iUNDEFINED;
	}
	
	public static long getNeededSleepDuration(int type){
		switch(type){
		case iLAUNCH: 	return EventFactory.LAUNCH_SLEEP;
		case iREINSTALL: return EventFactory.REINSTALL_SLEEP;
		case iPRESS: 	return EventFactory.PRESS_SLEEP;
		case iONCLICK: 	return EventFactory.ONCLICK_SLEEP;
		case iINPUT: return EventFactory.ONCLICK_SLEEP;
		case iUNDEFINED:
		default: return EventFactory.NON_SLEEP;
		}
	}
	public static String intToString(int type){
		switch(type){
		case iLAUNCH: 	return LAUNCH;
		case iREINSTALL: return REINSTALL;
		case iPRESS: 	return PRESS;
		case iONCLICK: 	return ONCLICK;
		case iINPUT:	return TEXT_INPUT;
		}
		return UNDEFINED;
	}
}
