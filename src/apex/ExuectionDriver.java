package apex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import components.Event;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.ListSZComparator;

public class ExuectionDriver {
	
	Stack<Event> newEventList = new Stack<Event>();
	GraphicalLayout currentUI = GraphicalLayout.Launcher;
	private boolean enableStepInspect = false;
	private Scanner sc;
	
	void prepare(){
		Common.eExecution.reinstall();
		String pkgName = Common.app.getPackageName();
		String actName = Common.app.getMainActivity().getJavaName();
		newEventList.add(EventFactory.createLaunchEvent(
				GraphicalLayout.Launcher, pkgName, actName));
		 
		if(enableStepInspect){
			sc = new Scanner(System.in);
		}
	}
	
	public Stack<Event> getNewEventList() {
		return newEventList;
	}

	public void setNewEventList(Stack<Event> newEventList) {
		this.newEventList = newEventList;
	}



	/**
	 * drive the execution
	 * @return if should 
	 */	
	boolean kick(){
		Common.TRACE();
		
		if(enableStepInspect){
			System.out.println("User intervention: ");
			while(true){
				String line = sc.nextLine().trim();
				if(line.equals("exit")){
					System.exit(0);
				}else if(line.equals("graph")){
					Common.model.showGUI(false);
				}else if(line.equals("next")){
					if(newEventList.isEmpty() == false){
						System.out.println("Response: "+newEventList.peek());
					}else if(Common.summaryManager.hasNext()){
						System.out.println("Response: "+Common.summaryManager.peek());
					}else{
						System.out.println("Response: "+"Empty");
					}
				}else if(line.equalsIgnoreCase("a")){
					enableStepInspect = false; break;
				}else if(line.isEmpty()) break;
			}
		}
		
		Event event = null;
		EventSummaryPair validationCandidate = null;
		if(!newEventList.isEmpty()){
			event = newEventList.pop();
			Common.TRACE("Pop new Event: "+event.toString());
			if(!event.getSource().equals(this.currentUI)){
				if(Common.eExecution.reposition(currentUI, event.getSource())){
					this.currentUI = event.getSource();
				}else{
					Common.eExecution.reinstall();
					this.currentUI = GraphicalLayout.Launcher;
					System.out.println("New event reposition failure");
					return true; 
				}
			}
		}else if( (validationCandidate = Common.summaryManager.next()) != null ){
			event = validationCandidate.getEvent();
			
			if(Common.DEBUG){System.out.println("Validation: "+validationCandidate.toString());}
			if(validationCandidate.isExecuted()) return true; //should not happen
			List<EventSummaryPair> solvingSequence = Common.summaryManager.getValidationSequence(validationCandidate);
			if(solvingSequence == null || solvingSequence.isEmpty()) return true;
			Common.eExecution.reinstall();
			EventExecutionResult midResult = Common.eExecution.doSequence(solvingSequence, true);
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
		EventExecutionResult finalResult = Common.eExecution.carrayout(event);
		if(finalResult.errorCode != EventExecutionResult.NO_ERROR){
			Common.TRACE("Execution error: "+EventExecutionResult.codeToString(finalResult.errorCode));
			//TODO to improve; currently reinstall app and reset to launcher
			switch(finalResult.errorCode){
			case EventExecutionResult.ERROR_LOAD_LAYOUT:
			case EventExecutionResult.ERROR_FOCUS_WDINDOW:{
				EventSummaryPair crashEvent = Common.summaryManager.findSummary(event, null);
				crashEvent.setTarget(finalResult.predefinedUI); //should be the ErrorScene
				Common.model.record(crashEvent);
				Common.eExecution.reinstall();
				currentUI = GraphicalLayout.Launcher;
			}break;
			// the followings are currently not implemented and should not happen
			case EventExecutionResult.ERROR_RETRIEVE_WIN_INFO: 
			case EventExecutionResult.ERROR_READ_LOGCAT:
			case EventExecutionResult.ERROR_CHECK_KEYBOARD:
			}
		}else if(finalResult.isCrashed){ //application crashed
			Common.TRACE("Application has crashed");
			EventSummaryPair crashEvent = Common.summaryManager.findSummary(event, null);
			crashEvent.setTarget(finalResult.predefinedUI); //should be the ErrorScene
			Common.model.record(crashEvent);
			Common.eExecution.reinstall();
			currentUI = GraphicalLayout.Launcher;
		}else{
			EventSummaryPair esPair = Common.summaryManager.findSummary(event, finalResult.sequences);
			GraphicalLayout dest 
				= finalResult.predefinedUI != null ? finalResult.predefinedUI
				: Common.model.findOrConstructUI(finalResult.focusedWin.actName, finalResult.node);
			esPair.setTarget(dest);
			currentUI = dest;
			List<Event> newEvents = Common.model.update(esPair, finalResult);
			if(newEvents != null) newEventList.addAll(newEvents);
			
			if(Common.DEBUG){
				Common.TRACE(dest.toString());
				Common.TRACE("Generate events size:"+((newEvents != null)?newEvents.size():0 ));
				if(newEvents!= null){
					for(Event nEvent : newEvents){
						Common.TRACE(nEvent.toString());
					}
				}
			}
			Common.model.record(esPair);
			checkTargetReach(esPair);
			Common.TRACE("Update current UI: "+dest.toString());
		}	
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
	
	void finish(){
		Common.eExecution.clearup();
	}
}
