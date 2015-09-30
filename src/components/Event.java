package components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DefaultEdge;
/**
 * The event class which also plays as edge in the graph 
 * @author zhenxu
 *
 */
public class Event extends DefaultEdge implements Serializable{
	
	static int aIndex = 0; //not sure if 100% necessary as e1 == e2 seems sufficient for the purpose
	int eventType, index = aIndex++; 
	GraphicalLayout source, dest;
	Map<String, Object> attributes;
	boolean isCloseKeyboard = false, ignoreByRecorder = false;
	boolean poly = false;
	
	
	public final static String[] ColumnsIdentifier = {
		"Index", "Source","Destintation", "Type", "Attributes", "Ig", "CK"
	};
	public String[] toStringArray(){
		return new String[]{
				index+"", 
				source!=null?this.source.toString():"",
				dest!= null ? this.dest.toString() :"",
				EventFactory.intToString(eventType),
				attributes!= null? attributes.toString():"",
				ignoreByRecorder+"",
				isCloseKeyboard+""
		};
	}
	
	Event(){
		attributes = new HashMap<String, Object>(); 
	}
	Event(int eventType, GraphicalLayout source){
		this.eventType = eventType;
		this.source = source;
	}
	public Event(Event other){
		this.dest = other.dest;
		this.source = other.source;
		this.eventType = other.eventType;
		this.attributes = new HashMap<String, Object>(other.attributes); 
		this.poly = other.poly;
	}
	public boolean isCloseKeyboard(){
		return this.isCloseKeyboard;
	}
	
	public Event clone(){ return new Event(this); }
	
	@Override	
	public boolean equals(Object other){
		if(other instanceof Event){
			Event input = (Event)other;
			//since the destination and source is now shown in the toString method-*
			if(!this.toString().equals(input.toString())){ return false; }
			if(!this.source.equals(input.source)) { return false; }
			if(this.dest == null){  if(input.dest != null) return false;
			}else{  if(!this.dest.equals(input.dest)) return false;  }
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.toString().hashCode();
	}
	
	@Override
	public String toString(){
		String typename = EventFactory.intToString(eventType);
		String result = "";
//		switch(this.eventType){
//		case EventFactory.iLAUNCH:{
//			result = typename+", "+this.getAttribute(EventFactory.att_pkgName) +", "+this.getAttribute(EventFactory.att_actName);	
//		}break;
//		case EventFactory.iREINSTALL:	{
//			result = typename+", "+this.getAttribute(EventFactory.att_pkgName)+", "+this.getAttribute(EventFactory.att_pkgPath);	
//			break;
//		}
//		case EventFactory.iPRESS: 	 	{
//			result = typename + " keycode "+this.getAttribute(EventFactory.att_keyCode);
//			break;
//		}
//		case EventFactory.iONCLICK: 	 	{
//			result = typename + " "+this.getAttribute(EventFactory.att_xCoordinate)+","+this.getAttribute(EventFactory.att_yCoordinate);
//			break;
//		}
//		case EventFactory.iINPUT:			{
//			result = typename + " "+this.getAttribute(EventFactory.att_xCoordinate)+","+
//									this.getAttribute(EventFactory.att_yCoordinate)+","+
//									this.getAttribute(EventFactory.att_textInput)
//									;
//			break;
//		}
//		case EventFactory.iUNDEFINED:	{
//			result = typename; 
//			break;
//		}
//		}
		
		result = typename+", "+this.attributes.toString();
//		if(this.source != null) result += " in "+this.source;
//		if(this.dest != null) result += " to "+this.dest;
		return result;
	}
	
	public String toFormatedString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Type:\t").append(EventFactory.intToString(eventType)).append("\n");
		sb.append("\tPoly: ").append(this.poly).append("   Attributes:\n");
//		sb.append("\t").append(this.attributes.toString()).append("\n");
		switch(this.eventType){
		case EventFactory.iLAUNCH: {
			sb.append("\tPakacge: ").append(getAttribute(EventFactory.att_pkgName)).append("\n");
			sb.append("\tActivity: ").append(getAttribute(EventFactory.att_actName)).append("\n");	
		}break;
		case EventFactory.iREINSTALL: {
			sb.append("\tPakacge: ").append(getAttribute(EventFactory.att_pkgName)).append("\n");
			sb.append("\tPath: ").append(getAttribute(EventFactory.att_pkgPath)).append("\n");	
		}break;
		case EventFactory.iPRESS: {
			sb.append("\tKeyCode: ").append(getAttribute(EventFactory.att_keyCode)).append("\n");
		}break;
		case EventFactory.iONCLICK: {
			sb.append("\tX Cor: ").append(getAttribute(EventFactory.att_xCoordinate)).append("\n");
			sb.append("\tY Cor: ").append(getAttribute(EventFactory.att_yCoordinate)).append("\n");
		}break;
		case EventFactory.iINPUT:{
			sb.append("\tX Cor: ").append(getAttribute(EventFactory.att_xCoordinate)).append("\n");
			sb.append("\tY Cor: ").append(getAttribute(EventFactory.att_yCoordinate)).append("\n");
			sb.append("\tText:  ").append(getAttribute(EventFactory.att_textInput)).append("\n");
		}break;
		case EventFactory.iUNDEFINED: { break; }
		}
//		sb.append("Source: ").append(source);
		sb.append("Destination: ").append(dest);
		return sb.toString();
	}
	

	public void putAttribute(String name, Object att){
		this.attributes.put(name, att);
	}
	public Object getAttribute(String name){
		return this.attributes.get(name);
	}
	public int getEventType() {
		return eventType;
	}
	public void setEventType(int eventType) {
		this.eventType = eventType;
	}
	public GraphicalLayout getSource() {
		return source;
	}
	public void setSource(GraphicalLayout source) {
		this.source = source;
	}
	public GraphicalLayout getDest() {
		return dest;
	}
	public void setDest(GraphicalLayout dest) {
		this.dest = dest;
	}
}
