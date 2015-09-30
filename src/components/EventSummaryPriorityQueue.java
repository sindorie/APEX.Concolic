package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import support.Logger;

public class EventSummaryPriorityQueue implements Serializable{

	public Map<Integer, List<EventSummaryPair>> map = new HashMap<Integer,List<EventSummaryPair>>();
	public PriorityQueue<Integer> queue = new PriorityQueue<Integer>();
	
	public EventSummaryPriorityQueue(){}
	
	/**
	 * Shallow Copy
	 * @param other
	 */
	public EventSummaryPriorityQueue(EventSummaryPriorityQueue other){
		this.map = other.map;
		this.queue = other.queue;
	}
	
	public boolean isEmpty(){
		return queue.isEmpty();
	}
	
	public EventSummaryPair peek(){
		if(queue.isEmpty()) return null;
		Integer level = queue.peek();
		List<EventSummaryPair> sumList = map.get(level);
		if(sumList == null || sumList.isEmpty()){
			Logger.trace("Something wrong");
			return null;
		}else{
			return sumList.get(0);
		}
	}
	
	//Assume the uniqueness
	public boolean add(EventSummaryPair esPair){
		Integer level = -new Integer(ESPriority.calculate(esPair));
		if(map.containsKey(level)){
			List<EventSummaryPair> sumList = map.get(level);
			sumList.add(esPair);
		}else{
			List<EventSummaryPair> sumList = new ArrayList<EventSummaryPair>();
			map.put(level, sumList);
			sumList.add(esPair);
			queue.add(level);
		}
		return true;
	}
	
	public EventSummaryPair poll(){
		if(queue.isEmpty()) return null;		
		Integer level = queue.peek();
		List<EventSummaryPair> sumList = map.get(level);
		EventSummaryPair esPair = sumList.remove(0);
		if(sumList.isEmpty()){
			queue.poll();
			map.remove(level);
		}
		return esPair;
	}
	
	public void remove(EventSummaryPair esPair){
		Integer level = -new Integer(ESPriority.calculate(esPair));
		if(map.containsKey(level)){
			List<EventSummaryPair> sumList = map.get(level);
			sumList.remove(esPair);
			
			if(sumList.isEmpty()){
				map.remove(level);
				queue.remove(level);
			}
		}
	}
	
	public void removeAll(Collection<EventSummaryPair> toRemove){
		for(EventSummaryPair esPair : toRemove){
			this.remove(esPair);
		}
	}
	public void addAll(Collection<EventSummaryPair> toRemove){
		for(EventSummaryPair esPair : toRemove){
			this.add(esPair);
		}
	}
	public Iterator<EventSummaryPair> iterator(){
		List<EventSummaryPair> list = new ArrayList<EventSummaryPair>();
		for(List<EventSummaryPair> esList : map.values()){
			list.addAll(esList);
		}
		return list.iterator();
	}
}
