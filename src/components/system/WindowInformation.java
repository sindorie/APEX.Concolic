package components.system;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import apex.staticFamily.StaticApp;
import components.EventResultBundle;
import support.TreeUtility;
import support.TreeUtility.Searcher;
 
/**
 * Window information extracted from ADB shell dumpsys
 * Only information on visible window is retrieved. 
 * @author zhenxu
 *
 */
public class WindowInformation{
	public String identifier, name, actName;
	public String mOwnerUid, mShowToOwnerOnly, pkgName, appop;
	public String mViewVisibility, mHaveFrame, mObscured;
	public String mFrame;
	public double width, height, startx, starty;
	public final static String RootNodeIdentifier = "Window #[\\d].*";
	public final static int SCOPE_LAUNCHER = 0, SCOPE_WITHIN = 1, SCOPE_OUT = 2;
	
	public WindowInformation(DefaultMutableTreeNode tree){
		TreeUtility.breathFristSearch(tree, new Searcher(){
			@Override
			public int check(TreeNode node) {
				String content = ((DefaultMutableTreeNode)node).getUserObject().toString();
				// Window #10 Window{1840d55e u0 NavigationBar}:
				if(content.startsWith("Window #")){
					content = content.replace("}:", "}");
					int pos = content.indexOf(' ', content.indexOf(' ')+1);
					WindowInformation.this.identifier = content.substring(pos+1, content.length());
					
					//TODO may not be correct; especially the actName.
					if(content.contains("PopupWindow")){
						//Window #3 Window{529d6ed4 u0 PopupWindow:5281b53c}:
						WindowInformation.this.name = "PopupWindow";
						WindowInformation.this.actName = "PopupWindow";
					}else{
						//Window #2 Window{52969e54 u0 the.app/the.app.MainActivity}:
						String[] parts = identifier.replaceAll("}|(Window\\{)", "").split(" ");
						WindowInformation.this.name = parts[2];
						String[] sub_parts = name.split("\\/");
						WindowInformation.this.actName = sub_parts[sub_parts.length - 1];
					}
					
					
				}else if(content.startsWith("mFrame=")){
					//mFrame=[0,0][1200,50] last=[0,0][1200,50]
					String[] parts = content.split(" ");
					String contentPairValue = parts[0].split("=")[1];
					String[] pair = contentPairValue.replaceAll("]|\\[", " ").trim().split("  ");
					String[] start = pair[0].split(",");
					WindowInformation.this.startx = Double.parseDouble(start[0]);
					WindowInformation.this.starty = Double.parseDouble(start[1]);
					String[] end = pair[1].split(",");
					WindowInformation.this.width = Double.parseDouble(end[0]) - WindowInformation.this.startx;
					WindowInformation.this.height = Double.parseDouble(end[1]) - WindowInformation.this.starty;
					return Searcher.SKIP;
				}else if(content.startsWith("mOwnerUid=")){
					//mOwnerUid=10006 mShowToOwnerOnly=false package=com.android.systemui appop=NONE
					String[] data = InformationCollector.extractMultiValue(content);
					WindowInformation.this.mOwnerUid = data[0];
					WindowInformation.this.mShowToOwnerOnly = data[1];
					WindowInformation.this.pkgName = data[2];
					WindowInformation.this.appop = data[3];
					return Searcher.SKIP;
				}else if(content.startsWith("mViewVisibility")){
					//mViewVisibility=0x0 mHaveFrame=true mObscured=false
					String[] data = InformationCollector.extractMultiValue(content);
					WindowInformation.this.mViewVisibility = data[0];
					WindowInformation.this.mHaveFrame = data[1];
					WindowInformation.this.mObscured = data[2];
					return Searcher.SKIP;
				}
				return Searcher.NORMAL;
			}
		});
	}
	
	public int isWithinApplciation(StaticApp app){
		if(pkgName.startsWith("com.android.browser.BrowserActivity")){
			return SCOPE_OUT;
		}
		if(pkgName.toLowerCase().contains("launcher")) return SCOPE_LAUNCHER;
		if(pkgName.equals(app.getPackageName())) return SCOPE_WITHIN;
		else return SCOPE_OUT;
	}
	
	@Override
	public String toString(){
		String result = identifier+", "+name+", "+this.mOwnerUid+", "+this.mShowToOwnerOnly+", "+pkgName+", "+appop+", ("+width+","+height+"), ("+startx+","+starty+")\n";
		return result;
	}
	
}
