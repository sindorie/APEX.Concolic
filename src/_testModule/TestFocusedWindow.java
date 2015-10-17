package _testModule;

import components.system.InformationCollector;
import components.system.WindowInformation;

public class TestFocusedWindow {

	public static void main(String[] args) {
		String serial = "077d4573344bf6bb";
		InformationCollector c = new InformationCollector(serial);
		WindowInformation info = c.getWindowOverview().getFocusedWindow();
		System.out.println("Focused window found: "+(info != null));	
	}

}
