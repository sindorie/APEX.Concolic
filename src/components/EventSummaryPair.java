package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.graph.DefaultEdge;

import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
 
public class EventSummaryPair extends DefaultEdge implements Serializable{ 
	private static int gIndex = 0;
	private int index = gIndex++;

	private Event event;
	private PathSummary summary;
	private boolean isExecuted = false;
	private int validationTries = 0;
	private GraphicalLayout target;
	
	public EventSummaryPair(Event event, PathSummary sum){
		this.event = event;
		this.summary = sum;
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
	public void increateCount() {
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
	public GraphicalLayout getSource(){
		return this.event.getSource();
	}
	@Override
	public GraphicalLayout getTarget(){
		return this.target;
	}
}