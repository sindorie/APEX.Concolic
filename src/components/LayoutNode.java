package components;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;


public class LayoutNode extends DefaultMutableTreeNode{ 
	LayoutNode parent;
	public String className, id, text, pkgName, content_desc;
	public boolean focusable, enable, password,
			scrollable, clickable, checkable, long_clickable,
			checked, focused, selected;
	public int startx, starty, endx, endy, index;
	
	public Map<String, String> att = new HashMap<String,String>();
	public LayoutNode(String title){
		this.setUserObject(title);
	}
	public LayoutNode(){ }
	
	
	/**
	 * 
	 * {package=com.android.launcher3, checkable=false, 
	 * clickable=false, index=0, content-desc=, focusable=false, 
	 * resource-id=, enabled=true, password=false, focused=false, 
	 * bounds=[0,0][1080,1776], checked=false, long-clickable=false, 
	 * text=, class=android.widget.LinearLayout, 
	 * scrollable=false, selected=false}
	 */
	public void addAtrribute(String key, String val){
		if(key == null || key.isEmpty() ) return;
		att.put(key, val);
		if(key.equals("package")){
			this.pkgName = val;
		}else if(key.equals("checkable")){
			this.checkable = Boolean.parseBoolean(val);
		}else if(key.equals("clickable")){
			this.clickable  = Boolean.parseBoolean(val);
		}else if(key.equals("index")){
			this.index = Integer.parseInt(val);
		}else if(key.equals("content-desc")){
			this.content_desc = val;
		}else if(key.equals("resource-id")){
			this.id = val;
		}else if(key.equals("enabled")){
			this.enable = Boolean.parseBoolean(val);
		}else if(key.equals("password")){
			this.password = Boolean.parseBoolean(val);
		}else if(key.equals("focused")){
			this.focused  = Boolean.parseBoolean(val);
		}else if(key.equals("bounds")){
//			System.out.println(key+": "+val);
			String[] parts = val.split("]\\[");
			String[] subp1 = parts[0].replace("[", "").split(",");
			String[] subp2 = parts[1].replace("]", "").split(",");
			this.startx = Integer.parseInt(subp1[0]);
			this.starty = Integer.parseInt(subp1[1]);
			
			this.endx = Integer.parseInt(subp2[0]);
			this.endy = Integer.parseInt(subp2[1]);
//			System.out.println(startx+","+starty+" -> "+endx+","+endy+": "+(startx+endx)/2+","+(starty+endy)/2);
		}else if(key.equals("class")){
			this.className = val;
		}else if(key.equals("scrollable")){
			this.scrollable = Boolean.parseBoolean(val);
		}else if(key.equals("checked")){
			this.checked = Boolean.parseBoolean(val);
		}else if(key.equals("long-clickable")){
			this.long_clickable = Boolean.parseBoolean(val);
		}else if(key.equals("selected")){
			this.selected = Boolean.parseBoolean(val);
		}else if(key.equals("focusable")){
			this.focusable = Boolean.parseBoolean(val);
		}
	}
	
	public void addChild(LayoutNode child){
		super.add(child);
		child.parent = this;
	}
	
	public LayoutNode getChildAt(int index){
		return (LayoutNode) super.getChildAt(index);
	}
	
	public LayoutNode getParent(){
		return parent;
	}
	
//	public double caculateDistance(LayoutNode other){
//		return caculateHelper(other , 0);
//	}
//
//	private double caculateHelper(LayoutNode other, int depth){
//		double baseDifference = localComparison(other);
//		double ratio = 1;
//		//local comparison
//		
//		//childComparison
//		
//		if(other == null){
//			double childDiffTotal = 0;
//			for(int i =0;i<this.getChildCount();i++){
//				LayoutNode child = this.getChildAt(i);
//				childDiffTotal = child.caculateHelper(null, depth+1);
//			}
//			return baseDifference+childDiffTotal*ratio;
//		}else if(this.getChildCount() > 0){
//			//there is child node
//			double childDiffTotal = 0;
//			int[] childStatus = new int[Math.max(this.getChildCount(), other.getChildCount())];
//			
//			
//			
//			
//			return baseDifference+childDiffTotal*ratio;
//		}else{
//			double childDiffTotal = 0;
//			for(int i =0;i<other.getChildCount();i++){
//				LayoutNode child = other.getChildAt(i);
//				childDiffTotal = child.caculateHelper(null, depth+1);
//			}
//			return baseDifference+childDiffTotal*ratio;
//		}
//	}
//	
//	private int localComparison(LayoutNode other){
//		if(other == null) return 3;
//		int diff = 0;
//		if(!this.className.equals(other.className)){ diff += 1; }
//		if(!this.id.equals(other.id)){ diff += 1;}
//		if(this.clickable != other.clickable){ diff += 1; }
//		return diff;
//	}
	
	
	
	/**
	 * The two node is considered equal if
	 * 1. same id
	 * 2. same class
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof LayoutNode){
			LayoutNode input = (LayoutNode)o;
			if(this.className == null){
				if(input.className != null) return false;
			}else{
				if(!this.className.equals(input.className)){
//					System.out.println("Difference ClassName:"+this.className+":"+input.className+";");
					return false;
				}
			}
			
			//due to devices issue, the id may not be the same 
//			if(this.id == null){
//				if(input.id != null) return false;
//			}else{
//				if(!this.id.equals(input.id)) return false;
//			}
			
//			if(this.enable != input.enable){
//				System.out.println("Enable attributes");
//				return false;
//			}
//			if(this.scrollable != input.scrollable){
//				System.out.println("Different scrollable");
//				return false;
//			}
//			if(this.clickable != input.clickable){
//				System.out.println("Different clickable");
//				return false;
//			}
//			if(this.long_clickable != input.long_clickable){
//				System.out.println("Different l clickable");
//				return false;
//			}
			//currently use exact comparison
			if(this.getChildCount() != input.getChildCount()) return false;
			for(int i=0;i<this.getChildCount();i++){
				LayoutNode node1 = this.getChildAt(i);
				LayoutNode node2 = input.getChildAt(i);
				if(!node1.equals(node2)) return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String toString(){
		return className+"-"+id+"-"+this.clickable;
	}
	
	public String toFormatedString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Title: "+ this.getUserObject());
		sb.append("ClassName: "+className+"\n");
		sb.append("ID: "+id+"\n");
		sb.append("Text: "+text+"\n");
		sb.append("PkgName: "+pkgName+"\n");
		sb.append("Content_desc: "+content_desc+"\n");
		sb.append("Focusable: "+focusable+"\n");
		sb.append("Enable: "+enable+"\n");
		sb.append("Password: "+password+"\n");
		sb.append("Scrollable: "+scrollable+"\n");
		sb.append("Clickable: "+clickable+"\n");
		sb.append("Checkable: "+checkable+"\n");
		sb.append("Long Clickable: "+long_clickable+"\n");
		
		sb.append("Checked: "+checked+"\n");
		sb.append("Focused: "+selected+"\n");
		sb.append("Selected: "+selected+"\n");
		
		sb.append("Start: "+startx+","+starty+"\n");
		sb.append("End: "+endx+","+endy+"\n");
		sb.append("Index: "+index+"\n");
		sb.append("Child count: "+this.getChildCount());
		return sb.toString();
	}
}




