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
	private static final long serialVersionUID = 1L;

	private Map<String, List<PathSummary>> sumMap = new HashMap<String,List<PathSummary>>();
	
	private EventSummaryStorage 
			segmentalStorage_invalid = new EventSummaryStorage(), 
			segmentalStorage_symbolic = new EventSummaryStorage(),
			segmentalStorage_concrete = new EventSummaryStorage(),
			concreteStorage = new EventSummaryStorage();
	
	private Map<EventSummaryPair, Sequence> summaryToSequence = new HashMap<EventSummaryPair, Sequence>();
	
	private EventSummaryPriorityQueue queue = new EventSummaryPriorityQueue();
	
	transient AnchorSolver solver = new AnchorSolver();
	
	final static int maxValidationCount = 10;

	public List<EventSummaryPair> getRemainingInQueue(){
		return this.queue.getRemaining();
	}
	public List<EventSummaryPair> getAllInvalidSegmentalSummary(){
		return segmentalStorage_invalid.getAlldata();
	}
	public List<EventSummaryPair> getAllSymbolicSegmentalSummary(){
		return segmentalStorage_symbolic.getAlldata();
	}
	public List<EventSummaryPair> getAllConcreteSegmentalSummary(){
		return segmentalStorage_concrete.getAlldata();
	}
	public List<EventSummaryPair> getAllConcreteSummary(){
		return concreteStorage.getAlldata();
	}
	
	/**
	 * find or construct an event summary pair, do the symbolic execution if 
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
	 * @param event
	 * @param separetedLogs 
	 * @return
	 */
	public EventSummaryPair findSummary(Event event, List<List<String>> separetedLogs){
		Common.TRACE(event.toString());
		List<List<String>> expandedList = null;
		List<String> signatures = null, combined = null;
		if(separetedLogs != null && !separetedLogs.isEmpty()){
			//the input logs are not empty; do the necessary initialization
			expandedList = new ArrayList<>();
			combined = new ArrayList<>();
			signatures = new ArrayList<>();
			for(List<String> log : separetedLogs){ 
				//expand a log into full execution log and find the first method signature
				String sig = this.extractMethodSig(log.get(0));
				signatures.add(sig);
				if(Common.DEBUG){
					Common.TRACE(sig);
					for(String line : log){
						Common.TRACE(line);
					}
				}
				ToDoPath tdp = Common.symbolic.expandLogcatOutput((ArrayList<String>)log);
				List<String> expanded = tdp.getExecutionLog();
				expandedList.add(expanded);
				combined.addAll(expanded);
			}
		}
		
		//Check if the concrete storage contains the event-path pair -> executed before
		String fSig = (signatures==null?"" : signatures.get(0));
		EventSummaryPair esp = concreteStorage.find(event, fSig, combined);
		if(esp != null) return esp;
		
		//log is empty -> no path summary but concrete execution 
		if(separetedLogs == null || separetedLogs.isEmpty()){
			EventSummaryPair result = new EventSummaryPair(event, null);
			concreteStorage.store(result, false);
			result.setExecuted(true);
			return result;
		}
		
		//for each segmental method, check records or do symbolic execution
		List<PathSummary> pList = new ArrayList<PathSummary>();
		for(int log_index =0 ; log_index< expandedList.size() ; log_index++){
			List<String> expandedLog = expandedList.get(log_index);
			List<String> rawLogcat = separetedLogs.get(log_index);
			String signature = signatures.get(log_index);
			Common.TRACE(signature+", "+expandedLog.size()+","+rawLogcat.size());
			
			//Check if there exist an event-path pair matching current info in the concrete-executed storage
			EventSummaryPair segESPair = segmentalStorage_concrete.find(event, signature, expandedLog);
			if(segESPair != null){ // an event summary pair concreted executed before
				Common.TRACE();
				PathSummary pSum = segESPair.getPathSummary();
				pList.add(pSum);
				continue;
			
			//Check if there exists an event-path pair matching current info in the invalid storage
			}else if( (segESPair = segmentalStorage_invalid.remove(event, signature, expandedLog) ) != null ){
				Common.TRACE();
				segmentalStorage_concrete.store(segESPair, false);
				segESPair.setExecuted(true);
				pList.add(segESPair.getPathSummary());
				continue;
				
			//Check if there exists an event-path pair matching current info in the segmental-symbolic storage
			}else if( (segESPair = segmentalStorage_symbolic.remove(event, signature, expandedLog) ) != null ){
				Common.TRACE();
				segmentalStorage_concrete.store(segESPair, false);
				segESPair.setExecuted(true);
				pList.add(segESPair.getPathSummary());
				continue;
				
			// the event summary pair list is not generated yet
			}else{ 
				Common.TRACE();
				List<PathSummary> sumList = sumMap.get(signature); //Check the cache
				if(sumList == null){ //do symbolic execution
					StaticMethod found = Common.app.getMethod(signature);
					if(found == null){
						Common.TRACE("Cannot find method:"+signature);
						continue;//failure
					}
					try{ 
						sumList = Common.symbolic.doFullSymbolic(found);
						if(sumList == null || sumList.isEmpty()) continue; // failure
					}catch(Exception e){
						System.out.println("Exception occur during symbolic execution on "+found.getSignature());
						e.printStackTrace();
						continue;//failure
					}
					Common.TRACE( "Add Summary "+(sumList!=null?sumList.size():0));
					sumMap.put(signature, sumList);
				}
				
				Common.TRACE("Strat matching");
				PathSummary matched = null;
				for(int sum_index = 0 ; sum_index< sumList.size(); sum_index++){
					PathSummary sum = sumList.get(sum_index);
					if(	sum.getExecutionLog().size() == expandedLog.size() &&  
							sum.getExecutionLog().equals(expandedLog)){
						if(matched == null){
							matched = sum;
							EventSummaryPair pair = setTargetLines(new EventSummaryPair(event.clone(), sum, true));
							segmentalStorage_concrete.store(pair, false);
						}else{ Common.TRACE("Matched moce than once");} /*Should not happen*/
						
					}else{
						EventSummaryPair pair = setTargetLines(new EventSummaryPair(event.clone(), sum));
						if(solver.hasValidConstraints(sum)){
							segmentalStorage_symbolic.store(pair, false);
							queue.add(pair);
						}else{ segmentalStorage_invalid.store(pair, false); }
					}
				}
				
				if(matched == null){
					Common.TRACE("Symbolic execution by logcat out:"+rawLogcat.size());
					try{ matched = Common.symbolic.doFullSymbolic((ArrayList<String>)rawLogcat);
					}catch(Exception e){
						Common.TRACE("Symbolic Execution failure");
						for(String line : rawLogcat){ Common.TRACE(line); }
						e.printStackTrace();
					}catch(Error e1){ e1.printStackTrace(); System.gc(); }
				}
				if(matched != null){ pList.add(matched);
				}else{ Common.TRACE("Matching failure"); }
			}
		}	
		
		PathSummary resultSummary = null;
		for(PathSummary sum : pList){
			if(resultSummary == null) resultSummary = sum;
			else if( sum != null) resultSummary = resultSummary.concat(sum);
		}
		
		EventSummaryPair esResult = setTargetLines(new EventSummaryPair(event, resultSummary));
		concreteStorage.store(esResult,false);
		esResult.setExecuted(true);
		Common.TRACE("Concrete execution: " + esResult.toString());
		return esResult;
	}

	/**
	 * Find the next candidate for validation
	 * @return
	 */
	public EventSummaryPair next(){
		EventSummaryPair result = null;
		while(!queue.isEmpty()){
			if(!queue.peek().isExecuted()){
				result = queue.poll();
				result.increaseCount();
				if(result.getValidationCount() < maxValidationCount){
					queue.add(result); 
				}
				break;
			}
			queue.poll();
		}
		Common.TRACE("Next Validation: "+result);
		return result;
	}
		
	/**
	 * Get a validation sequence for an event-path candidate
	 * @param esPair
	 * @return
	 */
	public List<EventSummaryPair> getValidationSequence(EventSummaryPair esPair){
		Common.TRACE();
		Sequence seq = summaryToSequence.get(esPair);
		if(seq == null){
			List<List<EventSummaryPair>> generated = solver.solve(esPair);
			seq = new Sequence();
			summaryToSequence.put(esPair, seq);
			seq.eCount = Common.model.getAllEdges().size();
			seq.vCount = Common.model.getAllVertex().size();
			seq.addAll(generated);
			if(generated == null || generated.isEmpty()) return null;
			return seq.next();
		}else{
			int newECount = Common.model.getAllEdges().size();
			int newVCount = Common.model.getAllVertex().size();
			if( newECount!=seq.eCount || newVCount!=seq.vCount ){
				seq.eCount = newECount;
				seq.vCount = newVCount;
				List<List<EventSummaryPair>> generated = solver.solve(esPair);
				if(generated != null) seq.addAll(generated);
			}
			return seq.next();
		}
	}
	
	private String extractMethodSig(String line){
		String result = line.replaceAll("^(Method_Starting,|Method_Returning,|execLog,)", "").trim();
		if(!line.startsWith("Method_Starting,")){
			Common.TRACE("Unexecpted first execution log: "+line);
			Common.TRACE("result: "+result);
		}
		return result; 
	}
	
	/**
	 * Given an event-path pair, check if it contains any target lines.
	 * @param esPair
	 * @return the processed input event-path pair
	 */
	private EventSummaryPair setTargetLines(EventSummaryPair esPair){
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
	
	class Sequence{
		List<List<EventSummaryPair>> waiting;
		List<List<EventSummaryPair>> tried;
		int vCount, eCount;//used to check if model has been updated needed.
		
		Sequence(){
			waiting = new ArrayList<>();
			tried = new ArrayList<>();
			vCount = eCount = 0;
		}
		
		void addAll(List<List<EventSummaryPair>> toAdd){ //could improve 
			if(toAdd == null) return;
			for(List<EventSummaryPair> e : toAdd){
				if(!executedBefore(e) && !waiting.contains(e) && !tried.contains(e)){
					//here could use some improvement in terms of time. TODO
					waiting.add(e);
				}
			}
			Collections.sort(toAdd, new ListSZComparator());
		}
		
		List<EventSummaryPair> next(){
			if(waiting == null || waiting.isEmpty()) return null;
			List<EventSummaryPair> result = waiting.get(0);
			tried.add(result);
			return result;
		}
		
		boolean executedBefore(List<EventSummaryPair> list){
			if(Common.model != null){
				List<List<EventSummaryPair>> slices = Common.model.getAllSlices();
				for(List<EventSummaryPair> slice : slices){
					if(slice.size() >= list.size()){
						boolean found = true;
						for(int i =0;i < list.size() ; i++){
							EventSummaryPair input = list.get(i);
							EventSummaryPair existed = slice.get(i);
							if(! input.getEvent().equals(existed.getEvent())){
								found = false; break;
							}
						}
						if(found){
							return true;
						}
					}
				}
			}
			return false;
		}
		
		boolean isEmpty(){
			return waiting == null || waiting.isEmpty();
		}
	}

}


















