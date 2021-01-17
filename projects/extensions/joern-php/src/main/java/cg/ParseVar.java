package cg;

import tools.php.ast2cpg.PHPCSVEdgeInterpreter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import ast.ASTNode;
import ast.expressions.BinaryOperationExpression;
import ast.expressions.CallExpressionBase;
import ast.expressions.Constant;
import ast.expressions.Expression;
import ast.expressions.Identifier;
import ast.expressions.NewExpression;
import ast.expressions.PropertyExpression;
import ast.expressions.StaticPropertyExpression;
import ast.expressions.StringExpression;
import ast.expressions.Variable;
import ast.php.expressions.ArrayExpression;
import ast.php.statements.GlobalStatement;
import ddg.DataDependenceGraph.DDG;
import inputModules.csv.csv2ast.ASTUnderConstruction;
import misc.MultiHashMap;
import ast.php.functionDef.*;

public class ParseVar {
	private  long varId;
	//type of function name variable must be String
	private Set<String> varValue;
	private  boolean isClass;
	private  String crtglobal;
	private  Set<Long> includeRet; 
	private  HashMap<Long, Set<String>> cache = new HashMap<Long, Set<String>>();
	private  Set<Long> involvedIds;
	
	public void init(long varId, boolean isclass, String global){
		this.varId = varId;
		//flush return value for the new varId
		varValue = new LinkedHashSet<String>();
		involvedIds = new HashSet<Long>();
		cache = new HashMap<Long, Set<String>>();
		isClass = isclass;
		includeRet = new HashSet<Long>(); 
		//System.err.println(isClass);
		crtglobal = global;
	}
	
	public Set<String> getVar() {
		//store value for each variable we have parsed
		if(!cache.containsKey(varId)) {
			cache.put(varId, varValue);
		}
			
		return varValue;
	}
	
	public void handle() {
		long rootId = varId;
		String varName = "";
		//System.err.println(varId);
		
		//If we have parsed the variable before, we save its value and get it from cache. 
		if(cache.containsKey(varId)) {
			varValue = cache.get(varId);
			return;
		}
		
		involvedIds.add(varId);
		//System.err.println(varId);
		//get variable's name
		long childId = PHPCSVEdgeInterpreter.parent2child.get(varId).get(0);
		ASTNode child = ASTUnderConstruction.idToNode.get(childId);
		//property fetch
		if(child.getProperty("type").equals("AST_VAR")) {
			childId = PHPCSVEdgeInterpreter.parent2child.get(childId).get(0);
			child = ASTUnderConstruction.idToNode.get(childId);
			//System.err.println(child.getEscapedCodeStr());
			if(!child.getEscapedCodeStr().equals("this")) {
				childId = PHPCSVEdgeInterpreter.parent2child.get(varId).get(1);
				child = ASTUnderConstruction.idToNode.get(childId);
			}
			varName = child.getEscapedCodeStr();
		}
		//static property fetch
		else if(child.getProperty("type").equals("AST_NAME")) {
			//System.err.println(varId);
			if(ASTUnderConstruction.idToNode.get(varId).getProperty("type").equals("AST_CONST")) {
				return;
			}
			String clsName = ((Identifier) child).getNameChild().getEscapedCodeStr();
			String mtdName = ASTUnderConstruction.idToNode.get(PHPCSVEdgeInterpreter.parent2child.get(varId).get(1)).getEscapedCodeStr();
			varName = clsName+"::"+mtdName;
		}
		//variable, child is AST_STRING type
		else if(child.getProperty("type").equals("string")) {
			varName = child.getEscapedCodeStr();
		}
		
		
		//get root node of the statement that variable belongs to.
		while (!DDG.Locate.containsKey(String.valueOf(rootId)+"_"+varName)
				&& !(ASTUnderConstruction.idToNode.get(varId) instanceof PropertyExpression)) {
			if(PHPCSVEdgeInterpreter.child2parent.containsKey(rootId)) {
				rootId = PHPCSVEdgeInterpreter.child2parent.get(rootId);
				//get node's type and make sure we stop before AST_STMT_LIST
				String rootType =  ASTUnderConstruction.idToNode.get(rootId).getProperty("type");
				if (rootType.equals("AST_STMT_LIST")) {
					//rootId = lastId;
					varValue.add(String.valueOf(varId));
					return;
				}
			}
			else {
				varValue.add(String.valueOf(varId));
				return;
			}
		}
		
		//get source root node
		Queue<Long> getSourceId = new LinkedList<Long>();
		getSourceId.offer(rootId);
		Set<Long> sourceRootId = getSource(getSourceId, varName);
		sourceRootId.remove(rootId);
		
		if(sourceRootId.isEmpty()) {
			varValue.add(String.valueOf(varId));
			return;
		}
		
			
		//get source node in that source root node.
		Iterator<Long> it = sourceRootId.iterator();
		while (it.hasNext()) {
			Long root = it.next();
			ParseStatement(root, varName);
	    }
	}
	
	//get source data from source data's statement.
	/*
	 * We need to parse different types of root node, currently this tool supports:
	 * 1. AST_ASSIGN
	 * 
	 */
	private void ParseStatement(Long Stmtid, String var) {
		String rootType =  ASTUnderConstruction.idToNode.get(Stmtid).getProperty("type");
		ASTNode expNode = new ASTNode();
		Set<String> expValue;
		
		switch (rootType) {
		case "AST_ASSIGN":
		case "AST_ASSIGN_REF":
			//System.err.println(varId+" "+Stmtid);
			//Long exp = PHPCSVEdgeInterpreter.parent2child.get(Stmtid).get(1);
			//System.err.println(varId+" "+Stmtid+" "+exp);
			//expNode = ASTUnderConstruction.idToNode.get(exp);
			expNode = ASTUnderConstruction.idToNode.get(Stmtid).getChild(1);
			if(expNode==null) {
				break;
			}
			expValue = new LinkedHashSet<String>(ParseExp(expNode)); 
			varValue.addAll(expValue);
			break;
		case "AST_STATIC_PROP":
		case "AST_PROP":
		case "AST_VAR":
			//System.err.println("!! "+Stmtid+" "+varId);
			varValue.add(Stmtid.toString());
			break;
		//exit when meet a param.
		case "AST_PARAM":
			if(isClass==true) {
				Long typeId = PHPCSVEdgeInterpreter.parent2child.get(Stmtid).get(0);
				expNode = ASTUnderConstruction.idToNode.get(typeId);
				expValue = new HashSet<String>();
				if(!expNode.getProperty("type").equals("AST_NAME")) {
					varValue.add(Stmtid.toString());
				}
				else {
					String typeName = expNode.getChild(0).getEscapedCodeStr();
					expValue.add(typeName);
					String namespace = ASTUnderConstruction.idToNode.get(Stmtid).getEnclosingNamespace();
					Long clsId = PHPCGFactory.getClassId(expValue.iterator().next(), Stmtid, namespace);
					Set<Long> cldIds = PHPCGFactory.getAllChild(clsId);
					//System.err.println(expValue.iterator().next()+" "+namespace+" "+cldIds);
					for(Long ldId: cldIds) {
						ASTNode cld = ASTUnderConstruction.idToNode.get(ldId);
						if(cld.getEnclosingNamespace() != null) {
							expValue.add(cld.getEnclosingNamespace()+"\\"+cld.getEscapedCodeStr());
						}
						else {
							expValue.add(cld.getEscapedCodeStr());
						}
					}
					involvedIds.add(Stmtid);
					varValue.addAll(expValue);
				}
			}
			else 
				varValue.add(Stmtid.toString());
			break;
		case "AST_EXPR_LIST":
			varValue.add(Stmtid.toString());
			break;
		case "AST_STATIC":
			expNode = ASTUnderConstruction.idToNode.get(Stmtid).getChild(1);
			expValue = new HashSet<String>(ParseExp(expNode)); 
			varValue.addAll(expValue);
			break;
		case "AST_UNSET":
			break;
		case "AST_GLOBAL":
			expNode = ASTUnderConstruction.idToNode.get(Stmtid);
			if(((GlobalStatement)expNode).getVariable().getNameExpression() instanceof StringExpression) {
				String varName = ((GlobalStatement)expNode).getVariable().getNameExpression().getEscapedCodeStr();
				if(varName.equals(crtglobal)) {
					break;
				}
				crtglobal = varName;
				if(PHPCGFactory.globalMap.containsKey(varName)) {
					for(Long assignId: PHPCGFactory.globalMap.get(varName)) {
						if(assignId==null) {
							continue;
						}
						ASTNode valueNode = ASTUnderConstruction.idToNode.get(assignId).getChild(1);
						//ParseVar parsevar = new ParseVar();
						//parsevar.init(1, false);
						varValue.addAll(ParseExp(valueNode));
					}
					if(!varValue.isEmpty()) {
						break;
					}
				}
			}
			crtglobal = "";
			varValue.add(expNode.getNodeId().toString());
			break;
		default:
			varValue.add(Stmtid.toString());
			//System.err.println("Unsopport root type: "+rootType+Stmtid+" "+varId);
		}
	}
	
	//get source data from source data's expression.
	@SuppressWarnings("unchecked")
	public LinkedList<String> ParseExp(ASTNode expNode){
		String expType = expNode.getProperty("type");
		LinkedList<String> expValue = new LinkedList<String>();
		//System.err.println(expNode+" "+varId);
		//System.err.println(expNode);
		Long nodeId;
		
		//get value of variables from cache 
		if(cache.containsKey(expNode.getNodeId())) {
			expValue.addAll(cache.get(expNode.getNodeId()));
			return expValue;
		}
		//involvedIds.add(expNode.getNodeId());
		
		//analyze different types of exp root type
		switch (expType) {
			case "AST_CONDITIONAL":
				if(expNode.getChild(1)!=null) {
					expValue.addAll(ParseExp(expNode.getChild(1)));
				}
				if(expNode.getChild(2)!=null) {
					expValue.addAll(ParseExp(expNode.getChild(2)));
				}
				break;
			case "AST_ASSIGN":
				if(expNode.getChild(1) instanceof Variable || 
						expNode.getChild(1) instanceof PropertyExpression) {
					ParseVar parse = new ParseVar();
					parse.init(expNode.getChild(1).getNodeId(), isClass, crtglobal);
					parse.handle();
					Set<String> parseVar = parse.getVar();
					for(String str: parseVar) {
						expValue.add(str);
						//System.err.println(str);
					}
					parse.reset();
				}
				else {
					expValue.addAll(ParseExp(expNode.getChild(1)));
				}
				break;
			case "AST_MAGIC_CONST":
				String magicFlags = expNode.getFlags();
				switch(magicFlags) {
					case "MAGIC_CLASS":
						expValue.add(expNode.getEnclosingClass());
						break;
					case "MAGIC_FUNCTION":
						expValue.add((((FunctionDef) ASTUnderConstruction.idToNode.get(expNode.getFuncId())).getName()));
						break;
					case "MAGIC_DIR":
						String path = PHPCGFactory.getDir(expNode.getNodeId());
						File A = new File(path);
						File parentFolder = new File(A.getParent());
						try {
							expValue.add(parentFolder.getCanonicalPath());
							break;
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						break;
					case "MAGIC_FILE":
						String path1 = PHPCGFactory.getDir(expNode.getNodeId());
						expValue.add(path1);
						break;
					default:
						expValue.add(expNode.getNodeId().toString());
				}
				break;
			case "AST_STATIC_PROP":
			case "AST_PROP":
			case "AST_VAR":
				//System.err.println(expNode);
				ParseVar parse = new ParseVar();
				parse.init(expNode.getNodeId(), isClass, crtglobal);
				parse.handle();
				Set<String> parseVar = parse.getVar();
				for(String str: parseVar) {
					expValue.add(str);
					//System.err.println("!! "+str+" "+varId);
				}
				//System.err.println(parseVar);
				parse.reset();
				break;
			case "AST_CONST":
				Identifier constNode = ((Constant)expNode).getIdentifier();
				String constValue = constNode.getNameChild().getEscapedCodeStr();
				if(constValue.equals("null")) {
					break;
				}
				else {
					expValue.add(constValue);
				}
				break;
			case "AST_NEW":
				if(isClass==true)
					expValue.addAll(ParseExp(expNode.getChild(0)));
				else 
					expValue.add(expNode.getNodeId().toString());
				break;
			case "AST_BINARY_OP":
				if(isClass==false) {
					/*
					if(expNode.getFlags().equals("BINARY_CONCAT")) {
						LinkedList<String> leftStrs = ParseExp(expNode.getChild(0));
						//System.err.println(leftStrs);
						//LinkedList<String> rightStrs = ParseExp(expNode.getChild(1));
						if(leftStrs.size()==1) {
							String leftStr = leftStrs.iterator().next();
							if(leftStr.charAt(0)<'0' || leftStr.charAt(0)>'9') {
								String fullStr = leftStr+"*";
								expValue.add(fullStr);
								break;
							}
						}
					}
					*/
					BinaryOperationExpression newNode = (BinaryOperationExpression) expNode;
					Expression leftStr = newNode.getLeft();
					Expression rightStr = newNode.getRight();
					LinkedList<String> leftStrs = ParseExp(leftStr);
					LinkedList<String> rightStrs = ParseExp(rightStr);
					for(String l: leftStrs) {
						if(l.isEmpty() || (l.charAt(0)>='0'&&l.charAt(0)<='9') || l.charAt(0)=='#') {
							l = "*";
						}
						for(String r: rightStrs) {
							if(r.isEmpty() || (r.charAt(0)>='0'&&r.charAt(0)<='9') || r.charAt(0)=='#') {
								r = "*";
							}
							expValue.add(l+r);
						}
					}
					break;
				}
				expValue.add(expNode.getNodeId().toString());
				break;
			case "AST_CALL":	
			case "AST_METHOD_CALL":
			case "AST_STATIC_CALL":
				MultiHashMap<Long, Long> tmpcg = PHPCGFactory.call2mtd;
				CallExpressionBase newNode = (CallExpressionBase) expNode;
				if(newNode.getTargetFunc().getProperty("type").equals("AST_NAME")) {
					Identifier callsite = (Identifier) newNode.getTargetFunc();
					if(callsite.getNameChild().equals("dirname")) {
						String path = PHPCGFactory.getDir(newNode.getNodeId());
						File A = new File(path);
						File parentFolder = new File(A.getParent());
						try {
							expValue.add(parentFolder.getCanonicalPath());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else {
						expValue.add(newNode.toString());
					}
					break;
				}
				if(isClass==true && tmpcg.containsKey(expNode.getNodeId())) {
					//System.err.println(expNode.getNodeId());
					//Long callMtdDefId = PHPCGFactory.call2mtd.get(expNode.getNodeId()).get(0);
					//System.err.println(callMtdDefId);
					for(Long callMtdDefId: PHPCGFactory.call2mtd.get(expNode.getNodeId())) {
					if(PHPCGFactory.ret.containsKey(callMtdDefId) && !callMtdDefId.equals(expNode.getFuncId())) {
						Long retId = PHPCGFactory.ret.get(callMtdDefId);
						includeRet.add(retId);
						
						//System.err.println("ret "+retId);
						if(ASTUnderConstruction.idToNode.get(retId) instanceof StringExpression) {
							expValue.add("#"+ASTUnderConstruction.idToNode.get(retId).getEscapedCodeStr());
							continue;
						}
						
						if(ASTUnderConstruction.idToNode.get(retId) instanceof NewExpression ) {
							
							LinkedList<String> retVals = ParseExp(ASTUnderConstruction.idToNode.get(retId).getChild(0));
							if(retVals.size()==1) {
								String retVal = retVals.iterator().next();
								if(retVal.charAt(0)<'0' || retVal.charAt(0)>'9') {
									String namespace = ((NewExpression) ASTUnderConstruction.idToNode.get(retId)).getEnclosingNamespace();
									if(retVal.equals("self")) {
										retVal = ((NewExpression) ASTUnderConstruction.idToNode.get(retId)).getEnclosingClass();
									}
									else if(retVal.equals("static")) {
										String className = ((NewExpression) ASTUnderConstruction.idToNode.get(retId)).getEnclosingClass();
										String nameSpace = ((NewExpression) ASTUnderConstruction.idToNode.get(retId)).getEnclosingNamespace();
										Long prtId = PHPCGFactory.getClassId(className, expNode.getNodeId(), nameSpace);
										Set<Long> clds = PHPCGFactory.getAllChild(prtId);
										clds.add(prtId);
										for(Long cld: clds) {
											expValue.add("#"+cld);
										}
										continue;
									}
									Long classId = PHPCGFactory.getClassId(retVal, retId, namespace);
									expValue.add("#"+String.valueOf(classId));
									continue;
								}
							}
						}
						else if(ASTUnderConstruction.idToNode.get(retId) instanceof Variable ||
								ASTUnderConstruction.idToNode.get(retId) instanceof StaticPropertyExpression ||
								ASTUnderConstruction.idToNode.get(retId) instanceof PropertyExpression) {
							//System.err.println("## "+retId);
							if(includeRet.contains(retId)) {
								continue;
							}
							ParseVar parseRet = new ParseVar();
							parseRet.init(retId, isClass, crtglobal);
							parseRet.handle();
							Set<String> retVals = parseRet.getVar();
							//only one return value
							if(retVals.size()==1) {
								String retVal = retVals.iterator().next();
								if(retVal.charAt(0)<'0' || retVal.charAt(0)>'9') {
									String namespace = ASTUnderConstruction.idToNode.get(retId).getEnclosingNamespace();
									if(retVal.contains("#")) {
										retVal = retVal.replace("#", "");
										expValue.add("#"+retVal);
										continue;
									}
									if(retVal.equals("self")) {
										//System.err.println(retId);
										retVal = (ASTUnderConstruction.idToNode.get(retId)).getEnclosingClass();
									}
									if(retVal.equals("static")) {
										String className = (ASTUnderConstruction.idToNode.get(retId)).getEnclosingClass();
										String nameSpace = (ASTUnderConstruction.idToNode.get(retId)).getEnclosingNamespace();
										Long prtId = PHPCGFactory.getClassId(className, expNode.getNodeId(), nameSpace);
										Set<Long> clds = PHPCGFactory.getAllChild(prtId);
										clds.add(prtId);
										for(Long cld: clds) {
											expValue.add("#"+cld);
										}
										continue;
									}
									Long classId = PHPCGFactory.getClassId(retVal, retId, namespace);
									//System.err.println(retVal+" "+varId+" "+classId);
									if(classId==null) {
										expValue.add(expNode.getNodeId().toString());
										continue;
									}
									expValue.add("#"+String.valueOf(classId));
									continue;
								}
							}
							parseRet.reset();
						}
						else if(ASTUnderConstruction.idToNode.get(retId) instanceof ArrayExpression) {
							//System.err.println(retId);
							LinkedList<String> retVals = ParseExp(ASTUnderConstruction.idToNode.get(retId));
							//System.err.println(retVals);
							expValue.addAll(retVals);
							//System.err.println(expValue);
							continue;
						}
						expValue.add(retId.toString());
						continue;
					}
					else {
						expValue.clear();
						expValue.add(expNode.getNodeId().toString());
						break;
					}
				}
					break;
				}
				expValue.add(expNode.getNodeId().toString());
				break;
			
			case "AST_DIM":
				LinkedList<String> array = ParseExp(expNode.getChild(0));
				//System.err.println(array);
				LinkedList<String> dim = ParseExp(expNode.getChild(1));
				//System.err.println(array);
				//System.err.println(dim);
				//Only support two-dimension array now
				HashMap<String, String> tmpArray = new HashMap<String, String>();
				int arraySize = array.size()/2;
				//can not get the array
				if(array.size()==1) {
					expValue.add(array.getFirst());
					break;
				}
				for(int i=0; i<arraySize; i++) {
					tmpArray.put(array.get(i), array.get(i+arraySize));
				}
				for(String idx: dim) {
					//idx is variable, we can't analyze this case now.
					if('0'<=idx.charAt(0) && '9'>=idx.charAt(0)) {
						expValue.addAll(tmpArray.values());
						break;
					}
					//System.err.println(tmpArray);
					if(idx.charAt(0)=='#') {
						idx = idx.substring(1);
					}
					//idx can be resolved
					if(!tmpArray.containsKey(idx)) {
						//System.err.println("Index "+ idx +" cannot be found in "+expNode);
						expValue.clear();
						expValue.add(expNode.getNodeId().toString());
						break;
					}
					else {
						expValue.add(tmpArray.get(idx));
					}
				}
				
				break;
			//case "AST_VAR":
			//	nodeId = expNode.getNodeId();
			//	expValue = ParseStatement(nodeId, expNode.getChild(0).getEscapedCodeStr());
			//	break;
			case "AST_ARRAY":
				nodeId = expNode.getNodeId();
				LinkedList<String> key = new LinkedList<String>();
				LinkedList<String> value = new LinkedList<String>();
				//Get name nodes of AST_ARRAY
				int defaultKey=0;
				//it is a empty array
				if(!PHPCSVEdgeInterpreter.parent2child.containsKey(nodeId)){
					expValue.add(expNode.getNodeId().toString());
					break;
				}
				for(Long arrayEleId: PHPCSVEdgeInterpreter.parent2child.get(nodeId).values()) {
					//node type is AST_ARRAY_ELEM
					if(ASTUnderConstruction.idToNode.get(arrayEleId).getChild(0).getProperty("type").equals("string")) {
						value.addAll(ParseExp(ASTUnderConstruction.idToNode.get(arrayEleId).getChild(0)));
					}
					else {
						value.add(ASTUnderConstruction.idToNode.get(arrayEleId).getChild(0).getNodeId().toString());
					}
					if(ASTUnderConstruction.idToNode.get(arrayEleId).getChild(1).getProperty("type").equals("string")) {
						key.addAll(ParseExp(ASTUnderConstruction.idToNode.get(arrayEleId).getChild(1)));
					}
					else {
						//System.err.println(ASTUnderConstruction.idToNode.get(arrayEleId).getChild(1).getProperty("type"));
						key.add(Integer.toString(defaultKey));
						defaultKey++;
					}
				}
				expValue.addAll(key);
				expValue.addAll(value);
				//System.err.println(expValue);
				break;
			case "AST_CLASS_CONST":
				for (String constVal: ParseExp(expNode.getChild(1))){
					if (constVal.equals("class")) {
						//add all classes to return value
						for (String tmp: ParseExp(expNode.getChild(0))) {
							expValue.add(tmp);
						}
					}
					else {
						expValue.add(expNode.getNodeId().toString());
					}
				}
				break;
			case "AST_NAME_LIST":
				nodeId = expNode.getNodeId();
				//Get name nodes of AST_NAME_LIST
				for(Long nameId: PHPCSVEdgeInterpreter.parent2child.get(nodeId).values()) {
					//node type is NAME
					expValue.addAll(ParseExp(ASTUnderConstruction.idToNode.get(nameId)));
				}
				break;
			case "AST_CLONE":
				expValue.addAll(ParseExp(expNode.getChild(0)));
				break;
			case "AST_NAME":
				expValue.addAll(ParseExp(expNode.getChild(0)));
				break;
			case "string":
				//The (normal) recursion end point, returns code field
				expValue.add(expNode.getEscapedCodeStr());
				break;
			case "integer":
				expValue.add('#'+expNode.getEscapedCodeStr());
				break;
			case "NULL":
			case "AST_CLOSURE":
			case "UNSET":
				break;
			case "AST_CAST":
				expValue.addAll(ParseExp(expNode.getChild(0)));
				break;
			case "AST_UNARY_OP":
			case "AST_ENCAPS_LIST":
				expValue.add(expNode.getNodeId().toString());
				break;
			default:
				expValue.add(expNode.getNodeId().toString());
				System.err.println("Unknown expresiion type "+expType+expNode.getNodeId());
		}
		includeRet = new HashSet<Long>();
		Set<String> tmp = new HashSet<String>();
		tmp.addAll(expValue);
		cache.put(expNode.getNodeId(), tmp);
		return expValue;
	}
	
	//get root node of source statement of root node of variable
	/*
	 * input: variable's root id
	 * output: source root id 
	 */
	protected Set<Long> getSource(Queue<Long> getSourceId, String varName) {
		Set<Long> sourceId = new HashSet<Long>();
		while(!getSourceId.isEmpty()) {
			Long node = getSourceId.poll();
			
			//it is a transition node.
			if(DDG.Locate.containsKey(node.toString()+"_"+varName)) { 
				LinkedList<Long> transitionId = DDG.Locate.get(node.toString()+"_"+varName);
				//System.err.println(transitionId);
				//add transition nodes to source node list
				transitionId.forEach((temp) -> {
					if(node>temp)
						getSourceId.offer(temp);
				});
			}
			
			//it is a root source node (in the function scope) , add it to return value list.
			else {
				//System.err.println(node.toString());
				sourceId.add(node);
			}
		}
		return sourceId;
	}
	
	public void reset() {
		varValue.clear();
	}
	
}

