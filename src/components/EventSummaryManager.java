package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import apex.Common;
import apex.staticFamily.StaticMethod;
import apex.symbolic.PathSummary;
import apex.symbolic.ToDoPath;

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
	
	
	public List<EventSummaryPair> getAllSymbolicSegmentalSummary(){
		return showData(segmentalStorage_symbolic);
	}
	public List<EventSummaryPair> getAllConcreteSegmentalSummary(){
		return showData(segmentalStorage_concrete);
	}
	public List<EventSummaryPair> getAllConcreteSummary(){
		return showData(concreteStorage);
	}
	private List<EventSummaryPair> showData(Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){
		List<EventSummaryPair> result = new ArrayList<>();
		for(Entry<Event, Map<String, Map<Integer, List<EventSummaryPair>>>> entry : storage.entrySet()){
			for(Entry<String, Map<Integer, List<EventSummaryPair>>> entry1 : entry.getValue().entrySet()){
				for(Entry<Integer, List<EventSummaryPair>> entry2 : entry1.getValue().entrySet()){
					List<EventSummaryPair> sums = entry2.getValue();
					if(sums != null) result.addAll(sums);
				}
			}
		}
		return result;
	}
	
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
	 * @param separetedLogs 
	 * @return
	 */
	public EventSummaryPair findSummary(Event event, List<List<String>> separetedLogs){
		Common.TRACE();
		
		List<List<String>> expandedList = null;
		List<String> signatures = null;
		List<String> combined = null;
		if(separetedLogs != null && !separetedLogs.isEmpty()){
			expandedList = new ArrayList<>();
			combined = new ArrayList<String>();
			signatures = new ArrayList<String>();
			for(List<String> log : separetedLogs){ 
				signatures.add(this.extractMethodSig(log.get(0)));
				ToDoPath tdp = Common.symbolic.expandLogcatOutput((ArrayList<String>)log);
				List<String> expanded = tdp.getExecutionLog();
				expandedList.add(expanded);
				combined.addAll(expanded);
			}
		}
		EventSummaryPair esp = get_internal(event, combined, concreteStorage);
		if(esp != null) return esp;
		if(separetedLogs == null || separetedLogs.isEmpty()){
			EventSummaryPair result = new EventSummaryPair(event, null);
			put_internal(result, concreteStorage);
			result.setExecuted(true);
			return result;
		}
		
		//for each segmental method, check records or do symbolic execution
		List<PathSummary> pList = new ArrayList<PathSummary>();
		for(int log_index =0 ; log_index< expandedList.size() ; log_index++){
			List<String> expandedLog = expandedList.get(log_index);
			List<String> rawLogcat = separetedLogs.get(log_index);
			String signature = signatures.get(log_index);
			
			Common.TRACE();
			if(Common.DEBUG){
				for(String line: expandedLog){
					System.out.println(line);
				}
			}
			
			EventSummaryPair segESPair = get_internal(event, expandedLog, segmentalStorage_concrete);
			if(segESPair != null){ // an event summary pair concreted executed before
				PathSummary pSum = segESPair.getPathSummary();
				pList.add(pSum);
				continue;
			}else if( (segESPair = remove_internal(event, expandedLog, segmentalStorage_symbolic)) != null ){
				//find in the symbolic one -- previously generated
				put_internal(segESPair, segmentalStorage_concrete);
				segESPair.setExecuted(true);
				pList.add(segESPair.getPathSummary());
			}else{ // the event summary pair list is not generated yet
				List<PathSummary> sumList = sumMap.get(signature);
				if(sumList == null){ //do symbolic execution
					StaticMethod found = findMethod(signature);
					if(found == null) continue;
					Common.TRACE();
					sumList = Common.symbolic.doFullSymbolic(found);
					Common.TRACE( (sumList!=null?sumList.size():0)+"");
					sumMap.put(signature, sumList);
				}
				if(sumList == null || sumList.isEmpty()) continue;

				Common.TRACE();
				PathSummary matched = null;
				for(int sum_index = 0 ; sum_index< sumList.size(); sum_index++){
					PathSummary sum = sumList.get(sum_index);
					
					Common.TRACE();
					if(Common.DEBUG){
						for(String line: sum.getExecutionLog()){
							System.out.println(line);
						}
					}
					if(	sum.getExecutionLog().size() == expandedLog.size() && sum.getExecutionLog().equals(expandedLog)){
						if(matched == null){
							matched = sum;
							EventSummaryPair pair = setTargetLines(new EventSummaryPair(event.clone(), sum, true));
							this.put_internal( pair , segmentalStorage_concrete);
						}else{
							System.out.println("Matched moce than once"); //should not happen
						}
					}else{
						EventSummaryPair pair = setTargetLines(new EventSummaryPair(event.clone(), sum));
						this.put_internal( pair , segmentalStorage_symbolic);
						queue.add(pair);
					}
				}
				if(matched == null){
					Common.TRACE();
					if(Common.DEBUG){
						for(String line : expandedLog){
							System.out.println(line);
						}	
					}
					Common.TRACE("Symbolic execution by logcat out");
					matched = Common.symbolic.doFullSymbolic((ArrayList<String>)rawLogcat);
				}
				if(matched != null){
					pList.add(matched);
				}else{
					System.out.println("Matching failure");
				}
				Common.TRACE();
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
		Common.TRACE();
		while(!queue.isEmpty()){
			if(!queue.peek().isExecuted()){
				EventSummaryPair result = queue.poll();
				result.increaseCount();
				queue.add(result);
				return result;
			}
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
		Common.TRACE();
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

	EventSummaryPair remove_internal(Event event, List<String> expandedLog,			
			Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){
		Common.TRACE();
		Map<String, Map<Integer, List<EventSummaryPair>>> primary = storage.get(event);
		if(primary == null) return null;
		
		String methodSig = null;
		if(expandedLog == null || expandedLog.isEmpty()) { methodSig = "";
		}else{ methodSig = extractMethodSig(expandedLog.get(0)); }
		
		Map<Integer, List<EventSummaryPair>> secondary = primary.get(methodSig);
		if(secondary == null) return null;
		
		List<EventSummaryPair> esList = null;
		if(expandedLog == null || expandedLog.isEmpty()){
			esList =  secondary.get(0);
		}else esList =  secondary.get(expandedLog.size());
		if(esList == null) return null; //did not find
		
		if(expandedLog == null || expandedLog.isEmpty()){
			if(esList.size() > 0 ){
				return esList.remove(0); //event-pathsummary(null) can only has one candidate --should not happen
			}else{ return null; }
		}else{
			EventSummaryPair result = null;
			int i=0;
			for(;i<esList.size() ; i++){
				EventSummaryPair es = esList.get(i);
				PathSummary sum = es.getPathSummary();
				if(sum == null) continue;
				if( sum.getExecutionLog().size() == expandedLog.size() &&
						sum.getExecutionLog().equals(expandedLog)){
					result = es; break;
				}
			}
			if(result != null) esList.remove(i);	
			return result;
		}
	}
	
	EventSummaryPair get_internal(Event event, List<String> expandedLog, 
			Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){
		Common.TRACE();
		Map<String, Map<Integer, List<EventSummaryPair>>> primary = storage.get(event);
		if(primary == null) return null;
		
		String methodSig = null;
		if(expandedLog == null || expandedLog.isEmpty()) { methodSig = "";
		}else{ methodSig = extractMethodSig(expandedLog.get(0)); }
		
		Map<Integer, List<EventSummaryPair>> secondary = primary.get(methodSig);
		if(secondary == null) return null;
		
		List<EventSummaryPair> esList = null;
		if(expandedLog == null || expandedLog.isEmpty()){
			esList =  secondary.get(0);
		}else esList =  secondary.get(expandedLog.size());
		if(esList == null) return null;
		
		if(expandedLog == null){
			for(EventSummaryPair es : esList){
				if(es.getPathSummary() == null){
					return es;
				}
			}
		}else{
			for(EventSummaryPair es : esList){
				PathSummary sum = es.getPathSummary();
				if(sum == null) continue;//should not happen
				if( sum.getExecutionLog().size() == expandedLog.size() &&
						sum.getExecutionLog().equals(expandedLog) ){
					return es;
				}
			}
		}
		return null;
	}
	
	void put_internal(EventSummaryPair esPair,
			Map<Event,Map<String, Map<Integer, List<EventSummaryPair>>>> storage){ //assume it does not exist
		
		Common.TRACE();
		PathSummary path = esPair.getPathSummary();
		Event event = esPair.getEvent();
		List<String> logs = ((path== null) ? null : path.getExecutionLog());
		
		Map<String, Map<Integer, List<EventSummaryPair>>> primary = storage.get(event);
		if(primary == null){ 
			primary = new HashMap<String, Map<Integer, List<EventSummaryPair>>>(); 
			storage.put(event, primary);
		}
		String methodSig =  ((path!= null) ? path.getMethodSignature() : ""); 
		Map<Integer, List<EventSummaryPair>> secondary = primary.get(methodSig);
		if(secondary == null){
			secondary = new HashMap<Integer, List<EventSummaryPair>>();
			primary.put(methodSig, secondary);
		}
		int size = (logs == null) ? 0 : logs.size();
		List<EventSummaryPair> esList = secondary.get(size);
		if(esList == null){ 
			esList = new ArrayList<EventSummaryPair>(); 
			secondary.put(size, esList);
		}
		esList.add(esPair); //does not check due to assumption
	}
	
	
	String extractMethodSig(String line){
		return line.replace("Method_Starting,", "").trim();
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


















