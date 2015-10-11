package components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;

import apex.Common;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
import support.TreeUtility;
import support.TreeUtility.Searcher;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.solver.CVCSolver;
import components.solver.SMTSolution;

public class AnchorSolver {

	boolean debug = false;
	SMTSolution solution = new SMTSolution();
	final static int INSAT = 0, SAT = 1, COMPLETE = 2;
	
	private int maxDepth = 10, maxWidth = 10;
//	List<List<EventSummaryPair>> sequenceList = new ArrayList<List<EventSummaryPair>>();
	List<DefaultMutableTreeNode> leaves;
	
	public boolean hasValidConstraints(PathSummary summary){
		if(summary == null) return false;
		List<Expression> constraints = summary.getPathConditions();
		List<Expression> symbolices = summary.getSymbolicStates();
		List<Expression>[] transformed = ExpressionTranfomator.transform(constraints, symbolices);
		List<Expression> transformedCon = transformed[ExpressionTranfomator.CONSTRAINT_INDEX];
		if(transformedCon == null) return false;
		List<Expression> result = new ArrayList<>();
		for(Expression expre : transformedCon){
			if(solution.getVarSet(expre).size() > 0) result.add(expre);
		}
		return !result.isEmpty();
	}
	
	
//	Map<String, Map<Integer, Map<List<String>, >>>
	

	
	
	
	
	/**
	 * Seq1: 		 e(i), e(i-1), ... e(target)
	 * Seq2: e(i+1), e(i), e(i-1), ... e(target)
	 * 
	 * Given the previous proof that seq1 satisfies, 
	 * 
	 * check e(i+1) if
	 * 1. related with the cumulative constraints
	 * 2. e(i+1) leads to the satisfaction of seq2
	 * 3. Not if: 
	 *  a. constraint( e(i+1),e(i) ) is the subset of constraint( e(i) )
	 *  b. Symbolic( e(i+1) ) is the same to Symbolic( e(i+1), e(i) )
	 *  
	 * @param esPair -- the event summary pair which is currently being investigated
	 * @param currentPack -- the current context which contains the cumulative path
	 * @return a new context or null if not pass the check
	 */
	SolvingPack checkQualification(EventSummaryPair esPair, SolvingPack currentPack){
		PathSummary newSum = esPair.getPathSummary();
		if(newSum == null) return null;
			
		PathSummary current_cumulative = currentPack.cumulativePath;
		PathSummary currentPath = currentPack.esPair.getPathSummary();
		
		//check related
		Set<Expression> cum_varSet = getConstraintVarSet(current_cumulative);
		Set<Expression> sum_varSet = getSymbolicVarSet(newSum);
	
		boolean related = false;
		for(Expression symVar : sum_varSet){
			if(cum_varSet.contains(symVar)){
				related = true; break;
			}
		}
		
		if(related){
			PathSummary concated = newSum.concat(current_cumulative); //the candidate for new cumulative path
			if(concated == null){ return null; } //should not happen
			int code = solveHelper(concated, esPair);
			switch(code){
			case INSAT:{ Common.TRACE("not staified");} break; //do nothing
			case SAT:{  
				Common.TRACE("Satisified");
				PathSummary sumA = newSum.concat(currentPath); //e(i+1), e(i) 
				PathSummary sumB = currentPath;	//e(i)
				
				if(isASubSetToB_constraint(sumA, sumB) && isASubSetToB_symbolic(sumA, sumB)){
					return null; //does not qualify
				}
				SolvingPack result = new SolvingPack();
				result.cumulativePath = concated;
				result.esPair = esPair;
				return result;
			}
			case COMPLETE:{
				Common.TRACE("COMPLETE");
				SolvingPack result = new SolvingPack();
				result.cumulativePath = concated;
				result.esPair = esPair;
				result.complete = true;
				return result;
			}
			}
		}
		return null;//does not satisfy or qualify certain condition
	}
	
	/**
	 * Only find the expression (variable) in Constraints
	 * @param sum -- the path summary needs to be checked
	 * @return a set of expressions which represent the variables
	 */
	Set<Expression> getConstraintVarSet(PathSummary sum){
		TransformedPack result = getTransformedInformation(sum);
		return result.variable_Constraint;
	}

	/**
	 * find all the 
	 * @param sum -- the path summary needs to be checked
	 * @return a set of expressions which represent the variables
	 */
	Set<Expression> getSymbolicVarSet(PathSummary sum){
		TransformedPack result = getTransformedInformation(sum);
		return result.variable_Symbolic;
	}
	
	/**
	 * Check if the constraint in path summary A is a subset
	 * to the constraint in path summary B
	 * 
	 * @param sumA
	 * @param sumB
	 * @return
	 */
	boolean isASubSetToB_constraint(PathSummary sumA, PathSummary sumB){
		TransformedPack packA = getTransformedInformation(sumA);
		TransformedPack packB = getTransformedInformation(sumB);
		List<Expression> toCheck = new ArrayList<Expression>();
		toCheck.addAll(packA.constraints);
		
		Expression root = new Expression("or");
		for(Expression expre : packB.constraints){
			Expression inversed = new Expression("not");
			inversed.add(expre);
			root.add(inversed);
		}
		toCheck.add(root);	
		
		List<String> statements = solution.buildScript(toCheck);
		try {
			List<String>[] feedBack = CVCSolver.solve(statements);
			int resultCode = solution.checkSatisfactionDetail(feedBack);
			if(resultCode == SMTSolution.UNSAT) return true;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	/**
	 * check if the path summary A has the same symbolic state as 
	 * path summary B
	 * @param sumA
	 * @param sumB
	 * @return
	 */
	boolean isASubSetToB_symbolic(PathSummary sumA, PathSummary sumB){
		TransformedPack packA = getTransformedInformation(sumA);
		TransformedPack packB = getTransformedInformation(sumB);
		Map<Expression, Expression> symA = packA.symbolics; 
		Map<Expression, Expression> symB = packB.symbolics;
		
		//check variable if A a subset to B
		Set<Expression> symA_varSet = symA.keySet();
		Set<Expression> symB_varSet = symB.keySet();
		
		for(Expression var : symA_varSet){
			if(!symB_varSet.contains(var)){
				return false; //no a subSet
			}
		}
		
		//check the assignment -- only check assignment is the same
		//may want to use solver to check TODO
		for(Expression var : symA_varSet){
			Expression assignA = symA.get(var);
			Expression assignB = symB.get(var);
			if(!assignA.equals(assignB)) return false;
			
		}
		
//		//use solver to check -- seem to be unncessary at this point
//		List<Expression> toCheck = new ArrayList<>();
//		Expression root = new Expression("or");
//		for(Expression var : symA_varSet){
//			Expression partA = symA.get(var);
//			Expression partB = symB.get(var);
//			
//			Expression top = new Expression("not");
//			Expression equal = new Expression("=");
//			top.add(equal);
//			equal.add(partA);
//			equal.add(partB);
//			
//			
//		}
		
		
		
		return true;
	}
	
	
	/**
	 * find the constraint of the given path summary in the transformed
	 * format.
	 * @param sum
	 * @return
	 */
	Set<Expression> getConstraints(PathSummary sum){
		TransformedPack result = getTransformedInformation(sum);
		return result.constraints;
	}
	
	/**
	 * find the symbolics state of the given path summary in the 
	 * transformed format.
	 * @param sum
	 * @return
	 */
	Map<Expression,Expression> getSymbolics(PathSummary sum){
		TransformedPack result = getTransformedInformation(sum);
		return result.symbolics;
	}
		
	
	Map<String, Map<Integer, Map<List<String>, TransformedPack>>> transformed_storage = new HashMap<>();
	/**
	 * find the processed information of the given path summary.
	 * Processed information means the constraint and symbolic states in transformed format.
	 * @param sum
	 * @return
	 */
	private TransformedPack getTransformedInformation(PathSummary sum){
		String sig = sum.getMethodSignature();
		Map<Integer, Map<List<String>, TransformedPack>> primary = transformed_storage.get(sig);
		if(primary == null){
			primary = new HashMap<Integer, Map<List<String>, TransformedPack>>();
			transformed_storage.put(sig, primary);
		}
		
		int size = sum.getExecutionLog().size();
		Map<List<String>, TransformedPack> secondary = primary.get(size);
		if(secondary == null){
			secondary = new HashMap<List<String>, TransformedPack>();
			primary.put(size, secondary);
		}
		
		List<String> exeLog = sum.getExecutionLog();
		TransformedPack result = secondary.get(exeLog);
		if(result == null){
			List<Expression>[] transformed = ExpressionTranfomator.transform(sum.getPathConditions(), sum.getSymbolicStates());
			List<Expression> constraints = transformed[ExpressionTranfomator.CONSTRAINT_INDEX];
			List<Expression> symbolicStates = transformed[ExpressionTranfomator.SYMBOLIC_INDEX];
			
			result = new TransformedPack();
			Set<Expression> result_constraint = new HashSet<>();
			Map<Expression,Expression> result_symbolics = new HashMap<>();
			Set<Expression> result_symbolic_varSet = new HashSet<Expression>();
			Set<Expression> result_constraint_varSet;
			
			result_constraint.addAll(constraints);
			solution.trim(result_constraint); //remove un-needed constraint like 1 < 2
			for(Expression sym_expre : symbolicStates ){
				Expression leftVar = (Expression) sym_expre.getChildAt(0);
				if(!solution.isVariable(leftVar)){
					Common.TRACE("Not Variable: "+leftVar.toYicesStatement());
					continue;
				}
				result_symbolic_varSet.add(leftVar);
				Expression assignment = (Expression) sym_expre.getChildAt(1);
				Expression previousVal = result_symbolics.put(leftVar, assignment);
				if(previousVal != null && !previousVal.equals(assignment)) {
					Common.TRACE("Multi-assignment");
					Common.TRACE("Var:"+leftVar.toYicesStatement());
					Common.TRACE("Pre:"+previousVal.toYicesStatement());
					Common.TRACE("New:"+assignment.toYicesStatement());
				}
			}
			result_constraint_varSet = solution.getVarSet(result_constraint);
			
			result.constraints = result_constraint;
			result.symbolics = result_symbolics;
			result.variable_Constraint = result_constraint_varSet;
			result.variable_Symbolic = result_symbolic_varSet;
			
			secondary.put(exeLog, result);
		}
		return result;	
	}
	
	/**
	 * A wrapper class which is in association with a path summary 'p'.
	 * The content includes the constraints and symbolic states of'p' in
	 * the transformed format.
	 * @author zhenxu
	 */
	class TransformedPack{
		Set<Expression> constraints;
		Map<Expression,Expression> symbolics;

		Set<Expression> variable_Constraint; //variables in Constraints
		Set<Expression> variable_Symbolic; //variables in symbolics
	}
	
	
	/**
	 * First create a sequence of events which 
	 * @param esPair
	 */
	public List<List<EventSummaryPair>> solve(EventSummaryPair esPair){
		Common.TRACE();
		SolvingPack pk = new SolvingPack();
		pk.esPair = esPair;
		pk.cumulativePath = esPair.getPathSummary();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(pk);
		leaves = new ArrayList<DefaultMutableTreeNode>();
		leaves.add(root);
		List<EventSummaryPair> allEsList = Common.model.getAllEdges();
		
		for(int i = 0 ; i < maxDepth ; i++){ 
			Common.TRACE("Round: "+i);
			if(leaves == null || leaves.isEmpty()) break;
			
			List<DefaultMutableTreeNode> newLeaves = new ArrayList<DefaultMutableTreeNode>();
			for(DefaultMutableTreeNode leaf : leaves){
				pk = (SolvingPack) leaf.getUserObject();
				
				List<SolvingPack> qualified = new ArrayList<SolvingPack>();
				//find related
				for(EventSummaryPair edge : allEsList){
					SolvingPack solvingContext = checkQualification(edge, pk);
					if(solvingContext != null){ // it is valid 
						qualified.add(solvingContext);
					}
				}

				//local prioritize
				//TODO	
//				List<Double> pirorityLabel = new ArrayList<>();
				for(SolvingPack pack : qualified){	
					leaf.add(new DefaultMutableTreeNode(pack));
				}
				
				
				//local cap
			}	
			
			leaves = newLeaves;
			if(leaves.size() > maxWidth){
				leaves = leaves.subList(0, maxWidth);
			}
		}
		
		final List<List<EventSummaryPair>> anchorSeqs = new ArrayList<List<EventSummaryPair>>();
		TreeUtility.depthFristSearch(root, new Searcher(){
			@Override
			public int check(TreeNode node) {
				if(node.isLeaf()){
					DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) node;
					SolvingPack p = (SolvingPack) leaf.getUserObject();
					if(p.complete){
						Object[] arr = leaf.getUserObjectPath();
						List<EventSummaryPair> seq = new ArrayList<EventSummaryPair>();
						for(int i = arr.length - 1; i>=0;i--){
							SolvingPack pack = (SolvingPack)arr[i];
							seq.add(pack.esPair);
						}
						anchorSeqs.add(seq);
					}
				}
				return Searcher.NORMAL;
			}
		});
		
		Common.TRACE("Anchor Sequence size: "+anchorSeqs.size());
		Collections.sort(anchorSeqs, new ListSZComparator());
		
		List<List<EventSummaryPair>> expandedList = new ArrayList<List<EventSummaryPair>>();
		Major: for(List<EventSummaryPair> anchors : anchorSeqs){
			EventSummaryPair first = anchors.get(0);
			List<EventSummaryPair> expanded = new ArrayList<EventSummaryPair>();
			Set<Expression> varSet = new HashSet<Expression>();
			if(first.getSource() != GraphicalLayout.Launcher){
				List<EventSummaryPair> seq = findLocalConnecter(GraphicalLayout.Launcher,
								first.getSource(), varSet);
				expanded.addAll(seq);
			}
			expanded.add(first);
			first.getPathSummary().getSymbolicStates();
			varSet.addAll(findSymbolicVar(first.getPathSummary()));
			
			for(int i = 0;i<anchors.size()-1;i++){
				EventSummaryPair esAnchor_src = anchors.get(i);
				EventSummaryPair esAnchor_des = anchors.get(i+1);
				if(esAnchor_src.getTarget() != esAnchor_des.getSource()){
					List<EventSummaryPair> seq = findLocalConnecter(GraphicalLayout.Launcher,
							first.getSource(), varSet);
					if(seq == null) continue Major;
					expanded.addAll(seq);
					varSet.addAll(findSymbolicVar(esAnchor_des.getPathSummary()));
				}
				expanded.add(esAnchor_des);
			}
			expandedList.add(expanded);
		}
		
		Collections.sort(expandedList, new ListSZComparator());
		return expandedList;
	}
	
	private Set<Expression> findSymbolicVar(PathSummary sum) {
		List<Expression> list = sum.getSymbolicStates();
		Set<Expression> set = new HashSet<Expression>();
		for (Expression sym : list) {
			set.add((Expression) sym.getChildAt(0));
		}
		return set;
	}

	
	int solveHelper(PathSummary concated, EventSummaryPair esPair){
		boolean isEntry = esPair.getEvent()!=null? esPair.getEvent().getEventType() == EventFactory.iLAUNCH : false;
			
		Set<Expression> constraints = getConstraints(concated);
//		Set<Expression> symbolics = getSymbolics(concated);
		
		List<String> script = solution.buildScript(constraints);
		List<String>[] solvingResult = null;
		try {
			solvingResult = CVCSolver.solve(script);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		if(solvingResult == null) return INSAT;
		if(solution.checkSatisfaction(solvingResult)){
			Set<Expression> varSet = solution.getVarSet(constraints);
			if(isEntry){ //remove variable in the 
				
			}
			
			
			if(solution.getVarSet(constraints).size() == 0 ){
				return COMPLETE;
			}else{ return SAT; }
		}else{ return INSAT; }
	}
	
	private List<EventSummaryPair> findLocalConnecter(GraphicalLayout source, GraphicalLayout dest, 
			final Set<Expression> exculded){
		DirectedMaskSubgraph<GraphicalLayout, EventSummaryPair> subgraph =
			new DirectedMaskSubgraph<GraphicalLayout, EventSummaryPair>(Common.model.getGraph(),
					new MaskFunctor<GraphicalLayout, EventSummaryPair>(){
						@Override
						public boolean isEdgeMasked(EventSummaryPair edge) {
							PathSummary sum = edge.getPathSummary();
							if(sum.getSymbolicStates() != null){
//								List<Expression> varSet = new ArrayList<Expression>();
								for(Expression var : sum.getSymbolicStates()){
									if(exculded.contains(var)) return true;
								}
							}
							return false;
						}
						@Override
						public boolean isVertexMasked(GraphicalLayout arg0) { return false; }	
			});
		return DijkstraShortestPath.findPathBetween(subgraph, source, dest);
	}
	

	class SolvingPack{
		EventSummaryPair esPair;
		PathSummary cumulativePath;
		boolean complete = false;
	}
}	
