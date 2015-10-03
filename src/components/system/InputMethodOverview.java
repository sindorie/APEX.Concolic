package components.system;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import support.TreeUtility;
import support.TreeUtility.Searcher;

public class InputMethodOverview {
	public final static String TAG = "InputMethodOverview";
	public static Map<String,String> predefinedInput;
	static{
		predefinedInput = new HashMap<String,String>();
		String[] paris ={//who care the name
			"0x20001","original",//original 
			"0x61","my name",//text person name
			"0x81","tqwe1234",//text password
			"0x12","12345",//numberPassword
			"0x21","obad@isu.edu",//textEmailAddress
			"0x3","(515)-123-4567",//phone
			"0x71","Durham 314 ISU, 50010",//text postal address
			"0x20001","Multi line",//textMultiLine
			"0x24","12:30:10",//time
			"0x14","2012/3/1",//date
			"0x2","012345",//number
			"0x1002","-1234",//numberSigned
			"0x2002","123.456",//numberDecimal
		};
		
		for(int i =0;i< paris.length; i+=2){
			predefinedInput.put(paris[i], paris[i+1]);
		}
	}
	
	public String inputType, imeOptions, privateImeOptions;
	public String actionLabel, actionId;
	public String initialSelStart, initialSelEnd, initialCapsMode;
	public String hintText, label;
	public String packageName, fieldId, fieldName;
	public String extras;
	
	public final static String DumpCommand = "dumpsys input_method";
	public final static String RootNodeIdentifier = "mInputEditorInfo:";
	
	public InputMethodOverview(){}
	public InputMethodOverview(final DefaultMutableTreeNode tree){
		TreeUtility.breathFristSearch(tree, new Searcher(){
			@Override
			public int check(TreeNode node) {
				TreeUtility.breathFristSearch(tree, new Searcher(){
					@Override
					public int check(TreeNode node) {
						String line = ((DefaultMutableTreeNode) node).getUserObject().toString();
						if(line.startsWith("inputType")){
							String[] data = InformationCollector.extractMultiValue(line);
							inputType = data[0];
							imeOptions = data[1];
							privateImeOptions = data[2];
							return Searcher.NORMAL;
						}
						if(line.startsWith("actionLabel")){
							String[] data = InformationCollector.extractMultiValue(line);
							actionLabel = data[0];
							actionId = data[1];
							return Searcher.NORMAL;
						}
						if(line.startsWith("initialSelStart")){
							String[] data = InformationCollector.extractMultiValue(line);
							initialSelStart = data[0];
							initialSelEnd = data[1];
							initialCapsMode = data[2];
							return Searcher.NORMAL;
						}
						if(line.startsWith("hintText")){
							String[] data = InformationCollector.extractMultiValue(line);
							hintText = data[0];
							label = data[1];
							return Searcher.NORMAL;
						}
						if(line.startsWith("packageName")){
							System.out.println(line);
							String[] data = InformationCollector.extractMultiValue(line);
							packageName = data[0];
							fieldId = data[1];
							fieldName = data[2];
							return Searcher.NORMAL;
						}
						if(line.startsWith("extras")){
							String[] data = InformationCollector.extractMultiValue(line);
							extras = data[0];
							return Searcher.NORMAL;
						}
						
						return Searcher.NORMAL;
					}
				});
				return Searcher.NORMAL;
			}
		});
	}
	
	@Override
	public String toString(){
		return 	inputType+";"+
				this.imeOptions+";"+
				this.privateImeOptions+";"+
				this.packageName+";"+
				this.fieldId+";"+
				this.fieldName+";";
	}
}
