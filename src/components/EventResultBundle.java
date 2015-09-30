package components;

import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import apex.symbolic.Expression;
 
 

public class EventResultBundle{ 
	
	public final static int SCOPE_WITHIN = 0, SCOPE_LAUNCHER = 1, SCOPE_OUT =2;
	
	public EventResultBundle(Event event){
		this.event = event;
	}
	public Event event;
	
	public boolean hasCrashed = false;
	public int scope = SCOPE_WITHIN;
	
	public int majorBranchIndex;
	public GraphicalLayout resultedGUI;
	public List<String> methodRoots;
	public List<WrappedSummary> mappedSummaries;
	
	//data during process
	public List<String> feedBack;
	public List<DefaultMutableTreeNode> methodIOTrees;
	
	
	public WrappedSummary getPrimaryBranch(){
		return mappedSummaries.get(majorBranchIndex);
	}
	
	public Map<Expression,Expression> getCombinedSymbolicStates(){
		
		return null;
	}
	public List<Expression> getCombinedConstraints(){
		
		return null;
	}
}