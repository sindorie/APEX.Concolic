package apex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import support.Utility;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
import components.EventSummaryPair;
import components.ExpressionTranfomator;

public class Statistic {

	public static boolean showGUI = false;
	
	int reinstallCount = -1;
	int totalConcreateEdge = 0;
	int totalLine = 0;
	int maxSequenceLength = -1;
	int reachedCount = -1;
	Set<String> concreatelines = new HashSet<String>();
	Map<String,List<EventSummaryPair>> targetToSequence = new HashMap<>();
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Effective Reinstall Count: "+reinstallCount).append(System.getProperty("line.separator"));
		sb.append("Total event-path edge: "+totalConcreateEdge).append(System.getProperty("line.separator"));
		sb.append("Total lines: "+totalLine).append(System.getProperty("line.separator"));
		sb.append("Max sequence size: "+maxSequenceLength).append(System.getProperty("line.separator"));
		sb.append("Target reach count: "+reachedCount).append(System.getProperty("line.separator"));
		sb.append("Concret line hit:").append(System.getProperty("line.separator"));
		for(String line : concreatelines){
			sb.append(line).append(System.getProperty("line.separator"));
		}
		sb.append("Target Hit Record:").append(System.getProperty("line.separator"));
		for(Entry<String,List<EventSummaryPair>> entry : targetToSequence.entrySet()){
			String target = entry.getKey();
			List<EventSummaryPair> seq = entry.getValue();
			if(seq == null){
				sb.append("Fail to reach: "+target).append(System.getProperty("line.separator"));
			}else{
				sb.append("Succeed to reach: "+target).append(System.getProperty("line.separator"));
				sb.append(seq).append(System.getProperty("line.separator"));
			}
		}
		
		return sb.toString();
	}
	
	public void check(){
		if(showGUI) Common.model.showGUI(true);
		
		totalConcreateEdge = Common.model.getAllEdges().size();
		Common.println("Total edges: "+totalConcreateEdge);
		if(Common.summaryManager.getAllConcreteSegmentalSummary().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Concrete segmental PathSummary"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Concrete segmental PathSummary:"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSegmentalSummary()){
				Common.println(esPair);
//				if(esPair.getPathSummary() != null){
//					for(String line : esPair.getPathSummary().getSourceCodeLog()){
//						Common.println(line);
//					}
//				}
			}
		}
		if(Common.summaryManager.getAllSymbolicSegmentalSummary().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Symbolic segmental PathSummary"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Symbolic segmental PathSummary:"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllSymbolicSegmentalSummary()){
				Common.println(esPair);
//				if(esPair.getPathSummary() != null){
//					for(String line : esPair.getPathSummary().getSourceCodeLog()){
//						Common.println(line);
//						
//					}
//				}
			}
		}
		if(Common.summaryManager.getAllConcreteSummary().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Concrete PathSummary"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Concrete PathSummary"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSummary()){
				Common.println(esPair);
			}
		}
		if(Common.summaryManager.getAllInvalidSegmentalSummary().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Invalid Segmental PathSummary"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Invalid Segmental Path Summary"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllInvalidSegmentalSummary()){
				Common.println(esPair);
				PathSummary summary = esPair.getPathSummary();
				List<Expression> constraints = summary.getPathConditions();
				List<Expression> symbolices = summary.getSymbolicStates();
				List<Expression>[] transformed = ExpressionTranfomator.transform(constraints, symbolices);
				List<Expression> transformedCon = transformed[ExpressionTranfomator.CONSTRAINT_INDEX];
				
				if(constraints.isEmpty()){
					Common.println("No Path Summary Constraints");
				}else{
					Common.println("Path Summary Constraints:");
					for(Expression expre : constraints){
						Common.println(expre.toYicesStatement());
					}
				}
				
				if(symbolices.isEmpty()){
					Common.println("No Path Summary Symbolic states");
				}else{
					Common.println("Path Summary Symbolic states:");
					for(Expression expre : symbolices){
						Common.println(expre.toYicesStatement());
					}
				}
				
				if(transformedCon.isEmpty()){
					Common.println("No Transformed Constraints");
				}else{
					Common.println("Transformed Constraints:");
					for(Expression expre : transformedCon){
						Common.println(expre.toYicesStatement());
					}
				}
				Common.println();
			}
		}
		
		if(Common.summaryManager.getRemainingInQueue().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Remaining Event Path Pair in Queue"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Remaining Event Path Pair:"));
			for(EventSummaryPair esPair : Common.summaryManager.getRemainingInQueue()){
				Common.println(esPair);
//				if(esPair.getPathSummary() != null){
//					for(String line : esPair.getPathSummary().getSourceCodeLog()){
//						Common.println(line);
//					}
//				}
			}
		}
		
		if(ExpressionTranfomator.UNIDENTIFIER_LIST.isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Unidentified Expression"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Unidentified Expressions:"));
			for(Expression expre : ExpressionTranfomator.UNIDENTIFIER_LIST){
				Common.println(expre.toYicesStatement());
			}
		}
		
		//check the all the lines
		for(EventSummaryPair esPair : Common.model.getAllEdges()){
			if(esPair.getPathSummary() != null){
				for(String concrete : esPair.getPathSummary().getSourceCodeLog()){
					int index = concrete.lastIndexOf(",");
					if(index > 0 ){
						concrete = concrete.substring(0, index).trim();
					}
					concreatelines.add(concrete);
				}
			}
		}
		totalLine = concreatelines.size();
		Common.println(Utility.format_HyphenWrapper("Concrete Hit "+concreatelines.size()));
		for(String line : concreatelines){ Common.println(line); }

		reinstallCount = Common.model.getAllSlices().size();
		Common.println(Utility.format_HyphenWrapper("Reinstall counts "+ reinstallCount));
		List<List<EventSummaryPair>> seqeuence = Common.model.getAllSlices();
		if(seqeuence != null){
			for(List<EventSummaryPair> list : seqeuence){
				if(list != null && list.size() > maxSequenceLength){
					maxSequenceLength = list.size();
				}
			}
		}
		
		if(Common.targets != null && Common.targets.isEmpty() == false){
			Common.println(Utility.format_HyphenWrapper("Target Reachability: "));
			int size = 0;
			for(String target : Common.targets){
				List<List<EventSummaryPair>> listOfSequence = Common.foundSequences.get(target);
				if(listOfSequence != null && listOfSequence.isEmpty() == false){
					Common.println("Target succeed: " + target);
					Common.println(listOfSequence.get(0));
					targetToSequence.put(target, listOfSequence.get(0));
					size += 1;
				}else{ Common.println("Target failed: "+ target); targetToSequence.put(target, null); }
			}
			reachedCount = size;
			Common.println("Total of "+size+" reached.");
		}
		
	}
	
}
