package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubtreeGraph {

	private static final int CUTOFF = 10;
	
	Map<Subtree, Map<Subtree, SubtreeEdgeData>> adjacencyList = new HashMap<Subtree, Map<Subtree, SubtreeEdgeData>>();
	ArrayList<SubtreeEdgeData> edges = new ArrayList<SubtreeEdgeData>();
	
	public SubtreeGraph (Map<Context, List<Subtree>> complements) {
		
		for (Context c : complements.keySet()) {
		  List<Subtree> subtrees = complements.get(c);
			int outerCounter = 0;
			for(Subtree A : subtrees) {
				int innerCounter = 0;
				for(Subtree B : subtrees) {
					if (innerCounter >= outerCounter) break;
					addEdge(A, B, c);
					innerCounter++;
				}
				outerCounter++;
			}
		}		
	}

	public void addEdge(Subtree A, Subtree B, Context complement) {
		assert (A != B);
		
		// if nodes do not exist in graph, add them
		if (!adjacencyList.containsKey(A)){
			adjacencyList.put(A, new HashMap<Subtree, SubtreeEdgeData>());
		}
		if (!adjacencyList.containsKey(B)){
			adjacencyList.put(B, new HashMap<Subtree, SubtreeEdgeData>());
		}
		
		if (!adjacencyList.get(A).containsKey(B) 
				&& !adjacencyList.get(B).containsKey(A)) {
			SubtreeEdgeData newEdge = new SubtreeEdgeData(A,B);
			edges.add(newEdge);
			adjacencyList.get(A).put(B, newEdge);
			adjacencyList.get(B).put(A, newEdge);
		}

		SubtreeEdgeData currEdge = adjacencyList.get(A).get(B);		
		currEdge.insert(complement);
	}
	
	/**
	 * Method: Get Equivalence Classes
	 * -------------------------------
	 * Finds the set of ASTs that are equivalent to one another.
	 * Does so by creating an undirected graph where there is an
	 * edge between two nodes if they have large enough intersecting
	 * complement hashes. Finds groups of equivalent subtrees by
	 * finding the connected components of the graph.
	 */
	public List<EquivalenceClass> getConnectedComponents() {
		System.out.println("get equivalence classes...");
		Set<Subtree> visited = new HashSet<Subtree>();
		List<EquivalenceClass> equivalenceClasses = new ArrayList<EquivalenceClass>();
		for (Subtree tree : adjacencyList.keySet()) {
			if (visited.contains(tree)) continue;

			boolean connected = false;
			for (Subtree neighbor : adjacencyList.get(tree).keySet()) {
				SubtreeEdgeData edgeData = adjacencyList.get(tree).get(neighbor);
				if (!edgeDiscard(edgeData)) connected = true;
			}
			if (connected == false) continue;
			Set<Subtree> component = findComponent(visited, tree);
			
			EquivalenceClass equivalence = new EquivalenceClass(component);
			equivalenceClasses.add(equivalence);
		}
		
		return equivalenceClasses;	
	}
	
	/**
	 * Method: Find Connected Component
	 * -------------------------------
	 * A simple, recursive breadth first search for a graph of Subtree
	 * pointers. Works by keeping a set of visited nodes.
	 */
	private Set<Subtree> findComponent(Set<Subtree> visited, Subtree currentNode) {
		if (visited.contains(currentNode)){
			return new HashSet<Subtree>();
		}
		visited.add(currentNode);
		Set<Subtree> connected = new HashSet<Subtree>();
		connected.add(currentNode);
		for (Subtree neighbor : adjacencyList.get(currentNode).keySet()){
			SubtreeEdgeData edgeData = adjacencyList.get(currentNode).get(neighbor);
			if (edgeDiscard(edgeData)) continue;
			Set<Subtree> neighborConnected = findComponent(visited, neighbor);
			connected.addAll(neighborConnected);
		}
		return connected;
	}
	
	private boolean edgeDiscard(SubtreeEdgeData edgeData) {
		//return !edgeData.areAnalogous();
		return !edgeData.areEquivalent();
	}
}

// can we build a map to output from complement subtree pairs (or hashpairs?)?