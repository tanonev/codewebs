package engine;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import util.FileSystem;
import util.Helper;

import models.AST;
import models.ASTID;
import models.Context;
import models.EquivalenceClass;
import models.NodeMapping;
import models.Program;
import models.Subtree;
import models.SubtreeLocation;

/**
 * Class: Factorizer
 * -----------------
 * Takes in a set of programs, finds analogies and reduces the programs to
 * have symbolic nodes in place of analogies. Is pretty sweet :).
 */
public class Factorizer {

	protected Map<ASTID, Program> programs;

	// This seems to only be used to count the total num of "replacements"
	// per iteration.
	int numReplacements = 0;
	HashMap<AST, Integer> astCounts;

	public Factorizer(Map<ASTID, Program> programs) {
		this.programs = programs;
	}

	/**
	 * Method: Factorize
	 * -----------------
	 * The main work of FactorAsts is done in this method. Runs factorization
	 * iterations until the size of programs stops shrinking. Before this method
	 * is called all initialization should be finished and
	 * necessary data should have been loaded. 
	 */
	public Map<ASTID, Program> factorize() {
		astCounts = createAstCounts(programs);
		
		int factorIteration = 0;
		int sameInARow = 0;
		int numPrograms = programs.size();

		while (sameInARow < 3) {
			detectFunctionallyEmptyLines();
			numPrograms = programs.size();
			runFactorizationIteration(factorIteration);
			factorIteration++;
			if (programs.size() == numPrograms) {
				sameInARow++;
			} else {
				sameInARow = 0;
			}
			printUpdate(numPrograms, programs.size());
			numPrograms = programs.size();
		}
		return this.programs;
	}

	/**
	 * Method: Run Factorization Iteration
	 * -----------------------------------
	 * First, remove "empties" (programs with unsubstantial lines).
	 * Then, chose the best equivalence pair: two asts that we are most 
	 * convinced are the same and factor that equivalence out of all
	 * programs.
	 */
	protected void runFactorizationIteration(int factorIteration){

		List<Subtree> subtrees = getSubtreesFromPrograms(factorIteration);
		EquivalenceClass pair = getBestEquivalence(subtrees);
		List<EquivalenceClass> equivalenceClasses = Collections.singletonList(pair);
		saveEquivalenceClasses(equivalenceClasses, factorIteration);
		factorPrograms(equivalenceClasses);
		
		printPairInfo(pair);
			
		Helper.saveReducedPrograms(programs, astCounts, factorIteration);
		
	}

	/**
	 * Method: Factor Programs
	 * -----------------------
	 * Given the newly identified equivalence classes (really just one pair)
	 * we then reduce all programs that have any of the involved subtrees and
	 * replace the analogous subtrees with a symbolic node.
	 */
	protected void factorPrograms(List<EquivalenceClass> equivalenceClasses) {
		numReplacements = 0;
		HashMap<AST, Program> uniquePrograms = remapPrograms(programs,
				astCounts, equivalenceClasses);
		programs = new HashMap<ASTID, Program>();
		for (Program reducedProgram : uniquePrograms.values()) {
			programs.put(reducedProgram.astId, reducedProgram);
		}

		System.out.println("total replacements: " + numReplacements);
	}

	/**
	 * Method: Get Best Equivalence
	 * --------------------------------
	 * Chooses the best equivalence pair of subtrees. The best pair is 
	 * determined to be the two subtrees that have maximal overlapScore (they 
	 * preserve program output in many different contexts and never change 
	 * output).
	 */
	protected EquivalenceClass getBestEquivalence(List<Subtree> subtrees) {
		int bestSoFar = 0;
		Set<Subtree> pair = null;
		for (int idx1 = 0; idx1 < subtrees.size(); idx1++) {
			Subtree first = subtrees.get(idx1);
			if (first.info.getCount() < bestSoFar)
				break;
			if (first.ast == AST.NULL)
				continue;
			for (int idx2 = idx1 + 1; idx2 < subtrees.size(); idx2++) {
				Subtree second = subtrees.get(idx2);
				if (second.info.getCount() < bestSoFar)
					break;
				if (second.ast == AST.NULL)
					continue;

				int overlapScore = first.info.getOverlapScore(second.info);
				if (overlapScore > bestSoFar) {
					bestSoFar = overlapScore;
					pair = new HashSet<Subtree>();
					pair.add(first);
					pair.add(second);
				}
			}
		}
		if(bestSoFar == 0) {
			throw new RuntimeException("no pair found.");
		}
		return new EquivalenceClass(pair);
	}

	/**
	 * Method: Remove Empty Subtrees
	 * -----------------------------
	 * I don't understand this completely. But I get the feeling that
	 * it is removing programs with no ops. Pls elaborate :).
	 */
	protected void detectFunctionallyEmptyLines() {
		List<Subtree> subtrees = getSubtreesFromPrograms();
		Subtree empty = getEmptySubtree(subtrees);
		int sum = 0;
		for (Subtree t : subtrees) {
			if (t == empty)
				continue;
			int count = 0;
			for (int idx = 0; idx < t.info.getCount(); idx++) {
				Context c = t.info.getComplements().get(idx);
				if (empty.info.containsComplement(c)) {
					if (empty.info.getOutput(c) == t.info.getOutput(c)) {
						programs.remove(t.info.getSubtreeLocations().get(
								idx).astId);
						count++;
					} else {
						if (count > 0) {
							count = 0;
						}
						break;
					}
				}
			}
			sum += count;
		}

		System.out.println("Asts deleted because they are empty: " + sum);
	}

	/**
	 * Method: Get Subtrees From Programs
	 * ----------------------------------
	 * See actual implementation.
	 */
	protected List<Subtree> getSubtreesFromPrograms() {
		return getSubtreesFromPrograms(-1);
	}

	/**
	 * Method: Get Subtrees From Programs
	 * ----------------------------------
	 * Given your set of programs, return a subtree object for all subforests
	 * of statements :). It needs the iteration number so that it can look
	 * up the subtree files if it needs to print anything.
	 */
	protected List<Subtree> getSubtreesFromPrograms(int iteration) {
		List<Subtree> subtrees = Helper.getSubtrees(programs,
				Integer.MAX_VALUE, iteration);
		return subtrees;
	}

	/**
	 * Method: Get Empty Subtree
	 * -------------------------
	 * This method finds one example of a subtree that is empty.
	 */
	private Subtree getEmptySubtree(List<Subtree> subtrees) {
		Subtree empty = null;
		for (Subtree t : subtrees) {
			if (t.ast == AST.NULL) {
				empty = t;
				break;
			}
		}
		assert (empty != null);
		return empty;
	}

	/**
	 * Method: Create Ast Count
	 * -----------------------
	 * Create a table to keep track of how many copies of an AST that we see.
	 */
	private HashMap<AST, Integer> createAstCounts(Map<ASTID, Program> programs) {
		HashMap<AST, Integer> ASTCounts = new HashMap<AST, Integer>();
		for (ASTID astId : programs.keySet()) {
			AST ast = programs.get(astId).ast;
			ASTCounts.put(ast, 1);
		}
		return ASTCounts;
	}

	/**
	 * Method: Reduce Programs
	 * -----------------------
	 * @param eqMap 
	 */
	private Program reduceProgram(Program program, List<NodeMapping> mappings,
			Map<Integer, EquivalenceClass> eqMap) {
		ASTID astId = program.astId;
		numReplacements += mappings.size();

		Collections.sort(mappings, new Comparator<NodeMapping>() {
			@Override
			public int compare(NodeMapping a, NodeMapping b) {

				int smallestInA = a.nodes.get(0);
				int smallestInB = b.nodes.get(0);
				if (smallestInA != smallestInB) {
					return smallestInA - smallestInB;
				}
				int nodesInA = a.nodes.size();
				int nodesInB = b.nodes.size();
				return nodesInA - nodesInB;
			}
		});
		Map<Integer, Integer> changeList = new TreeMap<Integer, Integer>();
		// System.out.println("mapping list:");
		for (NodeMapping mapping : mappings) {
			// System.out.println(mapping);
			boolean mapped = false;
			// System.out.println(mapping);
			for (int nodeId : mapping.nodes) {
				if (changeList.containsKey(nodeId))
					break;
				if (!mapped) {
					changeList.put(nodeId, mapping.equivalenceId);
					mapped = true;
				} else {
					changeList.put(nodeId, null);
				}
			}
		}

		saveNodeMap(changeList, astId);

		return program.getReducedProgram(changeList, eqMap);
	}

	private Map<ASTID, List<NodeMapping>> getProgramMappings(
			List<EquivalenceClass> equivalenceClasses,
			Map<ASTID, Program> programs) {
		Map<ASTID, List<NodeMapping>> programMappings = new HashMap<ASTID, List<NodeMapping>>();
		for (ASTID astId : programs.keySet()) {
			programMappings.put(astId, new ArrayList<NodeMapping>());
		}

		for (EquivalenceClass equivalenceClass : equivalenceClasses) {
			for (Subtree tree : equivalenceClass.subtrees) {
				Collection<SubtreeLocation> locs = tree.info
						.getSubtreeLocations();
				int equivalenceId = equivalenceClass.id;
				for (SubtreeLocation loc : locs) {
					ASTID astId = loc.astId;
					List<Integer> nodeIds = loc.nodeIds;
					if (nodeIds.get(0) == -1)
						continue; // this removes epsilon matching
					NodeMapping mapping = new NodeMapping(nodeIds,
							equivalenceId);
					programMappings.get(astId).add(mapping);
				}
			}
		}
		return programMappings;
	}

	private HashMap<AST, Program> remapPrograms(Map<ASTID, Program> corrects,
			Map<AST, Integer> ASTCounts,
			List<EquivalenceClass> equivalenceClasses) {
		// "map" and "reduce" (but not map-reduce)
		Map<Integer, EquivalenceClass> eqMap = getEquivalenceMap(
				equivalenceClasses);
				
		Map<ASTID, List<NodeMapping>> programMappings = getProgramMappings(
				equivalenceClasses, corrects);
		HashMap<AST, Program> uniquePrograms = new HashMap<AST, Program>();
		HashMap<AST, Integer> tmpASTCounts = new HashMap<AST, Integer>();
		for (ASTID astId : corrects.keySet()) {
			Program program = corrects.get(astId);

			List<NodeMapping> mappings = programMappings.get(astId);
			Program reducedProgram = reduceProgram(program, mappings, eqMap);
			uniquePrograms.put(reducedProgram.ast, reducedProgram);
			if (tmpASTCounts.containsKey(reducedProgram.ast)) {
				tmpASTCounts.put(
						reducedProgram.ast,
						tmpASTCounts.get(reducedProgram.ast)
						+ ASTCounts.get(program.ast));
			} else {
				tmpASTCounts
				.put(reducedProgram.ast, ASTCounts.get(program.ast));
			}
		}
		ASTCounts.clear();
		ASTCounts.putAll(tmpASTCounts);
		return uniquePrograms;
	}

	private Map<Integer, EquivalenceClass> getEquivalenceMap(
			List<EquivalenceClass> equivalenceClasses) {
		Map<Integer, EquivalenceClass> map = new
				HashMap<Integer, EquivalenceClass>();
		for(EquivalenceClass c : equivalenceClasses) {
			int id = c.id;
			map.put(id, c);
		}
		return map;
	}

	private void saveNodeMap(Map<Integer, Integer> changeList, ASTID astId) {
		String dir = FileSystem.getNodeMappingDir();
		String fileName = astId + ".txt";
		String outPath = dir + fileName;
		try {
			PrintWriter writer = new PrintWriter(outPath, "UTF-8");
			for (int nodeId : changeList.keySet()) {
				Integer equivalenceId = changeList.get(nodeId);
				writer.println(nodeId + " -> " + equivalenceId);
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to write equivalence", e);
		}
	}

	/**
	 * Method: Save Equivalence Classes 
	 * --------------------------------- 
	 * A helper method that writes the sets of equivalence classes to the 
	 * console.
	 */
	protected void saveEquivalenceClasses(
			List<EquivalenceClass> equivalenceClasses, int i) {
		for (EquivalenceClass equivalence : equivalenceClasses) {
			equivalence.saveToFile(i);
		}
	}

	protected void printPairInfo(EquivalenceClass pair) {
		for (Subtree tree : pair.getSubtrees()) {
			System.out.println(tree.info.getCount());
			System.out.println(tree.info.root);
			System.out.println(tree.info.code);
		}
	}

	private void printUpdate(
			int sizeBefore,
			int sizeAfter
			) { 
		System.out.println("Unique ASTs before: " + sizeBefore);
		System.out.println("Unique ASTs after: " + sizeAfter);
		
	}


}
