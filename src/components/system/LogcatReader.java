package components.system;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import components.WrappedSummary;

import support.CommandLine;


public class LogcatReader {
	
	String serial, adbLocation, tag;
	public LogcatReader(String serial, String adbLocation, String tag){
		this.serial = serial; this.adbLocation = adbLocation; this.tag = tag;
	}
	
	public void clearLogcat(){
		CommandLine.executeADBCommand("logcat -c", serial);
	}
	
	/**
	 * Read the feedback from logcat according to tag
	 * @return
	 */
	public List<String> readLogcatFeedBack(){
		ArrayList<String> result = new ArrayList<String>();
		try{
			String command = adbLocation + " -s "+serial+" logcat -v thread -d  -s "+tag;
			final Process pc = Runtime.getRuntime().exec(command);
			InputStream in = pc.getInputStream();
			InputStream err = pc.getErrorStream();
			
			StringBuilder sb = new StringBuilder();
			Thread.sleep(300);
			long point1 = System.currentTimeMillis();
			while(true){
				int count = in.available();
				if(count <= 0) break;
				byte[] buf = new byte[count];
				in.read(buf);
				sb.append(new String(buf));
				long point2 = System.currentTimeMillis();
				if(point2 - point1 > 300) break;
			}
			String errMsg = null;
			if(err.available()>0){
				int count = in.available();
				byte[] buf = new byte[count];
				in.read(buf);
				errMsg = new String(buf);
			}
			if(errMsg!=null) System.out.println("DEBUG:errMsg,  "+errMsg);
			
			String tmp = sb.toString().trim();
			if(tmp.equals("")) return result;//empty
			String[] parts = tmp.split("\n");
			for(String part: parts){
				result.add(part);
			}
			pc.destroy();
			CommandLine.executeADBCommand("logcat -c", serial);
		}catch(Exception e){
			System.out.println(e.getLocalizedMessage());
		}
		return result;
	}
	
	
	/**
	 * Find the major branch based on the amount of leaf nodes.
	 * Using >= 
	 * @param methodIOTree
	 * @return
	 */
	public int findMajorBranch(List<DefaultMutableTreeNode> methodIOTree, List<List<WrappedSummary>> mappedSummaryCandidates){
		//TODO to improve 
		int max, index = -1;
		int size = methodIOTree.size();
		if(size <= 0) return -1;
		max = methodIOTree.get(0).getLeafCount();
		for(int i=0;i<size;i++){
			if(mappedSummaryCandidates.get(i).isEmpty()) continue;
			if(index < 0){
				index = i;
				max = methodIOTree.get(i).getLeafCount();	
			}else{
				int curMax = methodIOTree.get(i).getLeafCount();
				if(curMax >= max){
					max = curMax;
					index = i;
				}
			}
		}
		return index;
	}
	
	public List<String> getMethodRoots(List<DefaultMutableTreeNode> methodIOTree){
		List<String> roots = new ArrayList<String>();
		for(DefaultMutableTreeNode node : methodIOTree){
			roots.add(node.getUserObject().toString());
		}
		return roots;
	}
	
	public List<DefaultMutableTreeNode> buildMethodCallTree(List<String> methodIOList){
		List<DefaultMutableTreeNode> roots = new ArrayList<DefaultMutableTreeNode>();
		int level = 0;
		DefaultMutableTreeNode current = null;
		for(String method : methodIOList){
			if(method.contains("METHOD_STARTING,")){
				if(level == 0){
					DefaultMutableTreeNode root = new DefaultMutableTreeNode();
					String[] parts = method.split("METHOD_STARTING,");
					root.setUserObject(parts[parts.length-1].trim());
					roots.add(root);
					current = root;
				}else{ //level > 0
					DefaultMutableTreeNode node = new DefaultMutableTreeNode();
					current.add(node);
					current = node;
				}
				level += 1;
			}else if(method.contains("METHOD_RETURNING,")){
//				String currentMethod = current.getUserObject().toString();
				
				
				if(level == 0){
					System.out.println("Method Call Tree 0 level at returning");
					continue;
				}
				current = (DefaultMutableTreeNode)current.getParent();
				level -= 1;
			}
		}
		return roots;
	}

	private String filter(String msg, boolean isStart){
		if(isStart){
			String[] parts = msg.split("METHOD_STARTING,");
			return parts[0].trim();
		}else{
			String[] parts = msg.split("METHOD_RETURNING,");
			return parts[0].trim();
		}
	}
}
