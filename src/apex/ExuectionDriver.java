package apex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import components.Event;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.ListSZComparator;

public class ExuectionDriver {
	
	Stack<Event> newEventList = new Stack<Event>();
	EventExecution eExecution = new EventExecution(Common.serial, Common.model, Common.app);
	GraphicalLayout currentUI = GraphicalLayout.Launcher;
	
	 void prepare(){
		 this.eExecution.reinstall();
		 String pkgName = Common.app.getPackageName();
		 String actName = Common.app.getMainActivity().getJavaName();
		 newEventList.add(EventFactory.createLaunchEvent(
				 GraphicalLayout.Launcher, pkgName, actName));
	 }
	
	/**
	 * drive the execution
	 * @return if should 
	 */
	boolean kick(){
		Common.TRACE();
		Event event = null;
		EventSummaryPair validationCandidate = null;
		if(!newEventList.isEmpty()){
			event = newEventList.pop();
			Common.TRACE("Pop new Event: "+event.toString());
			if(!event.getSource().equals(this.currentUI)){
				if(eExecution.reposition(currentUI, event.getSource())){
					this.currentUI = event.getSource();
				}else{
					eExecution.reinstall();
					this.currentUI = GraphicalLayout.Launcher;
					System.out.println("New event reposition failure");
					return true; 
				}
			}
		}else if( (validationCandidate = Common.summaryManager.next()) != null ){
			event = validationCandidate.getEvent();
			
			if(Common.DEBUG){System.out.println("Validation: "+validationCandidate.toString());}
			if(validationCandidate.isExecuted()) return true; //should not happen
			List<EventSummaryPair> solvingSequence = Common.summaryManager.getNextSequence(validationCandidate);
			if(solvingSequence == null || solvingSequence.isEmpty()) return true;
			eExecution.reinstall();
			EventExecutionResult midResult = eExecution.doSequence(solvingSequence, true);
			GraphicalLayout focuedWin 
				= midResult.predefinedUI != null ? midResult.predefinedUI
				: Common.model.findOrConstructUI(midResult.focusedWin.actName, midResult.node);
			this.currentUI = focuedWin;
			if(!focuedWin.equals(event.getSource())){
				return true; //failure
			}
		}else{
			System.out.println("Should end");
			return false;
		}
		Common.TRACE();
		EventExecutionResult finalResult = eExecution.carrayout(event);
		if(finalResult == null){
			System.out.println("Final reuslt is null for execution result");
			return true;
		}//something is wrong; -- Cannot retrieve focused window
		EventSummaryPair esPair = Common.summaryManager.findSummary(event, finalResult.sequences);
		GraphicalLayout dest 
			= finalResult.predefinedUI != null ? finalResult.predefinedUI
			: Common.model.findOrConstructUI(finalResult.focusedWin.actName, finalResult.node);
		esPair.setTarget(dest);
		currentUI = dest;
		List<Event> newEvents = Common.model.update(esPair, finalResult);
		if(newEvents != null) newEventList.addAll(newEvents);
		
		Common.TRACE("Generate events size:"+((newEvents != null)?newEvents.size():0 ));
		Common.model.record(esPair);
		checkTargetReach(esPair);
		Common.TRACE();
		return true;
	}
	
	void checkTargetReach(EventSummaryPair esPair){
		if(esPair.getPathSummary() == null) return;
		List<String> log = esPair.getPathSummary().getSourceCodeLog();
		if(log == null || Common.targets == null || Common.targets.isEmpty()) return;
		for(String line : log){
			if(Common.targets.contains(line)){
				Common.remaining.remove(line);
				List<List<EventSummaryPair>> seqRecord = Common.foundSequences.get(line);
				if(seqRecord == null){
					seqRecord = new ArrayList<List<EventSummaryPair>>();
					Common.foundSequences.put(line, seqRecord);
				}
				seqRecord.add(Common.model.getCurrentLine());
				Collections.sort(seqRecord, new ListSZComparator());
			}
		}
	}
}
