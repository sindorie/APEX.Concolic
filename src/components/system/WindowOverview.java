package components.system;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import support.Logger;
import support.TreeUtility;
import support.TreeUtility.Searcher;

public class WindowOverview {
	private List<WindowInformation> visibleWins;
	private String mCurConfiguration, mCurrentFocus, mFocusedApp, mInputMethodTarget;
	private boolean isKeyboardVisible = false;
	private WindowInformation focusedWindow;
	public final static String DumpCommand = "dumpsys window visible";
	
	/**
	 * Build from a information tree.
	 * @param tree
	 */
	public WindowOverview(DefaultMutableTreeNode tree){
		final List<WindowInformation> workSpace = new ArrayList<WindowInformation>();
		TreeUtility.breathFristSearch(tree, new Searcher(){
			@Override
			public int check(TreeNode node) {
				DefaultMutableTreeNode underInspection = (DefaultMutableTreeNode)node;
				String content = underInspection.getUserObject().toString();
				if(content.matches(WindowInformation.RootNodeIdentifier)){
					WindowInformation winInfo = new WindowInformation(underInspection);
					workSpace.add(winInfo);
					
					boolean isKeyboard = winInfo.name.equalsIgnoreCase("inputmethod");
					isKeyboardVisible |= isKeyboard;
					return Searcher.SKIP;
				}
				
				if(content.startsWith("mCurConfiguration")){
					WindowOverview.this.mCurConfiguration = InformationCollector.extractSingleData(content);
					return Searcher.SKIP;
				}
				
				if(content.startsWith("mCurrentFocus")){
					WindowOverview.this.mCurrentFocus = InformationCollector.extractSingleData(content);
					return Searcher.SKIP;
				}
				
				if(content.startsWith("mFocusedApp")){
					WindowOverview.this.mFocusedApp = InformationCollector.extractSingleData(content);
					return Searcher.SKIP;
				}
				
				if(content.startsWith("mInputMethodTarget")){
					WindowOverview.this.mInputMethodTarget = InformationCollector.extractSingleData(content);
					return Searcher.SKIP;
				}
				return Searcher.NORMAL;
			}
		});
		visibleWins = workSpace;
		for(WindowInformation winInfo : visibleWins){
			Logger.trace("identifier: "+winInfo.identifier);
			if(winInfo.identifier.equals(mCurrentFocus)){
				focusedWindow = winInfo;
				break;
			}
		}

		Logger.trace("mCurrentFocus: "+mCurrentFocus);
		Logger.trace("focusedWindow: "+focusedWindow);
	}
	
	/**
	 * Get the visibleInformaiton previously stored. 
	 * @return
	 */
	public List<WindowInformation> getVisibleWindows(){
		return this.visibleWins;
	}
	
	/**
	 * True when the input method is visible
	 * @return
	 */
	public boolean isKeyboardVisible() {
		return isKeyboardVisible;
	}
	
	/**
	 * get focused window information
	 * @return
	 */
	public WindowInformation getFocusedWindow() {
		return focusedWindow;
	}
	
	/**
	 * get the current configuration string
	 * @return
	 */
	public String getmCurConfiguration(){
		return this.mCurConfiguration;
	}
	
	/**
	 * get current focus information string
	 * @return
	 */
	public String getmCurrentFocus(){
		return this.mCurrentFocus;
	}
	
	/**
	 * get focused application information string
	 * @return
	 */
	public String getmFocusedApp(){
		return this.mFocusedApp;
	}
	
	/**
	 * get input method information string
	 * @return
	 */
	public String getmInputMethodTarget(){
		return this.mInputMethodTarget;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		String primary = 
				mCurConfiguration+"\n"+
				mCurrentFocus+"\n"+
				mFocusedApp+"\n"+
				mInputMethodTarget+"\n"
				;
		
		if(this.visibleWins == null || this.visibleWins.size() == 0){
			sb.append("Non visible window");
		}else{
			sb.append("Visible window in total: "+this.visibleWins.size());
		}
		sb.append(primary);
		return sb.toString();
	}
}
