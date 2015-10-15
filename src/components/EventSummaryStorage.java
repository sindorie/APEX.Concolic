package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import apex.symbolic.PathSummary;

public class EventSummaryStorage implements Serializable{

	private static final long serialVersionUID = 1L;
	private CubicHashMap<Event,String,Integer, List<EventSummaryPair>> storage = new CubicHashMap<>();
	
	/**
	 * Given the event, starting method signature and expanded execution logs,
	 * locate an event summary pair in the storage.
	 * @param event
	 * @param signature
	 * @param expandedLog
	 * @return the located event summary pair or null if failure to find
	 */
	public EventSummaryPair find(Event event, String signature, List<String> expandedLog){
		String methodSig = (signature==null?"":signature);
		Integer sz = (expandedLog == null ? 0 : expandedLog.size());
		List<EventSummaryPair> list = storage.get(event, methodSig, sz);
		if(list == null || list.isEmpty()) return null;
		if(expandedLog == null || expandedLog.isEmpty()){
			for(EventSummaryPair es : list){ if(es.getPathSummary() == null){ return es; } }
		}else{
			for(EventSummaryPair es : list){
				PathSummary sum = es.getPathSummary();
				if(sum == null) continue; //should not happen logically
				if(expandedLog.equals(sum.getExecutionLog())){ return es; }
			}
		}
		return null;
	}
	
	/**
	 * Try to store the event-path pair into the storage 
	 * Check the existence. If so ignore.
	 * @param esPair
	 * @param checkExistence
	 * @return true if successfully put the input into storage
	 */
	public boolean store(EventSummaryPair esPair, boolean checkExistence){
		PathSummary path = esPair.getPathSummary();
		Event event = esPair.getEvent();
		String methodSig =  ((path!= null) ? path.getMethodSignature() : ""); 
		List<String> logs = ((path== null) ? null : path.getExecutionLog());
		int size = (logs == null) ? 0 : logs.size();
		
		List<EventSummaryPair> list = storage.get(event, methodSig, size);
		if(list == null){
			list = new ArrayList<>();
			storage.put(event, methodSig, size, list);
		}
		if(checkExistence){ for(EventSummaryPair toCheck : list){ if(toCheck.equals(esPair)){return false;}  }}
		list.add(esPair); 
		return true;
	}
	
	/**
	 * Remove an event-path pair given event, signature and execution log
	 * @param event
	 * @param signature
	 * @param expandedLog
	 * @return the removed event-path pair if exists
	 */
	public EventSummaryPair remove(Event event, String signature, List<String> expandedLog){
		int size = (expandedLog == null? 0: expandedLog.size());
		List<EventSummaryPair> list = storage.get(event, signature, size);
		if(list == null || list.isEmpty()) return null;
		if(expandedLog == null || expandedLog.isEmpty()){
			if(list.size() > 0 ){ return list.remove(0); /*Logically can only have one element at most*/
			}else{ return null; } 
		}else{
			EventSummaryPair result = null;
			int i=0;
			for(;i<list.size() ; i++){
				EventSummaryPair es = list.get(i);
				PathSummary sum = es.getPathSummary();
				if(sum == null) continue;
				if( sum.getExecutionLog().size() == expandedLog.size() &&
						sum.getExecutionLog().equals(expandedLog)){
					result = es; break;
				}
			}
			if(result != null) list.remove(i);	
			return result;
		}
	}
	
	/**
	 * get all event-path pair
	 * @return a list of all data
	 */
	public List<EventSummaryPair> getAlldata(){
		List<EventSummaryPair> result = new ArrayList<>();
		for(List<EventSummaryPair> list : storage.getAllData()){ result.addAll(list); }
		return result;
	}
}
