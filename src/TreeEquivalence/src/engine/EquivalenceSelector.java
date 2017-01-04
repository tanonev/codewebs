package engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.RuntimeErrorException;

import models.AST;
import models.EquivalenceClass;
import models.EquivalenceClassGraph;
import models.Subtree;

public class EquivalenceSelector {

	private static final int THRESHOLD = 2;
	private static final int SUBTREES_FOR_BEST_AST = 1000;
	private static final int SUBTREES_FOR_LEAF_AST = 10000;
	
	List<Subtree> subtrees = null;

	public EquivalenceSelector(List<Subtree> subtrees) {
		this.subtrees = subtrees;
	}

	public EquivalenceClass choseOld(Set<EquivalenceClass> ignoreList) {
		// get all equivalences
		List<EquivalenceClass> eqs = getAllPotentialEquivalences(ignoreList);
		
		//chose the best one
		EquivalenceClass best = choseBestLeafish(eqs);
		
		return best;
	}
	
	public EquivalenceClass chose(Set<EquivalenceClass> ignoreList) {
		System.out.println("Num subtrees: " +subtrees.size());
		EquivalenceClass bestEstimate = getBestEstimate(ignoreList);
		List<Subtree> reduced = reduceSubtrees(bestEstimate);
		
		System.out.println("Reduced subtrees: " + reduced.size());
		// get all equivalences
		List<EquivalenceClass> eqs = getPotentialEquivalences(ignoreList, reduced);
		
		// Then, find the most leaf like node.
		EquivalenceClassGraph graph = getGraph(eqs, bestEstimate);
		EquivalenceClass bestLeaf = graph.getMostParents();
		
		return bestLeaf;
		//return bestEstimate;
	}

	private List<Subtree> reduceSubtrees(EquivalenceClass bestEstimate) {
		ArrayList<Subtree> reducedTrees = new ArrayList<Subtree>();
		for(Subtree tree : this.subtrees) {
			if(bestEstimate.containsTree(tree)) {
				reducedTrees.add(tree);
			}
		}
		return reducedTrees;
	}

	private EquivalenceClass getBestEstimate(Set<EquivalenceClass> ignoreList) {
		int bestSoFar = 0;
		int numConsidered = Math.min(SUBTREES_FOR_BEST_AST, subtrees.size());
		System.out.println("numConsidered: " + numConsidered);
		Set<Subtree> pair = null;
		for (int idx1 = 0; idx1 < numConsidered; idx1++) {
			Subtree first = subtrees.get(idx1);
			
			if (first.ast == AST.NULL)
				continue;
			for (int idx2 = idx1 + 1; idx2 < numConsidered; idx2++) {
				Subtree second = subtrees.get(idx2);
				//System.out.println(first.info.getCount() +", " + second.info.getCount());
				if (second.ast == AST.NULL)
					continue;

				
				int score = first.getAnalogyScore(second);
				if (score > bestSoFar) {
					System.out.println(score);
					bestSoFar = score;
					pair = new HashSet<Subtree>();
					pair.add(first);
					pair.add(second);
					//throw new RuntimeException("hello, world");
				}
			}
		}
		if(bestSoFar == 0) {
			throw new RuntimeException("no pair found.");
		}
		return new EquivalenceClass(pair);
	}

	private EquivalenceClass choseBestLeafish(List<EquivalenceClass> eqs) {
		System.out.println("Chosing best score. Drilling down for leaves");
		
		// First chose the "best"
		EquivalenceClass best = getBest(eqs);
		
		// Then, find the most leaf like node.
		EquivalenceClassGraph graph = getGraph(eqs, best);
		EquivalenceClass bestLeaf = graph.getMostParents();
		
		return bestLeaf;
	}

	private EquivalenceClass getBest(List<EquivalenceClass> eqs) {
		EquivalenceClass best = null;
		int bestScore = 0;
		for(EquivalenceClass eq : eqs) {
			int score = eq.getAnalogyScore();
			if(score > bestScore){
				bestScore = score;
				best = eq;
			}
		}
		return best;
	}
	
	private EquivalenceClassGraph getGraph(List<EquivalenceClass> eqs,
			EquivalenceClass start) {
		System.out.println("Get graph...");
		EquivalenceClassGraph graph = new EquivalenceClassGraph();
		populateGraph(graph, eqs, start);
		return graph;
	}

	private void populateGraph(EquivalenceClassGraph graph,
			List<EquivalenceClass> eqs, 
			EquivalenceClass start) {
		if(graph.hasNode(start)) return;
		graph.addNode(start);
		
		List<EquivalenceClass> children = getChildren(eqs, start);
		for(EquivalenceClass child : children) {
			if(child == start) continue;
			graph.addEdge(start, child);
			populateGraph(graph, eqs, child);
		}
	}

	private List<EquivalenceClass> getChildren(List<EquivalenceClass> eqs,
			EquivalenceClass eq) {
		
		List<EquivalenceClass> children = new ArrayList<EquivalenceClass>();
		for(EquivalenceClass a : eqs) {
			if(eq.containsClass(a)) {
				children.add(a);
			}
		}
		return children;
	}

	private EquivalenceClass choseBestLeaf(List<EquivalenceClass> eqs) {
		System.out.println("Chose the best leaf...");
		//EquivalenceClassGraph graph = new EquivalenceClassGraph(eqs);
		//graph.removeCycles();
		//List<EquivalenceClass> leaves = graph.getLeaves();
		int bestSoFar = 0;
		int numLeaves = 0;
		EquivalenceClass best = null;
		for(EquivalenceClass eq : eqs) {
			if(eq.isLeafClass(eqs)) {
				numLeaves++;
				int score = eq.getAnalogyScore();
				if(score > bestSoFar) {
					bestSoFar = score;
					best = eq;
				}
			}
		}
		System.out.println("Num equivalences: " + eqs.size());
		System.out.println("Num leaves: " + numLeaves);
		return best;
	}
	
	private List<EquivalenceClass> getPotentialEquivalences(
			Set<EquivalenceClass> ignoreList, List<Subtree> reduced) {
		System.out.println("Find potential equivalences from reduced...");
		List<EquivalenceClass> eqs = new ArrayList<EquivalenceClass>();

		int numElems = Math.min(subtrees.size(), SUBTREES_FOR_LEAF_AST);
		for (Subtree first : subtrees.subList(0, numElems)) {
			if (first.ast == AST.NULL) 
				continue;
			for (Subtree second : reduced) {
				if (second.ast == AST.NULL || first == second) 
					continue;

				if (isPotentialAnalogy(first, second)) {
					Set<Subtree> pair = new HashSet<Subtree>();
					pair.add(first);
					pair.add(second);
					EquivalenceClass newEq = new EquivalenceClass(pair);
					if(!ignoreList.contains(newEq)) {
						eqs.add(newEq);
					}
				}
			}
		}
		return eqs;
	}

	private List<EquivalenceClass> getAllPotentialEquivalences(
			Set<EquivalenceClass> ignoreList) {
		System.out.println("Find all potential equivalences...");
		List<EquivalenceClass> eqs = new ArrayList<EquivalenceClass>();

		for (int idx1 = 0; idx1 < subtrees.size(); idx1++) {
			Subtree first = subtrees.get(idx1);
			if (first.ast == AST.NULL)
				continue;
			for (int idx2 = idx1 + 1; idx2 < subtrees.size(); idx2++) {
				Subtree second = subtrees.get(idx2);
				if (second.ast == AST.NULL)
					continue;

				if (isPotentialAnalogy(first, second)) {
					Set<Subtree> pair = new HashSet<Subtree>();
					pair.add(first);
					pair.add(second);
					EquivalenceClass newEq = new EquivalenceClass(pair);
					if(!ignoreList.contains(newEq)) {
						eqs.add(newEq);
					}
				}
			}
		}
		return eqs;
	}
	
	private boolean isPotentialAnalogy(Subtree first, Subtree second) {
		return first.getAnalogyScore(second) > THRESHOLD;
	}



}
