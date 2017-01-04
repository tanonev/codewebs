package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import models.AST;
import models.ASTID;
import models.Program;

import org.json.JSONObject;

public class FileSystem {
	private static final String path = "../../data/";
	private static final String assnStr = "1_3";

	public static String getEquivalenceOutDir() {
		return path + "equivalence/equivalence_" + assnStr + "/";
	}
	
	public static String getNodeMappingDir() {
		return path + "equivalence/nodeMapping_" + assnStr + "/";
	}
	
	public static String getReducedOutDir() {
		return path + "equivalence/reduced_" + assnStr + "/";
	}
	
	public static String getSubtreeDir() {
		return path + "equivalence/subtree_" + assnStr + "/";
	}
	
	public static String getUnitTestOutputDir() {
		return path + "DumpOutputs" + "/";
	}
	
	public static String getNumSubmissionsDir() {
		return path + "DumpNumSubmissions" + "/";
	}
	
	public static void clearEquivalences() {
		String dir = getEquivalenceOutDir();
		File folder = new File(dir);
		
		File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	        	if (f.listFiles()!= null) {
	        		for(File c: f.listFiles()) {
	        			c.delete();
	        		}
	        	}
	            f.delete();
	        }
	    }
	}
	
	/**
	 * Method: Get Num Asts 
	 * -------------------- 
	 * Returns the number of asts for
	 * the current assignment. Computed by loading the list of corrects.
	 */
	public static int getNumAsts() {
		int numAsts = 0;
		try {
			Scanner corruptIn = new Scanner(new File(path + "matching/dist_"
					+ assnStr + ".txt"));
			String firstLine = corruptIn.nextLine();
			Scanner firstLineScanner = new Scanner(firstLine);
			while (firstLineScanner.hasNextInt()) {
				firstLineScanner.nextInt();
				numAsts += 1;
			}
			corruptIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return numAsts;
	}
	
	private static Set<Integer> getIncorrects() {
		return getAstSet("incorrects", "incorrects");
	}

	/**
	 * Method: Get Corrects 
	 * -------------------- 
	 * Returns a set containing the
	 * indices of the asts that got full points on the assignment. Currently
	 * hard coded to take in a particular assignment.
	 */
	public static Set<Integer> getCorrects() {
		return getAstSet("incorrects", "corrects");
	}

	private static Set<Integer> getAstSet(String dir, String name) {
		Set<Integer> correctSet = new HashSet<Integer>();
		Scanner correctIn;
		try {
			String fileName = dir + "/" + name + "_" + assnStr + ".txt";
			String filePath = path + fileName;
			correctIn = new Scanner(new File(filePath));
			while (correctIn.hasNextInt()) {
				correctSet.add(correctIn.nextInt());
			}
			correctIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return correctSet;
	}

	/**
	 * Method: Get Corrupts 
	 * -------------------- 
	 * Returns a set containing the
	 * indices of the asts that did not compile. Currently hard coded to take in
	 * a particular assignment.
	 */
	public static Set<Integer> getCorrupts() {
		try {
			Set<Integer> corruptSet = new HashSet<Integer>();
			Scanner corruptIn = new Scanner(new File(path + "matching/dist_"
					+ assnStr + ".txt"));
			String firstLine = corruptIn.nextLine();
			Scanner firstLineScanner = new Scanner(firstLine);
			int index = 0;
			while (firstLineScanner.hasNextInt()) {
				boolean isCorrupt = firstLineScanner.nextInt() == -1;
				if (isCorrupt) {
					corruptSet.add(index);
				}
				index += 1;
			}
			corruptIn.close();
			return corruptSet;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open file",e);
		}
	}
	
	/**
	 * Method: Load AST 
	 * -------------------- 
	 */
	public static JSONObject loadAst(int astId) {
		Scanner astIn;
		try {
			astIn = new Scanner(new File(path + "ast/ast_1_3/ast_" + astId
					+ ".json"));
			StringBuffer astStr = new StringBuffer();
			while (astIn.hasNextLine())
				astStr.append(astIn.nextLine());
			astIn.close();
			return new JSONObject(astStr.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open file",e);
		}
	}
	
	public static Program loadProgram(int astId, int output, int numSubmissions) {
		JSONObject obj = FileSystem.loadAst(astId);
		ArrayList<String> code = FileSystem.loadCode(astId);
		int[][] map = FileSystem.loadMap(astId);
		AST ast = AST.makeAst(obj.getJSONObject("root"));
		return new Program(new ASTID(astId), ast, code, map, obj.getJSONObject("root"), output, numSubmissions);
	}
	
	public static Map<ASTID, Program> loadAllCorrects() {
		int numAsts = getNumAsts();
		return loadAllCorrects(numAsts);
	}
	
	public static Map<ASTID, Program> loadAllCorrects(int maxNumAsts) {
		Set<Integer> corrects = FileSystem.getCorrects();
		return loadPrograms(maxNumAsts, corrects);
	}
	
	public static Map<ASTID, Program> loadAllIncorrects(int maxNumAsts) {
		Set<Integer> incorrects = FileSystem.getIncorrects();
		return loadPrograms(maxNumAsts, incorrects);
	}

	public static Map<ASTID, Program> loadAllPrograms(int maxNumAsts) {
		Set<Integer> filter = new HashSet<Integer>();
		return loadPrograms(maxNumAsts, filter);
	}
	
	// empty filter means include everything
	private static Map<ASTID, Program> loadPrograms(int num, Set<Integer> filter) {
		int numAsts = getNumAsts();
		Set<Integer> corrupts = FileSystem.getCorrupts();
		ArrayList<Integer> outputList = FileSystem.loadOutputs();
		ArrayList<Integer> numSubmissions = FileSystem.loadNumSubmissions();
		
		Map<ASTID, Program> filteredProgs = new HashMap<ASTID, Program>();
		for (int i = 0; i < Math.min(num, numAsts); i++) {
			if (i % 100 == 0)
				System.out.println(i);
			if (corrupts.contains(i))
				continue;
			if (filter.size() > 0 && !filter.contains(i))
				continue;
			Program current = loadProgram(i, outputList.get(i), numSubmissions.get(i) );
			filteredProgs.put(current.astId, current);
		}
		return filteredProgs;
	}
	
	public static HashSet<String> loadKeywords() {
	  try {
	    Scanner keywordsIn = new Scanner(new File(path + "starter/starter_" + assnStr + ".txt"));
	    HashSet<String> ans = new HashSet<String>();
	    while (keywordsIn.hasNext()) ans.add(keywordsIn.next());
	    keywordsIn.close();
	    return ans;
	  } catch (FileNotFoundException e) {
	    e.printStackTrace();
	    throw new RuntimeException("could not open file",e);
	  }
	}
	
	public static ArrayList<String> loadCode(int astId) {
		try {
			Scanner codeIn = new Scanner(new File(path + "ast/ast_1_3/ast_" + astId
					+ ".code"));
			ArrayList<String> code = new ArrayList<String>();
			while (codeIn.hasNextLine())
				code.add(codeIn.nextLine());
			codeIn.close();
			return code;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open file",e);
		}
	}

	public static int[][] loadMap(int astId) {
		try {
			Scanner mapIn = new Scanner(new File(path + "ast/ast_1_3/ast_" + astId
					+ ".map"));
			int mapN = mapIn.nextInt();
			int[][] map = new int[mapN][];
			for (int j = 0; j < mapN; j++) {
				int mapC = mapIn.nextInt();
				map[j] = new int[mapC];
				for (int k = 0; k < mapC; k++)
					map[j][k] = mapIn.nextInt();
			}
			mapIn.close();
			return map;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open file",e);
		}
	}
	
	public static ArrayList<Integer> loadOutputs() {
		try {
		
			Scanner outputsIn = new Scanner(new File(getUnitTestOutputDir() + "outputClasses_" + assnStr + ".txt")); 
			ArrayList<Integer> outputs = new ArrayList<Integer>();
			while (outputsIn.hasNextLine()){
				String line = outputsIn.nextLine();
				outputs.add(Integer.parseInt(line));
			}
			outputsIn.close();
			return outputs;
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open file", e);
		}
	}
	
	public static ArrayList<Integer> loadNumSubmissions() {
		try {
			Scanner numSubmissionsIn = new Scanner(new File(getNumSubmissionsDir() + "NumSubmissions_" + assnStr + ".txt"));
			ArrayList<Integer> numSubmissions = new ArrayList<Integer>();
			while (numSubmissionsIn.hasNextLine()) {
				String line = numSubmissionsIn.nextLine();
				String[] parts = line.split(", ");
				//int astId = Integer.parseInt(parts[0]);
				int num = Integer.parseInt(parts[1]);
				numSubmissions.add(num);
			}
			numSubmissionsIn.close();
			return numSubmissions;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open file", e);
		}
	}

}
