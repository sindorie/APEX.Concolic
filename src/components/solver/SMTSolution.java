package components.solver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.tree.TreeNode;

import apex.symbolic.Expression;
import components.ExpressionTranfomator;
import support.Logger;

public class SMTSolution {
	static int index = 0;
	static String 
		CHECK_START = "=========Checking===========",
		CHECK_END	= "===========Done=============";
	
	/**
	 * Given the outputs from the solver, check satisfaction
	 * @param results
	 * @return
	 */
	public boolean checkSatifaction(List<String>[] results){
		for(String st : results[0]){//0 should be stdout
			st = st.trim();
			if(st.equals("sat")) return true;
		}
		return false;
	}

	/**
	 * Check any variable expression exists in the cumulative constraints
	 * @param cumulatives
	 * @param syms
	 * @return
	 */
	public boolean isRelated(Collection<Expression> cumulatives, Map<Expression,Expression> syms){
		if(syms == null || syms.isEmpty()) return false;
		Set<Expression> vars = syms.keySet();
		for(Expression expre : vars){
			Logger.trace(expre.toYicesStatement());
		}
		for(Expression expre : this.getVarSet(cumulatives)){
			Logger.trace(expre.toYicesStatement());
		}
		for(Expression cum : cumulatives){
			for(Expression var : vars){
				if(cum.contains(var)) return true;
			}
		}
		return false;
	}
	
	public boolean isComplete(Collection<Expression> cumulativeExp){
		Set<Expression> result = new HashSet<Expression>();
		for(Expression expre : cumulativeExp){
			Set<Expression> vars = getVarSet(expre);
			result.addAll(vars);
		}
		for(Expression expre :result){
			//there exists at least one variable requiring solving
			if(expre.getChildCount() < 3) return false; 
		}
		return true;
	}
	
	/**
	 * Remove the expression where no variable exists
	 * @param cumulativeExp
	 */
	public void trim(Collection<Expression> cumulativeExp){
		List<Expression> toRemove = new ArrayList<Expression>();
		for(Expression expre : cumulativeExp){
			if(getVarSet(expre).size() == 0){
				toRemove.add(expre);
			}
		}
		cumulativeExp.removeAll(toRemove);
	}
	
	public Set<Expression> cloneAndBuildCumulativeConstraints(
			Collection<Expression> current, 
			Map<Expression,Expression> syms,
			Collection<Expression> cons){
		
		Set<Expression> clone = new HashSet<Expression>();
		for(Expression cur : current){ clone.add(cur.clone()); }
		for(Entry<Expression, Expression> sym : syms.entrySet()){
			Expression var = sym.getKey();
			Expression val = sym.getValue();
			for(Expression curCloned : clone){ curCloned.replace(var, val.clone()); }
		}
		for(Expression con : cons){ clone.add(con.clone()); }
		return clone;
	}
	
	
	public void updateCumulativeConstraints(Collection<Expression> current, List<Expression> syms, Collection<Expression> con){
		for(Expression sym : syms){
			Expression var = (Expression) sym.getChildAt(0);
			Expression val = (Expression) sym.getChildAt(1);
			for(Expression cur : current){
				cur.replace(var, val);
			}
		}
		current.addAll(con);
	}
	
	/**
	 * Expression structure
	 * 
	 * #Var 
	 * 	|
	 * 	+ Id
	 * 	+ Type
	 * 	+ ... //other data if existed
	 * 
	 */
	public Set<Expression> getVarSet(Expression constraint){
		final Set<Expression> varSet = new HashSet<Expression>();
		support.TreeUtility.breathFristSearch(constraint, new support.TreeUtility.Searcher() {
			@Override
			public int check(TreeNode node) {
				Expression expre = (Expression) node;
				String content = expre.getContent();
				if(content.equals(ExpressionTranfomator.VAR_TAG)){
					varSet.add(expre.clone());
					return support.TreeUtility.Searcher.SKIP;
				}
				return support.TreeUtility.Searcher.NORMAL;
			}
		});
		return varSet;
	}
	
	public Set<Expression> getVarSet(Collection<Expression> input){
		Set<Expression> result = new HashSet<Expression>();
		for(Expression expre : input){
			result.addAll(getVarSet(expre));
		}
		return result;
	}
	
	public int getNonSyntheticVarCount(Collection<Expression> input){
		Set<Expression> result = new HashSet<Expression>();
		for(Expression expre : input){
			Set<Expression> vars = getVarSet(expre);
			result.addAll(vars);
		}
		int count = 0;
		for(Expression expre :result){
			//there exists at least one variable requiring solving
			if(expre.getChildCount() < 3) count += 1;
		}
		return count;
	}
	
	public List<String> buildScript(Collection<Expression> constraints){
		//find all the variables
		// 1. create declaration statements
		// 2. create get-value statements
		final Set<String> varSmts = new HashSet<String>();
		final Set<String> getSmts = new HashSet<String>();
		for (Expression expre : constraints) {
			support.TreeUtility.breathFristSearch(expre, new support.TreeUtility.Searcher() {
				@Override
				public int check(TreeNode node) {
					Expression currentNode = (Expression) node;
					String content = currentNode.getContent();

					if (content.equals(ExpressionTranfomator.VAR_TAG)) {
						Expression idTag = (Expression) currentNode.getChildAt(0);
						Expression typeTag = (Expression) currentNode.getChildAt(1);

						// (declare-fun varName () type )
						String degSmt = "(declare-fun " + idTag.getContent() + " () " + typeTag.getContent() + ")";
						varSmts.add(degSmt);
						if(typeTag.getContent().startsWith("(Array ")){
							//TODO ignored for now
						}else{
							getSmts.add("(get-value ("+idTag+"))");
						}
					}
					return support.TreeUtility.Searcher.NORMAL;
				}
			});
		}

		// create assertion statements
		final Set<String> assertionSmts = new HashSet<String>();
		for (Expression expre : constraints) {
			String smt = recursiveBuildSmt(expre);
			assertionSmts.add("(assert " + smt + " )");
		}

		// assemble into list
		final List<String> statements = new ArrayList<String>();
		statements.add("(set-logic ALL_SUPPORTED)");
		statements.add("(set-option :produce-models true)");
		for (String varSmt : varSmts) {
			statements.add(varSmt);
		}
		for (String assertion : assertionSmts) {
			statements.add(assertion);
		}
		statements.add("(echo "+CHECK_START+")");
		statements.add("(check-sat)");
		statements.add("(echo "+CHECK_END+")");
		for (String valSmt : getSmts) {
			statements.add(valSmt);
		}
		return statements;
	}
	
	
	/**
	 * @param constraints -- expression statements which will be turned into scripts
	 * @return 
	 * @throws IOException 
	 */
	public File buildTmpSciptFile(List<String> statements) throws IOException{
		File tmpScript = File.createTempFile("Script#"+(index++), null);
		PrintWriter pw = new PrintWriter(tmpScript);
		for(String smt :statements){
			pw.println(smt);
		}
		pw.close();
		tmpScript.deleteOnExit();
		return tmpScript;
	}
	

	//TODO to improve
	/**
	 * Use pre-order traversal to build statement
	 * Assume the structure is correct and local transformation is not needed
	 * @param expre
	 * @return
	 */
	String recursiveBuildSmt(Expression expre){
		String content = expre.getContent();
		if(content.equals(ExpressionTranfomator.VAR_TAG)){ //considered as end node
			Expression idNode = (Expression) expre.getChildAt(0);
			return idNode.getContent();
		}else if(expre.isLeaf()){
			return content;
		}else{
			StringBuilder sb = new StringBuilder("(").append(content).append(" ");
			for(int i = 0 ; i<expre.getChildCount(); i++){
				Expression child = (Expression) expre.getChildAt(i);
				String smt = recursiveBuildSmt(child);
				sb.append(smt).append(" ");
			}
			sb.append(")");
			return sb.toString();
		}
	}
	
}
