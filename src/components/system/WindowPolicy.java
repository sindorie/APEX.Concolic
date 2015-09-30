package components.system;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import support.TreeUtility;
import support.TreeUtility.Searcher;

public class WindowPolicy {
	public final static String TAG = "WindowPolicy";

	public final static String DumpCommand = "dumpsys window policy";
	public final static String RootNodeIdentifier = "WINDOW MANAGER POLICY STATE.*";
	
	public double width, height;
	
	public WindowPolicy(){}
	public WindowPolicy(DefaultMutableTreeNode tree){
		TreeUtility.breathFristSearch(tree, new Searcher(){
			@Override
			public int check(TreeNode node) {
				String line = ((DefaultMutableTreeNode) node).getUserObject().toString();
				if(line.startsWith("mUnrestrictedScreen")){
					String[] pair = line.split("=");
					String[] wh = pair[1].split(" ")[1].split("x");
					width = Double.parseDouble(wh[0]);
					height = Double.parseDouble(wh[1]);
					return Searcher.STOP;
				}
				return Searcher.NORMAL;
			}
		});
	}
	
	public String toString(){
		return width +","+height;
	}
}


/** Sample: dumpsys window policy
WINDOW MANAGER POLICY STATE (dumpsys window policy)
    mSafeMode=false mSystemReady=true mSystemBooted=true
    mLidState=-1 mLidOpenRotation=-1 mHdmiPlugged=false
    mUiMode=1 mDockMode=0 mCarDockRotation=-1 mDeskDockRotation=-1
    mUserRotationMode=0 mUserRotation=0 mAllowAllRotations=1
    mCurrentAppOrientation=-1
    mCarDockEnablesAccelerometer=true mDeskDockEnablesAccelerometer=true
    mLidKeyboardAccessibility=0 mLidNavigationAccessibility=0 mLidControlsSleep=false
    mLongPressOnPowerBehavior=-1 mHasSoftInput=true
    mScreenOnEarly=true mScreenOnFully=true mOrientationSensorEnabled=true
    mOverscanScreen=(0,0) 800x1280
    mRestrictedOverscanScreen=(0,0) 800x1216
    mUnrestrictedScreen=(0,0) 800x1280
    mRestrictedScreen=(0,0) 800x1216
    mStableFullscreen=(0,0)-(800,1216)
    mStable=(0,33)-(800,1216)
    mSystem=(0,33)-(800,1216)
    mCur=(0,33)-(800,1216)
    mContent=(0,33)-(800,1216)
    mDock=(0,33)-(800,1216)
    mDockLayer=268435456 mStatusBarLayer=161000
    mShowingLockscreen=false mShowingDream=false mDreamingLockscreen=false
    mStatusBar=Window{52902514 u0 StatusBar}
    mNavigationBar=Window{528d6abc u0 NavigationBar}
    mKeyguard=Window{5292c8dc u0 Keyguard}
    mFocusedWindow=Window{52929b98 u0 PopupWindow:52a83b94}
    mFocusedApp=Token{529691c0 ActivityRecord{5296ece8 u0 com.android.deskclock/.DeskClock t5}}
    mTopFullscreenOpaqueWindowState=Window{529ebbc0 u0 com.android.deskclock/com.android.deskclock.DeskClock}
    mTopIsFullscreen=false mHideLockScreen=false
    mForceStatusBar=false mForceStatusBarFromKeyguard=false
    mDismissKeyguard=0 mWinDismissingKeyguard=null mHomePressed=false
    mAllowLockscreenWhenOn=false mLockScreenTimeout=60000 mLockScreenTimerActive=false
    mEndcallBehavior=2 mIncallPowerBehavior=1 mLongPressOnHomeBehavior=0
    mLandscapeRotation=1 mSeascapeRotation=3
    mPortraitRotation=0 mUpsideDownRotation=2
    mDemoHdmiRotation=1 mDemoHdmiRotationLock=false
    mUndockedHdmiRotation=-1
    BarController.StatusBar
      mState=WINDOW_STATE_SHOWING
      mTransientBar=TRANSIENT_BAR_NONE
    BarController.NavigationBar
      mState=WINDOW_STATE_SHOWING
      mTransientBar=TRANSIENT_BAR_NONE
 */