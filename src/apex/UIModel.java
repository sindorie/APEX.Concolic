package apex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.tree.TreeNode;

import org.jgraph.JGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.ListenableDirectedGraph;

import android.view.KeyEvent;
import support.TreeUtility;
import support.TreeUtility.Searcher;
import components.Event;
import components.EventFactory;
import components.EventSummaryPair;
import components.EventSummaryStorage;
import components.GraphicalLayout;
import components.LayoutNode;
import components.system.InputMethodOverview;

/**
 * A model of the application focuses on the User interface part.
 * Such model treat event-path as edge and each activity-layout as node.
 * 
 * Upon receiving a new activity-layout node, generate a list of potential
 * events which will be executed later. 
 * 
 * 
 * @author zhenxu
 *
 */
public class UIModel implements Serializable{	
	
	private Map<String, List<GraphicalLayout>> nameToUI;
	private ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> graph;
	private Map<GraphicalLayout, List<EventSummaryPair>> vertex_to_loopEdges;
	private List<List<EventSummaryPair>> slices;
	private List<EventSummaryPair> currentLine;
	private Map<GraphicalLayout, List<EventSummaryPair>> knownSequenceToUI;
	private EventSummaryStorage allKnownEdges;
	private transient List<Event> newEventBuffer;
	
	public UIModel(){
		graph = new ListenableDirectedGraph<GraphicalLayout, EventSummaryPair>(EventSummaryPair.class);
		vertex_to_loopEdges = new HashMap<GraphicalLayout, List<EventSummaryPair>>();
		nameToUI = new HashMap<String,List<GraphicalLayout>>();
		slices = new ArrayList<List<EventSummaryPair>>();
		knownSequenceToUI = new HashMap<GraphicalLayout, List<EventSummaryPair>>();
		allKnownEdges = new EventSummaryStorage();
		
		List<GraphicalLayout> list = new ArrayList<GraphicalLayout>();
		list.add(GraphicalLayout.Launcher);
		this.nameToUI.put(GraphicalLayout.Launcher.getActName(), list);
		graph.addVertex(GraphicalLayout.Launcher);
		
		List<GraphicalLayout> list1 = new ArrayList<GraphicalLayout>();
		list1.add(GraphicalLayout.ErrorScene);
		this.nameToUI.put(GraphicalLayout.ErrorScene.getActName(), list1);
		graph.addVertex(GraphicalLayout.ErrorScene);
	}
	
	
	/**
	 * Update the model with the event-path pair. 
	 * The execution result might imply an existence of text-input event
	 * @param esPair - the event-path pair	
	 * @param exeResult - a bundle of information for the carryout the event-path pair
	 * @return a list of event which has not yet be executed. 
	 */
	public List<Event> update(EventSummaryPair esPair, EventExecutionResult exeResult){
		Common.TRACE(esPair.toString());
		allKnownEdges.store(esPair,true);
		if(esPair.getSource() == esPair.getTarget()){
			List<EventSummaryPair> list = vertex_to_loopEdges.get(esPair.getSource());
			if(list == null){
				list = new ArrayList<EventSummaryPair>();
				vertex_to_loopEdges.put(esPair.getSource(), list);
			}
			if(!list.contains(esPair)) list.add(esPair);
		}else if(!this.graph.containsEdge(esPair)){
			graph.addEdge(esPair.getSource(), esPair.getTarget(), esPair); 
		}
		
		List<Event> events = newEventBuffer;
		if(exeResult.keyboardVisible){
			Event event = esPair.getEvent();
			if(event.getEventType() == EventFactory.iONCLICK){
				GraphicalLayout g = event.getSource();
				String iType = exeResult.iInfo.inputType;
				String toEnter = InputMethodOverview.predefinedInput.get(iType);
				if(toEnter == null){ toEnter = "hello"; }
				Event textInput = EventFactory.createTextEvet(event, toEnter);
				if(!g.candidates.contains(textInput)){
					if(events == null){
						events = new ArrayList<Event>();
					}
					events.add(textInput);
					g.candidates.add(textInput);
				}
			}
		}
		newEventBuffer = null;
		return events;
	}

	public GraphicalLayout findOrConstructUI(String actName, LayoutNode node){
		List<GraphicalLayout> list = nameToUI.get(actName);
		if(list == null){
			list = new ArrayList<GraphicalLayout>();
			nameToUI.put(actName, list);
			GraphicalLayout newLayout = new GraphicalLayout(actName, node);
			list.add(newLayout);
			this.graph.addVertex(newLayout);
			createNewEvent(newLayout);
			return newLayout;
		}else{
			for(GraphicalLayout ui : list){
				if(ui.hasTheSmaeLayout(node)){	
					return ui;
				}
			}
			GraphicalLayout newLayout = new GraphicalLayout(actName, node);
			list.add(newLayout);
			this.graph.addVertex(newLayout);
			createNewEvent(newLayout);
			return newLayout;
		}
	}
	public ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> getGraph(){
		return this.graph;
	}
	
	public void record(EventSummaryPair step){
		if(currentLine == null){
			currentLine = new ArrayList<EventSummaryPair>();
			this.slices.add(currentLine);
		}
		currentLine.add(step);
		GraphicalLayout dest = step.getTarget();
		List<EventSummaryPair> path = knownSequenceToUI.get(dest);
		if(path == null || path.size() > this.currentLine.size()){
			knownSequenceToUI.put(dest, new ArrayList<EventSummaryPair>(currentLine));
		}
	}
	
	public void hasReinstalled(){
		if(currentLine != null && !currentLine.isEmpty()){
			this.slices.add(currentLine);
			currentLine = null;
		}
	}
	public List<List<EventSummaryPair>> getAllSlices(){		
		return this.slices;
	}
	public List<EventSummaryPair> getCurrentLine(){
		if(currentLine == null) return null;
		return new ArrayList<EventSummaryPair>(currentLine);
	}
	
	public List<EventSummaryPair> getAllEdges(){
		return allKnownEdges.getAlldata();
	}
	public Set<GraphicalLayout> getAllVertex(){
		return this.graph.vertexSet();
	}
	
	public List<EventSummaryPair> findSequence(GraphicalLayout source, GraphicalLayout target){
		if(source == target) return new ArrayList<>();
		return DijkstraShortestPath.findPathBetween(graph, source, target);
	}
	
	public List<EventSummaryPair> findKownSequence(GraphicalLayout target){
		List<EventSummaryPair> list = knownSequenceToUI.get(target);
		return list;
	}
	
	/**
	 * if should exist the program on exist
	 * @param closeOnExit
	 */
	public void showGUI(boolean closeOnExit){
		JGraphModelAdapter<GraphicalLayout, EventSummaryPair> adapter = new JGraphModelAdapter<GraphicalLayout, EventSummaryPair>(graph);
		JGraph jgraph = new JGraph(adapter);
		jgraph.setEditable(false);
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(jgraph);
		frame.setSize(800, 600);
		if(closeOnExit) frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	void createNewEvent(final GraphicalLayout layout){
		final List<Event> toAdd = new ArrayList<Event>();
		toAdd.add(EventFactory.CreatePressEvent(layout, KeyEvent.KEYCODE_BACK));
		
		TreeUtility.breathFristSearch(layout.getRootNode(), new Searcher(){
			@Override
			public int check(TreeNode treeNode) {
				if(treeNode == null) return Searcher.NORMAL;
				LayoutNode node = (LayoutNode)treeNode;
				if(node.isLeaf()){
					if(node.clickable){ 
						Event next = EventFactory.createClickEvent(layout, node);
						if(!toAdd.contains(next)){
							toAdd.add(next); 
						}
					}else if(node.checkable){
						Event next = EventFactory.createClickEvent(layout, node);
						if(!toAdd.contains(next)){
							toAdd.add(next); 
						}
					}
				}else{
					if(node.getParent() != null && node.getParent().clickable){
						Event next = EventFactory.createClickEvent(layout, node);
						if(!toAdd.contains(next)){
							toAdd.add(next); 
						}
					}
				}
				return Searcher.NORMAL;
			}
		});
		
		layout.candidates.addAll(toAdd);
		if(newEventBuffer == null){ newEventBuffer = toAdd;
		}else{ newEventBuffer.addAll(toAdd); }
	}
}
