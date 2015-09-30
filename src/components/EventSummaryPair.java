package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.graph.DefaultEdge;

import apex.symbolic.Expression;
 

public class EventSummaryPair extends DefaultEdge implements Serializable{ 
	private static int gIndex = 0;
	private int index = gIndex++;
	
	private Event event;
	private List<WrappedSummary> summaryList;
//	private UISymbolicPair source, dest;
	private boolean isExecuted = false;
	private boolean canBeIgnored = false; 
	private int tries = 0;
	private List<String> methodRoots;
	private Map<Expression,Expression> combinedSym;
	private List<Expression> combinedCons;
	
	public final List<String> targetLines = new ArrayList<String>();
	
	private List<String> concreteExecutionLog;
	
	
	public final static String[] ColumnIdentifier = {
		"Event","Amount","Tries","is Conrete", "Method Roots","Combined Constraints", "Combined Symbolics"
	};
	
	public String[] toStringArray(){
		List<String> result = new ArrayList<String>();
		result.add(event.toFormatedString());
		result.add(summaryList==null?"0":summaryList.size()+"");
		result.add(tries+"");
		result.add(isExecuted+"");
		if(this.methodRoots != null && !this.methodRoots.isEmpty()){
			StringBuilder sb = new StringBuilder();
			for(String s : this.methodRoots){
				sb.append(s+"\n");
			}
			result.add(sb.toString());
		}else{
			result.add("");
		}
		if(summaryList!= null && !summaryList.isEmpty()){
			//Combined Constraints
			StringBuilder combConSb = new StringBuilder();
			for(Expression expre : this.getCombinedConstraint()){
				combConSb.append(expre.toYicesStatement()+"\n");
			}
			result.add(combConSb.toString());
			
			//Combined Symbolics
			StringBuilder combSymSb = new StringBuilder();
			for(Entry<Expression, Expression> entry : this.getCombinedSymbolic().entrySet()){
				combSymSb.append(entry.getKey().toYicesStatement()+" = "+entry.getValue().toYicesStatement()+"\n");
			}
			result.add(combSymSb.toString());
		}else{
			result.add("");
			result.add("");
		}
		return result.toArray(new String[0]);
	}
	
	public void increaseTryCount(){
		tries += 1;
	}
	public int getTryCount(){
		return tries;
	}
	
	public EventSummaryPair(Event event, List<WrappedSummary> summaries, List<String> methodRoots){
		this.event = event ; 
		if(summaries != null){
			this.summaryList = new ArrayList<WrappedSummary>(summaries);
			this.methodRoots = methodRoots;
		}
	}
	
	public boolean hasExactTheSameExecutionLog(List<WrappedSummary> inputList){
		if(this.summaryList == null){
			if(inputList == null) return true;
			else{ return false;}
		}else{
			if(inputList == null) return false;
			else{
				if(this.summaryList.size() != inputList.size()) return false;
				
				for(int i =0;i<inputList.size(); i++){
					WrappedSummary sum1 = this.summaryList.get(i);
					WrappedSummary sum2 = inputList.get(i);
					if(sum1 == null && sum2 == null) continue;
					if(sum1 != null){
						if(!sum1.equals(sum2)) return false;
					}
				}
				return true;
			}
		}
	}

	
	@Override
	public String toString(){
		return (event == null? "null" : event.toString())+" #"+index;// +" with "+(majorBranch < 0? "null" : summaryList.get(majorBranch));
	}
	
	@Override
	/**
	 * Compare the event and summarylist
	 */
	public boolean equals(Object o){
		if(o instanceof EventSummaryPair){
			EventSummaryPair other = (EventSummaryPair)o; 
			if(!other.event.equals(this.event)) return false;
//			if(this.summaryList == null){
//				if(other.summaryList == null){
//					return true;
//				}else{
//					return false;
//				}
//			}else{
//				if(other.summaryList == null){
//					return false;
//				}else{
//					return other.summaryList.equals(this.summaryList);
//				}
//			}
			
			
			if(other.summaryList == null ^ this.summaryList == null) return false;
			if(other.summaryList != null && !other.summaryList.equals(this.summaryList))  return false;
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		if(this.summaryList == null || summaryList.isEmpty()) return 0;
		//find the last no null summary
		for(int i = summaryList.size() - 1; i>=0 ; i--){
			WrappedSummary sum = summaryList.get(i);
			if(sum != null && !sum.executionLog.isEmpty()){
				return sum.executionLog.get(sum.executionLog.size()-1).hashCode();
			}
		}
		return 0;
	}
	
	public Map<Expression,Expression> getCombinedSymbolic(){
		if(combinedSym == null){
			Map<Expression,Expression> result = new HashMap<Expression,Expression>();
			if(summaryList != null){
				for(WrappedSummary sum : summaryList){
					if(sum == null) continue;
					result.putAll(sum.symbolicStates);
				}
			}
			combinedSym = result;
		}
		return combinedSym;
	}
	
	public boolean containLine(String line){
		if(this.getSummaryList() == null) return false;
		for(WrappedSummary sum : this.getSummaryList()){
			if(sum ==null || sum.executionLog == null) continue;
			if(sum.executionLog.contains(line)) return true;
		}
		return false;
	}
	
	public List<Expression> getCombinedConstraint(){
		if(combinedCons == null){
			List<Expression> result = new ArrayList<Expression>();
			if(summaryList != null){
				for(WrappedSummary sum : summaryList){
					if(sum == null) continue;
					for(Expression con : sum.constraints){
						if(!result.contains(con)){
							result.add(con);
						}
					}
				}
			}
			combinedCons = result;
		}
		return combinedCons;
	}	
	
	public String getIdentityString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.event.toString()).append("_");
		sb.append(this.event.getSource().toString()).append("_");
		List<Expression> con = this.getCombinedConstraint();
		if(con != null){
			for(Expression expre : con){
				sb.append(expre.toYicesStatement()+";");
			}
		}
		return sb.toString();
	}
	
	public void setConcreateExecuted(){
		this.isExecuted = true;
	}
	public boolean isConcreateExecuted(){
		return this.isExecuted;
	}
	
	public List<String> getMethodRoots() {
		return methodRoots;
	}
	public Event getEvent() {
		return event;
	}
	public List<WrappedSummary> getSummaryList() {
		return summaryList;
	}
	
	public boolean isIgnored(){
		return this.canBeIgnored;
	}
	
	public void setIgnored(){
		this.canBeIgnored = true;
	}
	

	public String toFormatedString(){
		StringBuilder sb = new StringBuilder();
		sb.append("ES index: "+this.index+"\n");
		
		if(event == null){ sb.append("Event: null\n");
		}else{
			sb.append("Event: \n");
			String detail = event.toFormatedString().replace("\n", "\n\t").trim();
			sb.append(detail).append("\n");
		}
		
		
		
		if(summaryList==null){
			sb.append("Summary list is empty\n");
		}else{
			int index = 1;
			for(WrappedSummary sum : summaryList){
				if(sum == null) continue;
				sb.append("#"+index+" Summary:\n");
				String detail = sum.toFormatedString().replace("\n", "\n\t").trim();
				sb.append(detail).append("\n");
				index += 1;
			}
		}
		sb.append("Is Concrete: "+this.isExecuted).append("\n");
		sb.append("Tries: "+this.tries).append("\n");
		sb.append("Method Roots: "+methodRoots).append("\n");
		
		combinedSym = this.getCombinedSymbolic();
		combinedCons = this.getCombinedConstraint();
		
		sb.append("Combined Symbolics: \n");
		for(Entry<Expression,Expression> entry : combinedSym.entrySet()){
			sb.append("\t");
			sb.append(entry.getKey().toYicesStatement()).append(" = ");
			sb.append(entry.getValue().toYicesStatement()).append("\n");
		}
		sb.append("Combined Constraints:\n");
		for(Expression expre : combinedCons){
			sb.append("\t").append(expre.toYicesStatement()).append("\n");
		}
		
		sb.append("String identity:\n");
		sb.append(this.getIdentityString()+"\n");
		return sb.toString();
	}
	public int getIndex(){
		return this.index;
	}
	
	public void setConcreteExecutionLog(List<String> log){
		this.concreteExecutionLog = log;
	}
	public List<String> getConcreteExecutionLog(){
		return this.concreteExecutionLog;
	}
}