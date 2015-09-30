package components.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import support.CommandLine;
import support.TreeUtility;
import support.TreeUtility.Searcher;

public class InformationCollector {
	public InputMethodOverview IMOverview;
	public WindowPolicy winPolicy;
	public WindowOverview winOverview;
	
	String serial;
	public InformationCollector(String serial){
		this.serial = serial;
	}
	private DefaultMutableTreeNode targetNode = null;
	public InputMethodOverview getInputMethodOverview(){
		CommandLine.clear();
		CommandLine.executeShellCommand(InputMethodOverview.DumpCommand, serial);
		String input = CommandLine.getLatestStdoutMessage();
		
		TreeUtility.breathFristSearch(TreeUtility.buildLineTree(input), new Searcher(){
			@Override
			public int check(TreeNode node) {
				String line = ((DefaultMutableTreeNode)node).getUserObject().toString();
				if(line.matches(InputMethodOverview.RootNodeIdentifier)){
					targetNode = ((DefaultMutableTreeNode)node);
					return Searcher.STOP;
				}
				return Searcher.NORMAL;
			}
		});
		return new InputMethodOverview(targetNode);
	}

	public WindowPolicy getWindowPolicy(){
		CommandLine.executeShellCommand(WindowPolicy.DumpCommand, serial);
		String input = CommandLine.getLatestStdoutMessage();		
		TreeUtility.breathFristSearch(TreeUtility.buildLineTree(input), new Searcher(){
			@Override
			public int check(TreeNode node) {
				String line = ((DefaultMutableTreeNode)node).getUserObject().toString();
				if(line.matches(WindowPolicy.RootNodeIdentifier)){
					targetNode = ((DefaultMutableTreeNode)node);
					return Searcher.STOP;
				}
				return Searcher.NORMAL;
			}
		});
		return new WindowPolicy(targetNode);
	}
	
	public WindowOverview getWindowOverview(){
		CommandLine.executeShellCommand(WindowOverview.DumpCommand, serial);
		String input = CommandLine.getLatestStdoutMessage();
		return new WindowOverview(TreeUtility.buildLineTree(input));
	}
	
	public static String[] extractMultiValue(String input){
		String[] parts = input.trim().split("=| ");
		List<String> result = new ArrayList<String>();
		int index = 1;
		while(index < parts.length){
			result.add(parts[index].trim());
			index += 2;
		}
		return result.toArray(new String[0]);
	}
	
	public static String extractSingleData(String input){
		return input.split("=")[1].trim();
	}
	
	public static Map<String,String> readPairs(String line){
		Map<String,String> result = new HashMap<String,String>();
		String[] parts = line.split("=| ");
		for(int i=0;i<parts.length;i++){
			result.put(parts[i], parts[i+1]);
		}
		return result;
	}
}


