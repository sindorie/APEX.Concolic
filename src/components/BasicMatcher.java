package components;

import java.util.ArrayList;
import java.util.List;

import apex.instrumentor.Blacklist;
import apex.staticFamily.StaticApp;
import apex.symbolic.PathSummary;


public class BasicMatcher{
	StaticApp app;
	public BasicMatcher(StaticApp app){
		this.app = app;
	}
	
	public int matchSummary(List<String> concreteLines, List<PathSummary> psList){
		int index = 0;
		for (PathSummary ps : psList){
			if (compareBPRecords(concreteLines, ps.getExecutionLog()))
				return index;
			index += 1;
		}
		return -1;
	}
	
	private boolean compareBPRecords(List<String> concrete, List<String> symbolic){
//		System.out.println(A);
//		System.out.println(B);
//		boolean result = true;
//		if (A.size() < 1 || B.size() < 1 || A.size() != B.size()){
//			result = false;
//		}else{
//			for (int i = 1, len = A.size(); i < len; i++){
//				if (!A.get(i).equals(B.get(i))){
//					result = false; break;
//				}
//			}
//		}
////		System.out.println("Comparison: "+ result);
//		return result;

		List<String> newSymbolic = new ArrayList<String>();
		for (String s : symbolic) {
			String className = s.split(":")[0];
			if (!Blacklist.classInBlackList(className)){ newSymbolic.add(s); }
		}
		boolean result = true;
		if (newSymbolic.size() < 1 ||  concrete.size() < 1 ||  newSymbolic.size() != concrete.size() ){
			result = false;
		}else {
//			for (int i = 0; i < concrete.size(); i++) {
//				if (!newSymbolic.get(i).equals(concrete.get(i)))
//					return false;
//			}
			return newSymbolic.equals(concrete);
		}
//		System.out.println("Comparison: "+ result);
		return result;
	}
}
