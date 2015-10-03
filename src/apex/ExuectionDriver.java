package apex;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import apex.staticFamily.StaticMethod;
import apex.symbolic.PathSummary;
import components.Event;
import components.EventSummaryPair;
import components.EventSummaryDeposit;
import components.ExpressionTranfomator;
import components.GraphicalLayout;
import components.WrappedSummary;

public class ExuectionDriver {
	
	List<Event> newEventList = new ArrayList<Event>();
	EventSummaryDeposit toValidate = new EventSummaryDeposit();
	ExpressionTranfomator tranform;
	EventExecution eExecution = new EventExecution();
	GraphicalLayout currentUI;
	/**
	 * drive the execution
	 * @return if should 
	 */
	void kick(){
		Event event = null;
		EventSummaryPair validationCandidate = null;
		if(!newEventList.isEmpty()){
			event = newEventList.get(0); 
			if(!event.getSource().equals(this.currentUI)){
				List<EventSummaryPair> repositionSequence = 
						Common.model.findKownSequence(event.getSource());
				if(repositionSequence == null) return;
				EventExecutionResult midResult = eExecution.doSequence(repositionSequence, true);
				GraphicalLayout focuedWin 
					= midResult.predefinedUI != null ? midResult.predefinedUI
					: Common.model.findOrConstructUI(midResult.focusedWin.actName, midResult.node);
				this.currentUI = focuedWin;
				if( event.getSource() != focuedWin ){
					return; // reposition failure
				}
			}
		}else if(!toValidate.isEmpty()){
			validationCandidate = toValidate.next();
			event = validationCandidate.getEvent();
			if(validationCandidate.isExecuted()) return; //should not happen
			//TODO solving;
			List<EventSummaryPair> solvingSequence = null;
			if(solvingSequence == null) return;
			eExecution.doSequence(solvingSequence, false);
		}
		
		EventExecutionResult finalResult = eExecution.carrayout(event);
		if(finalResult == null){}//something is wrong; -- Cannot retrieve focused window
		EventSummaryPair esPair = Common.esDeposit.findSummary(event, finalResult.sequences);
		GraphicalLayout dest 
			= finalResult.predefinedUI != null ? finalResult.predefinedUI
			: Common.model.findOrConstructUI(finalResult.focusedWin.actName, finalResult.node);
		esPair.setTarget(dest);
		currentUI = dest;
		Common.model.update(esPair);
		Common.model.record(esPair);
	}
}
