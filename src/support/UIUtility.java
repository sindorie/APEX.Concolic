package support;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeNode;

import apex.instrumentor.Blacklist;
import apex.staticFamily.StaticApp;
import apex.staticFamily.StaticClass;
import apex.staticFamily.StaticMethod;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;
import apex.symbolic.SymbolicExecution;

public class UIUtility {

	public static void showList(List<?> list){
		showList("list", list, JFrame.EXIT_ON_CLOSE);
	}
	
	public static void showList(String name, List<?> list, int operationCode){
		showList("list", list, null, JFrame.EXIT_ON_CLOSE);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void showList(String name, List<?> list, ListSelectionListener listener, int operationCode){
		JFrame frame = new JFrame(name);
		JList uiList = new JList();
		DefaultListModel lModel = new DefaultListModel();
		Iterator<?> iter = list.iterator();
		while(iter.hasNext()){
			Object element = iter.next();
			if(element == null) continue;
			lModel.addElement(element);
		}
		
		uiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if(listener != null ) uiList.addListSelectionListener(listener);
		uiList.setModel(lModel);
		JScrollPane jsp = new JScrollPane();
		jsp.setViewportView(uiList);
		frame.getContentPane().add(jsp);
		frame.pack();
		frame.setVisible(true);
	}
	
	
	
	public static void showTree(TreeNode node){
		showTree("tree",node,JFrame.EXIT_ON_CLOSE);
	}
	
	public static void showTree(String name, TreeNode node, int operationCode){
		JFrame frame = new JFrame(name);
		JTree tree = new JTree(node);
		TreeUtility.expandJTree(tree, -1);
		frame.getContentPane().add(tree);
		frame.pack();
		frame.setVisible(true);
	}
	
	
	public static JComponent createShowCaseForApk(StaticApp app, boolean enableBlackList){
		/*Top level panel*/
		final JSplitPane topSpliter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		final JSplitPane leftSubSpliter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JPanel detailPanel = new JPanel();
		topSpliter.setLeftComponent(leftSubSpliter);
		topSpliter.setRightComponent(detailPanel);
		
		/*Data sector*/
		SymbolicExecution se = new SymbolicExecution(app);
		final Map<String, DefaultListModel<StaticMethod>> classToMethodModelMap = new HashMap<String, DefaultListModel<StaticMethod>>();
		final Map<String, JComponent> methodDetailMap = new HashMap<String, JComponent>();
//		Map<String, JText>
		
		
		/*class list and method list*/
		final JList<StaticClass> classList = new JList<StaticClass>();
		final JList<StaticMethod> methodList = new JList<StaticMethod>();
		final JScrollPane classListWrapper = new JScrollPane();
		final JScrollPane methodListWrapper = new JScrollPane();
		classListWrapper.setViewportView(classList);
		methodListWrapper.setViewportView(methodList);
		leftSubSpliter.setLeftComponent(classListWrapper);
		leftSubSpliter.setRightComponent(methodListWrapper);

		/*list model setup*/
		DefaultListModel<StaticClass> classModel = new DefaultListModel<StaticClass>();
		for(StaticClass cls : app.getClasses()){
			if(enableBlackList && Blacklist.classInBlackList(cls.getDexName()))continue;
			classModel.addElement(cls);
			cls.getFields();
			
			DefaultListModel<StaticMethod> methodModel = new DefaultListModel<StaticMethod>();
			for(StaticMethod method : cls.getMethods()){
				if(enableBlackList && Blacklist.methodInBlackList(method.getSignature())) continue;
				methodModel.addElement(method);
				String methodSig = method.getSignature();
				List<PathSummary> sums = se.doFullSymbolic(methodSig);
				JComponent methodDetail = createRawSummaryPanel(sums);
				methodDetailMap.put(methodSig, methodDetail);
			}
			classToMethodModelMap.put(cls.getJavaName(), methodModel);
		}
		
		/*class list setup*/
		classList.setModel(classModel);
		classList.setBorder(BorderFactory.createTitledBorder("Class List"));
		classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		classList.setCellRenderer(new DefaultListCellRenderer() {
		    @Override
		    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        if(! (value instanceof StaticClass))throw new AssertionError();
		        StaticClass cls = (StaticClass) value;
		        int pos = cls.getJavaName().lastIndexOf("/");
		        if(pos < 0){ label.setText(cls.getJavaName());
		        }else{ label.setText(cls.getJavaName().substring(pos+1, cls.getJavaName().length())); }
		        return label;
		    }
		});
		classList.addListSelectionListener(new ListSelectionListener(){
			StaticClass previousClass = null;
			@Override
			public void valueChanged(ListSelectionEvent e) {
				StaticClass current = classList.getSelectedValue();
				if(current != null && current != previousClass){
					String key = current.getJavaName();
					DefaultListModel<StaticMethod> model = classToMethodModelMap.get(key);//should not be null
					methodList.setModel(model);
					previousClass = current;
				}
			}
		});
		
		/*method list setup*/
		methodList.setBorder(BorderFactory.createTitledBorder("Method List"));
		methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		methodList.setCellRenderer(new DefaultListCellRenderer() {
		    @Override
		    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        StaticMethod method = (StaticMethod) value;
		        String sig = method.getSignature();
		        int pos = sig.lastIndexOf("->");
		        if(pos < 0){ label.setText(sig);
		        }else{ label.setText(sig.substring(pos+2, sig.length())); }
		        return label;
		    }
		});
		methodList.addListSelectionListener(new ListSelectionListener(){
			StaticMethod previousClass = null;
			@Override
			public void valueChanged(ListSelectionEvent e) {
				StaticMethod current = methodList.getSelectedValue();
				if(current != null && current != previousClass){
					String key = current.getSignature();
					JComponent component = methodDetailMap.get(key);
					topSpliter.setRightComponent(component);
					previousClass = current;
				}
			}
		});
		
		System.out.println("class size: "+classModel.size());
		return topSpliter;
	}
	
	public static JComponent createRawSummaryPanel(List<PathSummary> list){
		DefaultListModel<PathSummary> model_raw = new DefaultListModel<PathSummary>();
		for(PathSummary sum : list){
			model_raw.addElement(sum);
		}
		
		final JList<PathSummary> leftList = new JList<PathSummary>();
//		leftList.setPreferredSize(new Dimension());
		leftList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		leftList.setModel(model_raw);
		leftList.setCellRenderer(new DefaultListCellRenderer() {
		    @Override
		    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        label.setText("PathSummary #"+index);
		        return label;
		    }
		});
		JScrollPane leftScroll = new JScrollPane();
		leftScroll.setViewportView(leftList);
		
		
		final JPanel rightSummaryContainer = new JPanel();
		rightSummaryContainer.setAlignmentX(0);
		BoxLayout layout = new BoxLayout(rightSummaryContainer, BoxLayout.Y_AXIS);
		rightSummaryContainer.setLayout(layout);
		JScrollPane rightContainer = new JScrollPane();
		rightContainer.setViewportView(rightSummaryContainer);
		
		leftList.addListSelectionListener(new ListSelectionListener(){
			PathSummary previous = null;
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting()){
				
					PathSummary sum = leftList.getSelectedValue();
					if(sum!= null && previous != sum){
						previous = sum;
						rightSummaryContainer.removeAll();
						rightSummaryContainer.add(new JLabel("Signature:"+sum.getMethodSignature()));					
						
						String toShow = String.join("\n", sum.getExecutionLog());
						JTextArea area = new JTextArea();
						JScrollPane areaScroll = new JScrollPane();
						areaScroll.setSize(500, 200);
						areaScroll.setViewportView(area);
						area.setText(toShow);
						rightSummaryContainer.add(areaScroll);
						
						rightSummaryContainer.add(new JLabel(String.format("Constraints: ")));
						for(Expression expre : sum.getPathConditions()){
							rightSummaryContainer.add(new JLabel("<html>"+expre.toYicesStatement()+"</html>"));
							JTree tree = new JTree(expre);
							JScrollPane treeScroll = new JScrollPane();
							treeScroll.setViewportView(tree);
							rightSummaryContainer.add(treeScroll);
							TreeUtility.expandJTree(tree, -1);
						}
						rightSummaryContainer.add(new JLabel(String.format("Constraints variables: ")));
//						for(Expression expre : sum.getPathConditions()){
//							Set<Variable> vars = expre.getUniqueVarSet();
//							for(Variable var : vars){
//								JLabel varLabel = new JLabel(var.toVariableDefStatement());
//								rightSummaryContainer.add(varLabel);
//							}
//						}
						
						rightSummaryContainer.add(new JLabel(String.format("Symbolic states:")));
						for(Expression expre : sum.getSymbolicStates()){
							rightSummaryContainer.add(new JLabel("<html>"+expre.toYicesStatement()+"</html>"));
							JTree tree = new JTree(expre);
							JScrollPane treeScroll = new JScrollPane();
							treeScroll.setViewportView(tree);
							rightSummaryContainer.add(treeScroll);
							TreeUtility.expandJTree(tree, -1);
						}
						rightSummaryContainer.add(new JLabel(String.format("Symbolic variables: ")));
//						for(Expression expre : sum.getSymbolicStates()){
//							Set<Variable> vars = expre.getUniqueVarSet();
//							for(Variable var : vars){
//								JLabel varLabel = new JLabel(var.toVariableDefStatement());
//								rightSummaryContainer.add(varLabel);
//							}
//						}
						rightSummaryContainer.revalidate();
					}
				}
			}
		});
		
		final JSplitPane topSpliter = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				leftScroll, rightContainer );
        topSpliter.setOneTouchExpandable(true);
        topSpliter.setDividerLocation(150);
		
		return topSpliter;
	}
	
	public static List<JComponent> createComponentsForPathSummary(PathSummary sum){
		List<JComponent> list = new ArrayList<JComponent>();
		list.add(new JLabel("Signature:"+sum.getMethodSignature()));					
		
		String toShow = String.join("\n", sum.getExecutionLog());
		JTextArea area = new JTextArea();
		JScrollPane areaScroll = new JScrollPane();
		areaScroll.setSize(500, 200);
		areaScroll.setViewportView(area);
		area.setText(toShow);
		list.add(areaScroll);
		
		list.add(new JLabel(String.format("Constraints: ")));
		for(Expression expre : sum.getPathConditions()){
			list.add(new JLabel("<html>"+expre.toYicesStatement()+"</html>"));
			JTree tree = new JTree(expre);
			JScrollPane treeScroll = new JScrollPane();
			treeScroll.setViewportView(tree);
			list.add(treeScroll);
			TreeUtility.expandJTree(tree, -1);
		}
		list.add(new JLabel(String.format("Constraints variables: ")));
//		for(Expression expre : sum.getPathConditions()){
//			Set<Variable> vars = expre.getUniqueVarSet();
//			for(Variable var : vars){
//				JLabel varLabel = new JLabel(var.toVariableDefStatement());
//				list.add(varLabel);
//			}
//		}
		
		list.add(new JLabel(String.format("Symbolic states:")));
		for(Expression expre : sum.getSymbolicStates()){
			list.add(new JLabel("<html>"+expre.toYicesStatement()+"</html>"));
			JTree tree = new JTree(expre);
			JScrollPane treeScroll = new JScrollPane();
			treeScroll.setViewportView(tree);
			list.add(treeScroll);
			TreeUtility.expandJTree(tree, -1);
		}
		list.add(new JLabel(String.format("Symbolic variables: ")));
//		for(Expression expre : sum.getSymbolicStates()){
//			Set<Variable> vars = expre.getUniqueVarSet();
//			for(Variable var : vars){
//				JLabel varLabel = new JLabel(var.toVariableDefStatement());
//				list.add(varLabel);
//			}
//		}
		return list;
	}
}
