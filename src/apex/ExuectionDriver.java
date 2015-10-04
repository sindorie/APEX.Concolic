package apex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import components.Event;
import components.EventSummaryPair;
import components.EventSummaryManager;
import components.ExpressionTranfomator;
import components.GraphicalLayout;

public class ExuectionDriver {
	
	List<Event> newEventList = new ArrayList<Event>();
	EventSummaryManager toValidate = new EventSummaryManager();
	EventExecution eExecution = new EventExecution(Common.serial, Common.model, Common.app);
	GraphicalLayout currentUI = GraphicalLayout.Launcher;
	Map<String, List<List<EventSummaryPair>>> foundSequences = 
				new HashMap<String, List<List<EventSummaryPair>>>();
	
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
		if(finalResult == null){
			System.out.println("Final reuslt is null for execution result");
			return;
		}//something is wrong; -- Cannot retrieve focused window
		EventSummaryPair esPair = Common.esManager.findSummary(event, finalResult.sequences);
		GraphicalLayout dest 
			= finalResult.predefinedUI != null ? finalResult.predefinedUI
			: Common.model.findOrConstructUI(finalResult.focusedWin.actName, finalResult.node);
		esPair.setTarget(dest);
		currentUI = dest;
		List<Event> newEvents = Common.model.update(esPair, finalResult);
		if(newEvents != null) newEventList.addAll(newEvents);
		
		Common.model.record(esPair);
		checkTargetReach(finalResult.log);
	}
	
	void checkTargetReach(List<String> log){
		if(log == null || Common.targets == null || Common.targets.isEmpty()) return;
		for(String line : log){
			if(Common.targets.contains(line)){
				Common.remaining.remove(line);
				List<List<EventSummaryPair>> seqRecord = foundSequences.get(line);
				if(seqRecord == null){
					seqRecord = new ArrayList<List<EventSummaryPair>>();
					foundSequences.put(line, seqRecord);
				}
				seqRecord.add(Common.model.getCurrentLine());
			}
		}
	}
}
