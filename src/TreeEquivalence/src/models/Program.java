package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class Program {

	public AST ast;
	public ArrayList<String> code;
	public int[][] map;
	public ASTID astId;
	public int output;
	public int numSubmissions;

	public JSONObject tree;

	public Program(ASTID astId, AST ast, ArrayList<String> code, int[][] map, JSONObject tree, int output, int numSubmissions) {
		this.ast = ast;
		this.code = code;
		this.map = map;
		this.astId = astId;
		this.tree = tree;
		this.output = output;
		this.numSubmissions = numSubmissions;
		precomputeHashArrays();
		precomputeContexts();
	}

	private int[][] hashes;
	private int[] pow;

	private void precomputeHashArrays() {
		hashes = new int[ast.size() + 1][ast.size() + 1];
		for (int i = 0; i <= ast.size(); i++) {
			int hashCode = 1;
			for (int j = i; j <= ast.size(); j++) {
				hashes[i][j] = hashCode;
				if (j < ast.size()) hashCode = 31 * hashCode + ast.getNode(j).hashCode();
			}
		}
		pow = new int[ast.size() + 2];
		pow[0] = 1;
		for (int i = 1; i < ast.size() + 2; i++) pow[i] = pow[i-1] * 31;
		ast.setHashCode(hashes[0][ast.size()]);
	}
	

	public int getOutput(){
		return this.output;
	}

	public Context[][] contexts; // [upper][lower]
	public int[] parents;
	
	private void precomputeContexts() {
	  contexts = new Context[ast.size()][ast.size()];
	  parents = new int[ast.size()];
	  Arrays.fill(parents, -1);
	  for (int i = 0; i < ast.size(); i++) {
	    int upperStart = i - ast.getNode(i).size + 1;
	    int upperEnd = i + 1;
	    for (int j = upperStart; j < upperEnd - 1; j++) {
	      if (parents[j] < 0) parents[j] = i;
	      contexts[i][j] = ast.getContext(i, j);
	      int lowerStart = j - ast.getNode(j).size + 1;
	      int lowerEnd = j + 1;
	      contexts[i][j].setHashCode(hashes[upperStart][lowerStart] * pow[upperEnd - lowerEnd + 1] + hashes[lowerEnd][upperEnd]);
	    }
	  }
	}
	
	public Collection<Context> getAllContexts() {
	  ArrayList<Context> ans = new ArrayList<Context>();
	  for (int i = 0; i < ast.size() - 1; i++) {
      ans.add(contexts[parents[i]][i]);
	  }
//	  for (Context[] arr : contexts) {
//	    for (Context c : arr) {
//	      if (c != null) ans.add(c);
//	    }
//	  }
	  return ans;
	}
	
	public Context getComplement(int root) {
		Context ans = ast.getComplement(root);
		int start = root - ast.getNode(root).size + 1;
		int end = root + 1;
		ans.setHashCode(hashes[0][start] * pow[ast.size() - end + 1] + hashes[end][ast.size()]);
		return ans;
	}

	public AST getSubtree(int root) {
		AST ans = ast.getSubtree(root);
		int start = root - ast.getNode(root).size + 1;
		int end = root + 1;
		ans.setHashCode(hashes[start][end]);
		return ans;
	}

	public List<ASTWithComplement> subforests(int root) {
		List<ASTWithComplement> ans = ast.subforests(root);
		for (ASTWithComplement awc : ans) {
			awc.ast.setHashCode(hashes[awc.start][awc.end]);
			awc.complement.setHashCode(hashes[0][awc.start] * pow[ast.size() - awc.end + 1] + hashes[awc.end][ast.size()]);
		}
		return ans;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Program)) return false;
		Program otherProgram = (Program) other;
		return this.ast.hashCode() == otherProgram.ast.hashCode();
	}

	@Override
	public int hashCode() {
		return this.ast.hashCode();
	}

	private Set<Integer> getMappedNodes() {
		Set<Integer> mappedNodes = new HashSet<Integer>();
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				mappedNodes.add(map[i][j]);
			}
		}
		return mappedNodes;
	}

	/**
	 * Method: Get Reduced AST
	 * ----------------------
	 * Warning. This method changes the underlying ast!!! To assuage such
	 * dangerous action, make a deep copy first.
	 * @param eqMap 
	 */
	public Program getReducedProgram(Map<Integer, Integer> changeList, 
			Map<Integer, EquivalenceClass> eqMap) {

		JSONObject tree = this.tree;
		Set<Integer> mappedNodes = getMappedNodes();
		Map<Integer, Integer> nodeChanges = new TreeMap<Integer, Integer>();
		Map<Integer, Integer> codeChanges = new TreeMap<Integer, Integer>();
		for (int nodeId : changeList.keySet()) {
			Integer equivalenceId = changeList.get(nodeId);
			JSONObject replaced = replaceNode(tree, nodeId, equivalenceId);
			if (replaced != null) {

				List<Integer> orphanedNodes = getDescendants(replaced);

				updateCodeChanges(mappedNodes, codeChanges, nodeId,
						equivalenceId, orphanedNodes);

				nodeChanges.put(nodeId, equivalenceId);
				for(int orphanedId : orphanedNodes) {
					nodeChanges.put(orphanedId, null);
				}

			}
		}
		AST newAst = AST.makeAst(tree);

		updateCode(codeChanges, eqMap);

		return new Program(this.astId, newAst, this.code, this.map, tree, this.output, this.numSubmissions);
	}

	private void updateCodeChanges(Set<Integer> mappedNodes,
			Map<Integer, Integer> codeChanges, int nodeId,
			Integer equivalenceId, List<Integer> orphanedNodes) {
		for(int orphanedId : orphanedNodes) {
			codeChanges.put(orphanedId, null);
		}
		if (!mappedNodes.contains(nodeId)) {
			for(int orphanedId : orphanedNodes) {
				if (mappedNodes.contains(orphanedId)) {
					codeChanges.put(orphanedId, equivalenceId);
					break;
				}
			}
		} else {
			codeChanges.put(nodeId, equivalenceId);
		}
	}

	private void updateCode(Map<Integer, Integer> nodeChanges, 
			Map<Integer, EquivalenceClass> eqMap) {
		ArrayList<List<Integer>> newMap = new ArrayList<List<Integer>>();
		ArrayList<String> newCode = new ArrayList<String>();

		Set<Integer> replaced = new HashSet<Integer>();
		for (int i = 0; i < map.length; i++) {
			ArrayList<Integer> newRow = new ArrayList<Integer>();
			String newLine = "";

			for (int j = 0; j < map[i].length; j++) {
				int nodeId = map[i][j];
				if (nodeChanges.containsKey(nodeId)){
					if (replaced.contains(nodeId)) continue;
					if (nodeChanges.get(nodeId) != null) {
						int equivalenceId = nodeChanges.get(nodeId);
						EquivalenceClass eq = eqMap.get(equivalenceId);
						String equivalenceCode = eq.getCodeString();
						for (int k = 0; k < equivalenceCode.length(); k++){  
							newRow.add(nodeId);
						}
						newLine += equivalenceCode;
					}
					replaced.add(nodeId);
				} else {
					newRow.add(nodeId);
					newLine += this.code.get(i).charAt(j);
				}
			}
			if (!newLine.isEmpty()) {
				newMap.add(newRow);
				newCode.add(newLine);
			}
		}

		this.code = newCode;
		this.map = new int[newMap.size()][];
		for (int i = 0; i < newMap.size(); i++) {
			map[i] = new int[newMap.get(i).size()];
			for (int j = 0; j < newMap.get(i).size(); j++) {
				map[i][j] = newMap.get(i).get(j);
			}
		}
	}

	private JSONObject replaceNode(JSONObject node, int nodeId, Integer equivalenceId) {
		int currentId = node.getInt("id");
		assert (currentId != nodeId);

		JSONArray children = node.getJSONArray("children");
		for (int i = 0; i < children.length(); i++) {
			JSONObject child = children.getJSONObject(i);
			int childId = child.getInt("id");
			if (childId == nodeId) {
				if (equivalenceId == null) {
					children.remove(i);
				} else {
					JSONObject newNode = createNewNode(equivalenceId, childId);
					children.put(i, newNode);
				}
				return child;
			} 

			JSONObject replaced = replaceNode(child, nodeId, equivalenceId);
			if (replaced != null) {
				return replaced;
			}
		}
		return null;
	}

	private List<Integer> getDescendants(JSONObject node) {
		List<Integer> descendants = new ArrayList<Integer>();
		JSONArray childArray = node.getJSONArray("children");
		for (int i = 0; i < childArray.length(); i++) {
			JSONObject child = childArray.getJSONObject(i);
			List<Integer> childDescendents = getDescendants(child);
			descendants.add(child.getInt("id"));
			descendants.addAll(childDescendents);
		}
		return descendants;
	}

	private JSONObject createNewNode(Integer equivalenceId, int childId) {
		JSONObject newNode = new JSONObject();
		newNode.put("id", childId);
		newNode.put("type", "EQUIV");
		newNode.put("name", "" + equivalenceId);
		newNode.put("children", new JSONArray());
		newNode.put("annotations", new JSONArray());
		newNode.getInt("id");
		return newNode;
	}

	private void populateChildMap(JSONObject node, Map<Integer, JSONObject> childMap) {
		int nodeId = node.getInt("id");
		childMap.put(nodeId, node);
		JSONArray children = node.getJSONArray("children");
		for (int i = 0; i < children.length(); i++) {
			JSONObject child = children.getJSONObject(i);
			populateChildMap(child, childMap);
		}
	}

	public int getScore() {
		return output;
	}

}
