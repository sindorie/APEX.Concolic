package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CubicHashMap <K1,K2,K3,E> implements Serializable{
	private int size = 0;
	private Map<K1, Map<K2, Map<K3, E>>> storage = new HashMap<>();
	
	/**
	 * Given three keys, put the data into the storage
	 * @param key1
	 * @param key2
	 * @param key3
	 * @param data
	 * @return
	 */
	public E put(K1 key1 , K2 key2, K3 key3, E data){
		Map<K2, Map<K3, E>> primary = storage.get(key1);
		if(primary == null){
			primary = new HashMap<K2, Map<K3, E>>();
			storage.put(key1, primary);
		}
		Map<K3, E> secondary = primary.get(key2);
		if(secondary == null){
			secondary = new HashMap<K3, E>();
			primary.put(key2, secondary);
		}
		E internal = secondary.put(key3, data);
		if( internal == null){ size += 1; } 
		return internal;
	}
	
	/**
	 * Remove the element identified by the three keys
	 * @param key1
	 * @param key2
	 * @param key3
	 * @return
	 */
	public E remove(K1 key1 , K2 key2, K3 key3){
		Map<K2, Map<K3, E>> primary = storage.get(key1);
		if(primary == null){ return null; }
		Map<K3, E> secondary = primary.get(key2);
		if(secondary == null){ return null; }
		E internal = secondary.remove(key3);
		if( internal != null){ size -= 1; } 
		return internal;
	}
	
	/**
	 * get the data by the three keys
	 * @param key1
	 * @param key2
	 * @param key3
	 * @return
	 */
	public E get(K1 key1 , K2 key2, K3 key3){
		Map<K2, Map<K3, E>> primary = storage.get(key1);
		if(primary == null){ return null; }
		Map<K3, E> secondary = primary.get(key2);
		if(secondary == null){ return null; }
		return secondary.get(key3);
	}
	
	/**
	 * Check the existence of data given three keys
	 * @param key1
	 * @param key2
	 * @param key3
	 * @return
	 */
	public boolean contains(K1 key1 , K2 key2, K3 key3){
		return this.get(key1, key2, key3) != null;
	}
	
	/**
	 * get the current amount of elements in the storage
	 * @return
	 */
	public int getSize(){ return this.size; }
	
	/**
	 * get all the data in the storage
	 * @return the result will not be empty
	 */
	public List<E> getAllData(){
		List<E> result = new ArrayList<>();
		for(Entry<K1, Map<K2, Map<K3, E>>> entry : storage.entrySet()){
			for(Entry<K2, Map<K3, E>> entry1 : entry.getValue().entrySet()){
				for(Entry<K3, E> entry2 : entry1.getValue().entrySet()){
					E data = entry2.getValue();
					if(data != null) result.add(data);
				}
			}
		}
		return result;
	}
}
