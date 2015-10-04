package apex;

import java.util.ArrayList;
import java.util.List;

import components.Event;
import components.EventSummaryPair;
import components.EventSummaryManager;
import components.ExpressionTranfomator;
import components.GraphicalLayout;

public class ExuectionDriver {
	
	List<Event> newEventList = new ArrayList<Event>();
	EventSummaryManager toValidate = new EventSummaryManager();
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
		}else if(!toValidate.hasNext()){
			validationCandidate = toValidate.next();
			event = validationCandidate.getEvent();
			if(validationCandidate.isExecuted()) return; //should not happen
			List<EventSummaryPair> solvingSequence = Common.esManager.getNextSequence(validationCandidate);
			if(solvingSequence == null) return;
			eExecution.doSequence(solvingSequence, false);
		}
		
		EventExecutionResult finalResult = eExecution.carrayout(event);
		if(finalResult == null){}//something is wrong; -- Cannot retrieve focused window
		EventSummaryPair esPair = Common.esManager.findSummary(event, finalResult.sequences);
		GraphicalLayout dest 
			= finalResult.predefinedUI != null ? finalResult.predefinedUI
			: Common.model.findOrConstructUI(finalResult.focusedWin.actName, finalResult.node);
		esPair.setTarget(dest);
		currentUI = dest;
		Common.model.update(esPair);
		Common.model.record(esPair);
	}
}
