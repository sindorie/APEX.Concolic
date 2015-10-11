package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import apex.Common;

public class EventSummaryPriorityQueue implements Serializable{

	private Map<Integer, List<EventSummaryPair>> map = new HashMap<Integer,List<EventSummaryPair>>();
	private PriorityQueue<Integer> inner_queue = new PriorityQueue<Integer>();
	
	public EventSummaryPriorityQueue(){}
	
	/**
	 * Shallow Copy
	 * @param other
	 */
	public EventSummaryPriorityQueue(EventSummaryPriorityQueue other){
		this.map = other.map;
		this.inner_queue = other.inner_queue;
	}
	
	public boolean isEmpty(){
		return inner_queue.isEmpty();
	}
	
	public EventSummaryPair peek(){
		if(inner_queue.isEmpty()) return null;
		Integer level = inner_queue.peek();
		List<EventSummaryPair> sumList = map.get(level);
		if(sumList == null || sumList.isEmpty()){
			return null;
		}else{
			return sumList.get(0);
		}
	}
	
	public EventSummaryPair poll(){
		if(inner_queue.isEmpty()) return null;
		Integer level = inner_queue.peek();
		List<EventSummaryPair> sumList = map.get(level);
		EventSummaryPair esPair = sumList.remove(0);
		if(sumList.isEmpty()){
			inner_queue.poll();
			map.remove(level);
		}
		return esPair;
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
			inner_queue.add(level);
		}
		return true;
	}
	
	public List<EventSummaryPair> getRemaining(){
		List<EventSummaryPair> result = new ArrayList<EventSummaryPair>();
		for(Integer i : inner_queue){
			List<EventSummaryPair> data = map.get(i);
			if(data != null){
				result.addAll(data);
			}
		}
		return result;
	}
	
	public void addAll(Collection<EventSummaryPair> toRemove){
		for(EventSummaryPair esPair : toRemove){
			this.add(esPair);
		}
	}
}
