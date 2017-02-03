package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import minions.Jasonizer;
import models.ast.AST;
import models.ast.Node;

import org.json.JSONObject;

import util.FileSystem;

/**
 * Class: Program
 * Program is a class type which represents a students homework solution.
 * Most importantly, it has a list of all the underlying codeBlocks.
 */
public class Program {

	// The astID of the program
	private int id;

	// The result class of the unit test
	private int output;

	// The juiciest part of a program are the codeBlocks that it contains
	private List<CodeBlock> codeBlocks = null;

	private List<Context> localContexts = null;

	// Deprecated. This is unused (but we may want them later)
	private Set<CodeBlock> buggyCodeBlocks;

	// Eventually we want to track how many students each part corresponds to.
	private int students;

	// It is useful to keep these things around
	private AST tree;
	private int[][] map;
	private List<String> codeList;

	protected Program() {}

	public Program(Set<String> keywords, int astId, ArrayList<String> code, int[][] map,
			JSONObject root, int output, int numSubmissions) {
		this.codeList = code;
		this.map = map;
		this.id = astId;
		this.output = output;
		this.students = numSubmissions;
		this.tree = new AST(root, keywords);
	}

	public AST getTree() {return tree;}

	public static Program loadProgram(int astId, int output, Set<String> keywords) {
		JSONObject obj = FileSystem.loadAst(astId);
		ArrayList<String> code = FileSystem.loadCode(astId);
		int[][] map = FileSystem.loadMap(astId);
		JSONObject root = obj.getJSONObject("root");
		return new Program(keywords, astId, code, map, root, output, -1);
	}

	public Program reduce() {
		Program reduced = new Program();

		reduced.id = this.id;
		reduced.tree = tree.getReduced();

		reduced.map = reduced.tree.getMap(this);
		String codeString = reduced.tree.getCodeString(this);
		reduced.codeList = Arrays.asList(codeString.split("\\r?\\n"));
		reduced.createCodeBlocks();
		reduced.students = students;
		reduced.output = output;

		return reduced;
	}

	public List<CodeBlock> getCodeBlocks() {
		if (codeBlocks == null) createCodeBlocks();
		return codeBlocks;
	}
    
    public List<Context> getReducedLocalContexts(List<Equivalence> equivalences) {
        List<Context> reducedContexts;
        for (int i = 0; i < tree.getPostorder.size() - 1; i++) {
            Context context = tree.makeLocalContext(null, i);
            for (Equivalence eq : equivalences) {
                context.reduce(eq);
            }
        }
        reducedLocalContexts.add(context);
        return reducedLocalContexts;
    }

	public List<Context> getLocalContexts() {
		if (localContexts == null) createLocalContexts();
		return localContexts;
	}

	public boolean isCorrect() {
		return output == 0;
	}

	public void markAsBuggy(CodeBlock codeBlock) {
		buggyCodeBlocks.add(codeBlock);
	}

	public int getSmallestBugSize() {
		int smallestSize = tree.getSize();
		for(CodeBlock block: buggyCodeBlocks) {
			if (block.size() < smallestSize) {
				smallestSize = block.size();
			}
		}
		return smallestSize;
	}

	public int getId() {
		return id;
	}

	public List<String> getCodeList() {
		return codeList;
	}

	public int[][] getMap() {
		return map;
	}

	public Node getRoot() {
		return tree.getARoot();
	}

	public void saveToFile(String dir) {
		writeJsonToFile(dir);
		writeCodeToFile(dir);
		writeTextToFile(dir);
		writeMapToFile(dir);

	}

	private void writeJsonToFile(String dirName) {
		JSONObject json = Jasonizer.jsonify(tree);
		String name = "ast_" + id + ".json";
		FileSystem.createFile(dirName, name, json.toString(4));
	}

	private void writeCodeToFile(String dirName) {
		String codeString = getCodeString();
		String name = "ast_" + id + ".code";
		FileSystem.createFile(dirName, name, codeString);
	}

	private void writeTextToFile(String dirName) {
		String codeString = getCodeString();
		String name = "ast_" + id + ".txt";
		FileSystem.createFile(dirName, name, codeString);
	}

	private void writeMapToFile(String dirName) {
		String codeString = getMapString();
		String name = "ast_" + id + ".map";
		FileSystem.createFile(dirName, name, codeString);
	}

	public String getCodeString() {
		String codeString = "";
		for(String line : codeList) {
			codeString += line + "\n";
		}
		return codeString;
	}

	public int getOutput() {
		return this.output;
	}

	/**
	 * This is used by the expander and saves a "discovery" to file.
	 */
	public void markEquivalence(CodeBlock block, Equivalence eq) {
		String discDir = FileSystem.getDiscoveriesDir();
		String progDir = discDir + "/" + id;
		String fileName = block.getId() + "_" + eq.getName() + ".txt";
		FileSystem.createFile(progDir, fileName, block.getSubforest().getCodeString());
		throw new RuntimeException("depricated");
	}

	/**
	 * This is used by the expander and saves a "bug discovery" to file.
	 */
	public void markAsBuggy(CodeBlock block, Equivalence eq) {
		String discDir = FileSystem.getDiscoveriesDir();
		String progDir = discDir + "/" + id;
		String fileName = block.getId() + "_" + eq.getName() + "_attempt.txt";
		FileSystem.createFile(progDir, fileName, block.getSubforest().getCodeString());
		throw new RuntimeException("depricated");
	}

	@Override
	public boolean equals(Object o) {
		Program other = (Program)o;
		return other.tree.equals(tree);
	}

	@Override
	public int hashCode(){
		return tree.hashCode();
	}

	//---------------------- Privates --------------------------//

	/**
	 * Create all the code blocks for this program. Should only be called once
	 * per program. Maintains and constructs a list of unique forests and 
	 * contexts.
	 * Question: Is there any reason the constructor should not call this?
	 */
	private void createCodeBlocks() {
		if (codeBlocks != null) {
			throw new RuntimeException("Only call create code blocks once!");
		}
		codeBlocks = new ArrayList<CodeBlock>();
		for (int j = 0; j < tree.getSize(); j++) {

			List<CodeBlock> blocks = tree.makeCodeBlocks(this, j);
			codeBlocks.addAll(blocks);
		}
	}
 

	private String getMapString() {
		String mapString = map.length + "\n";
		for(int i = 0; i < map.length; i++) {
			int[] row = map[i];
			String rowString = ""+ row.length;
			for(int j = 0; j < row.length; j++) {
				rowString += " " + row[j];
			}
			mapString += rowString;
			if(i != map.length - 1) {
				mapString += "\n";
			}
		}
		return mapString;
	}

	private void createLocalContexts() {
		if (localContexts != null) {
			throw new RuntimeException("Only call create local contexts once!");
		}
		localContexts = new ArrayList<Context>();
		for (int j = 0; j < tree.getSize() - 1; j++) {
			localContexts.add(tree.makeLocalContext(this, j));
		}
	}

	public int getStudents() {
		return students;
	}

	public CodeBlock getLCA(int startLine, int startIndex, int endLine, int endIndex) {
	  getCodeBlocks();
	  CodeBlock ans = codeBlocks.get(codeBlocks.size() - 1);
	  for (CodeBlock c : codeBlocks) {
	    if (c.getSubforest().containsId(map[startLine][startIndex]) && c.getSubforest().containsId(map[endLine][endIndex])) {
	      if (ans.size() > c.size()) ans = c;
	    }
	  }
	  return ans;
	}
}
