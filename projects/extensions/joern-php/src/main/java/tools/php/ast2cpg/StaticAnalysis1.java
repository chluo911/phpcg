package tools.php.ast2cpg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ast.ASTNode;
import ast.expressions.ArgumentList;
import ast.expressions.CallExpressionBase;
import ast.expressions.StringExpression;
import ast.expressions.Variable;
import ast.php.functionDef.FunctionDef;
import cg.PHPCGFactory;
import cg.ParseVar;
import inputModules.csv.csv2ast.ASTUnderConstruction;
import misc.MultiHashMap;

public class StaticAnalysis1 {
	//public static Stack<Long> path = new Stack<Long>();
	public static Set<Stack<Long>> vulpaths = new HashSet<Stack<Long>>();
	public static List<String> source = Arrays.asList(new String[] {"_GET", "_POST", "_COOKIE", "_REQUEST", "_ENV", "HTTP_ENV_VARS", "HTTP_POST_VARS", "HTTP_GET_VARS"});
	//public static Stack<Long> context = new Stack<Long>();
	
	public static void init() {
		for(Long sink: PHPCGFactory.sinks) {
			Stack<Long> path = new Stack<Long>();
			Stack<Long> context = new Stack<Long>();
			path.add(sink);
			taintAnalysis(sink, path, context);
		}
	}
	
	public static void taintAnalysis(Long taint, Stack<Long> path, Stack<Long> context) {
		//vulnerable path
		if(isSource(taint)) {
			vulpaths.add(path);
			context.pop();
			return;
		}
		//update context
		Long func = ASTUnderConstruction.idToNode.get(taint).getFuncId();
		Long previousFunc = context.peek();
		//returned from a function call 
		if(PHPCGFactory.mtd2mtd.containsKey(previousFunc) &&
				PHPCGFactory.mtd2mtd.get(previousFunc).contains(func)) {
			context.push(func);
		}
		//different function
		else if(func!=previousFunc) {
			while(!context.isEmpty()) {
				Long tmp = context.pop();
				if(tmp == func) {
					break;
				}
			}
			context.push(func);
		}
		//update path
		path.push(taint);
		//get all the next related id of the taint node
		HashSet<Long> nextIds = findNext(ASTUnderConstruction.idToNode.get(taint), context);
		//recursively analyze taint node, deep first strategy
		for(Long nextId: nextIds) {
			Stack<Long> saveContext = (Stack<Long>)(context);
			taintAnalysis(nextId, path, saveContext);
		}
		//restore the path
		path.pop();
	}

	//Parse the expression
	/*
	 * return next node Id if the type is a variable, return value from function call, or a parameter
	 * return identity if the type is class property, 
	 */
	private static HashSet<Long> findNext(ASTNode taintNode, Stack<Long> context) {
		
		//check weather the taint represents a class, if so, get the next node from identity
		Long prtNode = PHPCSVEdgeInterpreter.child2parent.get(taintNode.getNodeId());
		//the variable is a return value
		if(ASTUnderConstruction.idToNode.get(prtNode).getProperty("type").equals("AST_RETURN")) {
			Long funcId = taintNode.getFuncId();
			//if the function returns a class, we return any properties of this class
			if(PHPCGFactory.retCls.containsKey(funcId)) {
				String classId = PHPCGFactory.retCls.get(funcId).toString();
				String identity = classId+"::*";
				getNextFromIdentity(identity);
			}
		}
		
		//evaluate the expression
		ParseVar tmp = new ParseVar();
		tmp.init(0, false, "");
		LinkedList<String> vars = tmp.ParseExp(taintNode);
		for(String var: vars) {
			if(var.startsWith("$")) {
				try {
					Long varId = Long.parseLong(var.substring(1, var.length() - 1));
					ASTNode varNode = ASTUnderConstruction.idToNode.get(varId);
					String type = varNode.getProperty("type");
					//it is a variable, the next node id should be in the same function
					if(type.equals("AST_VAR")) {
						HashSet<Long> ret = new HashSet<Long>();
						ParseVar parsevar = new ParseVar();
						parsevar.init(-1, false, "");
						Set<String> introValue = parsevar.IntroDataflow(varNode.getNodeId());
						for(String value: introValue) {
							try {
								Long valueId = Long.parseLong(value.substring(1, var.length() - 1));
								ret.add(valueId);
							} catch (Exception e) {
							}
						}
						return ret;
					}
					//if it is a class property, get the next id from identity
					else if(type.equals("AST_STATIC_PROP") || type.equals("AST_PROP")) {
						String identity = ParseVar.parseInterRelation(varNode);
						getNextFromIdentity(identity);
					}
					//if it is a return value, return the return node of callee
					else if(type.equals("AST_CALL") || type.equals("AST_METHOD_CALL") || type.equals("AST_STATIC_CALL") || type.equals("AST_NEW")) {
						HashSet<Long> ret = new HashSet<Long>();
						
						if(PHPCGFactory.call2mtd.containsKey(varId)) {
							//get the callee
							Set<Long> tarFunc = new HashSet<Long>(PHPCGFactory.call2mtd.get(varId));
							for(Long func: tarFunc) {
								FunctionDef funcNode = (FunctionDef) ASTUnderConstruction.idToNode.get(func);
								//get the return node Id
								if(ParseVar.func2Ret.containsKey(funcNode.getNodeId())) {
									for(Long retId: ParseVar.func2Ret.get(funcNode.getNodeId())) {
										ret.add(retId);
									}
								}
							}
						}
						return ret;
					}
					//if it is a parameter, return the argument of the caller
					else if(type.equals("AST_PARAM")) {
						HashSet<Long> ret = new HashSet<Long>();
						ASTNode param = (FunctionDef) ASTUnderConstruction.idToNode.get(varId);
						//get the function node
						Long prtId = param.getFuncId();
						//if the function is called by a previous function call, then that function must be the only caller
						Long onlyCaller = (long) -1;
						if(context.size()>1) {
							Long tmpVal = context.pop();
							onlyCaller = context.peek();
							context.push(tmpVal);
						}
						
						if(PHPCGFactory.mtd2call.containsKey(prtId)) {
							Set<Long> callers = new HashSet<Long>(PHPCGFactory.mtd2call.get(prtId));
							Long paramList = PHPCSVEdgeInterpreter.child2parent.get(varId);
							//get the parameter Id
							int paramId;
							for(paramId=0; paramId<PHPCSVEdgeInterpreter.parent2child.get(paramList).size(); paramId++) {
								//find the location of parameter
								if(PHPCSVEdgeInterpreter.parent2child.get(paramList).get(paramId).equals(varId)) {
									//System.err.println("Param Id: "+paramId);
									break;
								}
							}
							//get the caller
							for(Long caller: callers) {
								CallExpressionBase callsite = (CallExpressionBase) ASTUnderConstruction.idToNode.get(caller);
								ArgumentList arg = callsite.getArgumentList();
								//get the function caller locates
								Long func = callsite.getFuncId();
								//if the function returns from a previous call, then the caller must be the previous call, otherwise, return all possible caller argument
								if(onlyCaller==-1 || onlyCaller==func) {
									ret.add(func);
								}
								
							}
						}
					}
				} catch(Exception e) {
					
				}
			}
		}
		
		return null;
	}

	private static void getNextFromIdentity(String identity) {
		// TODO Auto-generated method stub
		
	}

	//check if taint is source 
	private static boolean isSource(Long taint) {
		//check if it is the source
		if(ASTUnderConstruction.idToNode.get(taint).getProperty("type").equals("AST_VAR") &&
				((Variable) ASTUnderConstruction.idToNode.get(taint)).getNameExpression() instanceof StringExpression) {
			String taintName = ((StringExpression) ((Variable) ASTUnderConstruction.idToNode.get(taint)).getNameExpression()).getEscapedCodeStr();
			if(source.contains(taintName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean checkGlobalorProperty(Long taint) {
		//check if the taint is a class property
		ASTNode taintNode = ASTUnderConstruction.idToNode.get(taint);
		if(taintNode.getProperty("type").equals("AST_PROP") ||
				taintNode.getProperty("type").equals("AST_STATIC_PROP")) {
			return true;
		}
		//check if the taint is a return value;
		Long prtNode = PHPCSVEdgeInterpreter.child2parent.get(taint);
		if(ASTUnderConstruction.idToNode.get(prtNode).getProperty("type").equals("AST_RETURN")) {
			Long funcId = taintNode.getFuncId();
			//the sink is a class
			if(PHPCGFactory.retCls.containsKey(funcId)) {
				return true;
			}
		}
		return false;
	}
}
