package components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
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

import components.solver.SMTSolution;

import support.Logger;
import support.TreeUtility; 
import support.Utility;
import apex.symbolic.Expression;
import apex.symbolic.PathSummary;

public class WrappedSummary implements Serializable {
	
	private static boolean enableGUI = true;
	public static DefaultListModel<PathSummary> model_raw;
	public static DefaultListModel<WrappedSummary> mode_wrap;
	private static JComponent rawSummaryPanel, wrappedSumPanel;
	static{
		model_raw = new DefaultListModel<PathSummary>();
		mode_wrap = new DefaultListModel<WrappedSummary>();
		
		if(enableGUI){
			rawSummaryPanel = createRawSummaryPanel();
			wrappedSumPanel = createWrappedSummaryPanel();
			Logger.registerJPanel("Raw summary", rawSummaryPanel);
			Logger.registerJPanel("wrapped summary", wrappedSumPanel);
		}
	}
	
	public final Map<Expression,Expression> symbolicStates = new HashMap<Expression,Expression>();
	public final List<Expression> constraints = new ArrayList<Expression>();
	public final List<String> executionLog = new ArrayList<String>();
	public final PathSummary summaryReference;
	public final String methodSignature;
	public boolean isExecuted = false;
	
	public WrappedSummary(PathSummary summary){
		summaryReference = summary;
//		if(summary.getPathConditions() != null)
//		for(Expression expre : summary.getPathConditions()){
//			System.out.println("CON: "+expre.toYicesStatement());
//		}
//		if(summary.getSymbolicStates() != null)
//		for(Expression expre : summary.getSymbolicStates()){
//			System.out.println("SYM: "+expre.toYicesStatement());
//		}
		
		this.preprocessExecutionLog(summary.getExecutionLog());
		this.preprocessExpression(summary.getPathConditions(), summary.getSymbolicStates());
		this.methodSignature = summary.getMethodSignature();

		model_raw.addElement(summary);
		mode_wrap.addElement(this);
	}
	
	public static List<PathSummary> unwrapSummary(List<WrappedSummary> wsList){
		List<PathSummary> result = new ArrayList<PathSummary>();
		for(WrappedSummary wsum : wsList){
			result.add(wsum.summaryReference);
		}
		return result;
	}
	
	public static List<WrappedSummary> wrapSummaryList(List<PathSummary> sums){
		if(sums == null) return null;
		
		List<WrappedSummary> sumList = new ArrayList<WrappedSummary>();
		for(PathSummary sum : sums){
			sumList.add(new WrappedSummary(sum));
		}
		return sumList;
	}
	
	@Override
	public boolean equals(Object o){
		if( o instanceof WrappedSummary){
			WrappedSummary sum = (WrappedSummary)o;
			if(this.executionLog == null){
				return sum.executionLog == null;
			}else{
				return this.executionLog.equals(sum.executionLog);
			}
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		//TODO may not be considered as correct in the future
		return this.executionLog.size();
	}
	
	@Override
	public String toString(){
		return this.executionLog.toString();
	}
	
	public String toFormatedString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.methodSignature+"\n");
		SMTSolution solution = new SMTSolution();
		
		sb.append("Symbolics:\n");
		for(Entry<Expression,Expression> entry : symbolicStates.entrySet()){
			sb.append("\t");
			sb.append(entry.getKey().toYicesStatement()).append(" = ");
			sb.append(entry.getValue().toYicesStatement()).append("\n");
		}
		sb.append("Variables In Symbolics:\n");
		for(Entry<Expression,Expression> entry : symbolicStates.entrySet()){
			sb.append("\t");
			sb.append(entry.getKey().toYicesStatement()).append("\n");
//			sb.append("\t");
//			sb.append(entry.getValue()).append("\n");
		}
		
		sb.append("Constraints:\n");
		for(Expression expre : constraints){
			sb.append("\t");
			sb.append(expre.toYicesStatement()).append("\n");
		}
		sb.append("Variables in Constraints:\n");
		
		for(Expression expre : solution.getVarSet(constraints)){
			sb.append("\t");
			sb.append(expre.toYicesStatement()).append("\n");
		}
		
		sb.append("Execution Log:\n");
		sb.append("\t");
		sb.append(Utility.join("\n", this.executionLog)).append("\n");
		
		return sb.toString();
	}
	
//	private void preprocessConstraint(List<Expression> cons){
//		this.constraints.addAll(ExpressionTranfomator.transform(cons));
//	}
//	
//	private void preprocessSymbolicStates(List<Expression> inputs){
//		List<Expression> syms = ExpressionTranfomator.transform(inputs);
//		for(Expression symPair : syms){
//			if(symPair.getChildCount() != 2){
//				System.out.println("preprocess Symbolic States with "+symPair.toYicesStatement());
//				continue;
//			}
//			Expression left = (Expression)symPair.getChildAt(0);
//			Expression right = (Expression)symPair.getChildAt(1);
//			this.symbolicStates.put(left.clone(), right.clone());
//		}
//	}
	
	
	private void preprocessExpression(List<Expression> cons, List<Expression> syms){
		List<Expression> list[] = ExpressionTranfomator.transform(cons, syms);
		this.constraints.addAll(list[ExpressionTranfomator.CONSTRAINT_INDEX]);
		for(Expression symPair : list[ExpressionTranfomator.SYMBOLIC_INDEX]){
			if(symPair.getChildCount() != 2){
				System.out.println("preprocess Symbolic States with "+symPair.toYicesStatement());
				continue;
			}
			Expression left = (Expression)symPair.getChildAt(0);
			Expression right = (Expression)symPair.getChildAt(1);
			this.symbolicStates.put(left.clone(), right.clone());
		}
	}
	
	private void preprocessExecutionLog(List<String> logs){
		this.executionLog.addAll(logs);
	}
	
	
	private static JComponent createRawSummaryPanel(){
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
						
						String toShow = Utility.join("\n", sum.getExecutionLog());
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
	
	private static JComponent createWrappedSummaryPanel(){
		final JList<WrappedSummary> wSumList = new JList<WrappedSummary>();
		wSumList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		wSumList.setModel(mode_wrap);
		wSumList.setCellRenderer(new DefaultListCellRenderer() {
		    @Override
		    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        label.setText("Wrapped PathSummary #"+index);
		        return label;
		    }
		});
		JScrollPane leftScroll = new JScrollPane();
		leftScroll.setViewportView(wSumList);
		
		final JPanel rightSummaryContainer = new JPanel();
		rightSummaryContainer.setAlignmentX(0);
		BoxLayout layout = new BoxLayout(rightSummaryContainer, BoxLayout.Y_AXIS);
		rightSummaryContainer.setLayout(layout);
		JScrollPane rightContainer = new JScrollPane();
		rightContainer.setViewportView(rightSummaryContainer);
		
		wSumList.addListSelectionListener(new ListSelectionListener(){
			WrappedSummary previous = null;
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting()){
				
				WrappedSummary sum = wSumList.getSelectedValue();
				if(sum!= null && previous != sum){
					previous = sum;
					rightSummaryContainer.removeAll();
					
					rightSummaryContainer.add(new JLabel("Signature:"+sum.methodSignature));					
					
					String toShow = Utility.join("\n", sum.executionLog);
					JTextArea area = new JTextArea();
					JScrollPane areaScroll = new JScrollPane();
					areaScroll.setViewportView(area);
					areaScroll.setSize(500, 200);
					area.setText(toShow);
					rightSummaryContainer.add(areaScroll);
					
					rightSummaryContainer.add(new JLabel(String.format("Constraints: ")));
					for(Expression expre : sum.constraints){
						rightSummaryContainer.add(new JLabel("<html>"+expre.toYicesStatement()+"</html>"));
						JTree tree = new JTree(expre);
						JScrollPane treeScroll = new JScrollPane();
						treeScroll.setViewportView(tree);
						rightSummaryContainer.add(treeScroll);
						TreeUtility.expandJTree(tree, -1);
					}
					rightSummaryContainer.add(new JLabel(String.format("Constraints variables: ")));
//					for(Expression expre : sum.constraints){
//						Set<Variable> vars = expre.getUniqueVarSet();
//						for(Variable var : vars){
//							JLabel varLabel = new JLabel(var.toVariableDefStatement());
//							rightSummaryContainer.add(varLabel);
//						}
//					}
					
					rightSummaryContainer.add(new JLabel(String.format("Symbolic states:")));
					Set<Variable> symVarSet = new HashSet<Variable>();
					for(Entry<Expression,Expression> entry : sum.symbolicStates.entrySet()){
						Expression variable = entry.getKey();
						Expression value = entry.getValue();
						rightSummaryContainer.add(
								new JLabel("<html>"+variable.toYicesStatement()+" = "+value.toYicesStatement()+"</html>")
							);
//						symVarSet.addAll(variable.getUniqueVarSet());
//						symVarSet.addAll(value.getUniqueVarSet());
						JTree tree = new JTree(value);
						JScrollPane treeScroll = new JScrollPane();
						treeScroll.setViewportView(tree);
						rightSummaryContainer.add(treeScroll);
						TreeUtility.expandJTree(tree, -1);
					}
					
					rightSummaryContainer.add(new JLabel(String.format("Symbolic variables: ")));
					for(Variable expre : symVarSet){
						JLabel varLabel = new JLabel(expre.toVariableDefStatement());
						rightSummaryContainer.add(varLabel);
					}
					
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
}
