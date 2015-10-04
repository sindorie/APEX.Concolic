package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GraphicalLayout implements Serializable{ 
	
	String actName;
	LayoutNode layout;
	public int index;
	public final static GraphicalLayout Launcher;
	public final static String LAUNCHER_NAME = "Launcher";
	public final List<Event> candidates = new ArrayList<Event>();
	private static int gIndex = 0;
	static{ Launcher = new GraphicalLayout(LAUNCHER_NAME,null); }
	
	public GraphicalLayout(String actName, LayoutNode node){
		this.layout = node;
		this.actName = actName;
		index = gIndex++;
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
			}else{
				return input.layout == null;
			}
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
