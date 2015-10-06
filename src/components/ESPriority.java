package components;

import java.io.Serializable;
import java.util.Comparator;

import apex.Common;
import components.EventSummaryPair;

public class ESPriority implements Comparator<EventSummaryPair>, Serializable{

	/**
	 * Calculate the priority of input
	 * Less tries, more syms, less constraints, more targets
	 * 
	 * @param esPair
	 * @return
	 */
	public static int calculate(EventSummaryPair esPair){
		int tryCount = esPair.getValidationCount();
		int symCount = esPair.getPathSummary().getSymbolicStates().size();
		int conCount = esPair.getPathSummary().getPathConditions().size();
		int distence = Common.model!=null ? Common.model.findSequence(GraphicalLayout.Launcher, 
				esPair.getSource()).size() : 0; //-- this change over time cannot control well.
		int targets = esPair.getTargetLines() == null? 0 : esPair.getTargetLines().size();
		
		return -tryCount + symCount - conCount - distence + targets; //
	}

	@Override
	public int compare(EventSummaryPair o1, EventSummaryPair o2) {
		return calculate(o2) - calculate(o1);
	}
}
