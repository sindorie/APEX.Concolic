package apex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import support.Utility;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
import components.EventSummaryPair;
import components.ExpressionTranfomator;

public class Statistic {

	public void check(){
		Common.model.showGUI(true);
		
		Common.println("Total edges: "+Common.model.getAllEdges().size());
		if(Common.summaryManager.getAllConcreteSegmentalSummary().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Concrete segmental PathSummary"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Concrete segmental PathSummary:"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllConcreteSegmentalSummary()){
				Common.println(esPair);
				if(esPair.getPathSummary() != null){
					for(String line : esPair.getPathSummary().getSourceCodeLog()){
						Common.println(line);
					}
				}
			}
		}
		if(Common.summaryManager.getAllSymbolicSegmentalSummary().isEmpty()){
			Common.println(Utility.format_HyphenWrapper("No Symbolic segmental PathSummary"));
		}else{
			Common.println(Utility.format_HyphenWrapper("Symbolic segmental PathSummary:"));
			for(EventSummaryPair esPair : Common.summaryManager.getAllSymbolicSegmentalSummary()){
				Common.println(esPair);
				if(esPair.getPathSummary() != null){
					for(String line : esPair.getPathSummary().getSourceCodeLog()){
						Common.println(line);
						
					}
				}
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
				if(esPair.getPathSummary() != null){
					for(String line : esPair.getPathSummary().getSourceCodeLog()){
						Common.println(line);
					}
				}
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
		Set<String> lines = new HashSet<>();
		for(EventSummaryPair esPair : Common.model.getAllEdges()){
			if(esPair.getPathSummary() != null){
				for(String concrete : esPair.getPathSummary().getSourceCodeLog()){
					int index = concrete.lastIndexOf(",");
					if(index > 0 ){
						concrete = concrete.substring(0, index).trim();
					}
					lines.add(concrete);
				}
			}
		}
		Common.println(Utility.format_HyphenWrapper("Concrete Hit "+lines.size()));
		for(String line : lines){ Common.println(line); }

		Common.println(Utility.format_HyphenWrapper("Reinstall counts "+Common.model.getAllSlices().size()));
		
		if(Common.targets != null && Common.targets.isEmpty() == false){
			Common.println(Utility.format_HyphenWrapper("Target Reachability: "));
			int size = 0;
			for(String target : Common.targets){
				List<List<EventSummaryPair>> listOfSequence = Common.foundSequences.get(target);
				if(listOfSequence != null && listOfSequence.isEmpty() == false){
					Common.println("Target succeed: " + target);
					Common.println(listOfSequence.get(0));
					size += 1;
				}else{ Common.println("Target failed: "+ target); }
			}
			Common.println("Total of "+size+" reached.");
		}
		
	}
	
}
