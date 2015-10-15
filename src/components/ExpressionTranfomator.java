package components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apex.Common;
import apex.symbolic.Expression;


/**
 * Assumption
 * 	Pointer comparison is not taken into consideration. 
 * 	e.g. obj1 == obj2 
 * 	One-dimension array
 * 
 * Implementation Note
 * 	Assignment field_arr1 = field_arr2 => field_arr1_SZ = field_arr2_SZ
 * 	Assignment field_arr1 = tmpArr# => field_arr1_SZ = tmpArr#_SZ
 * 	Solution:
 * 	Upon process $array, some additional expressions will be generated (pending ones)
 * 	Upon processing "=", the pending ones  if exists will be used to complete the assignment chain
 * 
 * TODO and check List 
 * 	Check what else needs to be labeled. Var from $array is labeled
 * 	Pending expression generation mechanism needs to be better understood
 * 	And the use of pending ones needs to further improvement
 * 
 * 	Due to the repetitive clone during the transformation, the memory consumption seems to be O(n^2)
 * 
 * @author zhenxu
 *
 */
public class ExpressionTranfomator {

	public final static List<Expression> UNIDENTIFIER_LIST = new ArrayList<>();
	
	/**
	 * If the current work is performed on symbolics
	 */
	private boolean isSymbolic, printed = false;
	
	private List<Expression> 
		resultedSymbolics = new ArrayList<Expression>(), 
		resultedConstraints = new ArrayList<Expression>();
	
	/**
	 * Reason this is needed
	 * a. currently exception is not taken into consideration
	 * b. arr[index] where index >=0 and index < arr.length must be met
	 * 	in order to let the solver work properly
	 * Pending expression is used by nodes on upper level
	 * 	e.g. 
	 * 		field <- Array
	 * 					+ id
	 * 					+ sz -- could be a tree
	 */
	private transient List<Expression> 
		complementalSymbolics = new ArrayList<Expression>(),
		complementalConstraints = new ArrayList<Expression>(),
		pendingExpression = new ArrayList<Expression>();
	
	/**
	 * transform the given constraints and symbolics
	 * @param constraints
	 * @param symbolics
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Expression>[] transform(List<Expression> constraints, List<Expression> symbolics){
		ExpressionTranfomator tranformator = new ExpressionTranfomator();
		tranformator.work(constraints, symbolics);
		return new List[]{tranformator.resultedConstraints, tranformator.resultedSymbolics};
	}
	
	/**
	 * Work on the constraints and symbolics to create transformed constraints
	 * and symbolics
	 * @param consraints
	 * @param symbolics
	 */
	private void work(List<Expression> constrtaints, List<Expression> symbolics){
		if(constrtaints != null){
			isSymbolic = false;
			for(Expression con : constrtaints){
				complementalSymbolics.clear();
				complementalConstraints.clear();
				pendingExpression.clear();
				
				Expression transformed = recusiveBuildDispatcher(con);
				if(transformed == null) continue;
				resultedConstraints.add(transformed);
				
				resultedSymbolics.addAll(complementalSymbolics);
				resultedConstraints.addAll(complementalConstraints);
			}
		}
		
		if(symbolics != null){
			isSymbolic = true;
			for(Expression sym : symbolics){
				complementalSymbolics.clear();
				complementalConstraints.clear();
				pendingExpression.clear();
					
				Expression transformed = recusiveBuildDispatcher(sym);
				if(transformed == null) continue;
				resultedSymbolics.add(transformed);
				
				resultedSymbolics.addAll(complementalSymbolics);
				resultedConstraints.addAll(complementalConstraints);
			}
		}
	}
	
	
	public Expression recusiveBuildDispatcher(Expression input){
		if(input == null) return null;
		Expression result = null;
		try{
			result = internal_dispatcher(input);
		}catch(Exception e){ Common.TRACE(input.toYicesStatement()); throw e; }
		return result;
	}
	
	
	/**
	 * Recursive transform the tree. 
	 * Play as a dispatcher
	 * @param input
	 * @return 
	 */
	private Expression internal_dispatcher(Expression input){
		String content = input.getContent().trim();
		Expression result = null;
		if(content.equalsIgnoreCase(KW_FINSTANCE) ||
			content.equalsIgnoreCase(KW_FSTATIC) ){
			result = formateField(input);
		}else if(content.equalsIgnoreCase(KW_API)){
			result = processAPI(input);
		}else if(content.equalsIgnoreCase(KW_THIS)){
			//should not be encountered under here
			result = null;
		}else if(content.equalsIgnoreCase(KW_ARRAY)){
			result = processArray(input);
		}else if(content.equalsIgnoreCase(KW_ARRAY_LENGTH)){
			result = processArrayLength(input);
		}else if(content.equalsIgnoreCase(KW_CONST_STRING)){
			result = processConstString(input);
		}else if(content.equalsIgnoreCase(KW_NEWINSTANCE)){
			result = processNewInstance(input);
		}else if(content.equalsIgnoreCase(KW_RETURN)){
			//should not occur
//			return null;
		}else if(content.equals(KW_NULL)){
			//currently ignore it TODO
			result = null;
		}else if(content.matches(tmpVairableMatcher)){
		}else if(input.isLeaf()){
			//TODO may want to check content
			result = input.clone();
		}else{
			String mappedOperator = operatorMap.get(content);
			if(mappedOperator != null){
				result = processOperator(mappedOperator, input);
			}
		};
		
		if(result == null){
//			System.out.println("Unkown: '"+ content+"'"+input.toYicesStatement());
			this.showError(input);
		}
		return result; //invalid case
	}
	
	Expression processNewInstance(Expression input){
		Expression type = (Expression) input.getChildAt(0);
		String content = type.getContent();
		Expression newNode = null;
		if(content.equals(STRING_CLZ)){
			//assume no child TODO
			newNode = new Expression("\"\"");
		}else if(content.equals(STRINGBUILDER_CLZ)){
			//assume no child TODO
			newNode = new Expression("\"\"");
		}
		return newNode;
	}
	
	Expression processArrayLength(Expression input){
		/** assume only field variable possible **/
		Expression expre = (Expression) input.getChildAt(0);
		Expression field = formateField(expre);
		if(field == null){ return null; }
		return findCorrespondingArraySizeVar(field);
	}
	
	Expression processArray(Expression input){
		/**
		 * $array
		 * 	+ size
		 * 	+ type
		 */
		Expression sizeNode = (Expression) input.getChildAt(0);
		Expression typeNode = (Expression) input.getChildAt(1);
		
		String type = typeNode.getContent();
		String mappedType = typeMap.get(type);
		//TODO assume one dimension array
		if(mappedType == null){
			return null;
		}
		
		Expression newNode = new Expression(VAR_TAG);
		String content = "tmpVarArr"+(arrIndex++);
		newNode.add(new Expression(content));
		newNode.add(new Expression("(Array Int "+mappedType+")"));
		newNode.add(new Expression(SYNTHESIS));//label as synthetic variable
		
		//create the size symbolic
		Expression szRight_sym = internal_dispatcher(sizeNode);
		if(szRight_sym == null) return null;
		Expression szLeft_sym = findCorrespondingArraySizeVar(newNode);
		Expression sizeAssignmentSym = new Expression("=");
		sizeAssignmentSym.add(szLeft_sym);
		sizeAssignmentSym.add(szRight_sym);
		
		pendingExpression.add(sizeAssignmentSym);
		return newNode;
	}
	
	/**
	 * 
	 * @param operator -- the mapped operator
	 * @param input	-- the node containing the original content
	 * @return
	 */
	Expression processOperator(String operator, Expression input){
		if(		operator.equals("+") || 
				operator.equals("-") || 
				operator.equals("*") || 
				operator.equals("/") ){
			/*Binary Arithmetic operation*/
			return standardBinaryOperation(operator, input);
		}else if(operator.equals("and") ||
				operator.equals("or") ||
				operator.equals("xor") ){
			/*Binary Boolean operation*/
			Expression op1 = (Expression) input.getChildAt(0);
			Expression op2 = (Expression) input.getChildAt(1);
			
			String content_op1 = op1.getContent().trim();
			String content_op2 = op2.getContent().trim();
			Expression formated_op1, formated_op2;
			
			if(content_op1.equals("1")){
				formated_op1 = new Expression("true");
			}else if(content_op1.equals("0")){
				formated_op1 = new Expression("false");
			}else{
				formated_op1 = internal_dispatcher(op1);
			}
			
			if(content_op2.equals("1")){
				formated_op2 = new Expression("true");
			}else if(content_op2.equals("0")){
				formated_op2 = new Expression("false");
			}else{
				formated_op2 = internal_dispatcher(op2);
			}
			
			if(formated_op1 == null || formated_op2 == null) return null;
			Expression newNode = new Expression(operator);
			newNode.add(formated_op1);
			newNode.add(formated_op2);
			return newNode;
		}else if(operator.equals("not")){
			/*Unary Boolean operation*/
			Expression op1 = (Expression) input.getChildAt(0);
			String content_op1 = op1.getContent().trim();
			Expression formated_op1;
			
			if(content_op1.equals("1")){
				formated_op1 = new Expression("true");
			}else if(content_op1.equals("0")){
				formated_op1 = new Expression("false");
			}else{
				formated_op1 = internal_dispatcher(op1);
			}
			
			if(formated_op1 == null) return null;
			Expression newNode = new Expression(operator);
			newNode.add(formated_op1);
			return newNode;
		}else if(operator.equals("<") ||
				operator.equals(">") ||
				operator.equals("<=") ||
				operator.equals(">=") ){
			/*Comparison operation*/
			return standardBinaryOperation(operator, input);
		}else if(operator.equals("=") || 
				operator.equals("distinct")){ //distinct is the same as /=
			/*Assignment or Equal*/
			if(input.getParent() != null) return null; //The input node should be the root
			
			Expression left_input = (Expression) input.getChildAt(0);
			/**
			 * Assignment -> array assignment, size assignment may be created
			 * 				 boolean assignment, 1/0 on left should be converted into true/false
			 * 
			 * =
			 * 	+ var 
			 * 		+ id
			 * 		+ type (array/boolean)
			 * 	+ val (could be a tree)
			 * 
			 * 
			 * Array equals? -> does not expect to see this
			 * 	Pointer comparison is not taken into consideration. 
			 */
			Expression transformed = standardBinaryOperation(operator, input);
			if(transformed == null) return null;
			
			Expression t_leftNode = (Expression) transformed.getChildAt(0);
			Expression t_rightNode = (Expression) transformed.getChildAt(1);
			
			if(t_leftNode.getContent().equals(VAR_TAG)){
				String t_leftVar_type = ((Expression)t_leftNode.getChildAt(1)).getContent().trim();
				//TODO boolean array is ignored, one dimension array only
				if(t_leftVar_type.startsWith("(Array")){
					/**
					 * This indicates the right assignment should also be an array type
					 * 	Create the expected array_SZ id and try to find it in the pending ones
					 * 	Create the assignment chain properly and accordingly
					 */
					
					//the variable might be in store/select chunk
					Expression locatedRightVar = recursiveFindArrayVar(t_rightNode);
					
					if(locatedRightVar != null){
						//the new expression on the left
						Expression szLeftVar = findCorrespondingArraySizeVar(t_leftNode);
						Expression szRightVar = findCorrespondingArraySizeVar(locatedRightVar);
						String idSz = ((Expression)szRightVar.getChildAt(0)).getContent().trim();
						
						Expression foundPended = null;
						for(Expression pended : this.pendingExpression){
							if(!pended.getContent().equals("=")) continue;
							
							/**
							 * pended:
							 * =
							 * 	+ var
							 * 	+ var
							 */
							Expression pended_left = (Expression) pended.getChildAt(0);
							Expression pended_left_id = (Expression) pended_left.getChildAt(0);
							if(pended_left_id.getContent().trim().equals(idSz)){
								foundPended = pended;
								break;
							}
						}
						
						Expression generatedExpression = null;
						if(foundPended == null){//no match
							generatedExpression = new Expression("=");
							generatedExpression.add(szLeftVar);
							Expression newSzNode = new Expression(VAR_TAG);
							newSzNode.add(new Expression(idSz));
							newSzNode.add(new Expression(TYPE_INT));
							generatedExpression.add(newSzNode);
						}else{
							foundPended.remove(0);
							foundPended.insert(szLeftVar, 0);
							generatedExpression = foundPended;
						}
						
						if(this.isSymbolic){
							this.complementalSymbolics.add(generatedExpression);
						}else{
							this.complementalConstraints.add(generatedExpression);
						}
					}
				}else if(t_leftVar_type.equals(TYPE_BOOL)){ //boolean variable
						String rightContent = t_rightNode.getContent().trim();
					if(rightContent.equals("1")){
						t_rightNode.setUserObject("true");
					}else if(rightContent.equals("0")){
						t_rightNode.setUserObject("false");
					}
				}
			}else if(left_input .getContent().equals(KW_API)){
				Expression methodSig = (Expression) left_input.getChildAt(0);
				String mSig = methodSig.getContent().trim();
				if(mSig.endsWith("Z")){//the return type is boolean, then check right
					String rightContent = t_rightNode.getContent().trim();
					if(rightContent.equals("0")){
						t_rightNode.setUserObject("false");
					}else if(rightContent.equals("1")){
						t_rightNode.setUserObject("true");
					}
				}
			}
			return transformed;
		}else if(operator.equals("store")){
			/*Array element store operation*/
			/**
			 * Input structure
			 * 	update
			 * 		+ arr
			 * 		+ ()
			 * 			+ index
			 * 		+ value
			 * 
			 * Afterwards
			 * 	store
			 * 		+ arr
			 * 		+ index
			 * 		+ value
			 * 
			 * Complemental Constraints
			 * 	index >= 0 
			 * 	index < arr.length
			 */
			
			Expression arr = (Expression) input.getChildAt(0);
			Expression index = (Expression) input.getChildAt(1).getChildAt(0);
			Expression val = (Expression) input.getChildAt(2);
			
			Expression formated_arr = internal_dispatcher(arr);
			Expression formated_index = internal_dispatcher(index);
			Expression formated_val = internal_dispatcher(val);
			
			if(formated_arr==null ||  formated_index==null || formated_val==null){
				return null;
			}
			
			createComplementalConstraints(formated_arr, formated_index);
			
			Expression newNode = new Expression(operator);
			newNode.add(formated_arr);
			newNode.add(formated_index);
			newNode.add(formated_val);
			return newNode;
		}else if(operator.equals("select")){
			/*Array element select operation*/
			/**
			 * input structure:
			 * 	$aget
			 * 		+ arr
			 * 		+ index
			 * 
			 * Complemental consraints
			 * 	index >= 0
			 * 	index < arr.length
			 */
			Expression secltionNode = standardBinaryOperation(operator, input);
			if(secltionNode == null) return null;
			
			Expression arr = (Expression) secltionNode.getChildAt(0);
			Expression index = (Expression) secltionNode.getChildAt(1); 
			createComplementalConstraints(arr, index);
			
			return secltionNode;
		}
		System.out.println("Unkown operator:"+operator);
		return null;
	}
	
	/**
	 * select arr index 
	 * store arr index element
	 * 	=> index>=0 && index<arr.length
	 * Such constraints are deposited into complemented constraints
	 * @param formated_arr
	 * @param formated_index
	 */
	void createComplementalConstraints(Expression formated_arr, Expression formated_index){
		//create the complemental constraints
		if(formated_arr.getChildCount() < 3){ //non-synthetic variable
			Expression lowerBound = new Expression(">=");
			lowerBound.add(formated_index.clone());
			lowerBound.add(new Expression("0"));
			Expression upperBound = new Expression("<");
			upperBound.add(formated_index.clone());
			upperBound.add(findCorrespondingArraySizeVar(formated_arr));
			this.complementalConstraints.add(lowerBound);
			this.complementalConstraints.add(upperBound);
		}else{
			//find the upper bound in the pending ones
			Expression lowerBound = new Expression(">=");
			lowerBound.add(formated_index.clone());
			lowerBound.add(new Expression("0"));
			this.complementalConstraints.add(lowerBound);
			
			Expression upperContent = null;
			Expression szVar = findCorrespondingArraySizeVar(formated_arr);
			for(Expression pended : pendingExpression){
				Expression left = (Expression) pended.getChildAt(0);
				if(left.equals(szVar)){
					upperContent = (Expression) pended.getChildAt(1);
					break;
				}
			}
			
			if(upperContent != null){
				Expression upperBound = new Expression("<");
				upperBound.add(formated_index.clone());
				upperBound.add(upperContent.clone());
				this.complementalConstraints.add(upperBound);
			}
		}
	}
	
	Expression findCorrespondingArraySizeVar(Expression transformed_arr){
		Expression arrIdNode = (Expression) transformed_arr.getChildAt(0);
		String arrId = arrIdNode.getContent()+ARRAY_SZ_POSTFIX;
		Expression newNode = new Expression(VAR_TAG);
		newNode.add(new Expression(arrId));
		newNode.add(new Expression(TYPE_INT));
		return newNode;
	}
	
	Expression recursiveFindArrayVar(Expression start){
		String content = start.getContent().trim();
		if(content.equals(VAR_TAG)){
			return start;
		}else if(content.equals(KW_SELECT)){
			return recursiveFindArrayVar((Expression)start.getChildAt(0));
		}else if(content.equals(KW_STORE)){
			return recursiveFindArrayVar((Expression)start.getChildAt(0));
		}
		return null;
	}
	
	Expression standardBinaryOperation(String operator, Expression input){
		Expression newNode = new Expression(operator);
		Expression op1 = (Expression) input.getChildAt(0);
		Expression op2 = (Expression) input.getChildAt(1);
		
		Expression formated_op1 = internal_dispatcher(op1);
		Expression formated_op2 = internal_dispatcher(op2);
		if(formated_op1 == null || formated_op2 == null){
			return null;
		}
		
		newNode.add(formated_op1);
		newNode.add(formated_op2);
		return newNode;
	}
	
	Expression processConstString(Expression input){
		Expression expre = (Expression) input.getChildAt(0);
		String content = expre.getContent();
		if(content == null || content.isEmpty()){
			return new Expression("");
		}else{
			return expre.clone();
		}
	}
	
	/**
	 * 
	 * $API
	 * 	+inovke StringBuilder->append(Z)StringBuilder
	 *  +StringBuilder or ""
	 *  +String
	 * 
	 * @param input
	 * @return
	 */
	Expression processAPI(Expression input){
		//assume the current node is $API
		Expression methodSig = (Expression) input.getChildAt(0);
		//invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String; --old version
		//Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
		String content = methodSig.getContent().trim();
		//'invoke {v#...' 'Class' 'method' 'arguments' 'return'
//		String[] decoded = content.split("},\\s|(->)|\\(|\\)");
		String[] decoded = content.split("->|\\(|\\)");
//		System.out.println(content+"  :  "+Arrays.toString(decoded));
		Pattern argP = Pattern.compile("((L[a-zA-Z0-9\\$_\\/]*;)|[ZBSCIJFD])\\[*");
		Matcher argM = argP.matcher(decoded[2]);
		List<String> argTypes = new ArrayList<String>();
		while(argM.find()){ argTypes.add(argM.group().trim()); }

//		System.out.println(input.toYicesStatement());
//		System.out.println(Arrays.toString(decoded));
		//check StringBuilder API
		String clz = decoded[0].trim();
		String method = decoded[1].trim();
		String type = decoded[3].trim();
//		System.out.println(type);
		
//		System.out.println("clz: "+clz);
//		System.out.println("method: "+method);
//		System.out.println("return type: "+type);
//		System.out.println("Arguments: "+argTypes);
		
		if(clz.equalsIgnoreCase(STRINGBUILDER_CLZ)){
			//can only deal with a few
			//append -> str.++
			if(method.equals("append")){
				Expression fStr = (Expression) input.getChildAt(1);
				Expression sStr = (Expression) input.getChildAt(2);
				
				//check if the first string is "" -- local special case
				String fContent = fStr.getContent().trim();
				String sContent = sStr.getContent().trim();
				
				
				Expression strAppend = new Expression("str.++");
				Expression str1 = internal_dispatcher(fStr);
				if(str1 == null) return null;
				strAppend.add(str1);
				if(type.equals("I")){
					/**
					 * str.++
					 * 	+ str1
					 *	+ int.to.str
					 *		+ int
					 */
					Expression strP = new Expression("int.to.str");
					Expression str2 = internal_dispatcher(sStr);
					if(str2 == null) return null;
					strP.add(str2);
					strAppend.add(strP);
				}else if(type.equals("Z")){
					/**
					 * Append(Z)
					 * 	+ StringBuilder 
					 * 	+ 1/0 <- turn it into true/false
					 * 
					 * Append(Z)
					 * 	+ StringBuilder
					 * 	+ API/Finstance <- no special treatment
					 */
					if(sContent.equals("1")){
						strAppend.add(new Expression("\"true\""));
					}else if(sContent.equals("0")){
						strAppend.add(new Expression("\"false\""));
					}else{
						Expression str2 = internal_dispatcher(sStr);
						if(str2 == null) return null;
						Expression ifStmt = buildIfStmt(str2, 
								new Expression("\"true\""),new Expression("\"false\""));
						strAppend.add(ifStmt);
					}
				}else if(type.equals(STRING_CLZ)||
						type.equals(STRINGBUILDER_CLZ) ){
					Expression str2 = internal_dispatcher(sStr);
					strAppend.add(str2);
					if(str2 == null) return null;
				}else{
					//other type is not supported
					return null;
				}
				
				if(fContent.equals("\"\"")){
					return (Expression) strAppend.getChildAt(1);
				}else{ return strAppend; }
			}else if(method.equals("toString")){
				Expression obj = (Expression) input.getChildAt(1);
				return internal_dispatcher(obj);
			}
		}else if(clz.equalsIgnoreCase(STRING_CLZ)){
			if(method.equals("contains")){
				//TODO improvement include regular expression
				Expression container = (Expression) input.getChildAt(1);
				Expression containee = (Expression) input.getChildAt(2);
				
				Expression transformedContainer = internal_dispatcher(container);
				Expression transformedContainee = internal_dispatcher(containee);
				if(transformedContainer == null || transformedContainee == null) return null;
				
				
				Expression newNode = new Expression("str.contains");
				newNode.add(transformedContainer);
				newNode.add(transformedContainee);
				return newNode;
			}else if(method.equals("subString")){
				Expression obj = (Expression) input.getChildAt(1);
				Expression startIndex = (Expression) input.getChildAt(2);
				
				Expression t_obj = internal_dispatcher(obj);
				Expression t_startIndex = internal_dispatcher(startIndex);
				if(t_obj == null || t_startIndex == null) return null;
				
				switch(argTypes.size()){
				case 1:{ //subString(i);
					Expression newNode = new Expression("str.substr");
					newNode.add(t_obj);
					newNode.add(t_startIndex);
					Expression sLen = new Expression("-");
					Expression objLen = new Expression("str.len");
					objLen.add(t_obj);
					sLen.add(objLen);
					sLen.add(t_startIndex);
					newNode.add(sLen);
					
					return newNode;
				}
				case 2:{ //subString(i, j);
					Expression endIndex = (Expression) input.getChildAt(3);
					Expression t_endIndex = internal_dispatcher(endIndex);
					if(t_endIndex == null)return null;
					
					Expression newNode = new Expression("str.substr");
					newNode.add(t_obj);
					newNode.add(t_startIndex);
					newNode.add(t_endIndex);
					return newNode;
				}
				}
				
				//should not reach here
			}else if(method.equalsIgnoreCase("startsWith")){
//				(str.prefixof s t)
				Expression obj = (Expression) input.getChildAt(1);
				Expression prefix = (Expression) input.getChildAt(2);
				
				Expression t_obj = internal_dispatcher(obj);
				Expression t_prefix = internal_dispatcher(prefix);
				
				if(obj == null || t_prefix == null) return null;
				Expression newNode = new Expression("str.prefixof");
				newNode.add(t_obj);
				newNode.add(t_prefix);
				return newNode;
			}else if(method.equalsIgnoreCase("endsWith")){
				Expression obj = (Expression) input.getChildAt(1);
				Expression postfix = (Expression) input.getChildAt(2);
				
				Expression t_obj = internal_dispatcher(obj);
				Expression t_postfix = internal_dispatcher(postfix);
				
				if(obj == null || t_postfix == null) return null;
				Expression newNode = new Expression("str.suffixof");
				newNode.add(t_obj);
				newNode.add(t_postfix);
				return newNode;
			}else if(method.equalsIgnoreCase("length")){
//				System.out.println(input.toYicesStatement());
				
				Expression obj = (Expression) input.getChildAt(1);
				Expression t_obj = internal_dispatcher(obj);
				if(t_obj == null)return null;
				
				Expression newNode = new Expression("str.len");
				newNode.add(t_obj);
				return newNode;
			}
		}
		
//		support.Logger.trace(Arrays.toString(decoded) + "#"+argTypes);
		return null;
	}
	
	Expression buildIfStmt(Expression condition, Expression choice1,Expression choice2){
		Expression newNode = new Expression("ite");
		newNode.add(condition);
		newNode.add(choice1);
		newNode.add(choice2);
		return newNode;
	}
	
	
	/**
	 * VarTag
	 * 	+ id
	 * 	+ type
	 * @param expre
	 * @return
	 */
	Expression formateField(Expression expre){
		String[] id_type = retrieveFiledId(expre);
		if(id_type == null){
//			support.Logger.trace(expre.toYicesStatement());
			return null;
		}
		
		Expression var = new Expression(VAR_TAG);
		var.add(new Expression(formateString(id_type[0]))); //id
		//find the accepted type 
		String type = id_type[1].trim();
		int arrIndex = type.lastIndexOf('[');
		if(arrIndex >= 0 ){ // array type
			int dim = arrIndex + 1;
			String elementType = type.substring(arrIndex+1, type.length());
			String mappedType = typeMap.get(elementType);
			if(mappedType == null){
//				support.Logger.trace(mappedType);
				return null; //unknown element type 
			}
			String arrType = recursiveBuildArrayType(dim, mappedType);
			type = arrType;
		}else{
			type = typeMap.get(type); 
		}
		
		if(type == null){
//			support.Logger.trace(id_type[1]);
			return null;
		}
		var.add(new Expression(type)); //type
		return var;
	}
	
	
	/**
	 * Build the array type for solver
	 * Z[[[ -> (Array Int (Array Int (Array Int Bool )))
	 * @param dim
	 * @param type
	 * @return
	 */
	
	String recursiveBuildArrayType(int dim, String type){
		if(dim < 1){
			return type;
		}else{
			return "(Array Int "+recursiveBuildArrayType(dim-1, type)+" )";
		}
	}
	
	
	/**
	 * FieldInstance
	 * 	+ parent_object_type -> field_id : type  
	 *  + parent_object_instance
	 *  
	 * FieldStatic
	 * 	+ parent_object_type -> field_id : type
	 * 
	 * @param expre
	 * @return {ID, type}
	 */
	String[] retrieveFiledId(Expression expre){
		String content = expre.getContent();
		if(content.equalsIgnoreCase(KW_FINSTANCE)){
			Expression sig = (Expression) expre.getChildAt(0);
			Expression obj = (Expression) expre.getChildAt(1);
			
			String sigContent = sig.getContent().trim();
			String[] decodedSig = sigContent.split("(->)|:");//parent_obj->field_id:type
			String field_id = decodedSig[1].trim();
			String field_type = decodedSig[2].trim();
			String[] parentID_type = retrieveFiledId(obj);
			
			String combinedId = parentID_type[0].trim() + field_id;
			return new String[]{combinedId, field_type};
//			return parentObjId+"#"+field_id;
		}else if(content.equals(KW_FSTATIC)){
			Expression sig = (Expression) expre.getChildAt(0);
			String sigContent = sig.getContent();
			String[] decodedSig = sigContent.split("(->)|:");//parent_obj->field_id:type
			String field_id = decodedSig[1].trim();
			String field_type = decodedSig[2].trim();
			return new String[]{field_type+"#"+field_id, field_type};
		}else if(content.equals(KW_THIS)){
			Expression child = (Expression) expre.getChildAt(0);
			return new String[]{child.getContent(),child.getContent()};
		}
		return null; // Not supposed to happen if it is field instance or static field
	}

	void replaceNodeInParent(Expression node, Expression content){
		Expression parent = (Expression) node.getParent();
		int index = parent.getIndex(node);
		parent.insert(content	, index);
	}
	
	
	static String formateString(String input){
		return input.replaceAll("[^0-9a-zA-Z]", "/").trim(); // replace all non word
	}
	
	public final static int SYMBOLIC_INDEX = 1, CONSTRAINT_INDEX = 0;
	private static int arrIndex = 0;
	
	
	public final static String 
	VAR_TAG = 				"#Var",
	SYNTHESIS = 			"syn",
	tmpVairableMatcher = 	"\\$?[vp]\\d+$",
	FilterString = 			"[:\\;\\$\\-\\>\\/\\\\]",
	ARRAY_SZ_POSTFIX = 		"_SZ",
	
//	META_TYPE = "TYPE",
	TYPE_BOOL =		"Bool",
	TYPE_INT =		"Int",
	TYPE_REAL =		"Real",
	TYPE_STRING =	"String",
	STRING_CLZ = 		"Ljava/lang/String;",
	STRINGBUILDER_CLZ = "Ljava/lang/StringBuilder;",
			
	KW_FINSTANCE = 		"$Finstance",
	KW_FSTATIC = 		"$Fstatic",
	KW_NEWINSTANCE = 	"$new-instance",
	KW_CONST_STRING = 	"$const-string",
	KW_CONST_CLASS = 	"$const-class",
	KW_INSTANCEOF = 	"$instance-of",
	KW_ARRAY = 			"$array",
	KW_RETURN = 		"$return",
	KW_API = 			"$api",
	KW_THIS = 			"$this",
	KW_ARRAY_LENGTH =	"$array-length",
	KW_AGET = 			"$aget",
	KW_UPDATE = 		"update",
	KW_STORE = 			"store",
	KW_SELECT = 		"select",
	KW_NULL =			"null"
	;
	
	static Map<String,String> typeMap = new HashMap<String,String>();
	static Map<String,String> operatorMap = new HashMap<String,String>();
	static int objIndex = 0;
	static{
		String[] typePairs = {
				"Z",TYPE_BOOL,
				"B",TYPE_INT,
				"S",TYPE_INT,
				"C",TYPE_INT,
				"I",TYPE_INT,
				"J",TYPE_INT,
				"F",TYPE_REAL,
				"D",TYPE_REAL,

				"Bool", TYPE_BOOL,
				"Int", 	TYPE_INT,
				"Real",	TYPE_INT,
				
				"boolean",	TYPE_BOOL,
				"char",		TYPE_INT,
				"short",	TYPE_INT,
				"int", 		TYPE_INT,
				"long",		TYPE_INT,
				"double",	TYPE_REAL,
				"float",	TYPE_REAL,
				
				STRINGBUILDER_CLZ,		TYPE_STRING,
				STRING_CLZ,				TYPE_STRING,
			};	
		for(int i=0;i<typePairs.length;i+=2){
			typeMap.put(typePairs[i], typePairs[i+1]);
		}
		String[] operatorPairs = {
				"add", "+",
				"sub", "-",
				"mul", "*",
				"div", "/",
				"and", "and",
				"or", "or",
				"xor", "xor",
				"neg", "not",  
				"<","<",
				">",">",
				"=","=",
				"==","=",
				"/=","distinct",
				"!=","distinct",
				"<=","<=",
				">=",">=",
				
				//special array
				"update","store",
				"$aget","select",
				
		};
		for(int i =0;i<operatorPairs.length;i+=2){
			operatorMap.put(operatorPairs[i], operatorPairs[i+1]);
		}
	}
	
	private void showError(Expression current){
		if(printed == false){
			UNIDENTIFIER_LIST.add(current.clone());
			printed = true;
		}
	}
}
