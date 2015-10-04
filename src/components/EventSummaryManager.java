package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apex.Common;
import apex.staticFamily.StaticMethod;
import apex.symbolic.PathSummary;

public class EventSummaryManager implements Serializable{
	
	Map<String, List<PathSummary>> sumMap = new HashMap<String,List<PathSummary>>();
	
	Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> segmentalStorage_symbolic = 
			new HashMap<Event,Map<String, Map<Integer, List<EventSummaryPair>>>>();
	
	Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> segmentalStorage_concrete = 
			new HashMap<Event,Map<String, Map<Integer, List<EventSummaryPair>>>>();
	
	Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> concreteStorage = 
			new HashMap<Event,Map<String, Map<Integer, List<EventSummaryPair>>>>();
	
	Map<EventSummaryPair, Sequence> summaryToSequence = new HashMap<EventSummaryPair, Sequence>();
	
	EventSummaryPriorityQueue queue = new EventSummaryPriorityQueue();
	
	/**
	 * find or construct the event summary pair, do the symbolic execution if 
	 * necessary
	 * 
	 * 'logs' contains a list of string list. the element string list is an exec  
	 * log for a method previous extracted from the logcat. Therefore the 'logs'  
	 * are a list of exec log for a list of methods. 
	 * 
	 * All the event summary pairs for validation are segmental (by contrast, a 
	 * concrete execution log might match multiple segmental summary in order as
	 * there could be more than one method in parallel called. 
	 * 
	 * 
	 * @param event --
	 * @param logs 
	 * @return
	 */
	public EventSummaryPair findSummary(Event event, List<List<String>> logs){
		//check if the path 	
		List<String> combined = new ArrayList<String>();
		for(List<String> log : logs){ combined.addAll(log);}
		
		EventSummaryPair esp = get_internal(event, combined, concreteStorage);
		if(esp != null) return esp;
			
		if(logs == null || logs.isEmpty()){
			EventSummaryPair result = new EventSummaryPair(event, null);
			put_internal(result, concreteStorage);
			return result;
		}
		
		//for each segmental method, check records or do symbolic execution
		List<PathSummary> pList = new ArrayList<PathSummary>();
		for(List<String> log : logs){
			EventSummaryPair seg = get_internal(event, log, segmentalStorage_concrete);
			if(seg != null){ // an event summary pair concreted executed before
				PathSummary pSum = seg.getPathSummary();
				pList.add(pSum);
				continue;
			}else if( (seg = remove_internal(event, log, segmentalStorage_symbolic)) != null ){
				//find in the symbolic one -- previously generated
				put_internal(seg, segmentalStorage_concrete);
				seg.setExecuted(true);
				pList.add(seg.getPathSummary());
			}else{ // the event summary pair list is not generated yet
				String first = log.get(0); 
				List<PathSummary> sumList = sumMap.get(first);
				if(sumList == null){ //do symbolic execution
					StaticMethod found = findMethod(first);
					if(found == null) continue;
					sumList = Common.symbolic.doFullSymbolic(found);
					sumMap.put(first, sumList);
				}
				if(sumList == null || sumList.isEmpty()) continue;
				
				int logSize = log.size();
				PathSummary matched = null;
				for(int i = 0 ; i< sumList.size(); i++){
					PathSummary sum = sumList.get(i);
					if(logSize == sum.getBranchExecutionLog().size() &&
							sum.matchesExecutionLog((ArrayList<String>)log) ){
						if(matched != null){
							System.out.println("Matched moce than once"); 
							matched = sum;
							EventSummaryPair pair = setTargetLines(new EventSummaryPair(event.clone(), sum, true));
							this.put_internal( pair , segmentalStorage_concrete);
							queue.add(pair);
						}else{	
							this.put_internal(
									setTargetLines(new EventSummaryPair(event.clone(), sum))
									, segmentalStorage_symbolic);
						}
					}
				}
				if(matched == null){matched = Common.symbolic.doFullSymbolic((ArrayList<String>)log);}
				if(matched != null) pList.add(matched);
			}
		}
		
		PathSummary resultSummary = null;
		for(PathSummary sum : pList){
			if(resultSummary == null) resultSummary = sum;
			else if( sum != null) resultSummary = resultSummary.concat(sum);
		}
		
		EventSummaryPair esResult = setTargetLines(new EventSummaryPair(event, resultSummary));
		put_internal(esResult, concreteStorage);
		esResult.setExecuted(true);
		return esResult;
	}

	/**
	 * Find the next candidate for validation
	 * @return
	 */
	public EventSummaryPair next(){
		while(!queue.isEmpty()){
			if(!queue.peek().isExecuted()) return queue.poll();
			queue.poll();
		}
		return null;
	}
	
	public boolean hasNext(){
		while(!queue.isEmpty()){
			if(!queue.peek().isExecuted()) return true;
			queue.poll();
		}
		return false;
	}
	
	public List<EventSummaryPair> getNextSequence(EventSummaryPair esPair){
		Sequence seq = summaryToSequence.get(esPair);
		if(seq == null){
			AnchorSolver solve = new AnchorSolver();
			List<List<EventSummaryPair>> generated = solve.solve(esPair);
			
			seq = new Sequence();
			summaryToSequence.put(esPair, seq);
			
			seq.eCount = Common.model.getAllEdges().size();
			seq.vCount = Common.model.getAllVertex().size();
			
			if(generated == null || generated.isEmpty()) return null;
			return seq.next();
		}else{
			int newECount = Common.model.getAllEdges().size();
			int newVCount = Common.model.getAllVertex().size();
			if( newECount!=seq.eCount || newVCount!=seq.vCount ){
				AnchorSolver solve = new AnchorSolver();
				List<List<EventSummaryPair>> generated = solve.solve(esPair);
				seq.addAll(generated);
			}
			return seq.next();
		}
	}
	
	EventSummaryPair setTargetLines(EventSummaryPair esPair){
		if(Common.targets == null || Common.targets.isEmpty()) return esPair;
		List<String> logs = esPair.getPathSummary().getExecutionLog();
		List<String> existence = new ArrayList<String>();
		for(String ta : Common.targets){
			if(logs.contains(ta)){
				existence.add(ta);
			}
		}
		esPair.setTargetLiness(existence);
		return esPair;
	}

	EventSummaryPair remove_internal(Event event, List<String> actualLogs,			
			Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){
		Map<String, Map<Integer, List<EventSummaryPair>>> primary = storage.get(event);
		if(primary == null) return null;
		
		String firstLine = null;
		if(actualLogs == null || actualLogs.isEmpty()) { firstLine = "";
		}else{ firstLine = actualLogs.get(0); }
		
		Map<Integer, List<EventSummaryPair>> secondary = primary.get(firstLine);
		if(secondary == null) return null;
		
		List<EventSummaryPair> esList = null;
		if(actualLogs == null || actualLogs.isEmpty()){
			esList =  secondary.get(0);
		}else esList =  secondary.get(actualLogs.size());
		
		EventSummaryPair result = null;
		int i=0;
		for(;i<esList.size() ; i++){
			EventSummaryPair es = esList.get(i);
			if(es.getPathSummary().matchesExecutionLog((ArrayList<String>)actualLogs)){
				result = es; break;
			}
		}
		esList.remove(i);	
		return result;
	}
	
	EventSummaryPair get_internal(Event event, List<String> actualLogs, 
			Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){
		Map<String, Map<Integer, List<EventSummaryPair>>> primary = storage.get(event);
		if(primary == null) return null;
		
		String firstLine = null;
		if(actualLogs == null || actualLogs.isEmpty()) { firstLine = "";
		}else{ firstLine = actualLogs.get(0); }
		
		Map<Integer, List<EventSummaryPair>> secondary = primary.get(firstLine);
		if(secondary == null) return null;
		
		List<EventSummaryPair> esList = null;
		if(actualLogs == null || actualLogs.isEmpty()){
			esList =  secondary.get(0);
		}else esList =  secondary.get(actualLogs.size());
		
		for(EventSummaryPair es : esList){
			if(es.getPathSummary().matchesExecutionLog((ArrayList<String>)actualLogs)){
				return es;
			}
		}
		return null;
	}
	
	void put_internal(EventSummaryPair esPair,
			Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){ //assume it does not exist
		
		PathSummary path = esPair.getPathSummary();
		Event event = esPair.getEvent();
		List<String> logs = path.getBranchExecutionLog();
		
		Map<String, Map<Integer, List<EventSummaryPair>>> primary = storage.get(event);
		if(primary == null){ 
			primary = new HashMap<String, Map<Integer, List<EventSummaryPair>>>(); 
			storage.put(event, primary);
		}
		String firstLine = (logs == null || logs.isEmpty()) ? "" : logs.get(0);
		Map<Integer, List<EventSummaryPair>> secondary = primary.get(firstLine);
		if(secondary == null){
			secondary = new HashMap<Integer, List<EventSummaryPair>>();
			primary.put(firstLine, secondary);
		}
		int size = (logs == null) ? 0 : logs.size();
		List<EventSummaryPair> esList = secondary.get(size);
		
		if(esList == null){ esList = new ArrayList<EventSummaryPair>(); }
		esList.add(esPair);
	}
	
	StaticMethod findMethod(String line){
		//should be "Method_Return,"
		String mLine = line.replace("Method_Starting,", "");
		return Common.app.getMethod(mLine);
	}
	
	class Sequence{
		List<List<EventSummaryPair>> waiting;
		List<List<EventSummaryPair>> tried;
		
		int vCount, eCount;//used to check if new validation needed.
		Sequence(){
			waiting = new ArrayList<>();
			tried = new ArrayList<>();
			vCount = eCount = 0;
		}
		void addAll(List<List<EventSummaryPair>> toAdd){
			if(toAdd == null) return;
			for(List<EventSummaryPair> e : toAdd){
				if(!waiting.contains(e))waiting.add(e);
			}
			Collections.sort(toAdd, new ListSZComparator());
		}
		
		List<EventSummaryPair> next(){
			if(waiting == null || waiting.isEmpty()) return null;
			List<EventSummaryPair> result = waiting.get(0);
			tried.add(result);
			return result;
		}
		
		boolean isEmpty(){
			return waiting == null || waiting.isEmpty();
		}
	}

}


















