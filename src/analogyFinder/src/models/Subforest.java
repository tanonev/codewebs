package models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ast.Forest;
import models.ast.Node;

import org.json.JSONObject;

import util.FileSystem;

/**
 * Class Subforest
 * A subforest represents a subtree or a forest of statement subtrees (in many
 * ways, this class is a sibling class of a context). In general there is one
 * subforest instance for each codeBlock from each program, but two identical
 * subforests from different programs will hash to the same value (and pass
 * equals). 
 */
public class Subforest {

	// To save memory these are pointers.
	private Forest forest;
	
	// This is a back pointer which is useful for reconstructing codeStrings
	private Program program;

	public Subforest(Program program, Forest forest) {
		this.forest = forest;
		this.program = program;
	}

	public int size() {
		return forest.getSize();
	}

	public String getCodeString() {
		return forest.getCodeString(program);
	}
	
	public int getLineNumber() {
	  return forest.getLineNumber(program);
	}
	
	public boolean isEmpty() {
		return forest == Forest.NULL;
	}
	
	public void markEquivalence(Equivalence equivalence) {
		for(Node root : forest.getRoots()) {
			root.markEquivalence(equivalence);
		}
	}
	
	public Forest getForest() {
		return this.forest;
	}

	@Override
	public int hashCode() {
		return forest.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		if (hashCode() != obj.hashCode())
			return false;
	
		Subforest other = (Subforest) obj;
		return other.forest.equals(forest);
	}
	
	public List<Node> getRoots() {
		return forest.getRoots();
	}

	public static Subforest loadFromFile(String path, Set<String> keywords) {
		JSONObject json = FileSystem.loadJson(path);
		Forest forest = Forest.loadFromJson(json, keywords);
		return new Subforest(null, forest);
	}

	public boolean containsId(int id) {
		return forest.containsId(id);
	}
	
	public boolean isIsomorphic(Subforest other) {
		if (!this.equals(other)) return false;
		Map<String, String> association = new HashMap<String, String>();
		return forest.checkValidIdentifierMap(other.forest, association);
	}
	
	public Subforest getReduced(Equivalence eq) {
		return new Subforest(program, forest.getReduced(eq));
	}
	
	public Subforest getReduced(List<Equivalence> equivalences) {
		return new Subforest(program, forest.getReduced(equivalences));		
	}
}
