package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.graph.DefaultEdge;

import apex.symbolic.PathSummary;
 
public class EventSummaryPair extends DefaultEdge implements Serializable{ 
	private static int gIndex = 0;
	private int index = gIndex++;

	private Event event;
	private PathSummary summary;
	private boolean isExecuted = false;
	private int validationTries = 0;
	private GraphicalLayout target;
	private List<String> targetLines;
	
	public EventSummaryPair(Event event, PathSummary sum, boolean concrete){
		this.event = event;
		this.summary = sum;
		this.isExecuted = concrete;
		this.targetLines = new ArrayList<String>();
	}
	public EventSummaryPair(Event event, PathSummary sum){
		this.event = event;
		this.summary = sum;
		this.targetLines = new ArrayList<String>();
	}
	public void setTargetLiness(List<String> lines){
		targetLines.addAll(lines);
	}
	public List<String> getTargetLines(){
		return targetLines;
	}
	public boolean isExecuted() {
		return isExecuted;
	}
	public void setExecuted(boolean isExecuted) {
		this.isExecuted = isExecuted;
	}
	public Event getEvent() {
		return event;
	}
	public void increaseCount() {
		this.validationTries += 1;
	}
	public int getValidationCount(){
		return this.validationTries;
	}
	public PathSummary getPathSummary(){
		return this.summary;
	}
	public void setTarget(GraphicalLayout tar){
		this.target = tar;
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if( o instanceof EventSummaryPair){
			EventSummaryPair other = (EventSummaryPair)o;
			if(!this.event.equals(other.event)) return false;
			if(this.summary == null){
				return other.summary == null;
			}else{
				if(other.summary == null) return false;
				return this.summary.getExecutionLog().equals(other.summary.getExecutionLog());
			}
		}	
		return false;
	}
	
	@Override 
	public String toString(){
		return this.event.toString()+"_"+summary==null?"null":summary.getExecutionLog().get(0);
	}
	
	@Override
	public int hashCode(){
		return this.event.hashCode();
	}
	@Override
	public GraphicalLayout getSource(){
		return this.event.getSource();
	}
	@Override
	public GraphicalLayout getTarget(){
		return this.target;
	}
}