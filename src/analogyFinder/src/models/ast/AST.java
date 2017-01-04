package models.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import models.CodeBlock;
import models.Context;
import models.Program;
import models.Subforest;

import org.json.JSONObject;

import util.IdCounter;

/**
 * Class AST
 * This is a subclass of Forest. Its distinctive feature is the ability to
 * create all of its code blocks (and run a dynamic programming algorithm so
 * that calculating hash values for subtrees and contexts is super fast).
 */
public class AST extends Forest{

	private static final boolean INCLUDE_EMPTY_TREE = true;

	private Node root;

	private IdCounter blockIdCounter = new IdCounter();

	// Precomputed and cached hash codes
	int[] hashes = null;
	int[] pow = null;

	protected AST() {}

	public AST(JSONObject jsonRoot, Set<String> keywords) {
		postorder = new ArrayList<Node>();
		this.root = makePostorder(jsonRoot, keywords);
		for(int i = 0; i < postorder.size(); i++) {
			Node n = postorder.get(i);
			n.setPostorderIndex(i);
		}
	}

	public AST getReduced() {
		AST copy = new AST();
		copy.postorder = new ArrayList<Node>();
		copy.root = copy.makePostorder(root);
		for(int i = 0; i < copy.postorder.size(); i++) {
			Node n = copy.postorder.get(i);
			n.setPostorderIndex(i);
			//n.setId(i);
		}
		return copy;
	}

	public Node getARoot() {
		return root;
	}

	public List<CodeBlock> makeCodeBlocks(Program program, int root) {
		precomputeHashArrays();

		// Get the single code block rooted at root.
		CodeBlock treeBlock = makeCodeBlock(program, root);

		// Get the list of code blocks including all lines that start at root.
		List<CodeBlock> multiCodeBlocks = makeMultilineCodeBlocks(program, root);

		// Put them all into a list.
		List<CodeBlock> codeBlocks = new ArrayList<CodeBlock>();
		codeBlocks.add(treeBlock);
		codeBlocks.addAll(multiCodeBlocks);
		return codeBlocks;
	}

	// ----------------- Making Code Blocks ---------------------------- //

	private void precomputeHashArrays() {
		if (hashes != null) return;
		int treeSize = this.getSize();
		hashes = new int[treeSize + 1];
		int hashCode = 1;
		for (int i = 0; i <= treeSize; i++) {
			hashes[i] = hashCode;
			if (i < treeSize) {
				hashCode = 31 * hashCode + this.getNode(i).hashCode();
			}
		}
		pow = new int[treeSize + 2];
		pow[0] = 1;
		for (int i = 1; i < treeSize + 2; i++) pow[i] = pow[i-1] * 31;
	}

	private CodeBlock makeCodeBlock(Program program, int root) {
		Subforest subforest = makeSubforest(program, root);
		Context context = makeComplement(program, root);
		int id = blockIdCounter.getNextId();
		return new CodeBlock(program, subforest, context, id);
	}

	private List<CodeBlock> makeMultilineCodeBlocks(Program program, int i) {
		List<CodeBlock> ans = new ArrayList<CodeBlock>();
		String nodeType = postorder.get(i).getType();
		if (nodeType.equals("STATEMENT_LIST")) {

			ArrayList<Integer> partialSums = new ArrayList<Integer>();
			int partial = 0;
			partialSums.add(partial);
			while (1 + partial < postorder.get(i).getSize()) {
				partial += postorder.get(i-1-partial).getSize();
				partialSums.add(partial);
			}
			for (int j = 0; j < partialSums.size(); j++) {
				for (int k = j + 2; k < partialSums.size(); k++) {
					int start = i - partialSums.get(k);
					int end = i - partialSums.get(j);

					ans.add(makeMultilineCodeBlock(program, start, end));
				}

				int loc = i - partialSums.get(j);
				if (INCLUDE_EMPTY_TREE) {
					ans.add(makeEmptyLineCodeBlock(program, loc));
				}
			}

		}
		return ans;
	}

	private CodeBlock makeEmptyLineCodeBlock(Program program, int loc) {
		Subforest forest = new Subforest(program, Forest.NULL);
		Context complement = makeComplement(program, loc, loc);
		int id = blockIdCounter.getNextId();
		return new CodeBlock(program, forest, complement, id);
	}

	private CodeBlock makeMultilineCodeBlock(Program program, int start, int end) {
		Forest nodes = new Forest(postorder.subList(start, end));
		nodes.setHashCode(hashes[end] - pow[end - start] * (hashes[start] - 1));
		Subforest forest = new Subforest(program, nodes);
		Context complement = makeComplement(program, start, end);
		int id = blockIdCounter.getNextId();
		return new CodeBlock(program, forest, complement, id);
	}

	public Context makeLocalContext(Program p, int root) {
    precomputeHashArrays();
		if (root == postorder.size() - 1) return null;
		int start = root - postorder.get(root).getSize() + 1;
		int end = root + 1;
		return makeLocalContext(p, start, end);
	}

	private Context makeLocalContext(Program p, int start, int end) {
		if (end == postorder.size()) return null;
		int upperRoot = postorder.get(end - 1).getParent().getPostorderIndex();
		return makeContext(p, upperRoot, start, end);
	}

	private Context makeContext(Program p, int upperRoot, int lowerStart, int lowerEnd) {
		int start = upperRoot - postorder.get(upperRoot).getSize() + 1;
		int end = upperRoot + 1;
		return makeContext(p, start, end, lowerStart, lowerEnd);
	}

	private Context makeContext(Program p, int upperStart, int upperEnd, int lowerStart, int lowerEnd) {
		Forest left = new Forest(postorder.subList(upperStart, lowerStart));
		Forest right = new Forest(postorder.subList(lowerEnd, upperEnd));
		left.setHashCode(hashes[lowerStart] - pow[lowerStart - upperStart] * (hashes[upperStart] - 1));
		right.setHashCode(hashes[upperEnd] - pow[upperEnd - lowerEnd] * (hashes[lowerEnd] - 1));
		Context c = new Context(left, right, p, lowerStart, lowerEnd);
		c.setHashCode(pow[upperEnd - lowerEnd + 1] * left.hashCode() + right.hashCode());
		return c;

	}

	private Context makeComplement(Program p, int root) {
		int start = root - postorder.get(root).getSize() + 1;
		int end = root + 1;
		return makeComplement(p, start, end);
	}

	private Context makeComplement(Program p, int start, int end) {
		return makeContext(p, 0, postorder.size(), start, end);
	}

	private Subforest makeSubforest(Program program, int root) {
		int start = root - postorder.get(root).getSize() + 1;
		int end = root + 1;
		Forest nodes = new Forest(postorder.subList(start, end));
		nodes.setHashCode(hashes[end] - pow[end - start] * (hashes[start] - 1));
		return new Subforest(program, nodes);
	}

}
