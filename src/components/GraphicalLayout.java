package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import support.Logger;

public class GraphicalLayout implements Serializable{ 
	
	public final static GraphicalLayout Launcher;
	public final List<Event> candidates = new ArrayList<Event>();
	private static int gIndex = 0;
	static{
		Launcher = new GraphicalLayout("Launcher",null);
	}
	
	
	String actName;
	LayoutNode layout;
	public int index = gIndex++;
//	public GraphicalLayout associated;//only I will touch this ...
	
	
	public GraphicalLayout(String actName, LayoutNode node){
		this.layout = node;
		this.actName = actName;
	}
		
	public String getActName() {
		return actName;
	}

	public LayoutNode getRootNode() {
		return layout;
	}

	@Override
	public String toString(){
		return this.actName + "_"+index;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof GraphicalLayout){
			GraphicalLayout input = (GraphicalLayout)other;
			if(!input.actName.equals(this.actName))return false;
			if(this.layout != null){
				return this.layout.equals(input.layout);
//				if(!this.layout.equals(input.layout)){
//					return false;
//				}
			}else{
				return input.layout == null;
//				if(input.layout != null) return false;
			}
//			return true;
		}
		return false;
	}
	
	public boolean hasTheSmaeLayout(LayoutNode input){
		if( this.layout == null){ return input == null;
		}else{ return this.layout.equals(input); }
	}
	
	@Override
	public int hashCode(){
		return actName.hashCode();
	}
	
}
