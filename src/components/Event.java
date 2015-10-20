package components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.graph.DefaultEdge;
/**
 * The event class which also plays as edge in the graph 
 * @author zhenxu
 */
public class Event implements Serializable{
	int eventType; 
	GraphicalLayout source;
	Map<String, Object> attributes;
	boolean isCloseKeyboard = false, ignoreByRecorder = false;
	
	Event(){
		attributes = new HashMap<String, Object>(); 
	}
	Event(int eventType, GraphicalLayout source){
		this.eventType = eventType;
		this.source = source;
	}
	public Event(Event other){
		this.source = other.source;
		this.eventType = other.eventType;
		this.attributes = new HashMap<String, Object>(other.attributes); 
	}
	public boolean isCloseKeyboard(){
		return this.isCloseKeyboard;
	}
	
	public Event clone(){ return new Event(this); }
	
	@Override	
	public boolean equals(Object other){
		if(other instanceof Event){
			Event input = (Event)other;
			return this.toString().equals(input.toString());
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
		result = typename+":"+source+":[";
		for(Entry<String,Object> entry : this.attributes.entrySet()){
			result += entry.getKey()+"="+entry.getValue().toString()+":";
		}
		result += "]";
		return result;
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
	
}
