package cg;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ast.ASTNode;
import ast.expressions.BinaryOperationExpression;
import ast.expressions.CallExpressionBase;
import ast.expressions.Identifier;
import ast.expressions.NewExpression;
import ast.expressions.PropertyExpression;
import ast.expressions.StringExpression;
import ast.expressions.Variable;
import ast.php.declarations.ClassDef;
import ast.php.expressions.ArrayElement;
import ast.php.expressions.ArrayExpression;
import ast.php.expressions.MethodCallExpression;
import ast.php.expressions.StaticCallExpression;
import ast.php.functionDef.Closure;
import ast.php.functionDef.Method;
import ast.php.functionDef.FunctionDef;
import ast.php.functionDef.TopLevelFunctionDef;
import inputModules.csv.PHPCSVNodeTypes;
import inputModules.csv.csv2ast.ASTUnderConstruction;
import misc.MultiHashMap;
import outputModules.csv.exporters.CSVCFGExporter;
import tools.php.ast2cpg.PHPCSVEdgeInterpreter;
import tools.php.ast2cpg.PHPCSVNodeInterpreter;

public class PHPCGFactory {

	// maintains a map of known function names (e.g., "B\foo" -> function foo() in namespace B)
	private static MultiHashMap<String,FunctionDef> functionDefs = new MultiHashMap<String,FunctionDef>();
	// maintains a list of function calls
	private static LinkedList<CallExpressionBase> functionCalls = new LinkedList<CallExpressionBase>();
	
	// maintains a map of known static method names (e.g., "B\A::foo" -> static function foo() in class A in namespace B)
	private static MultiHashMap<String,Method> staticMethodDefs = new MultiHashMap<String,Method>();
	// maintains a list of static method calls
	private static LinkedList<StaticCallExpression> staticMethodCalls = new LinkedList<StaticCallExpression>();
	
	// maintains a map of known constructors (e.g., "B\A" -> static function __construct() in class A in namespace B)
	public static MultiHashMap<String,Method> constructorDefs = new MultiHashMap<String,Method>();
	//maintains a map of known destructors
	public static MultiHashMap<String, Method> destructorDefs = new MultiHashMap<String,Method>();
	// maintains a list of static method calls
	private static LinkedList<NewExpression> constructorCalls = new LinkedList<NewExpression>();
	
	//Full name=>methodDef AST node
	private static MultiHashMap<String,Method> nonStaticMethodDefs = new MultiHashMap<String,Method>();
	//method name=>methodDef AST node
	private static MultiHashMap<String,Method> nonStaticMethodNameDefs = new MultiHashMap<String,Method>();
	//maintains a list of non-static method calls
	private static LinkedList<MethodCallExpression> nonStaticMethodCalls = new LinkedList<MethodCallExpression>();
	//resolve variable value
	
	//private static ParseVar parsevar = new ParseVar();
	public static HashMap<String, Long> classDef = new HashMap<String, Long>();
	private static HashMap<HashMap<Long, Long>, String> save = new HashMap<HashMap<Long, Long>, String>();
	public static MultiHashMap<Long, Long> inhe = new MultiHashMap<Long, Long>();
	public static MultiHashMap<Long, Long> ch2prt = new MultiHashMap<Long, Long>();
	public static MultiHashMap<Long, Long> prt2ch = new MultiHashMap<Long, Long>();
	//MethodDef node id => return classDef node id 
	public static HashMap<Long, Long> ret = new HashMap<Long, Long>();
	public static MultiHashMap<Long, Long> call2mtd = new MultiHashMap<Long, Long>();
	public static MultiHashMap<Long, Long> mtd2mtd = new MultiHashMap<Long, Long>();
	public static LinkedList<Long> collectAllFun = new LinkedList<Long>();
	public static MultiHashMap<String, Long> globalMap = new MultiHashMap<String, Long>();
	public static Set<Long> func_get_args = new HashSet<Long>();
	public static Set<Long> call_user = new HashSet<Long>();
	private final static Lock lock = new ReentrantLock();
	private final static Lock lock1 = new ReentrantLock();
	private final static Lock lockR = new ReentrantLock();
	private final static Lock lockC = new ReentrantLock();
	private final static Lock lockp = new ReentrantLock();
	
	//public static MultiHashMap<Long, Long> parentCall = new MultiHashMap<Long, Long>();
	//public static MultiHashMap<Long, String> collectThis = new MultiHashMap<Long, String>();
	//public static MultiHashMap<Long, String> collectParent = new MultiHashMap<Long, String>();;
	public static Set<Long> topFunIds = new HashSet<Long>();
	//private static MultiHashMap<Long, Long> func2cls = new MultiHashMap<Long, Long>();
	public static Set<Long> magicMtdDefs = new HashSet<Long>();  
	public static Set<String> classUsed = new HashSet<String>();
	public static MultiHashMap<String, Long> fullname2Id = new MultiHashMap<String, Long>();
	public static MultiHashMap<String, Long> name2Id = new MultiHashMap<String, Long>();
	public static int callsiteNumber = 0;
	//public static int unknownCallsite = 0;
	public static Set<Long> unknownIds = new HashSet<Long>();
	//public static HashMap<Long, Set<Long>> cls2Allcls = new HashMap<Long, Set<Long>>();
	public static HashMap<Long, Long> ignoreIds = new HashMap<Long, Long>();
	public static Set<Long> allFuncDef = new HashSet<Long>();
	public static int omit=0;
	public static Set<Long> omitIds = new HashSet<Long>();
	public static HashMap<String, Long> path2TopFile = new HashMap<String, Long>();
	public static Set<String> removed = new HashSet<String>();
	//public static HashMap<Long, String> topIdcache= new HashMap<Long, String>();
	public static MultiHashMap<String, String> filepaths = new MultiHashMap<String, String>();
	public static HashMap<Long, String> id2Name = new HashMap<Long, String>();
	
	public static Set<String> spiderAndScanner = new HashSet<String>();
	//public static Map<Long, Long> howmany = new TreeMap<Long, Long>();
	public static MultiHashMap<Long, String> allUse = new MultiHashMap<Long, String>();
	
	public static Set<String> initial = new HashSet<String>();
	public static Integer spider = new Integer(0);
	public static Set<Long> suspicious = new HashSet<Long>();
	
	public static Set<Long> Abstract = new HashSet<Long>();
	
	//public static MultiHashMap<String, FunctionDef> name2Def = new MultiHashMap<String, FunctionDef>();
	
	//public static int entry=0, dependent=0;
	public static Set<Long> removeId = new HashSet<Long>();
	public static int topsum = 0;
	public static Set<Long> condes = new HashSet<Long>();
	//public static Set<FunctionDef> constructSet = new HashSet<FunctionDef>();
	/**
	 * Creates a new CG instance based on the lists of known function definitions and function calls.
	 * 
	 * Call this after all function definitions and calls have been added to the lists using
	 * addFunctionDef(FunctionDef) and addFunctionCall(CallExpression).
	 * 
	 * After a call graph has been constructed, these lists are automatically reset.
	 * 
	 * @return A new call graph instance.
	 */
	public static CG newInstance() {
		CG cg = new CG();
		
		init();
		
		//System.err.println("File Use" + allUse);
		System.err.println("@");
		createFunctionCallEdges(cg);
		//System.err.println(removed);
		String fileName="1.txt";
        try
        {
                FileWriter writer=new FileWriter(fileName);
                writer.write(removed.toString());
                writer.close();
        } catch (IOException e)
        {
                e.printStackTrace();
        }
        
		System.err.println("@@");
		createConstructorCallEdges(cg);
		System.err.println("@@@");
		createStaticMethodCallEdges(cg);
		System.err.println("@@@@");
		createNonStaticMethodCallEdges(cg);
		System.err.println("@@@@@");
		//System.err.println(CSVCFGExporter.cfgSave);
		getVarCall(cg);
		System.err.println("@@@@@@");
		//System.err.println(FileDependencyCheck.includeDirs);
		//System.err.println(FileDependencyCheck.excludeDirs);
		System.err.println(callsiteNumber+" "+unknownIds.size()+" "+topsum);
		//System.err.println(mtd2mtd);
		/*
		if(suspicious.contains((long) 616273)) {
			System.err.println(call2mtd.get((long) 616273));
		}
		*/
		reset();
		
		return cg;
	}
	
	/*
	private static void searchentry() {
		for(Long funId: allFuncDef) {
			if(removeId.contains(funId)) {
				continue;
			}
			FunctionDef func = (FunctionDef) ASTUnderConstruction.idToNode.get(funId);
			if(func instanceof TopLevelFunctionDef
					&& (func.getEnclosingNamespace()==null || func.getEnclosingNamespace().isEmpty())
					&& !getDir(funId).contains("/vendor/")
					&& !(func instanceof Closure)) {
				entry++;
			}
			else {
				dependent++;
			}
		}
	}*/

	static void init() {
		setIgnoreIds();
		setInheritance();
		for(Long topId: topFunIds) {
			String path = getDir(topId);
			if(!path.contains("/vendor/")) {
				path2TopFile.put(path, topId);
			}
		}
		
		/*
		for(Long nodeid: PHPCSVNodeInterpreter.filepath.keySet()) {
			//System.err.println(nodeid);
			String path = getDir(nodeid);
			filepaths.add(path, PHPCSVNodeInterpreter.filepath.get(nodeid));
		}
		
		for(String filename: filepaths.keySet()) {
			if(!filecache.containsKey(filename)) {
//				if(filename="")
				Set<String> has = new HashSet<String>();
				has.add(filename);
				getFileDependency(filename, has);
			}
		}
		*/
		//System.err.println("file dependency:"+filecache);
		
		for(Long nodeid : PHPCSVEdgeInterpreter.collectUse.keySet()) {
			Long topid = toTopLevelFile.getTopLevelId(nodeid);
			allUse.add(topid, PHPCSVEdgeInterpreter.collectUse.get(nodeid));
		}
	}
	
	/*
	private static void getFileDependency(String filename, Set<String> has) {
		for(String file: filepaths.get(filename)) {
			for(String fileHasRequire: filepaths.keySet()) {
				//filename requires fileHasRequire
				if(fileHasRequire.contains(file)) {
					//System.err.println(filename +" " + fileHasRequire);
					if(has.contains(fileHasRequire)) {
						continue;
					}
					if(!filecache.containsKey(fileHasRequire)) {
						//System.err.println(filename+" "+file+" "+fileHasRequire);
						has.add(fileHasRequire);
						getFileDependency(fileHasRequire, has);
					}	
					filecache.add(filename, fileHasRequire);
					List<String> tt = filecache.get(fileHasRequire);
					//System.err.println(tt);
					filecache.addAll(filename, tt);
				}
			}
			System.err.println(filename+" "+file);
			filecache.add(filename, file);
		}
	}
	*/
	
	public static String getDir(Long astid) {
		/*
		if(topIdcache.containsKey(astid)) {
			return topIdcache.get(astid);
		}
		*/
		Long topId = toTopLevelFile.getTopLevelId(astid);
		//FunctionDef funNode =  (FunctionDef) ASTUnderConstruction.idToNode.get(funId);
		if(!ASTUnderConstruction.idToNode.get(topId).getFlags().equals("TOPLEVEL_FILE")) {
			System.err.println("Fail to find top file for target function "+astid);
			return "";
		}
		TopLevelFunctionDef topFile = (TopLevelFunctionDef) ASTUnderConstruction.idToNode.get(topId);
		String phpPath = topFile.getName();
		phpPath = phpPath.substring(1, phpPath.length()-1);
		phpPath = phpPath.replace("//", "/");
		//topIdcache.put(astid, phpPath);
		return phpPath;
	}
	
	private static void setIgnoreIds() {
		
		parseXdebug();
		Set<Long> tmp = new HashSet<Long>();
		//System.err.println("inin:"+initial);
		//String baseDir = CommandLineInterface.baseDir;
		for(Long id: allFuncDef) {
			//ignore all first-party functions
			if(ASTUnderConstruction.idToNode.get(id) instanceof TopLevelFunctionDef) {
				String filename = getDir(id);
				//System.err.println("111 "+initial);
				//System.err.println("222 "+filename);
				for(String ini: initial) {
					if(filename.endsWith(ini)) {
						ignoreIds.put(id, (long) 1);
					}
				}
			}
			else {
				for(String funcName: spiderAndScanner) {
					if(!fullname2Id.containsKey(funcName)) {
						continue;
					}
					
					if(fullname2Id.get(funcName).contains(id)) {
						ignoreIds.put(id, (long) 1);
						tmp.add(id);
					}
				}
			}
		}
		System.err.println(ignoreIds.keySet());
		spider = ignoreIds.size();
		
		
		/*
		LinkedList<Long> saved = new LinkedList<Long>();
		
		File record = new File("xdebug.txt");
		try {
			Scanner scan = new Scanner(record);
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				line = line.replace("[", "");
				line = line.replace(" ", "");
				line = line.replace("]", "");
				String[] numbers = line.split(",");
				for(String num: numbers) {
					Long astid = Long.parseLong(num);
					saved.add(astid);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.err.println(saved);
		
		for(Long i: saved) {
			ignoreIds.put(i, (long)1);
		}
		*/
		spider = ignoreIds.size();
		
		System.err.println("spider "+ignoreIds.size());
	}

	
	private static Set<Long> getChild(Long id) {
		Set<Long> all = new HashSet<Long>();
		Queue<Long> que = new LinkedList<Long>();
		que.add(id);
		all.add(id);
		while(!que.isEmpty()) {
			Long id1 = que.peek();
			que.poll();
			//System.err.println(id1);
			MultiHashMap<Long, Long> tmpcg = new MultiHashMap<Long, Long>();
			if((tmpcg.containsKey(id1))) {
				Set<Long> mtds = (Set<Long>) tmpcg.get(id1);
				if(mtds == null) {
					continue;
				}
				for(Long callee: mtds) {
					if(!all.contains(callee)) {
						que.add(callee);
						all.add(callee);
					}
				}
			}
		}
		return all;
	}

	private static void parseXdebug() {
		File xdebug = new File("/var/log/xdebug/");
		String f1 = new String();
		for (File profile : xdebug.listFiles()) {
			 try {
				Scanner scan = new Scanner(profile);
				while(scan.hasNextLine()){
			        String line = scan.nextLine();
			        if(line.startsWith("f1=")
			        		&& line.contains(" ")) {
			        	f1 = line.split(" ")[1];
			        }
			        else if(line.startsWith("fn=")
			        		&& line.split(" ").length==2) {
			        	String str = line.split(" ")[1];
			        	if(str.startsWith("php::")
			        			||str.startsWith("include::") || str.startsWith("include_once::")
			        			||str.startsWith("require::") || str.startsWith("require_once::")) {
			        		continue;
			        	}
			        	if(str.startsWith("{main}")
			        			&& f1.contains("mediawiki-1.28.0")) {
			        		f1 = f1.substring(line.indexOf("mediawiki-1.28.0"));
			        		f1 = f1.replace("\\", "/");
				        	initial.add(f1);
				        	continue;
			        	}
			        	//System.err.println("line: "+line);
			        	str = str.replace("->", "::");
			        	str = str.replace("::__construct", "");
			        	spiderAndScanner.add(str);
			        }
			        else if(line.startsWith("cmd:")
			        		&& line.contains("mediawiki-1.28.0")) {
			        	//System.err.println(line);
			        	String str = line.substring(line.indexOf("mediawiki-1.28.0"));
			        	str = str.replace("\\", "/");
			        	initial.add(str);
			        }
			    }
				scan.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void getVarCall(CG cg) {
		//System.err.println(save);
		MultiHashMap<Long, Long> src2cls = new MultiHashMap<Long, Long>();
		MultiHashMap<Long, String> ambiguousCall = new MultiHashMap<Long, String>();
		HashMap<Long, String> cls2MtdName = new HashMap<Long, String>();
		for(HashMap<Long, Long> src2Dst: save.keySet()){
		//save.keySet().parallelStream().forEach(src2Dst -> {
			//System.err.println("AAA "+src2Dst);
			Long v = src2Dst.keySet().iterator().next();
			src2cls.add(v, src2Dst.get(v));
			String className = hasInstanceOf(src2Dst);
			//get class name from instance of
			String methodname = save.get(src2Dst);
			cls2MtdName.put(src2Dst.get(v), methodname);
			Long dataSrc = src2Dst.keySet().iterator().next();
			Long callsite = src2Dst.values().iterator().next();
			if(!className.equals("")) {
				//System.err.println(src2Dst.values());
				String namespace = ASTUnderConstruction.idToNode.get(callsite).getEnclosingNamespace();
				Long classId = getClassId(className, callsite, namespace);
				Set<Long> cldIds = getAllChild(classId);
				boolean find=false;
				for(Long cld: cldIds) {
					if(getMethodCall(cg, (CallExpressionBase) ASTUnderConstruction.idToNode.get(callsite), cld, methodname, false)){
			    		find = true;
			    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
				    }
				}
				if(getMethodCall(cg, (CallExpressionBase) ASTUnderConstruction.idToNode.get(callsite), classId, methodname, false)){
		    		find = true;
		    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
			    }
				if(find==false) {
					//System.err.println("Fail to find target method for instanceof variable called at " + callsite + " "+classId + " "+methodname);
				}
			}
			//still don't know the class name
			else {
				if(nonStaticMethodNameDefs.containsKey(methodname)) {
					//List<Method> psbClsIds = nonStaticMethodNameDefs.get(methodname);
					//System.err.println(psbClsIds.size()+methodname);
					if(!ambiguousCall.containsKey(dataSrc) || !ambiguousCall.get(dataSrc).contains(methodname))
						ambiguousCall.add(dataSrc, methodname);
				}
				else {
					//System.err.println("Fail to find method name " + methodname + " " + src2Dst);
				}
			}
		}
		
		
		//System.err.println(ambiguousCall);
		//MultiHashMap<Long, Long> psbClsIds = new MultiHashMap<Long, Long>();
		//traverse ambiguous class
		int sum=ambiguousCall.keySet().size();
		AtomicInteger clock = new AtomicInteger(0);
		//for(Long clsId: ambiguousCall.keySet()) {
		ambiguousCall.keySet().parallelStream().forEach(clsId -> {
			clock.incrementAndGet();
			System.err.println("BBB "+clock+" "+sum);
			HashMap<Long, String> callsiteIds = new HashMap<Long, String>();
			for(HashMap<Long, Long> tmp: save.keySet()) {
				if(tmp.containsKey(clsId)) {
					callsiteIds.put(tmp.get(clsId), save.get(tmp));
				}
			}
					
			Set<Long> filter = new HashSet<Long>();
			MultiHashMap<Set<Long>, Method> psbMethodNodes = new MultiHashMap<Set<Long>, Method>();
			for(String mtdName: ambiguousCall.get(clsId)) {
				List<Method> methodDefNodes = nonStaticMethodNameDefs.get(mtdName);
				LinkedList<Long> crt = new LinkedList<Long>();
				//get all possible classes that have that method defined in its class scope
				for(Method psbCls: methodDefNodes) {
					//if(ignoreIds.containsKey(psbCls.getNodeId())) {
					//	continue;
					//}
					Long psbMtdId = psbCls.getNodeId();
					String classname = psbCls.getEnclosingClass();
					String namespace = psbCls.getEnclosingNamespace();
					Long psbClsId = getClassId(classname, psbMtdId, namespace);
					//Set<Long> tmp = getAllChild(psbClsId);
					//Set<Long> tmp = new HashSet<Long>();
					Set<Long> tmp = new HashSet<Long>();
					tmp.add(psbClsId);
					crt.addAll(tmp);
					psbMethodNodes.add(tmp, psbCls);
				}
				//try to filter some classes if the class cannot call all methods
				if(filter.isEmpty()) {
					//if there is only one method name, than all class and their child classes may be the value of that variable
					filter.addAll(crt);
				}
				else {
					filter.retainAll(crt);
				}
			}
			System.err.println("BBB1");
			
			MultiHashMap<String, Method> tmp1 = new MultiHashMap<String, Method>();
			
			tmp1.addAll(nonStaticMethodDefs);
			tmp1.addAll(staticMethodDefs);
			
			if(filter.isEmpty()) {
				
				for(HashMap<Long, Long> src2Dst: save.keySet()) {
					if(src2Dst.containsKey(clsId)) {
						Long callsite = src2Dst.get(clsId);
						String mathodname = save.get(src2Dst);
						String methodKey = "*::"+mathodname;
						System.err.println("BBB2 "+callsite);
						addCallEdgeIfDefinitionKnown(cg, tmp1, (CallExpressionBase) ASTUnderConstruction.idToNode.get(callsite), methodKey, false);
						//a non static method can call a static method
						//addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, (CallExpressionBase) ASTUnderConstruction.idToNode.get(callsite), methodKey, false);
					}
				}
				//System.err.println("Fail to find candidate class for method "+clsId);
			}
			else {
				//System.err.println(clsId);
				for(Set<Long> allCls: psbMethodNodes.keySet()) {
					//unknownCallsite++;
					Set<Long> tmp = new HashSet<Long>();
					tmp.addAll(allCls);
					tmp.retainAll(filter);
					if(!tmp.isEmpty()) {
						//System.err.println(psbMethodNodes.get(allCls));
						for(FunctionDef candidate: psbMethodNodes.get(allCls)) {
							String methodName = candidate.getEscapedCodeStr();
							String methodKey = ((Method)candidate).getEnclosingClass() + "::" + ((Method)candidate).getName();
							if( !candidate.getEnclosingNamespace().isEmpty())
								methodKey = candidate.getEnclosingNamespace() + "\\" + methodKey;
							/*
							for(HashMap<Long, Long> tmp1: save.keySet()) {
								if(tmp1.keySet().iterator().next().equals(clsId) &&
										save.get(tmp1).equals(methodName)) {
									Long callsiteId = tmp1.get(clsId);
									unknownIds.add(callsiteId);
									System.err.println("DDD "+psbMethodNodes.size()+" "+callsiteId);
									addCallEdge(cg, (CallExpressionBase) ASTUnderConstruction.idToNode.get(callsiteId), candidate, false);
									
								}
							}
							*/
							for(Long cls: src2cls.get(clsId)) {
								ASTNode callId = ASTUnderConstruction.idToNode.get(cls);
								Long mtdId = callId.getFuncId();
								if(cls2MtdName.get(cls).equals(methodName)) {
									//mtd2mtd.add(mtdId, candidate.getNodeId());
									lock.lock();
							        try {
							        	suspicious.add(callId.getNodeId());
							        } finally {
							            lock.unlock();
							        }
							        
									if(addCallEdge(cg, (CallExpressionBase) ASTUnderConstruction.idToNode.get(cls), candidate, false)) {
										addAllcld(cg, candidate, methodKey, (CallExpressionBase) ASTUnderConstruction.idToNode.get(cls), false, tmp1);
									}
									
								}
							}
							
						}
					}
				}
			}
		});
		
	}
	
	private static String hasInstanceOf(HashMap<Long, Long> nodes) {
		Long srcRootId = nodes.keySet().iterator().next();
		Long dstRootId = nodes.values().iterator().next();
		
		while(!CSVCFGExporter.cfgSave.containsKey(dstRootId)) {
			if(PHPCSVEdgeInterpreter.child2parent.containsKey(dstRootId)) {
				dstRootId = PHPCSVEdgeInterpreter.child2parent.get(dstRootId);
			}
			else {
				//System.err.println(nodes.values().iterator().next());
				return "";
			}
		}
		
		List<Long> srcIds = CSVCFGExporter.cfgSave.get(dstRootId);
		//System.err.println(srcIds+" "+dstRootId);
		if(srcIds.size() == 0) {
			return "";
		}
		do {
			srcIds.sort(Comparator.reverseOrder());
			Long tmp = srcIds.get(0);
			//System.err.println(dstRootId);
			if(tmp>dstRootId) {
				srcIds.remove(0);
				continue;
			}
			//System.err.println(tmp);
			if(srcIds.size()==1 && 
					ASTUnderConstruction.idToNode.get(tmp)!=null &&
					ASTUnderConstruction.idToNode.get(tmp).getProperty("type").equals("AST_INSTANCEOF") &&
					isIfEle(nodes.values().iterator().next(), PHPCSVEdgeInterpreter.child2parent.get(tmp))) {
				ParseVar parsevar = new ParseVar();
				parsevar.init(PHPCSVEdgeInterpreter.parent2child.get(tmp).get(0), true, "");
				parsevar.handle();
				Set<String> classValues = parsevar.getVar();
				//if(srcRootId==139407)
				//	System.err.println(classValues);
				if(classValues.contains(srcRootId.toString())) {
					ASTNode className = ASTUnderConstruction.idToNode.get(tmp).getChild(1);
					//System.err.println(className.getNodeId());
					if(className.getProperty("type").equals("AST_NAME")) {
						String classname = className.getChild(0).getEscapedCodeStr();
						//System.err.println(classname);
						return classname;
					}
					else {
						System.err.println("Fail to get class name of AST_INSTANCEOF");
						return "";
					}
				}
				parsevar.reset();
			}
			
			if(CSVCFGExporter.cfgSave.containsKey(tmp)) {
				srcIds.remove(0);
				List<Long> tmp1 = CSVCFGExporter.cfgSave.get(tmp);
				for(Long ele: tmp1) {
					if(!srcIds.contains(ele) && ele<tmp) {
						srcIds.add(ele);
					}
				}
			}
			else {
				break;
			}
			
		}while(!srcIds.isEmpty());
		return "";
	}
	
	private static boolean isIfEle(Long src, Long dst) {
		boolean ret=false;
		while(src>=dst) {
			//System.err.println(src+" "+dst);
			if(src.equals(dst)) {
				//System.err.println(src+" "+dst);
				return true;
			}
			src = PHPCSVEdgeInterpreter.child2parent.get(src);
		}
		return ret;
	}
	
	private static void setInheritance() {
		for(Long clsId: inhe.keySet()) {
			Set<Long> prts = getParentClassId(clsId);
			for(Long prt: prts) {
				if(clsId.equals(prt)) {
					continue;
				}
				ch2prt.add(clsId, prt);
				prt2ch.add(prt, clsId);
			}
		}
	}
	
	public static Set<Long> getAllChild(Long prtId){
		
		Set<Long> cldList = new HashSet<Long>();
		Queue<Long> crtCld = new LinkedList<Long>();
		crtCld.offer(prtId);
		while(!crtCld.isEmpty()) {
			Long crtId = crtCld.poll();
			if(prt2ch.containsKey(crtId)) {
				for(Long ele: prt2ch.get(crtId)) {
					crtCld.add(ele);
					cldList.add(ele);
				}
			}
		}
		
		return cldList;
	}

	private static void createFunctionCallEdges(CG cg) {
		
		int x1 = functionCalls.size();
		AtomicInteger c1= new AtomicInteger(0);
		//Stream<CallExpressionBase> stream = StreamSupport.stream(functionCalls.spliterator(), true);
				//stream.forEach (functionCall -> {
		//for( CallExpressionBase functionCall : functionCalls) {
		functionCalls.parallelStream().forEach(functionCall -> {
			System.err.println(x1+" "+c1+" "+functionCall.getNodeId());
			c1.incrementAndGet();
			lockp.lock();
	        try {
	        	if(getDir(functionCall.getNodeId()).endsWith(".phtml")) {
	        		ignoreIds.put(functionCall.getFuncId(), (long)1);
	        	}
	        } finally {
	        	lockp.unlock();
	        }
			// make sure the call target is statically known
			if( functionCall.getTargetFunc() instanceof Identifier) {
				
				Identifier callIdentifier = (Identifier)functionCall.getTargetFunc();
				if(callIdentifier.getNameChild().getEscapedCodeStr().equals("error_log")) {
					removeId.add(callIdentifier.getFuncId());
					Long funid = callIdentifier.getFuncId();
					String path = getDir(funid);
					if(ASTUnderConstruction.idToNode.get(funid) instanceof TopLevelFunctionDef) {
						lockR.lock();
				        try {
				        	removed.add("\""+path+"\"");
				        } finally {
				            lockR.unlock();
				        }
						
					}
					else {
						String clsname = functionCall.getEnclosingClass();
						String funcname = ((FunctionDef)(ASTUnderConstruction.idToNode.get(funid))).getName();
						if(clsname == null || clsname == "") {
							lockR.lock();
					        try {
					        	removed.add("\""+path+"$"+funcname+"\"");
					        } finally {
					            lockR.unlock();
					        }
						}
						else {
							lockR.lock();
					        try {
					        	removed.add("\""+path+"$"+clsname+"$"+funcname+"\"");
					        } finally {
					            lockR.unlock();
					        }
						}
					}
				}
				
				if(callIdentifier.getNameChild().getEscapedCodeStr().equals("func_get_args")) {
					func_get_args.add(callIdentifier.getFuncId());
				}
				
				//redirect functions
				if(callIdentifier.getNameChild().getEscapedCodeStr().equals("header")) {
					ASTNode firstArg = functionCall.getArgumentList().getArgument(0);
					String redirectpath = "";
					if(firstArg.getProperty("type").equals("string")) {
						redirectpath = firstArg.getEscapedCodeStr();
					}
					else if(firstArg.getProperty("type").equals("AST_BINARY_OP") &&
							firstArg.getFlags().equals("BINARY_CONCAT")) {
						if(firstArg.getChild(0).getProperty("type").equals("string")) {
							redirectpath = firstArg.getChild(0).getEscapedCodeStr();
						}
						if(firstArg.getChild(0).getProperty("type").equals("AST_BINARY_OP") &&
								firstArg.getFlags().equals("BINARY_CONCAT")) {
							redirectpath = firstArg.getChild(0).getChild(0).getEscapedCodeStr();
						}
					}
					
					if(redirectpath==null||!redirectpath.startsWith("Location: ")) {
						return;
					}
					System.err.println(redirectpath);
					
					String callpath = getDir(functionCall.getNodeId());
					if(!path2TopFile.containsKey(callpath)) {
						return;
					}
					Long callsiteTop = path2TopFile.get(callpath);
					//only LOcation: 
					if(Pattern.matches("Location:\\s*", redirectpath)) {
                        //unknownIds.add(functionCall.getNodeId());
                        System.err.println("RR: "+redirectpath);
                        for(Long topId: path2TopFile.values()) {
                        	if(!topId.equals(callsiteTop)) {
                        		lock.lock();
                                try {
                                	call2mtd.add(callIdentifier.getNodeId(), topId);
                                	mtd2mtd.add(callsiteTop, topId);
                                } finally {
                                    lock.unlock();
                                }
                        	}
                        	//addCallEdge(cg, functionCall, (FunctionDef) ASTUnderConstruction.idToNode.get(topId), false);
                        }
                        return;
					}

					
					//resolve redirect path
					String callsitepath = getDir(functionCall.getNodeId());
					String realredirectpath = redirectpath.split("\\s+")[1];
					File a = new File(callsitepath);
					File parentFolder = new File(a.getParent());
					File redirectFile = new File(parentFolder, realredirectpath);
					if(redirectFile.exists()) {
						String absolute;
						try {
							absolute = redirectFile.getCanonicalPath();
							//System.err.println("Redirect to "+absolute+" "+functionCall);
							if(path2TopFile.containsKey(absolute)) {
								if(!callsiteTop.equals(path2TopFile.get(absolute))) {	
									lock.lock();
							        try {
							        	call2mtd.add(callIdentifier.getNodeId(), path2TopFile.get(absolute));
							        	mtd2mtd.add(callsiteTop, path2TopFile.get(absolute));
							        } finally {
							            lock.unlock();
							        }
									topsum++;
								}
								//addCallEdge(cg, functionCall, (FunctionDef) ASTUnderConstruction.idToNode.get(path2TopFile.get(absolute)), false);
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					} 
					else  {
						//System.err.println("Fail to find redirect path: "+realredirectpath);
						//unknownIds.add(functionCall.getNodeId());
						//System.err.println(redirectpath);
						for(Long topId: path2TopFile.values()) {
							if(!callsiteTop.equals(topId)) {
								lock.lock();
						        try {
						        	call2mtd.add(callIdentifier.getNodeId(), topId);
						        	mtd2mtd.add(callsiteTop, topId);
						        } finally {
						            lock.unlock();
						        }
							}
							//addCallEdge(cg, functionCall, (FunctionDef) ASTUnderConstruction.idToNode.get(topId), false);
						}
					}
				}
				else if(callIdentifier.getNameChild().getEscapedCodeStr().equals("http_redirect")) {
					System.err.println(functionCall);
				}
				else if(callIdentifier.getNameChild().getEscapedCodeStr().equals("usort") ||
							callIdentifier.getNameChild().getEscapedCodeStr().equals("array_walk")) {
					if(functionCall.getArgumentList().size()<2) {
						return;
					}
					ASTNode secondArg = functionCall.getArgumentList().getArgument(1);
					if(secondArg.getProperty("type").equals("AST_ARRAY")) {
						String classname = new String();
						String methodname = new String();
						ArrayElement classEle = ((ArrayExpression) secondArg).getArrayElement(0);
						ArrayElement methodEle = ((ArrayExpression) secondArg).getArrayElement(1);
						Long clsId = new Long(-1);
						if(classEle.getValue().getProperty("type").equals("string")) {
							String namespace = secondArg.getEnclosingNamespace();
							//Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
							classname = ((StringExpression)(classEle.getValue())).getEscapedCodeStr();
							if(classname.equals("static") || classname.equals("parent")) {
								classname = secondArg.getEnclosingClass(); 
								Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
								//if(classname.equals("static"))
								//	clds = getAllChild(prtId);
								clsId = prtId;
							}
							else if(classname.equals("self")) {
								classname = secondArg.getEnclosingClass(); 
								Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
								clsId = prtId;
							}
							else {
								Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
								clsId = prtId;
							}
						}
						else if( classEle.getValue() instanceof Variable
								&& ((Variable)classEle.getValue()).getNameExpression() instanceof StringExpression
								&& ((StringExpression)((Variable)classEle.getValue()).getNameExpression()).getEscapedCodeStr().equals("this")) {
							String namespace = secondArg.getEnclosingNamespace();
							classname = secondArg.getEnclosingClass(); 
							clsId = getClassId(classname, functionCall.getNodeId(), namespace);
						}
						if(methodEle.getValue().getProperty("type").equals("string")) {
							methodname = (((StringExpression)(methodEle.getValue())).getEscapedCodeStr());
						}
						
						if(clsId!=-1 && !methodname.isEmpty()) {
							getMethodCall(cg, functionCall, clsId, methodname, false);
						}
					}
				}
				else if(callIdentifier.getNameChild().getEscapedCodeStr().equals("call_user_func")
						|| callIdentifier.getNameChild().getEscapedCodeStr().equals("call_user_func_array")
						|| callIdentifier.getNameChild().getEscapedCodeStr().equals("spl_autoload_register")
						|| callIdentifier.getNameChild().getEscapedCodeStr().equals("spl_autoload_unregister")) {
					//System.err.println(functionCall);
					callsiteNumber++;
					call_user.add(functionCall.getNodeId());
					if(functionCall.getArgumentList().size()==0) {
						return;
					}
					ASTNode firstArg = functionCall.getArgumentList().getArgument(0);
					//use call_user_func calls method
					if(firstArg.getProperty("type").equals("AST_ARRAY")) {
						String classname = new String();
						Set<String> methodname = new HashSet<String>();
						Set<Long> clds = new HashSet<Long>();
						
						ArrayElement classEle = ((ArrayExpression) firstArg).getArrayElement(0);
						ArrayElement methodEle = ((ArrayExpression) firstArg).getArrayElement(1);
						
						if(classEle.getValue().getProperty("type").equals("string")) {
							String namespace = firstArg.getEnclosingNamespace();
							//Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
							classname = ((StringExpression)(classEle.getValue())).getEscapedCodeStr();
							if(classname.equals("static") || classname.equals("parent")) {
								classname = firstArg.getEnclosingClass(); 
								Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
								//if(classname.equals("static"))
								//	clds = getAllChild(prtId);
								clds.add(prtId);
							}
							else if(classname.equals("self")) {
								classname = firstArg.getEnclosingClass(); 
								Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
								clds.add(prtId);
							}
							else {
								Long prtId = getClassId(classname, functionCall.getNodeId(), namespace);
								clds.add(prtId);
							}
						}
						else if( classEle.getValue() instanceof Variable
								&& ((Variable)classEle.getValue()).getNameExpression() instanceof StringExpression
								&& ((StringExpression)((Variable)classEle.getValue()).getNameExpression()).getEscapedCodeStr().equals("this")) {
							String namespace = firstArg.getEnclosingNamespace();
							classname = firstArg.getEnclosingClass(); 
							clds.add(getClassId(classname, functionCall.getNodeId(), namespace));
							//System.err.println(ClassDefId);
							//classId.add(ClassDefId);
						}
						else if(classEle.getValue() instanceof NewExpression && 
								((NewExpression)classEle.getValue()).getTargetClass() instanceof Identifier) {
							NewExpression classNew = (NewExpression) classEle.getValue();
							Identifier classNode = (Identifier) classNew.getTargetClass();
							String className = classNode.getNameChild().getEscapedCodeStr();
							String namespace = classNode.getEnclosingNamespace();
							clds.add(getClassId(className, functionCall.getNodeId(), namespace));
							//classId.add(ClassDefId);
						}
						else if(classEle.getValue() instanceof Variable){
							ASTNode classVar = classEle.getValue();
							ParseVar parsevar = new ParseVar();
							parsevar.init(classVar.getNodeId(), true, "");
							parsevar.handle();
							Set<String> classValue = parsevar.getVar();
							//System.err.println(classValue);
							String namespace = firstArg.getEnclosingNamespace();
							for(String classvalue: classValue) {
								//String classvalue = classValue.iterator().next();
								//we get it from a return value
								if(classvalue.charAt(0)=='#') {
									classvalue = classvalue.substring(1);
									clds.add(Long.parseLong(classvalue));
									//System.err.println(ClassDefId);
								}
								else if(classvalue.charAt(0)>'9' || classvalue.charAt(0)<'0') {
									clds.add(getClassId(classvalue, functionCall.getNodeId(), namespace));
									//System.err.println(ClassDefId);
								}
								else {
									clds.add((long) -1);
								}
							}
							parsevar.reset();
						}
						else {
							clds.add((long) -1);
						}
						
						if(methodEle.getValue().getProperty("type").equals("string")) {
							methodname.add(((StringExpression)(methodEle.getValue())).getEscapedCodeStr());
						}
						//then try to get method name
						else if(methodEle.getValue() instanceof BinaryOperationExpression) {
							BinaryOperationExpression methodNameNode = (BinaryOperationExpression) methodEle.getValue();
							ParseVar parsevar = new ParseVar();
							parsevar.init(1, false, "");
							LinkedList<String> methodNameStrs = parsevar.ParseExp(methodNameNode);
							//System.err.println(methodNameStrs);
							if(methodNameStrs.size()==1) {
								String methodNameStr = methodNameStrs.iterator().next();
								if(methodNameStr.charAt(0)<'0' || methodNameStr.charAt(0)>'9') {
									methodname.add(methodNameStr);
								}
							}
							parsevar.reset();
						}
						else if(methodEle.getValue() instanceof Variable) {
							Variable methodNameNode = (Variable) methodEle.getValue();
							ParseVar parsevar = new ParseVar();
							parsevar.init(methodNameNode.getNodeId(), false, "");
							parsevar.handle();
							Set<String> methodNameStrs = parsevar.getVar();
							for(String methodNameStr: methodNameStrs) {
								if(methodNameStr.charAt(0)>'9' || methodNameStr.charAt(0)<'0') {
									methodname.add(methodNameStr);
								}
							}
							parsevar.reset();
						}
						else {
							methodname.add("*");
						}
						
						//System.err.println(clds+" "+methodname);
						
						for(Long clsDefId: clds) {
							if(clsDefId==null) {
								continue;
							}
							for(String mtdkey: methodname) {
								if(clsDefId==-1) {
									String methodKey = "*::"+mtdkey;
									//System.err.println("~~ "+methodKey);
									if(mtdkey.equals("*")) {
										continue;
									}
									addCallEdgeIfDefinitionKnown(cg, nonStaticMethodDefs, functionCall, methodKey, false);
									//a non static method can call a static method
									addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, functionCall, methodKey, false);
									continue;
								}
								getMethodCall(cg, functionCall, clsDefId, mtdkey, false);
							}	
						}
					}
					//call function
					else {
						if(firstArg.getProperty("type").equals("string")) {
							String funcname = ((StringExpression)(firstArg)).getEscapedCodeStr();
							addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, funcname, false);
							//addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, functionCall, funcname, false);
						}
						else if(firstArg.getProperty("type").equals("AST_MAGIC_CONST")
								&& (firstArg.getFlags().contains("MAGIC_METHOD")
										|| firstArg.getFlags().contains("MAGIC_FUNCTION"))){
							//cal your self
						}
						else {
							System.err.println("CCC"+functionCall);
							addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, "*", false);
							//addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, functionCall, methodKey, false);
						}
					}
				}
				
				
				// if call identifier is fully qualified,
				// just look for the function's definition right away
				if( callIdentifier.getFlags().contains( PHPCSVNodeTypes.FLAG_NAME_FQ)) {
					String functionKey = callIdentifier.getNameChild().getEscapedCodeStr();
					addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, functionKey, false);
				}

				// otherwise, i.e., if the call identifier is not fully qualified,
				// first look in the current namespace, then if the function is not found,
				// look in the global namespace
				// (see http://php.net/manual/en/language.namespaces.rules.php)
				else {
					boolean found = false;
					// note that looking in the current namespace first only makes
					// sense if we are not already in the global namespace anyway
					if( !callIdentifier.getEnclosingNamespace().isEmpty()) {
						String functionKey = callIdentifier.getEnclosingNamespace() + "\\"
								+ callIdentifier.getNameChild().getEscapedCodeStr();
						found = addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, functionKey, false);
					}
					
					// we did not find the function or were already in global namespace;
					// try to find the function in the global namespace
					if( !found) {
						String functionKey = callIdentifier.getNameChild().getEscapedCodeStr();
						addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, functionKey, false);
					}
					if(functionCall.getNodeId()==6088972) {
						System.err.println(functionCall);
					}
				}
			}
			//we don't know function name
			else {
				//System.err.println(functionCall);
				ParseVar parsevar = new ParseVar();
				parsevar.init(functionCall.getTargetFunc().getNodeId(), false, "");
				parsevar.handle();
				//System.err.println(varId);
				Set<String> classValues = parsevar.getVar();
				boolean findValue = true;
				for(String funName: classValues) {
					if(!funName.isEmpty() && funName.charAt(0)>='0' && funName.charAt(0)<='9') {
						findValue = false;
						break;
					}
					//System.err.println(funName+" "+functionCall);
					addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, funName, false);
				}
				if(findValue==false) {
					addCallEdgeIfDefinitionKnown(cg, functionDefs, functionCall, "*", false);
				}
				parsevar.reset();
			}
			//System.err.println("Statically unknown function call at node id " + functionCall.getNodeId() + "!");
		});
	}
	
	private static void getStaticCall(Identifier classIdentifier, String methodname, CG cg, StaticCallExpression staticCall) {
		if( classIdentifier.getFlags().contains( PHPCSVNodeTypes.FLAG_NAME_FQ)) {
			String staticMethodKey = classIdentifier.getNameChild().getEscapedCodeStr()
					+ "::" + methodname;
			if(!addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, staticCall, staticMethodKey, false)) {
				//System.err.println("Fully qualified static method name "+staticCall.getNodeId()+" "+staticMethodKey+"is not found");
			}
		}
		//parent::method
		else if(classIdentifier.getNameChild().getEscapedCodeStr().equals("parent")) {
			String className = staticCall.getEnclosingClass();
			String nameSpace = classIdentifier.getEnclosingNamespace();
			//get self::class nodeId
		    Long ClassDefId = getClassId(className,  staticCall.getNodeId(), nameSpace);
		    int find=0;
		    if(ClassDefId != null) {
		    	//System.err.println(ClassDefId);
		    	//if(classId==176423) {
		    	//	System.err.println(staticCall.getNodeId());
		    	//}
		    	List<Long> prtIds = ch2prt.get(ClassDefId);
		    	//if(classId==176423) {
		    	//	System.err.println(prtIds);
		    	//}
		    	//System.err.println(prtIds+" "+ClassDefId + staticCall.getNodeId());
		    	if(prtIds==null) {
		    		find++;
		    	}
		    	else for(Long prtId: prtIds) {
			    	//System.err.println(prtId);
			    	if(getMethodCall(cg, staticCall, prtId, methodname, false)){
				    	find++;
				    }
			    	
			    }
		    }
		}
		//static call
		else if(classIdentifier.getNameChild().getEscapedCodeStr().equals("static")) {
			String className = staticCall.getEnclosingClass();
			String nameSpace = classIdentifier.getEnclosingNamespace();
			Long prtId = getClassId(className, staticCall.getNodeId(), nameSpace);
			Set<Long> clds = getAllChild(prtId);
			//boolean find=false;getMethodCall
			clds.add(prtId);
			//System.err.println("origin: "+clds);
			for(Long cld: clds) {
				//System.err.println("after: "+clds);
				if(getMethodCall(cg, staticCall, cld, methodname, false)){
		    		//find = true;
		    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
			    }
				//System.err.println("after1: "+clds);
			}
			if(getMethodCall(cg, staticCall, prtId, methodname, false)){
	    		//find = true;
	    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
		    }
		}
		//self::method and className::method
		else {
				String className = classIdentifier.getNameChild().getEscapedCodeStr();
				String nameSpace = classIdentifier.getEnclosingNamespace();
				if (classIdentifier.getNameChild().getEscapedCodeStr().equals("self")) {
			    	className = staticCall.getEnclosingClass();
			    }
				Long ClassDefId = getClassId(className, staticCall.getNodeId(), nameSpace);
				
			    //int find=0;
			    if(ClassDefId != null) {
			    	//System.err.println(classId);
			    	if(!getMethodCall(cg, staticCall, ClassDefId, methodname, false)){
			    		
				    }
			    }
			    else{
			    	//System.err.println("Fail to find target class for static::method called at "+staticCall.getNodeId()+" "+className);
				}
		}
	}
	
	private static void createStaticMethodCallEdges(CG cg) {
		int x2 = staticMethodCalls.size();
		AtomicInteger c2 = new AtomicInteger(0);
		//for( StaticCallExpression staticCall : staticMethodCalls) {
		staticMethodCalls.parallelStream().forEach(staticCall -> {
			c2.incrementAndGet();
			System.err.println(x2+" "+c2+" "+staticCall.getNodeId());
			lockp.lock();
	        try {
	        	if(getDir(staticCall.getNodeId()).endsWith(".phtml")) {
	        		ignoreIds.put(staticCall.getFuncId(), (long)1);
	        	}
	        } finally {
	        	lockp.unlock();
	        }
			// make sure the call target is statically known
			if( staticCall.getTargetClass() instanceof Identifier
					&& staticCall.getTargetFunc() instanceof StringExpression) {
				
				Identifier classIdentifier = (Identifier)staticCall.getTargetClass();
				StringExpression methodName = (StringExpression)staticCall.getTargetFunc();
				
				// if class identifier is fully qualified,
				// just look for the static method's definition right away
				getStaticCall(classIdentifier, methodName.getEscapedCodeStr(), cg, staticCall);
			}
			//classname is a variable or methodname is a variable
			else{
				//methodname is a string and class name is a variable
				if(staticCall.getTargetFunc() instanceof StringExpression) {
					String methodKey = "*::"+staticCall.getTargetFunc().getEscapedCodeStr();
					if(!addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, staticCall, methodKey, false)) {
						//System.err.println("Fully qualified static method name "+staticCall.getNodeId()+" "+staticMethodKey+"is not found");
					}
				}
				//method name is a variable
				else{
					//class name is a string
					if(staticCall.getTargetClass() instanceof Identifier) {
						Identifier classIdentifier = (Identifier)staticCall.getTargetClass();
						Set<String> methodnames = getMethodName(staticCall);
						for(String methodname: methodnames) {
							getStaticCall(classIdentifier, methodname, cg, staticCall);
						}
					}
					else{
						System.err.println("Statically unknown static method call at node id " + staticCall.getNodeId() + "!");
					}
				}
			}
		});
	}

	private static void createConstructorCallEdges(CG cg) {
		int x3=constructorCalls.size();
		AtomicInteger c3 = new AtomicInteger(0);
		//for( NewExpression constructorCall : constructorCalls) {
		constructorCalls.parallelStream().forEach(constructorCall -> {
			c3.incrementAndGet();
			System.err.println(x3+" "+c3+" "+constructorCall.getNodeId());
			lockp.lock();
	        try {
	        	if(getDir(constructorCall.getNodeId()).endsWith(".phtml")) {
	        		ignoreIds.put(constructorCall.getFuncId(), (long)1);
	        	}
	        } finally {
	        	lockp.unlock();
	        }
			// make sure the call target is statically known
			if( constructorCall.getTargetClass() instanceof Identifier) {
				
				Identifier classIdentifier = (Identifier)constructorCall.getTargetClass();
				String constructorKey = classIdentifier.getNameChild().getEscapedCodeStr();
				String nameSpace = classIdentifier.getEnclosingNamespace();
				
				// if class identifier is fully qualified,
				// just look for the constructor's definition right away
				
				//ignore built-in class
				
				if( classIdentifier.getFlags().contains( PHPCSVNodeTypes.FLAG_NAME_FQ)) {
					//if(!addCallEdgeIfDefinitionKnown(cg, constructorDefs, constructorCall, constructorKey)) {
						//System.err.println("Fully qualified constructor name "+constructorCall.getNodeId()+" "+constructorKey+" is not found");
					//}
					//addCallEdgeIfDefinitionKnown(cg, constructorDefs, constructorCall, constructorKey, false);
					Long classId = getClassId(constructorKey, constructorCall.getNodeId(), "");
					getMethodCall(cg, constructorCall, classId, "__construct", false);
					//getMethodCall(cg, constructorCall, classId, "__destruct", false);
				}
				else if(constructorKey.equals("static")) {
					constructorKey = constructorCall.getEnclosingClass();
					Long prtId = getClassId(constructorKey, constructorCall.getNodeId(), nameSpace);
					Set<Long> clds = getAllChild(prtId);
					boolean find=false;
					for(Long cld: clds) {
						if(getMethodCall(cg, constructorCall, cld, "__construct", false)){
				    		find = true;
				    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
					    }
						//getMethodCall(cg, constructorCall, cld, "__destruct", false);
					}
					if(getMethodCall(cg, constructorCall, prtId, "__construct", false)){
			    		find = true;
			    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
				    }
					//getMethodCall(cg, constructorCall, prtId, "__destruct", false);
					if(find==false) {
						//System.err.println("Fail to find target method for new static constructor called at"+ constructorCall.getNodeId());
					}
				}
				else {
					if (constructorKey.equals("self")) {
						constructorKey = constructorCall.getEnclosingClass();
				    }
					
					Long ClassDefId = getClassId(constructorKey, constructorCall.getNodeId(), nameSpace);
					//System.err.println(classIdentifier.getNodeId()+" "+ClassDefId);
				    //int find=0;
				    if(ClassDefId != null) {
				    	//System.err.println(classId);
				    	if(!getMethodCall(cg, constructorCall, ClassDefId, "__construct", false)){
				    		//class may do not have a defined constructor
				    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
					    }
				    	//getMethodCall(cg, constructorCall, ClassDefId, "__destruct", false);
				    }
				    else {
				    	//System.err.println("Fail to find target class for constructor called at "+constructorCall.getNodeId()+" "+constructorKey);
					}
				}
			}
			else {
				//System.err.println("!!!"+constructorCall);
				String constructorKey = "*";
				addCallEdgeIfDefinitionKnown(cg, constructorDefs, constructorCall, constructorKey, false);
				//addCallEdgeIfDefinitionKnown(cg, destructorDefs, constructorCall, constructorKey, false);
			}
		});
	}

	private static void withMethodKey(CG cg, MethodCallExpression callsite, String methodKey) {
		if((callsite).getTargetObject() instanceof Variable){
			Variable classVar = (Variable)callsite.getTargetObject();
			ParseVarCall(cg, classVar, callsite, methodKey);
		}
		//var->prop->method
		else if(callsite.getTargetObject() instanceof PropertyExpression){
			PropertyExpression classProp = (PropertyExpression)callsite.getTargetObject();
			ParseVarCall(cg, classProp, callsite, methodKey);
			//System.err.println("Unknown methoddCall type at node id "+methodCall.getNodeId());
		}
		//new class->method
		else if(callsite.getTargetObject() instanceof NewExpression) {
			NewExpression classNew = (NewExpression) callsite.getTargetObject();
			if(classNew.getTargetClass() instanceof Identifier) {
				Identifier classNode = (Identifier) classNew.getTargetClass();
				String className = classNode.getNameChild().getEscapedCodeStr();
				String namespace = classNode.getEnclosingNamespace();
				Long ClassDefId = getClassId(className, callsite.getNodeId(), namespace);
				//System.err.println(className+callsite+namespace+ClassDefId);
				if(ClassDefId==null) {
					return;
				}
				if(!getMethodCall(cg, callsite, ClassDefId, methodKey, false)) {
					//System.err.println("Fail to get target class for new class::method");
				}
			}
			else {
				HashMap<Long, Long> key = new HashMap<Long, Long>();
				key.put(callsite.getTargetObject().getNodeId(), callsite.getNodeId());
				lock1.lock();
		        try {
		        	save.put(key, methodKey);
		        } finally {
		            lock1.unlock();
		        }
			}
		}
		//We don't parse variables with strange representation 
		else { 
			HashMap<Long, Long> key = new HashMap<Long, Long>();
			key.put(callsite.getTargetObject().getNodeId(), callsite.getNodeId());
			lock1.lock();
	        try {
	        	save.put(key, methodKey);
	        } finally {
	            lock1.unlock();
	        }
			//System.err.println("Unknown methoddCall type at node id "+methodCall.getNodeId());
		}
	}
	
	private static void createNonStaticMethodCallEdges(CG cg) {
		int x4=nonStaticMethodCalls.size();
		AtomicInteger c4 = new AtomicInteger(0);
		//for( MethodCallExpression methodCall : nonStaticMethodCalls) {
		nonStaticMethodCalls.parallelStream().forEach(methodCall -> {
			c4.getAndIncrement();
			System.err.println(x4+" "+c4+" "+methodCall.getNodeId());
			lockp.lock();
	        try {
	        	if(getDir(methodCall.getNodeId()).endsWith(".phtml")) {
	        		ignoreIds.put(methodCall.getFuncId(), (long)1);
	        	}
	        } finally {
	        	lockp.unlock();
	        }
			//System.err.println(methodCall.getNodeId());
			// make sure the call target is statically known
			if( methodCall.getTargetFunc() instanceof StringExpression) {
				StringExpression methodName = (StringExpression)methodCall.getTargetFunc();
				String methodKey = methodName.getEscapedCodeStr();
				// let's count the dynamic methods
				if( nonStaticMethodNameDefs.containsKey(methodKey)) {
					//System.err.println(methodKey);
					// check whether there is only one matching function definition
					if( nonStaticMethodNameDefs.get(methodKey).size() == 1) {
						//addCallEdge( cg, methodCall, nonStaticMethodNameDefs.get(methodKey).get(0), false);
						lock.lock();
				        try {
				        	call2mtd.add(methodCall.getNodeId(), nonStaticMethodNameDefs.get(methodKey).get(0).getNodeId());
				        	mtd2mtd.add(methodCall.getFuncId(), nonStaticMethodNameDefs.get(methodKey).get(0).getNodeId());
				        } finally {
				            lock.unlock();
				        }
					}
					else { // there is more than one matching function definition
						// we can still map $this->foo(), though, because we know what $this is
						List<Method> allMatch = nonStaticMethodNameDefs.get(methodKey);
						Long firstClass = getClassId("-1", allMatch.get(0).getNodeId(), "-1");
						boolean flag = true;
						for(Method candidate: allMatch) {
							Long crtClass = getClassId("-1", candidate.getNodeId(), "-1");
							if(!(getAllChild(firstClass).contains(crtClass) 
									|| getAllChild(crtClass).contains(firstClass))) {
								flag = false;
							}
						}
						//override methods
						if(flag==true) {
							for(Method candidate: allMatch) {
								lock.lock();
						        try {
						        	call2mtd.add(methodCall.getNodeId(), candidate.getNodeId());
						        	mtd2mtd.add(methodCall.getFuncId(), candidate.getNodeId());
						        } finally {
						            lock.unlock();
						        }
								System.err.println("override "+candidate+" "+candidate.getName());
							}
							return;
						}
						
						if( methodCall.getTargetObject() instanceof Variable
							&& ((Variable)methodCall.getTargetObject()).getNameExpression() instanceof StringExpression
							&& ((StringExpression)((Variable)methodCall.getTargetObject()).getNameExpression()).getEscapedCodeStr().equals("this")) {
							
							String enclosingClass = methodCall.getEnclosingClass();
							String nameSpace = methodCall.getEnclosingNamespace();
							
							//allThisinParentCall
							//Long callsiteFun = methodCall.getFuncId();
							//collectThis.add(callsiteFun, methodKey);
							
							Long ClassDefId = getClassId(enclosingClass, methodCall.getNodeId(), nameSpace);
									
							//System.err.println(classIdentifier.getNodeId()+" "+ClassDefId);
						    //int find=0;
						    if(ClassDefId != null) {
						    	//System.err.println(ClassDefId+methodKey);
						    	if(!getMethodCall(cg, methodCall, ClassDefId, methodKey, false)){
						    		//this->abstract methods, call abstract methods which are implemented in its child classes
						    		boolean find=false;
						    		Set<Long> clds = getAllChild(ClassDefId);
						    		
						    		for(Long cld: clds){
						    			if(getMethodCall(cg, methodCall, cld, methodKey, true)) {
						    				find=true;
						    			}
						    		}
						    		if(find==false) {
						    			//System.err.println("Fail to find target method for $this->method"
							    				//+ " called at "+methodCall.getNodeId()+" "+methodKey);
						    		}
							    }
						    }
						    else {
						    	//System.err.println("Fail to find target class for $this->method"
						    			//+ " called at " + methodCall.getNodeId()+" "+enclosingClass);
						    }
						   					
						}
						//var->method
						else {
							//System.err.println(methodCall);
							withMethodKey(cg, methodCall, methodKey);
						}
					}
					
				}
				//calling method name is not defined  
				else {
					//System.err.println("Fail to find target method name for method"+ " called at "+methodCall.getNodeId());
				}
			}
			
			//methodName is also a variable
			else {
				String enclosingClass = methodCall.getEnclosingClass();
				String nameSpace = methodCall.getEnclosingNamespace();
				Set<Long> ClassDefId = new HashSet<Long>();
				Set<String> methodname = new HashSet<String>();
				//Set<Long> classId = new HashSet<Long>();
				
				//we first get class name(if we can get it)
				if( methodCall.getTargetObject() instanceof Variable
						&& ((Variable)methodCall.getTargetObject()).getNameExpression() instanceof StringExpression
						&& ((StringExpression)((Variable)methodCall.getTargetObject()).getNameExpression()).getEscapedCodeStr().equals("this")) {
					
					Long callsiteFun = methodCall.getFuncId();
					//collectThis.add(callsiteFun, "*");
					ClassDefId.add(getClassId(enclosingClass, methodCall.getNodeId(), nameSpace));
					//System.err.println(ClassDefId);
					//classId.add(ClassDefId);
				}
				else if(methodCall.getTargetObject() instanceof NewExpression && 
						((NewExpression) methodCall.getTargetObject()).getTargetClass() instanceof Identifier) {
					NewExpression classNew = (NewExpression) methodCall.getTargetObject();
					Identifier classNode = (Identifier) classNew.getTargetClass();
					String className = classNode.getNameChild().getEscapedCodeStr();
					String namespace = classNode.getEnclosingNamespace();
					ClassDefId.add(getClassId(className, methodCall.getNodeId(), namespace));
					//classId.add(ClassDefId);
				}
				else if(methodCall.getTargetObject() instanceof Variable ||
						methodCall.getTargetObject() instanceof PropertyExpression){
					ASTNode classVar = methodCall.getTargetObject();
					ParseVar parsevar = new ParseVar();
					parsevar.init(classVar.getNodeId(), true, "");
					parsevar.handle();
					Set<String> classValue = parsevar.getVar();
					//System.err.println(classValue);
					for(String classvalue: classValue) {
						//String classvalue = classValue.iterator().next();
						//we get it from a return value
						if(classvalue.charAt(0)=='#') {
							System.err.println(classVar);
							classvalue = classvalue.substring(1);
							ClassDefId.add(Long.parseLong(classvalue));
							//System.err.println(ClassDefId);
						}
						else if(classvalue.charAt(0)>'9' || classvalue.charAt(0)<'0') {
							ClassDefId.add(getClassId(classvalue, methodCall.getNodeId(), nameSpace));
							//System.err.println(ClassDefId);
						}
					}
					parsevar.reset();
				}
				else {
					ClassDefId.add((long) -1);
				}
				
				methodname = getMethodName(methodCall);
				
				
				//System.err.println(ClassDefId);
				//System.err.println(methodname);
				
				for(Long clsDefId: ClassDefId) {
					if(clsDefId==null) {
						continue;
					}
					for(String mtdname: methodname) {
						if(clsDefId==-1) {
							String methodKey = "*::"+mtdname;
							addCallEdgeIfDefinitionKnown(cg, nonStaticMethodDefs, methodCall, methodKey, false);
							//a non static method can call a static method
							addCallEdgeIfDefinitionKnown(cg, staticMethodDefs, methodCall, methodKey, false);
							continue;
						}
						getMethodCall(cg, methodCall, clsDefId, mtdname, false);
					}
				}
				
				//System.err.println("Statically unknown non-static method call at node id " + methodCall.getNodeId());
			}
		});
	}
	
	private static Set<String> getMethodName(CallExpressionBase callsite){
		Set<String> ret = new HashSet<String>();
		//then try to get method name
		if(callsite.getTargetFunc() instanceof BinaryOperationExpression) {
			BinaryOperationExpression methodNameNode = (BinaryOperationExpression) callsite.getTargetFunc();
			ParseVar parsevar = new ParseVar();
			parsevar.init(1, false, "");
			LinkedList<String> methodNameStrs = parsevar.ParseExp(methodNameNode);
			//System.err.println(methodNameStrs);
			if(methodNameStrs.size()==1) {
				String methodNameStr = methodNameStrs.iterator().next();
				if(methodNameStr.charAt(0)<'0' || methodNameStr.charAt(0)>'9') {
					ret.add(methodNameStr);
				}
			}
			parsevar.reset();
		}
		else if(callsite.getTargetFunc() instanceof Variable) {
			Variable methodNameNode = (Variable) callsite.getTargetFunc();
			ParseVar parsevar = new ParseVar();
			parsevar.init(methodNameNode.getNodeId(), false, "");
			parsevar.handle();
			Set<String> methodNameStrs = parsevar.getVar();
			for(String methodNameStr: methodNameStrs) {
				if(methodNameStr.charAt(0)>'9' || methodNameStr.charAt(0)<'0') {
					ret.add(methodNameStr);
				}
			}
			parsevar.reset();
		}
		if(ret.isEmpty()) {
			ret.add("*");
		}
		return ret;
	}
	
	/**
	 * Checks whether a given function key is known and if yes,
	 * adds a corresponding edge in the given call graph.
	 * 
	 * @return true if an edge was added, false otherwise
	 */
	private static boolean addCallEdgeIfDefinitionKnown(CG cg, MultiHashMap<String,? extends FunctionDef> defSet, CallExpressionBase functionCall, String functionKey, boolean prt2cld) {
		
		boolean ret = false;
		
		//System.err.println("FFF "+functionKey);
		//It's a function call and we can't parse function name or it's a constructor call and classname is a variable
		if(functionKey.contains("*")) {
			unknownIds.add(functionCall.getNodeId());
			//unknownCallsite++;
			//System.err.println(functionKey+" "+functionCall);
			String functioname = functionKey.replace("*", "");
			
			if((functioname.equals("::") || functioname.equals(""))
					&& !getDir(functionCall.getNodeId()).contains("/vendor/")
					&& !(functionCall instanceof NewExpression)) {
				omitIds.add(functionCall.getFuncId());
				omit++;
				return true;
			}
			
			/*
			if(name2Def.containsKey(functionKey)) {
				for(FunctionDef funDef: name2Def.get(functionKey)) {
					//mtd2mtd.add(functionCall.getFuncId(), funDef.getNodeId());
					ret = addCallEdge( cg, functionCall, funDef, prt2cld) || ret;
				}
				return ret;
			}
			*/
			
			//for(String funcKey: defSet.keySet()) {
			
			lock.lock();
	        try {
	        	suspicious.add(functionCall.getNodeId());
	        } finally {
	            lock.unlock();
	        }
			
			defSet.keySet().parallelStream().forEach(funcKey -> {
				if(funcKey.contains(functioname)) {
					//name2Def.addAll(functionKey, defSet.get(funcKey));
					for(FunctionDef func: defSet.get(funcKey)){
					//defSet.get(funcKey).parallelStream().forEach(func -> {
						if(addCallEdge( cg, functionCall, func, prt2cld)){
							addAllcld(cg, func, funcKey, functionCall, prt2cld, defSet);
						}
					}
				}
			});
			
			return true;
		}
		
		//System.err.println("aaa "+functionKey + defSet);
		
		// check whether we know the called function
		if( defSet.containsKey(functionKey))	{
			//System.err.println("bbb "+defSet.get(functionKey));
			for(FunctionDef func: defSet.get(functionKey)) {
				if(addCallEdge( cg, functionCall, func, prt2cld)) {
					addAllcld(cg, func, functionKey, functionCall, prt2cld, defSet);
				}
			}
		}
		
		return ret;
	}
	
	private static void addAllcld(CG cg, FunctionDef func, String funcKey, CallExpressionBase functionCall, boolean prt2cld, MultiHashMap<String,? extends FunctionDef> defSet) {
		if(func instanceof Method) {
			Method prt = (Method) func;
			String classname = prt.getEnclosingClass();
			String namespace = prt.getEnclosingNamespace();
			Long prtCls = getClassId(classname, prt.getNodeId(), namespace);
			Set<Long> clds = getAllChild(prtCls);
			for(Long cld: clds) {
				ClassDef clsNode = (ClassDef) ASTUnderConstruction.idToNode.get(cld);
				String clsname = clsNode.getName();
				String clsnamespace = clsNode.getEnclosingNamespace();
				String[] tmp = funcKey.split("::");
				String funname;
				if(tmp.length>1) {
					funname = "::"+tmp[1];
				}
				//constructor
				else {
					funname = "";
				}
				String newKey = clsname+funname;
				if( !clsnamespace.isEmpty())
					newKey = clsnamespace + "\\" + newKey;
				if(defSet.containsKey(newKey)) {
					for(FunctionDef func1: defSet.get(newKey)){
						addCallEdge( cg, functionCall, func1, prt2cld);	
					}
				}
			}
		}
	}
	
	/**
	 * Adds an edge to a given call graph.
	 * 
	 * @return true if an edge was added, false otherwise
	 */
	private static boolean addCallEdge(CG cg, CallExpressionBase functionCall, FunctionDef functionDef, boolean prt2cld) {
		
		//boolean ret = false;
		/*
		if(ignoreIds.containsKey(functionDef.getNodeId())
				|| mtd2mtd.containsKey(functionCall.getFuncId()) 
				&& mtd2mtd.get(functionCall.getFuncId()).contains(functionDef.getNodeId())) {
			return true;
		}
		*/
		
		if(getDir(functionCall.getNodeId()).contains("vendor")
				&& !getDir(functionDef.getNodeId()).contains("vendor")) {
			return false;
		}
		
		Long funid = functionCall.getFuncId();
		while(ASTUnderConstruction.idToNode.get(funid) instanceof Closure) {
			funid = ASTUnderConstruction.idToNode.get(funid).getFuncId();
		}
		
		lock.lock();
        try {
        	//mtd2mtd.add(funid, functionDef.getNodeId());
        	if(call2mtd.containsKey(functionCall.getNodeId())
        			&& call2mtd.get(functionCall.getNodeId()).contains(functionDef.getNodeId())) {
        		return true;
        	}
        } finally {
            lock.unlock();
        }
		
		//List<String> files = new ArrayList<String>();
		
		/*
		
		if(filecache.containsKey(getDir(functionCall.getNodeId()))) {
			files = filecache.get(getDir(functionCall.getNodeId()));
		}
		Long funDefId = functionDef.getNodeId();
		String funDefpath = getDir(funDefId);
		boolean flag = false;
		for(String file: files) {
			if (funDefpath.contains(file)) {
				flag = true;
				break;
			}
		}
		
		if(flag==false
				&&!(allUse.containsKey(toTopLevelFile.getTopLevelId(functionCall.getNodeId()))
						&&allUse.get(toTopLevelFile.getTopLevelId(functionCall.getNodeId())).contains(functionDef.getEnclosingNamespace()+"\\"+functionDef.getEnclosingClass()))) {
			return false;
		}
		*/
		
		//call site arguments number must bigger than function definition parameter's number
		int callArgSize = functionCall.getArgumentList().size();
		//System.err.println(functionCall);
		int functionDefSize = functionDef.getParameterList().size();
		
		if(callArgSize>functionDefSize 
				&&!func_get_args.contains(functionDef.getNodeId())
				&&!call_user.contains(functionCall.getNodeId())) {
			return false;
		}
		
		
		if(functionDef instanceof Method && 
				(functionDef.getFlags().contains("MODIFIER_PRIVATE") ||  
						functionDef.getFlags().contains("MODIFIER_PROTECTED"))) {
			String callsiteClassName = functionCall.getEnclosingClass();
			String callsiteNamespace = functionCall.getEnclosingNamespace();
			Long callsiteClassId = getClassId(callsiteClassName, functionCall.getNodeId(), callsiteNamespace);
			
			String mtdDefClassName = ((Method) functionDef).getEnclosingClass();
			String mtdDefNamespace = functionDef.getEnclosingNamespace();
			Long mtdClsId =  getClassId(mtdDefClassName, functionDef.getNodeId(), mtdDefNamespace);
			Set<Long> cldIds = getAllChild(mtdClsId);
			cldIds.add(mtdClsId);
			
			//call from a function
			if(callsiteClassId==null) {
				return false;
			}
			//only methods in the same class could call this method
			else if(functionDef.getFlags().contains("MODIFIER_PRIVATE")) {
				if(!callsiteClassId.equals(mtdClsId)) {
					return false;
				}
			}
			else if(functionDef.getFlags().contains("MODIFIER_PROTECTED")) {
				if(!cldIds.contains(callsiteClassId) && prt2cld==false) {
					return false;
				}
			}
		}
		
		if(functionCall instanceof StaticCallExpression 
				&& ((StaticCallExpression)functionCall).getTargetClass() instanceof Identifier ) {
			Identifier classname = (Identifier) ((StaticCallExpression)functionCall).getTargetClass();
			if(!classname.getNameChild().getEscapedCodeStr().equals("parent") 
					&& !classname.getNameChild().getEscapedCodeStr().equals("self")
					&& !classname.getNameChild().getEscapedCodeStr().equals("static")) {
				//System.err.println("123 "+classname.getNameChild().getEscapedCodeStr());
				if(!(functionDef instanceof Method && functionDef.getFlags().contains("MODIFIER_STATIC"))){
					return false;
				}
			}
		}
		
		if(functionDef instanceof Method) {
			String classKey = ((Method)functionDef).getEnclosingClass();
			if( !functionDef.getEnclosingNamespace().isEmpty())
				classKey = functionDef.getEnclosingNamespace() + "\\" + classKey;
			classUsed.add(classKey);
		}
		
		lock.lock();
		try {
			call2mtd.add(functionCall.getNodeId(), functionDef.getNodeId());
        } finally {
            lock.unlock();
        }
		
		
		lock.lock();
        try {
        	if(suspicious.contains(functionCall.getNodeId())) {
        		return true;
        	}
        } finally {
            lock.unlock();
        }
        
		
		if(!functionCall.getFuncId().equals(functionDef.getNodeId())){
			lock.lock();
	        try {
	        	mtd2mtd.add(funid, functionDef.getNodeId());
	        } finally {
	            lock.unlock();
	        }
		}
		//callsite is needed, thus functionDef is needed.
		
		/*
		if(ignoreIds.containsKey(functionCall.getFuncId())) {
			ignoreIds.put(functionDef.getNodeId(), (long) 1);
		}
		*/
		
		System.err.println(functionCall+" "+functionDef);
		
		//for(Long id: topFunIds) {
		/*
		topFunIds.parallelStream().forEach(id -> {
			//System.err.println(id+" "+getDir(id));
			if(getDir(id)==null) {
				System.err.println("xxx "+id);
			}
			if(getDir(id).equals(getDir(functionDef.getNodeId()))) {
				ignoreIds.put(id, (long) 1);
				//break;
				//System.err.println("new file");
			}
		});
		*/
		return true;
	}
	
	private static void reset() {
	
		functionDefs.clear();
		functionCalls.clear();
		
		staticMethodDefs.clear();
		staticMethodCalls.clear();
		
		//constructorDefs.clear();
		constructorCalls.clear();
		
		nonStaticMethodDefs.clear();
		nonStaticMethodNameDefs.clear();
		nonStaticMethodCalls.clear();

	}
	
	/**
	 * Adds a new known function definition.
	 * 
	 * @param functionDef A PHP function definition. If a function definition with the same
	 *                    name was previously added, then the new function definition will
	 *                    be used for that name and the old function definition will be returned.
	 * @return If there already exists a PHP function definition with the same name,
	 *         then returns that function definition. Otherwise, returns null. For non-static method
	 *         definitions, always returns null.
	 */
	public static FunctionDef addFunctionDef( FunctionDef functionDef) {
		
		
		allFuncDef.add(functionDef.getNodeId());
		// artificial toplevel functions wrapping toplevel code cannot be called
		if( functionDef instanceof TopLevelFunctionDef) {
			topFunIds.add(functionDef.getNodeId());
			//collectAllFun.add(functionDef.getNodeId());
			return null;
		}
			
		// we also ignore closures as they do not have a statically known reference
		else if( functionDef instanceof Closure)
			return null;
		
		// finally, abstract methods cannot be called either
		else if( functionDef instanceof Method
				&& functionDef.getFlags().contains(PHPCSVNodeTypes.FLAG_MODIFIER_ABSTRACT)) {
			Abstract.add(functionDef.getNodeId());
			return null;
		}
		
		
		// it's a static method
		else if( functionDef instanceof Method
				&& functionDef.getFlags().contains(PHPCSVNodeTypes.FLAG_MODIFIER_STATIC)) {
			// use A\B\C::foo as key for a static method foo in class A\B\C
			String staticMethodKey = ((Method)functionDef).getEnclosingClass() + "::" + functionDef.getName();
			String cls = ((Method)functionDef).getEnclosingClass();
			
			if( !functionDef.getEnclosingNamespace().isEmpty()) {
				staticMethodKey = functionDef.getEnclosingNamespace() + "\\" + staticMethodKey;
				cls = functionDef.getEnclosingNamespace() + "\\" + cls;
			}
				
			//magic function
			if(functionDef.getName().startsWith("__")) {
				magicMtdDefs.add(functionDef.getNodeId());
			}
			
			
			fullname2Id.add(staticMethodKey, functionDef.getNodeId());
			id2Name.put(functionDef.getNodeId(), cls);
			collectAllFun.add(functionDef.getNodeId());
			nonStaticMethodNameDefs.add(((Method)functionDef).getName(), (Method)functionDef);
			staticMethodDefs.add( staticMethodKey, (Method)functionDef);
			return null;
		}
		
		// it's a constructor
		// Note that a PHP constructor cannot be static, so the previous case for static methods evaluates to false;
		// also note that there are two possible constructor names: __construct() (recommended) and ClassName() (legacy)
		else if( functionDef instanceof Method
				&& (functionDef.getName().equals("__construct")
						|| functionDef.getName().equals(((Method)functionDef).getEnclosingClass()))) {
				
			// use A\B\C as key for the unique constructor of a class A\B\C
			String constructorKey = ((Method)functionDef).getEnclosingClass();
			if( !functionDef.getEnclosingNamespace().isEmpty())
				constructorKey = functionDef.getEnclosingNamespace() + "\\" + constructorKey;
			
			//id2Name.add(functionDef.getNodeId(), cls);
			fullname2Id.add(constructorKey, functionDef.getNodeId());
			name2Id.add(constructorKey, functionDef.getNodeId());
			collectAllFun.add(functionDef.getNodeId());
			//condes.add(functionDef.getNodeId());
			constructorDefs.add( constructorKey, (Method)functionDef);
			return null;
		}
		
		else if( functionDef instanceof Method
				&& (functionDef.getName().startsWith("__"))) {
				
			// use A\B\C as key for the unique constructor of a class A\B\C
			String constructorKey = ((Method)functionDef).getEnclosingClass();
			if( !functionDef.getEnclosingNamespace().isEmpty())
				constructorKey = functionDef.getEnclosingNamespace() + "\\" + constructorKey + "::__destruct";
			
			fullname2Id.add(constructorKey, functionDef.getNodeId());
			condes.add(functionDef.getNodeId());
			destructorDefs.add( constructorKey, (Method)functionDef);
			return null;
		}
		
		// other methods than the above are non-static methods
		else if( functionDef instanceof Method) {
			// use foo as key for a non-static method foo in any class in any namespace;
			// note that the enclosing namespace of a non-static method definition is irrelevant here,
			// as that is usually not known at the call site (neither is the class name, except
			// when the keyword $this is used)
			//String methodKey = ((Method)functionDef).getName();
			String cls = ((Method)functionDef).getEnclosingClass();
			String methodKey = ((Method)functionDef).getEnclosingClass() + "::" + ((Method)functionDef).getName();
			if( !functionDef.getEnclosingNamespace().isEmpty()) {
				methodKey = functionDef.getEnclosingNamespace() + "\\" + methodKey;
				cls = functionDef.getEnclosingNamespace() + "\\" + cls;
			}
				
			
			//collect all magic methods
			if(((Method)functionDef).getName().startsWith("__")) {
				magicMtdDefs.add(functionDef.getNodeId());
			}
			
			id2Name.put(functionDef.getNodeId(), cls);
			fullname2Id.add(methodKey, functionDef.getNodeId());
			nonStaticMethodNameDefs.add(((Method)functionDef).getName(), (Method)functionDef);
			nonStaticMethodDefs.add( methodKey, (Method)functionDef);
			
			collectAllFun.add(functionDef.getNodeId());
			return null;
		}
		
		// it's a function (i.e., not inside a class)
		else {
			// use A\B\foo as key for a function foo() in namespace \A\B
			String functionKey = functionDef.getName();
			if( !functionDef.getEnclosingNamespace().isEmpty())
				functionKey = functionDef.getEnclosingNamespace() + "\\" + functionKey;
			
			fullname2Id.add(functionKey, functionDef.getNodeId());
			collectAllFun.add(functionDef.getNodeId());
			/*
			if(functionKey.equals("define")) {
				System.err.println(functionDef);
			}
			*/
			functionDefs.add( functionKey, functionDef);
			return null;
		}		
	}
	
	/**
	 * Adds a new function call.
	 * 
	 * @param functionCall A PHP function/method/constructor call. An arbitrary number of
	 *                     distinguished calls to the same function/method/constructor can
	 *                     be added.
	 */
	public static boolean addFunctionCall( CallExpressionBase callExpression) {
		
		// Note: we cannot access any of the CallExpression's getter methods here
		// because this method is called from the PHPCSVNodeInterpreter at the point
		// where it constructs the CallExpression. That is, this method is called for each
		// CallExpression *immediately* after its construction. At that point, the PHPCSVNodeInterpreter
		// has not called the CallExpression's setter methods  (as it has not yet interpreted the
		// corresponding CSV lines).
		// Hence, we only store the references to the CallExpression objects themselves.
	
		callsiteNumber++;
		if( callExpression instanceof StaticCallExpression)
			return staticMethodCalls.add( (StaticCallExpression)callExpression);
		else if( callExpression instanceof NewExpression)
			return constructorCalls.add( (NewExpression)callExpression);
		else if( callExpression instanceof MethodCallExpression)
			return nonStaticMethodCalls.add( (MethodCallExpression)callExpression);
		else
			return functionCalls.add( callExpression);
	}
	
	//classDefId::methodName
	private static boolean getMethodCall(CG cg, CallExpressionBase methodCall, Long classDefId, String methodName, boolean prt2cld) {
		Queue<Long> parents = new LinkedList<Long>();
		parents.offer(classDefId);
		//System.err.println(classDefId);
		
		methodName = "::"+methodName;
		if(methodName.equals("::__construct" ) || methodName.equals("::" )) {
			methodName = "";
		}
		
		MultiHashMap<String, Method> allMethodDefs = new MultiHashMap<String, Method>();
		
		if(methodCall instanceof NewExpression) {
			allMethodDefs.putAll(constructorDefs);
		}
		else if(methodCall instanceof StaticCallExpression
				&& ((StaticCallExpression) methodCall).getTargetClass() instanceof Identifier
				&& !((Identifier) (((StaticCallExpression) methodCall).getTargetClass())).getNameChild().getEscapedCodeStr().equals("parent")
				&& !((Identifier) (((StaticCallExpression) methodCall).getTargetClass())).getNameChild().getEscapedCodeStr().equals("static")
				&& !((Identifier) (((StaticCallExpression) methodCall).getTargetClass())).getNameChild().getEscapedCodeStr().equals("self")) {
			allMethodDefs.putAll(staticMethodDefs);
		}
		else {
			allMethodDefs.putAll(nonStaticMethodDefs);
			allMethodDefs.putAll(constructorDefs);
			allMethodDefs.putAll(staticMethodDefs);
		}
		
		while(!parents.isEmpty()) {
			Long currentClassId = parents.poll();
			//
			ClassDef currentClassNode = (ClassDef) ASTUnderConstruction.idToNode.get(currentClassId);
			if(currentClassNode==null) {
				//System.err.println("!!"+methodCall.getNodeId());
				return false;
			}
			
			String Namespace = currentClassNode.getEnclosingNamespace();
			String className = currentClassNode.getName();
			String MethodKey = new String();
			
			if( !Namespace.isEmpty()) {
				MethodKey = Namespace + "\\" + className + methodName;
			}
			else {
				MethodKey = className + methodName;
			}	
			
			//System.err.println(staticMethodKey);
			//static call only call static method
			if(addCallEdgeIfDefinitionKnown(cg, allMethodDefs, methodCall, MethodKey, prt2cld)) {
				return true;
			}
			if(ch2prt.containsKey(currentClassId)) {
				List<Long> prtIds = ch2prt.get(currentClassId);
				//System.err.println(currentClassId);
				for(Long prt: prtIds) {
					if(prt!=currentClassId) {
						parents.offer(prt);
					}
				}
			}
		}
		return false;
	}
	
	//From className get its classId
	public static Long getClassId(String className, Long callSiteId, String nameSpace) {
		if(className.equals("-1")) {
			className = ASTUnderConstruction.idToNode.get(callSiteId).getEnclosingClass();
			nameSpace = ASTUnderConstruction.idToNode.get(callSiteId).getEnclosingNamespace();
		}
		Long classId = null;
		HashMap<String, String> alias;
		//LinkedList<String> inclusion = Inclusion.getInclusion(toTopLevelFile.getTopLevelId(callSiteId));
		lockC.lock();
        try {
        	alias = Inclusion.getAliasMap(toTopLevelFile.getTopLevelId(callSiteId));
        } finally {
            lockC.unlock();
        }
        String fullName = nameSpace + "\\" + className;;
		String aliaName = "-1";
		
		if(alias.containsKey(className)) {
			aliaName = alias.get(className) ;
		}
		
		if(nameSpace==null || nameSpace.equals("")){
			fullName = className;
		}
		
		for(String clsDef: classDef.keySet()) {
			if(clsDef.equals(aliaName)) {
				//System.err.println(clsDef);
				classId = classDef.get(clsDef);
				return classId;
			}
		}
		
		for(String clsDef: classDef.keySet()) {
			if(clsDef.equals(fullName)) {
				//System.err.println(clsDef);
				classId = classDef.get(clsDef);
				return classId;
			}
		}
		for(String clsDef: classDef.keySet()) {
			if(clsDef.equals(className)) {
				classId = classDef.get(clsDef);
				return classId;
			}
		}
		return classId;
	}
	
	private static Set<Long> getParentClassId(Long ClassId){
		Set<Long> prtIds = new HashSet<Long>();
		//LinkedList<String> inclusion = Inclusion.getInclusion(toTopLevelFile.getTopLevelId(ClassId));
		HashMap<String, String> alias = Inclusion.getAliasMap(toTopLevelFile.getTopLevelId(ClassId));
		
		Set<String> inheritance = new HashSet<String>();
		for(Long prtId: inhe.get(ClassId)) {
			ParseVar parsevar = new ParseVar();
			parsevar.init(1, false, "");
			inheritance.addAll(parsevar.ParseExp(ASTUnderConstruction.idToNode.get(prtId)));
		}
		
		ClassDef ClassNote = (ClassDef) ASTUnderConstruction.idToNode.get(ClassId);
		String nameSpace = ClassNote.getEnclosingNamespace();
		String fullName = new String();
		
		boolean find = false;
		for(String prt: inheritance) {
			if(alias.containsKey(prt)) {
				fullName =alias.get(prt) ;
			}
			else {
				fullName = nameSpace + "\\" + prt;
			}
			
			for(String clsDef: classDef.keySet()) {
				if(clsDef.equals(fullName) || clsDef.equals(prt)) {
					//System.err.println(clsDef);
					prtIds.add(classDef.get(clsDef));
					find = true;
					break;
				}
			}
		}
		if(find==false && inheritance.size()!=0 ) {
			//System.err.println("Fail to find parent class for class "+ inheritance.iterator().next() + " "+ClassId);
		}
		return prtIds;
	}
	
	private static void ParseVarCall(CG cg, ASTNode classVar, CallExpressionBase callsite, String methodName) {
		String nameSpace = classVar.getEnclosingNamespace();
		long varId = classVar.getNodeId();
		//System.err.println(varId);
		ParseVar parsevar = new ParseVar();
		parsevar.init(varId, true, "");
		parsevar.handle();
		//System.err.println(varId);
		Set<String> classValues = parsevar.getVar();
		
		
		if(classValues.size()==0) {
			Long expId = callsite.getNodeId();
			HashMap<Long, Long> key = new HashMap<Long, Long>();
			key.put(expId,  callsite.getNodeId());
			lock1.lock();
	        try {
	        	save.put(key, methodName);
	        } finally {
	            lock1.unlock();
	        }
			//System.err.println("Fail to find source of variable in $variable->name at "+varId);
			return;
		}
		boolean find=false, hasSave=false;
		//System.err.println(classVar+" "+callsite+" "+classValues);
		
		for(String classValue: classValues) {
			if(classValue.equals("")) {
				continue;
			}
			if(classValue.charAt(0)<='9' && classValue.charAt(0)>='0') {
				try {
					Long expId = Long.parseLong(classValue);
					HashMap<Long, Long> key = new HashMap<Long, Long>();
					key.put(expId,  callsite.getNodeId());
					hasSave=true;
					lock1.lock();
			        try {
			        	save.put(key, methodName);
			        } finally {
			            lock1.unlock();
			        }
					continue;
				}
				catch( Exception e ) {
			        //System.err.println("!"+callsite.getNodeId());
			    }
			}
			
			//we already get its classDef id
			if(classValue.charAt(0)=='#') {
				try {
					Long ClassDefId = Long.parseLong(classValue.substring(1));
					if(getMethodCall(cg, callsite, ClassDefId, methodName, false)){
			    		find=true;
				    }
					continue;
			    }
			    catch( Exception e ) {
			        //System.err.println("!"+callsite.getNodeId());
			    }
			}
			
			if(classValue.equals("self")) {
				classValue = callsite.getEnclosingClass();
			}
			
			Long ClassDefId = getClassId(classValue, callsite.getNodeId(), nameSpace);
			
			//System.err.println(classIdentifier.getNodeId()+" "+ClassDefId);
		    //int find=0;
		    if(ClassDefId != null) {
		    	//System.err.println(classId);
		    	if(getMethodCall(cg, callsite, ClassDefId, methodName, false)){
		    		find=true;
		    		//System.err.println(callsite.getNodeId());
		    		//class may do not have a defined constructor
		    		//System.err.println("Fail to find target method for constructor called at "+constructorCall.getNodeId()+" "+ClassDefId);
			    }
		    }
		    else {
		    	Long expId = varId;
				HashMap<Long, Long> key = new HashMap<Long, Long>();
				key.put(expId,  callsite.getNodeId());
				hasSave=true;
				lock1.lock();
		        try {
		        	save.put(key, methodName);
		        } finally {
		            lock1.unlock();
		        }
				continue;
		    	//System.err.println("Fail to find target class for dynamic class called at "+callsite.getNodeId()+" "+classValue);
		    }
		}
		if(find==false && hasSave==false) {
			HashMap<Long, Long> key = new HashMap<Long, Long>();
			key.put(varId,  callsite.getNodeId());
			lock1.lock();
	        try {
	        	save.put(key, methodName);
	        } finally {
	            lock1.unlock();
	        }
			//System.err.println("Fail to find target method for dynamic class called at "+callsite.getNodeId());
		}
		parsevar.reset();
	}
}


