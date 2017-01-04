package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import models.Program;

import org.json.JSONObject;


public class FileSystem {
	private static String path = "/home/andy/codeweb/data/";
	private static String assnStr = "1_3";
	private static String programPath = "";

	public static void setProgramPath(String newPath) {
		programPath = newPath;
	}

	public static void setPath(String newPath){
		path = newPath;
	}

	public static void setAssignment(String assnStr) {FileSystem.assnStr = assnStr;}

	public static String getEquivalenceOutDir() {
		return path + "equivalence/equivalence_" + assnStr + "/";
	}

	public static String getNodeMappingDir() {
		return path + "equivalence/nodeMapping_" + assnStr + "/";
	}

	public static String getReducedOutDir() {
		return path + "equivalence/reduced_" + assnStr + "/";
	}
	
	public static String getReducedAllDir() {
		return path + "equivalence/reduced_" + assnStr + "_all/";
	}

	public static String getSubtreeDir() {
		return path + "equivalence/subtree_" + assnStr + "/";
	}

	public static String getUnitTestOutputDir() {
		return path + "DumpOutputs" + "/";
	}

	public static String getDiscoveriesDir() {
		return path + "equivalence/discoveries_" + assnStr + "/";
	}


	public static String getSeedDir() {
		return path + "equivalence/seed_" + assnStr + "/";
	}

	public static String getExpandedDir() {
		return path + "equivalence/expanded_" + assnStr + "/";
	}
	
	public static void saveToIgnore(Set<Integer> toIgnore) {
		String fileString = "";
		for(Integer id : toIgnore ) {
			fileString += id + "\n";
		}
		createFile(path+ "equivalence" , "toIgnore.txt", fileString);
	}
	
	public static Set<Integer> loadToIgnore() {
		Set<Integer> toIgnore = new HashSet<Integer>();
		String filePath = path+ "equivalence/toIgnore.txt";
		try {
			Scanner keywordsIn = new Scanner(new File(filePath));
			while (keywordsIn.hasNext()) {
				String line = keywordsIn.next();
				int id = Integer.parseInt(line);
				toIgnore.add(id);
			}
			keywordsIn.close();
			return toIgnore;
		} catch (FileNotFoundException e) {
			return toIgnore;
		}
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

	private static String getProgramDir() {
		String dir = programPath;
		if(programPath.isEmpty()) {
			dir = path + "ast/ast_" + assnStr + "/";
		}
		return dir;
	}

	public static void copyFile(String from, String to) {
		File source = new File(from);
		File dest = new File(to);
		
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {
			dest.createNewFile();
			inputChannel = new FileInputStream(source).getChannel();
			outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
			inputChannel.close();
			outputChannel.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

    public static void setConfig(String configPath) {
    	try{
            Scanner configIn = new Scanner(new File(configPath));
            while (configIn.hasNextLine()){
                String line = configIn.nextLine();
                String[] tokens = line.split(" ");
                if (tokens[0].equals("DATA")){
                    path = tokens[1];
                }
                if (tokens[0].equals("ASSIGNMENT")){
                    assnStr = tokens[1];
                }
            }
            configIn.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("could not open file",e);
        }
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

	public static JSONObject loadJson(String path) {
		Scanner astIn;
		try {
			astIn = new Scanner(new File(path));
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

	/**
	 * Method: Load AST 
	 * -------------------- 
	 */
	public static JSONObject loadAst(int astId) {
		Scanner astIn;
		try {
			String dir = getProgramDir();
			String fullPath = dir + "ast_" + astId + ".json";

			astIn = new Scanner(new File(fullPath));
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

	public static ArrayList<String> loadCode(int astId) {
		try {
			String dir = getProgramDir();
			String fullPath = dir + "ast_" + astId + ".code";
			Scanner codeIn = new Scanner(new File(fullPath));
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
			String dir = getProgramDir();
			String fullPath = dir + "ast_" + astId + ".map";
			Scanner mapIn  = new Scanner(new File(fullPath));

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

	public static void createEmptyFile(String dir, String fileName) {
		new File(dir).mkdirs();
		try {
			String path = dir + "/" + fileName;
			FileWriter file = new FileWriter(path);
			file.write("");
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void createFile(String dir, String fileName,
			String text) {
		new File(dir).mkdirs();
		try {
			String path = dir + "/" + fileName;
			FileWriter file = new FileWriter(path);
			file.write(text);
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
