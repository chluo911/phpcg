package tools.php.ast2cpg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ast.NullNode;
import ast.php.functionDef.Closure;
import ast.php.functionDef.FunctionDef;
import ast.php.functionDef.Method;
import ast.php.functionDef.TopLevelFunctionDef;
import cg.FileDependencyCheck;
import cg.PHPCGFactory;
import cg.PruneCG;
import cg.toTopLevelFile;
import inputModules.csv.csv2ast.ASTUnderConstruction;
import misc.MultiHashMap;

public class Debloat {
	//private MultiHashMap<Long, Long> callgraph = new MultiHashMap<Long, Long>();
	private Set<Long> allFunIds = new HashSet<Long>();
	private Set<Long> initialMethodIds = new HashSet<Long>();
	private Set<String> cmds = new HashSet<String>();
	public static Set<Long> neededIds = new HashSet<Long>();
	//private Set<Long> unNeededIds = new HashSet<Long>();
	private MultiHashMap<String, Integer> line2remove = new MultiHashMap<String, Integer>();
	private boolean isApplication;
	private Set<Long> magicIds;
	private Set<String> classUsed;
	private MultiHashMap<String, String[]> vulnerableFuncs = new MultiHashMap<String, String[]>();
	private HashMap<String, String> packages = new HashMap<String, String>();
	String baseDir;
	private MultiHashMap<Long, Long> whocallme = new MultiHashMap<Long, Long>();
	private Set<Long> vulnerableIds = new HashSet<Long>();
	Set<Long> vulFunRemove = new HashSet<Long>();
	
	public static Set<Long> entry = new HashSet<Long>();
	public static Set<Long> dependent = new HashSet<Long>();

	
	Debloat(){
		//callgraph = PruneCG.cg;
		//initialMethodIds.addAll(PHPCGFactory.topFunIds);
		for(Long topFunid: PHPCGFactory.topFunIds) {
			Long topId = toTopLevelFile.getTopLevelId(topFunid);
			if(!ASTUnderConstruction.idToNode.get(topId).getFlags().equals("TOPLEVEL_FILE")) {
				System.err.println("Fail to find top file for target function "+topId);
				continue;
			}
			TopLevelFunctionDef topFile = (TopLevelFunctionDef) ASTUnderConstruction.idToNode.get(topId);
			String phpPath = topFile.getName();
			phpPath = phpPath.substring(1, phpPath.length()-1);
			phpPath = phpPath.replace("//", "/");
			
			baseDir = CommandLineInterface.baseDir;
			if(FileDependencyCheck.excludeDirs.containsKey(baseDir))
			for(String excludePath: FileDependencyCheck.excludeDirs.get(baseDir)) {
				if(phpPath.contains(excludePath)) {
					//System.err.println(phpPath);
					continue;
				}
			}
			initialMethodIds.add(topFunid);
		}
		
		allFunIds = new HashSet<Long>(PHPCGFactory.collectAllFun);
		magicIds = new HashSet<Long>(PHPCGFactory.magicMtdDefs);
		classUsed = new HashSet<String>(PHPCGFactory.classUsed);
		//System.err.println(magicIds);
		//System.err.println(classUsed);
		// TODO, add this commend line
		//cmds.add("/home/users/chluo/phpMyAdmin-4.7.0-all-languages/vendor");
		// TODO, add this commend line
		//isApplication = true;
	
	}
	
	/*
	 * input: path to the application
	 * output: path to installed.json or NULL if there is no installed.json in base folder
	 */
	File getInstalledJson(File base) {
		File ret = null;
		for (File file : base.listFiles()) {
		    if (file.isFile()) {
		      if (file.getName().equals("installed.json")) {
		    	  return file;
		      }
		    }
		    else {
		    	ret = getInstalledJson(file);
		    	if(ret!=null) {
		    		return ret;
		    	}
		    }
		  }
		return ret;
	}
	
	/*
	 * output: {package name: package version}
	 */
	void getAllPackages() {
		//find installed.json
		File base = new File(baseDir);
		File installed = getInstalledJson(base);
		if(installed == null) {
			System.err.println("Fail to find installed.json file");
		}
		//extract all installed packages and their version information
		FileReader reader;
		try{
			reader = new FileReader(installed);
			char[] chars = new char[(int) installed.length()];
			reader.read(chars);
			String jsonString = new String(chars);
			if(reader != null){
		        reader.close();
		    }
			
			JSONArray jarr = new JSONArray(jsonString);
			for(int i=0; i<jarr.length(); i++) {
				String pkg = jarr.getJSONObject(i).getString("name");
				String version = jarr.getJSONObject(i).getString("version");
				packages.put(pkg, version);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * get version number
	 * input: x
	 * output: number(x)
	 */
	 private int getCode(String codeStr) {
	        int code = 0;
	        for (int i = 0; i < codeStr.length(); i++) {
	            int digit = codeStr.charAt(i) - '0';
	            code = code * 10 + digit;
	        }
	        return code;
	}
	
	/*
	 * Compare two version number
	 * return 1 if v1>v2, 0 if v1=v2, -1 if v1<v2
	 */
	int compareVersion(String v1, String v2) {
		v2 = v2.replace(">", "");
		v2 = v2.replace("<", "");
		v2 = v2.replace("=", "");
		String[] version1StrArr = v1.split("\\.");
        String[] version2StrArr = v2.split("\\.");
        int len = Math.max(version1StrArr.length, version2StrArr.length);
        int readIdx = 0;
        while (readIdx < len) {
            int version1Code = 0;
            if (readIdx < version1StrArr.length) {
                version1Code = getCode(version1StrArr[readIdx]);
            }
            int version2Code = 0;
            if (readIdx < version2StrArr.length) {
                version2Code = getCode(version2StrArr[readIdx]);
            }
            if (version1Code < version2Code) {
                return -1;
            }
            if (version1Code > version2Code) {
                return 1;
            }
            readIdx++;  
        }
        return 0;
	}
	
	/*
	 * Check if v1 is included by v2
	 * return true(included) or false(not included)
	 */
	boolean CheckVersion(String v1, String v2) {
		String[] multiV2 = v2.split("||");
		//transform vx.y.z to x.y.z
		v1 = v1.replace("v", "");
		//System.err.println(v1+" "+v2);
		
		for(String v: multiV2) {
			String[] vers = v.split("&&");
			boolean ret = true;
			for(String ver: vers) {
				//v1 is not included in ver
				if(ver.startsWith(">=") && compareVersion(v1, ver)==-1) {
					ret = false;
					break;
				}
				else if(ver.startsWith(">") && !(compareVersion(v1, ver)==1)){
					ret = false;
					break;
				}
				else if(ver.startsWith("<=") && compareVersion(v1, ver)==1) {
					ret = false;
					break;
				}
				else if(ver.startsWith("<") && !(compareVersion(v1, ver)==-1)){
					ret = false;
					break;
				}
			}
			//satisfy a condition
			if(ret==true) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Get vulnerable functions in installed packages of the application,
	 * First collect all packages installed, then find if there is any package(and its version) is vulnerable
	 * Assign vulnerbleFunc with vulnerable functions and the package name.
	 * output: vulnerable functions, vulnerable packages, vulnerable link 
	 */
	void getVulnerableFunc() {
		File database = new File("./database.json");
		//get all installed packages
		getAllPackages();
		FileReader reader;
		try{
			reader = new FileReader(database);
			char[] chars = new char[(int) database.length()];
			reader.read(chars);
			String jsonString = new String(chars);
			if(reader != null){
		        reader.close();
		    }
			
			JSONObject jobj = new JSONObject(jsonString);
			
			//find if there is any package vulnerable
			for(String pkg: packages.keySet()) {
				//System.err.println(pkg);
				if(jobj.has(pkg)) {
					//System.err.println("AAA"+pkg);
					String installedVersion = packages.get(pkg);
					JSONArray arr = jobj.getJSONArray(pkg);
					//iterate each CVE in that package
					for(int i = 0; i < arr.length(); i++) {
						String vulnerableVersion = arr.getJSONObject(i).getString("version");
						if(CheckVersion(installedVersion, vulnerableVersion)) {
							String[] info = new String[2];
							info[0] = pkg;
							info[1] = arr.getJSONObject(i).getString("link");
							if(arr.getJSONObject(i).getJSONArray("location").getJSONObject(0).get("Function") instanceof JSONArray) {
								JSONArray functions = arr.getJSONObject(i).getJSONArray("location").getJSONObject(0).getJSONArray("Function");
								for(int j = 0; j < functions.length(); j++) {
									vulnerableFuncs.add(functions.getString(j), info);
								}
							}
							else {
								vulnerableFuncs.add(arr.getJSONObject(i).getJSONArray("location").getJSONObject(0).getString("Function"), info);
							}
						}
					}
				}
			}
			//System.err.println(vulnerableFuncs);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void handle() throws IOException {
		//getNeedDirIds();
		if(!getInitialMethod()) {
			return;
		}
		
		
		//getVulnerableFunc();
		getNeedIds();
		//AlertVulFunc();
		
		System.err.println("all "+allFunIds.size());
		//retain all needed functions
		allFunIds.removeAll(neededIds);
		//remove all vulnerable functions with user's permission
		//allFunIds.addAll(vulFunRemove);
		
		System.err.println("Spider: "+PHPCGFactory.spider);
		System.err.println("unneeded: "+allFunIds.size());
		System.err.println("initial: "+initialMethodIds.size());
		System.err.println("needed "+neededIds.size());
		//neededIds.removeAll(ParseCG.neededIds);
		//System.err.println("xxx "+initialMethodIds);
		//System.err.println("yyy "+neededIds);
		getRemoveLines();
		removeLines(); 
	}
	
	/*
	 * Output all functions that call vulnerable functions
	 * Let users decide weather they want to use them
	 * Remove vulnerable functions according to user's decision
	 */
	/*
	void AlertVulFunc() {
		HashMap<Long, String> id2name = new HashMap<Long, String>();
		for(String name: PHPCGFactory.name2Id.keySet()) {
			id2name.put(PHPCGFactory.name2Id.get(name), name);
		}
		for(String vulFun: vulnerableFuncs.keySet()) {
			if(PHPCGFactory.name2Id.containsKey(vulFun)) {
				Long vulId = PHPCGFactory.name2Id.get(vulFun);
				if(!neededIds.contains(vulId)) {
					continue;
				}
				System.err.println("Vulnerable function: "+id2name.get(vulId));
				System.err.println("Functions call the vulnerable function:");
				Queue<Long> vulQue = new LinkedList<Long>();
				vulQue.offer(vulId);
				HashSet<Long> hasTraverse = new HashSet<Long>();
				hasTraverse.add(vulId);
				Set<Long> relatedFuns = new HashSet<Long>();
				while(!vulQue.isEmpty()) {
					Long vulid = vulQue.poll();
					if(!whocallme.containsKey(vulid)) {
						continue;
					}
					List<Long> callers = whocallme.get(vulid);
					//find which functions call that vulnerable function
					for(Long caller: callers) {
						if(!hasTraverse.contains(caller)) {
							if(id2name.containsKey(caller))
								relatedFuns.add(caller);
							hasTraverse.add(caller);
							vulQue.offer(caller);
							//get Path of caller
							Long topId = toTopLevelFile.getTopLevelId(caller);
							TopLevelFunctionDef topFile = (TopLevelFunctionDef) ASTUnderConstruction.idToNode.get(topId);
							String callPath = topFile.getName();
							callPath = callPath.substring(1, callPath.length()-1);
							callPath = callPath.replace("//", "/");
							//if caller is from first-party code, we echo it
							if(!callPath.contains("/vendor/")) {
								//System.err.println(caller);
								if(id2name.containsKey(caller)) {
									System.err.println(id2name.get(caller) + " from "+callPath);
								}
								else {
									System.err.println(callPath);
								}
							}
							
						}
					}
				}
				//System.err.println(PHPCGFactory.callsiteNumber + " " + PHPCGFactory.unknownIds.size());
				System.err.println("Do you want to remove this vulnerable function? (input Y if you want to)");
				@SuppressWarnings("resource")
				Scanner scanner = new Scanner(System. in);
				String userReply = scanner. nextLine();
				if(userReply.equals("Y")) {
					vulFunRemove.add(vulId);
					vulFunRemove.addAll(relatedFuns);
				}
				else {
					
				}
			}
		}
	}*/
	
	private void getRemoveLines() {
		for(Long unNeedId: allFunIds) {
			if(PHPCGFactory.Abstract.contains(unNeedId)) {
				continue;
			}
			String path = getPath(unNeedId);
			FunctionDef funDef = (FunctionDef) ASTUnderConstruction.idToNode.get(unNeedId);
			if(funDef.getChild(2) instanceof NullNode) {
				continue;
			}
			//System.err.println(funDef);
			int startline = funDef.getChild(2).getLocation().startLine;
			startline=startline+1;
			int endline = funDef.getLocation().endLine;
			if(startline>=endline) {
				continue;
			}
			line2remove.add(path, startline);
			line2remove.add(path, endline);
		}
		//System.err.println(line2remove);
	}
	
	private void removeLines() throws IOException {
		for(String path: line2remove.keySet()) {
			if(!path.endsWith(".php")) {
				continue;
			}
			
			List<Integer> lines = line2remove.get(path);
			//Collections.sort(lines);
			File inputFile = new File(path);
			File tempFile = new File("myTempFile.php");
			
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			
			int crtLineno = 0;
			boolean toStart=false;
			String currentLine;
			String errLine = "$trace = debug_backtrace();\r\n" + 
					"	  error_log(__FILE__);\r\n" + 
					"	  var_dump(__FUNCTION__);\r\n" + 
					"     error_log( print_r( $trace, true ));\r\n" + 
					"	  die();";
			//String errLine = "$trace = debug_backtrace();\r\n "
				//	+ "roo('<html><head>    <meta charset=\"utf-8\">    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no\">    <title>Error, Target Function Has Been Removed</title>    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\">    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\" integrity=\"sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp\" crossorigin=\"anonymous\">    <style>        * {            font-family: tahoma;        }        div.container .panel {            position: relative !important;        }        div.container {            width: 50% !important;            height: 50% !important;            overflow: auto !important;            margin: auto !important;            position: absolute !important;            top: 0 !important;            left: 0 !important;            bottom: 0 !important;            right: 0 !important;        }    </style></head><body>    <div class=\"container\">        <div class=\"panel panel-danger center\">            <div class=\"panel-heading\" style=\"text-align: left;\"> Error </div>            <div class=\"panel-body\">                <p class=\"text-center\">');\r\n "
			//		+ "print_r($trace);\r\n" 
			//		+ "print_r('</p>            </div>        </div>    </div></body></html>');\r\n"
			//		+ "die();";
			//String errLine = "$trace = error_log('123');\r\n";
			
			int i=0;
			while((currentLine = reader.readLine()) != null) {
			    crtLineno++;
			    if(lines.contains(crtLineno)) {
			    	toStart = toStart^true;
			    	i=0;
			    }
			    if(toStart!=true) {
			    	 writer.write(currentLine + System.getProperty("line.separator"));
			    }
			    else {
			    	i++;
			    	if(i==1)
			    		writer.write(errLine + System.getProperty("line.separator"));
			    }
			}
			writer.close(); 
			reader.close();
			
			if(!tempFile.renameTo(inputFile)) {
				System.err.println("Fail to debloat file "+path);
			}
		}
	}
	
	private String getPath(Long FunctionId) {
		String path = new String();
		Long topId = toTopLevelFile.getTopLevelId(FunctionId);
		TopLevelFunctionDef topFile = (TopLevelFunctionDef) ASTUnderConstruction.idToNode.get(topId);
		path = topFile.getName();
		path = path.substring(1, path.length()-1);
		return path;
	}
	
	//get which methods are initially needed
	protected boolean getInitialMethod() {
		initialMethodIds = PHPCGFactory.ignoreIds.keySet();
		return true;
	}
	
	//get all methods an app / a library needs
	private void getNeedIds() {
		Queue<Long> queue = new LinkedList<Long>(initialMethodIds);
		while(!queue.isEmpty()) {
			Long crtId = queue.peek();
			//FunctionDef func = (FunctionDef) ASTUnderConstruction.idToNode.get(crtId);
			/*
			if(func instanceof TopLevelFunctionDef
					&& (func.getEnclosingNamespace()==null || func.getEnclosingNamespace().isEmpty())
					&& !PHPCGFactory.getDir(crtId).contains("/vendor/")
					&& !(func instanceof Closure)) {
				entry.add(crtId);
			}
			else {
				dependent.add(crtId);
			}
			*/
			//System.err.println("xxx"+entry.size()+" "+dependent.size());
			//System.err.println(callgraph.size());
			if(crtId!=null && PruneCG.cg.containsKey(crtId)) {
				if(PruneCG.cg.get(crtId)==null) {
					continue;
				}
				Set<Long> calledFuns = new HashSet<Long>(PruneCG.cg.get(crtId));
				for(Long calledFun: calledFuns) {
					whocallme.add(calledFun, crtId);
					if(!queue.contains(calledFun) && !neededIds.contains(calledFun)) {
						queue.add(calledFun);
						//System.err.println(crtId+" "+calledFun);
						
						Long topfile = toTopLevelFile.getTopLevelId(calledFun);
						if(PruneCG.callcons.containsKey(topfile)) {
							List<Long> cans = PruneCG.callcons.get(topfile);
							for(Long id: cans) {
								if(!queue.contains(id) && !neededIds.contains(id)) {
									queue.add(id);
								}
							}
						}
						
					}
				}
			}
			neededIds.add(queue.poll());
		}
		
		Set<String> allDirs =  new HashSet<String>();
		for(Long id: neededIds) {
			if(id==null) {
				continue;
			}
			System.err.println(id);
			allDirs.add(PHPCGFactory.getDir(id));
		}
		
		for(Long id: PHPCGFactory.condes) {
			if(allDirs.contains(PHPCGFactory.getDir(id))) {
				neededIds.add(id);
			}
		}
	
	}
	
	private boolean InCmdDir(String path) {
		String Unified_form_path = path.replace("//", "/");
		for(String cmd: cmds) {
			if(Unified_form_path.startsWith(cmd)) {
				return true;
			}
		}
		return false;
	}
}

