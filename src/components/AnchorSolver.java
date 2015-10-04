package components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
	
	private int maxDepth = 10;
	List<List<EventSummaryPair>> sequenceList = new ArrayList<List<EventSummaryPair>>();
	List<DefaultMutableTreeNode> leaves;
	/**
	 * First create a sequence of events which 
	 * @param esPair
	 */
	public List<List<EventSummaryPair>> solve(EventSummaryPair esPair){
		Pack pk = new Pack();
		pk.esPair = esPair;
		pk.cumulative = esPair.getPathSummary();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(pk);
		leaves = new ArrayList<DefaultMutableTreeNode>();
		leaves.add(root);
		List<EventSummaryPair> allEsList = Common.model.getAllEdges();
		
		for(int i = 0 ; i < maxDepth ; i++){ 
			if(leaves == null || leaves.isEmpty()) break;
			
			List<DefaultMutableTreeNode> newLeaves = new ArrayList<DefaultMutableTreeNode>();
			for(DefaultMutableTreeNode leaf : leaves){
				pk = (Pack) leaf.getUserObject();
				
				List<EventSummaryPair> related = new ArrayList<EventSummaryPair>();
				//find related
				for(EventSummaryPair e : allEsList){
					for(Expression sym : e.getPathSummary().getSymbolicStates()){
						for(Expression con : pk.cumulative.getPathConditions()){
							if(con.contains(sym)){
								//related
								related.add(e);
								break;
							}
						}
					}
				}
				
				for(EventSummaryPair es : related){
					PathSummary concated = es.getPathSummary().concat(pk.cumulative);
					int code = solveHelper(concated);
					switch(code){
					case INSAT:{ }break; //do nothing
					case SAT:{  
						Pack newPk = new Pack();
						newPk.cumulative = concated;
						newPk.esPair = es;
						DefaultMutableTreeNode newLeaf = new DefaultMutableTreeNode(newPk);
						leaf.add(newLeaf);
						newLeaves.add(newLeaf);
					}break;
					case COMPLETE:{
						Pack newPk = new Pack();
						newPk.cumulative = concated;
						newPk.esPair = es;
						DefaultMutableTreeNode newLeaf = new DefaultMutableTreeNode(newPk);
						leaf.add(newLeaf);
						newPk.complete = true;
					}break;
					}
				}
				leaves = newLeaves;
			}	
		}
		
		final List<List<EventSummaryPair>> anchorSeqs = new ArrayList<List<EventSummaryPair>>();
		TreeUtility.depthFristSearch(root, new Searcher(){
			@Override
			public int check(TreeNode node) {
				if(node.isLeaf()){
					DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) node;
					Pack p = (Pack) leaf.getUserObject();
					if(p.complete){
						Object[] arr = leaf.getUserObjectPath();
						List<EventSummaryPair> seq = new ArrayList<EventSummaryPair>();
						for(int i = arr.length - 1; i>=0;i--){
							Pack pack = (Pack)arr[i];
							seq.add(pack.esPair);
						}
						anchorSeqs.add(seq);
					}
				}
				return Searcher.NORMAL;
			}
		});
		
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
	
	int solveHelper(PathSummary concated){
		List<Expression> constraints = concated.getPathConditions();
		List<Expression>[] transformed = ExpressionTranfomator.transform(constraints, null);
		List<Expression> transformedCon = transformed[ExpressionTranfomator.CONSTRAINT_INDEX];
		
		List<String> script = solution.buildScript(transformedCon);
		List<String>[] solvingResult = null;
		try {
			solvingResult = CVCSolver.solve(script);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		if(solvingResult == null) return INSAT;
		if(solution.checkSatifaction(solvingResult)){
			if(solution.getVarSet(transformedCon).size() == 0 ){
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
								List<Expression> varSet = new ArrayList<Expression>();
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
	
	private Set<Expression> findSymbolicVar(PathSummary sum){
		List<Expression> list = sum.getSymbolicStates();
		Set<Expression> set = new HashSet<Expression>();
		for(Expression sym : list){
			set.add((Expression)sym.getChildAt(0));
		}
		return set;
	}
	
	
	class Pack{
		EventSummaryPair esPair;
		PathSummary cumulative;
		boolean complete = false;
	}
}	
