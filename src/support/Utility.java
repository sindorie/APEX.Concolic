package support;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utility {
	
	/**
	 * Permutation over the input which is a 2D array. The size of the list elements can vary. 
	 * Only address checking for the excluded list.
	 * 
	 * @param input
	 * @param excludedForEachColumn
	 * @return
	 */
	public static <E> List<List<E>> permutate(List<List<E>> input){
		return permuatationHelper(input, 0);
	}
	
	private static <E> List<List<E>> permuatationHelper(List<List<E>> input, int index){
		if(index >= input.size() ) return null;
		List<E> list = input.get(index);
		if(list == null){
			return permuatationHelper(input,index+1);
		}else{
			List<List<E>> result = new ArrayList<List<E>>();
			List<List<E>> partial = permuatationHelper(input,index+1);
			if(partial == null || partial.isEmpty()){
				for(E element : list){
					if(element == null){ continue; }
					List<E> local = new ArrayList<E>();
					local.add(element);
					result.add(local);
				}
				return result;
			}else{
				for(E element : list){
					if(element == null){ continue; }
					for(List<E> post:partial){
						List<E> local = new ArrayList<E>();
						local.add(element);
						local.addAll(post);
						result.add(local);
					}
				}
				if(result.isEmpty()){
					return partial;
				}
				return result;
			}
		}
	}
	
	public static boolean writeToDisk(Object object, String filePath){
		try{
			FileOutputStream fout = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fout);   
			oos.writeObject(object);
			oos.close();
			fout.close();
		   }catch(Exception ex){
			   ex.printStackTrace();
			   return false;
		   }
		return true;
	}
	
	public static Object readFromDisk(String filePath){
		try {
			FileInputStream fin = new FileInputStream(filePath);
			ObjectInputStream oin = new ObjectInputStream(fin);
			Object o = oin.readObject();
			oin.close();
			fin.close();
			return o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String join(String joiner,List<String> collection){
		if(collection != null && collection.size() > 0){
			StringBuilder sb = new StringBuilder();
			sb.append(collection.get(0));
			for(int i = 1; i< collection.size() ; i++){
				sb.append(joiner).append(collection.get(i));
			}
			return sb.toString();
		}
		return "";
	}
}
