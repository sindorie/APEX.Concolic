package support;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import apex.symbolic.Expression;
import components.GraphicalLayout;
import components.LayoutNode;

public class TreeUtility {
	
	/**
	 * Expands all nodes in a JTree.
	 *
	 * @param tree      The JTree to expand.
	 * @param depth     The depth to which the tree should be expanded.  Zero
	 *                  will just expand the root node, a negative value will
	 *                  fully expand the tree, and a positive value will
	 *                  recursively expand the tree to that depth.
	 */
	public static void expandJTree (javax.swing.JTree tree, int depth)
	{
	    javax.swing.tree.TreeModel model = tree.getModel();
	    expandJTreeNode(tree, model, model.getRoot(), 0, depth);
	} // expandJTree()


	/**
	 * Expands a given node in a JTree.
	 *
	 * @param tree      The JTree to expand.
	 * @param model     The TreeModel for tree.     
	 * @param node      The node within tree to expand.     
	 * @param row       The displayed row in tree that represents
	 *                  node.     
	 * @param depth     The depth to which the tree should be expanded. 
	 *                  Zero will just expand node, a negative
	 *                  value will fully expand the tree, and a positive
	 *                  value will recursively expand the tree to that
	 *                  depth relative to node.
	 */
	public static int expandJTreeNode (javax.swing.JTree tree,
	                                   javax.swing.tree.TreeModel model,
	                                   Object node, int row, int depth)
	{
	    if (node != null  &&  !model.isLeaf(node)) {
	        tree.expandRow(row);
	        if (depth != 0)
	        {
	            for (int index = 0;
	                 row + 1 < tree.getRowCount()  &&  
	                            index < model.getChildCount(node);
	                 index++)
	            {
	                row++;
	                Object child = model.getChild(node, index);
	                if (child == null)
	                    break;
	                javax.swing.tree.TreePath path;
	                while ((path = tree.getPathForRow(row)) != null  &&
	                        path.getLastPathComponent() != child)
	                    row++;
	                if (path == null)
	                    break;
	                row = expandJTreeNode(tree, model, child, row, depth - 1);
	            }
	        }
	    }
	    return row;
	} // expandJTreeNode()
	
	
	public final static String chunk = "    ";
	public static List<Expression> textTreeToExpression_file(String fileName) throws FileNotFoundException{
		File f = new File(fileName);
		Scanner sc = new Scanner(f);
		List<Expression> list = new ArrayList<>();
		Expression expre = null;
		int current = -1;
		while(sc.hasNextLine()){
			String line = sc.nextLine();
			if(line == null || line.trim().isEmpty()) continue;
			int count = (line.length() - line.replace(chunk, "").length()) / chunk.length();
			if(count == 0){
				if(expre != null){ list.add((Expression)expre.getRoot()); }
				expre = new Expression(line.trim());
				current = 0;
			}else{
				while(current >= count){
					expre = (Expression) expre.getParent();
					current --;
				}
				current = count;
				Expression child = new Expression(line.trim());
				expre.add(child);
				expre = child;
			}
		}
		if(expre != null){ list.add((Expression)expre.getRoot()); }
		sc.close();
		return list;
	}
	
	public static Expression textTreeToExpression(String input){
		String[] list = input.split("\n");
		Expression expre = null;
		int current = -1;
		for(String line : list){
			if(line.trim().isEmpty()) continue;
			
			int count = (line.length() - line.replace(chunk, "").length()) / chunk.length();
			if(count == 0){
				expre = new Expression(line.trim());
				current = 0;
			}else{
				while(current >= count){
					expre = (Expression) expre.getParent();
					current --;
				}
				current = count;
				Expression child = new Expression(line.trim());
				expre.add(child);
				expre = child;
			}
		}
		return expre;
	}
	
	public static String treeToText(TreeNode node){
		return treeToText(node, 0);
	}
	
	private static String treeToText(TreeNode node, int level){
		StringBuilder sb = new StringBuilder();
		for(int i =0;i<level;i++){
			sb.append(chunk);
		}
		String segment = node.toString();
		if(segment == null) segment = "";
		sb.append(segment).append("\n");
		for(int i =0 ;i<node.getChildCount(); i ++){
			sb.append(treeToText((TreeNode)node.getChildAt(i),level+1));
		}
		return sb.toString();
	}
	
	public static interface Searcher{
		public final static int STOP = 2, SKIP = 1, NORMAL = 0;
		public int check(TreeNode node);
	}
	
	public static void breathFristSearch(TreeNode tree, Searcher searcher){
		List<TreeNode> queue = new ArrayList<TreeNode>();
		queue.add(tree);
		MAJOR: while(queue.isEmpty() == false){
			TreeNode node = queue.remove(0);
			switch(searcher.check(node)){
			case Searcher.STOP: break MAJOR;
			case Searcher.SKIP: continue MAJOR;
			case Searcher.NORMAL:
			default: //nothing 
			}
			if(node == null) continue;
			for(int index = 0; index < node.getChildCount(); index ++){
				TreeNode child = node.getChildAt(index);
//				if(child == null) continue;
				queue.add(child);
			}
		}
	}
	
	public static void depthFristSearch(TreeNode tree, Searcher searcher){ 
		Stack<TreeNode> stack = new Stack<TreeNode>();
		stack.push(tree);
		MAJOR: while(stack.isEmpty() == false){
			TreeNode node = stack.pop();
			switch(searcher.check(node)){
			case Searcher.STOP: break MAJOR;
			case Searcher.SKIP: continue MAJOR;
			case Searcher.NORMAL:
			default: //nothing 
			}
			for(int index = node.getChildCount() - 1; index >=0; index-- ){
				stack.push(node.getChildAt(index));
			}
		}
	}
	
	public static DefaultMutableTreeNode buildLineTree(String input){
		Scanner sc = new Scanner(input);
		return buildLineTree(sc);
	}

	public static DefaultMutableTreeNode buildLineTree(Scanner reader){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
		DefaultMutableTreeNode current = root;
		Stack<Integer> levelStack = new Stack<Integer>();
		levelStack.push(-1); //the level of root
		while(reader.hasNextLine()){
			String line = reader.nextLine();
			String trimed = line.trim();
			if(trimed.isEmpty()) continue; //TODO may want to check
			
			//count the initial white space
			int currentLevel = 0;
			for(;currentLevel<line.length();currentLevel++){
				if(line.charAt(currentLevel) != ' '){
					break;
				}
			}
			
			int parentLevel = levelStack.peek();
			//image a tree where root is at the top.
			switch(levelComparison(parentLevel,currentLevel)){
			case DEEPER:{ //encounter a block which is deeper than the parent
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(trimed);
				current.add(child);
				current = child;
				levelStack.push(currentLevel);
			}break;
			case SAME:{
				DefaultMutableTreeNode sameLevelNode = new DefaultMutableTreeNode(trimed);
				((DefaultMutableTreeNode)current.getParent()).add(sameLevelNode);
				current = sameLevelNode;
			}break;
			case SHALLOWER:{
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(trimed);
				do{
					levelStack.pop();
					parentLevel = levelStack.peek();
					current = (DefaultMutableTreeNode)current.getParent();
					int comparison = levelComparison(parentLevel,currentLevel);
					if(comparison == DEEPER){
						current.add(child);
						current = child;
						levelStack.push(currentLevel);
						break;
					}
				}while(true);
			}
			}
		}
		return root;
	}
	
	/**
	 * It is just confusing if i dont write a helper
	 */
	private final static int DEEPER = 0, SAME = 1, SHALLOWER = 2;
	private static int levelComparison(int parentLevel, int currentLevel){
		if(currentLevel > parentLevel){ return DEEPER;}
		else if(currentLevel == parentLevel){ return SAME;}
		else return SHALLOWER;
	}
	
	private static int localCount;
	public static int countNodes(GraphicalLayout layout){
		return countNodes(layout.getRootNode());
	}
	
	public static int countNodes(LayoutNode node){
		if(node == null) return -1;
		localCount = 0;
		TreeUtility.depthFristSearch(node, new TreeUtility.Searcher() {
			@Override
			public int check(TreeNode node) {
				localCount += 1;
				return Searcher.NORMAL;
			}
		});
		return localCount;
	}
}
