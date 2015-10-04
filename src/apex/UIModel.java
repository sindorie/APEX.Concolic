package apex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.ListenableDirectedGraph;

import components.EventSummaryPair;
import components.GraphicalLayout;
import components.LayoutNode;

public class UIModel {	
	
	private Map<String, List<GraphicalLayout>> nameToUI;
	private ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> graph;
	private Map<GraphicalLayout, List<EventSummaryPair>> vertex_to_loopEdges;
	private List<List<EventSummaryPair>> slices;
	private List<EventSummaryPair> currentLine;
	private Map<GraphicalLayout, List<EventSummaryPair>> knownSequence;
	private List<EventSummaryPair> allKnownEdges;
	
	public UIModel(){
		graph = new ListenableDirectedGraph<GraphicalLayout, EventSummaryPair>(EventSummaryPair.class);
		vertex_to_loopEdges = new HashMap<GraphicalLayout, List<EventSummaryPair>>();
		nameToUI = new HashMap<String,List<GraphicalLayout>>();
		slices = new ArrayList<List<EventSummaryPair>>();
		knownSequence = new HashMap<GraphicalLayout, List<EventSummaryPair>>();
		allKnownEdges = new ArrayList<EventSummaryPair>();
		
		this.addUI(GraphicalLayout.Launcher);
		graph.addVertex(GraphicalLayout.Launcher);
	}
	
	public void update(EventSummaryPair esPair){
		allKnownEdges.add(esPair);
		if(esPair.getSource() == esPair.getTarget()){
			List<EventSummaryPair> list = vertex_to_loopEdges.get(esPair.getSource());
			if(list == null){
				list = new ArrayList<EventSummaryPair>();
				vertex_to_loopEdges.put(esPair.getSource(), list);
			}
			list.add(esPair);
		}else{ graph.addEdge(esPair.getSource(), esPair.getTarget(), esPair); }
	}

	public GraphicalLayout findOrConstructUI(String actName, LayoutNode node){
		List<GraphicalLayout> list = nameToUI.get(actName);
		if(list == null){
			list = new ArrayList<GraphicalLayout>();
			nameToUI.put(actName, list);
			GraphicalLayout newLayout = new GraphicalLayout(actName, node);
			list.add(newLayout);
			return newLayout;
		}else{
			for(GraphicalLayout ui : list){
				if(ui.hasTheSmaeLayout(node)){	
					return ui;
				}
			}
			GraphicalLayout newLayout = new GraphicalLayout(actName, node);
			list.add(newLayout);
			return newLayout;
		}
	}
	public ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> getGraph(){
		return this.graph;
	}
	
	void record(EventSummaryPair step){
		if(currentLine == null){
			currentLine = new ArrayList<EventSummaryPair>();
			currentLine.add(step);
			
			GraphicalLayout dest = step.getTarget();
			List<EventSummaryPair> path = knownSequence.get(dest);
			if(path == null || path.size() > this.currentLine.size()){
				knownSequence.put(dest, new ArrayList<EventSummaryPair>(currentLine));
			}
		}
	}
	void hasReinstalled(){
		if(currentLine != null){
			this.slices.add(currentLine);
			currentLine = null;
		}
	}
	
	public List<EventSummaryPair> getAllEdges(){
		return allKnownEdges;
	}
	public Set<GraphicalLayout> getAllVertex(){
		return this.graph.vertexSet();
	}
	
	public List<EventSummaryPair> findSequence(GraphicalLayout source, GraphicalLayout target){
		return DijkstraShortestPath.findPathBetween(graph, source, target);
	}
	
	List<EventSummaryPair> findKownSequence(GraphicalLayout target){
		List<EventSummaryPair> list = knownSequence.get(target);
		return list;
	}
	
	
	private void addUI(GraphicalLayout g){
		List<GraphicalLayout> list = this.nameToUI.get(g.getActName());
		if(list == null){
			list = new ArrayList<GraphicalLayout>();
			this.nameToUI.put(g.getActName(), list);
		}
		list.add(g);
	}
	
	
}
