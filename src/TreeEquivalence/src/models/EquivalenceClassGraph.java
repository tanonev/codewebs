package models;



import java.util.*;

public class EquivalenceClassGraph {

	private Map<EquivalenceClass, Set<EquivalenceClass>> edgeList = null;
	
	/**
	 * Contructor
	 * ----------
	 * You can build one up from scratch...
	 */
	public EquivalenceClassGraph() {
		edgeList = new HashMap<EquivalenceClass, Set<EquivalenceClass>>();
	}
	
	/**
	 * Contructor
	 * ----------
	 * Warning: Not implemented. Its too slow.
	 */
	public EquivalenceClassGraph(List<EquivalenceClass> eqs) {
		System.out.println("Creating graph...");
		edgeList = new HashMap<EquivalenceClass, Set<EquivalenceClass>>();
		for(EquivalenceClass c : eqs) {
			addNode(c);
		}
		System.out.println("Creating edges...");
		/*for(int i = 0; i < eqs.size(); i++) {
			EquivalenceClass first = eqs.get(i);
			for(int j = i + 1; j < eqs.size(); j++) {
				EquivalenceClass second = eqs.get(j);
				if(first.containsClass(second)) {
					//addEdge(a, b);
				}
			}
		}*/
		for(EquivalenceClass a : eqs) {
			for (EquivalenceClass b : eqs) {
				if (a == b) continue;
				if (a.containsClass(b)) break;
			}
		}
	}
	
	public EquivalenceClass getMostParents() {
		Map<EquivalenceClass, Integer> ancestorCount = new 
				HashMap<EquivalenceClass, Integer>();
		EquivalenceClass mostParents = null;
		
		for(EquivalenceClass parent : edgeList.keySet()) {
			mostParents = parent;
			for(EquivalenceClass child : edgeList.get(parent)) {
				if(!ancestorCount.containsKey(child)) {
					ancestorCount.put(child, 0);
				}
				int old = ancestorCount.get(child);
				ancestorCount.put(child, old+1);
			}
		}
		
		int bestSoFar = 0;
		for(EquivalenceClass eq : ancestorCount.keySet()) {
			int count = ancestorCount.get(eq);
			if(count > bestSoFar) {
				bestSoFar = count;
				mostParents = eq;
			}
		}
		return mostParents;
	}
	
	public EquivalenceClass getFurthestNode(EquivalenceClass start) {
		Set<EquivalenceClass> visited = new HashSet<EquivalenceClass>();
		EquivalenceClass lastVisited = start;
		Queue<EquivalenceClass> fringe = new LinkedList<EquivalenceClass>();
		fringe.add(start);
		while (!fringe.isEmpty()) {
			EquivalenceClass next = fringe.remove();
			if(visited.contains(next)) continue;
			lastVisited = next;
			visited.add(next);
			for (EquivalenceClass child : edgeList.get(next)) {
				fringe.add(child);
			}
		}
		return lastVisited;
	}
	
	public boolean hasNode(EquivalenceClass node) {
		return edgeList.containsKey(node);
	}
	
	public void addNode(EquivalenceClass node) {
		edgeList.put(node, new HashSet<EquivalenceClass>());
	}
	
	// add a directed edge
	public void addEdge(EquivalenceClass start, EquivalenceClass end) {
		edgeList.get(start).add(end);
	}

	public void removeCycles() {
		System.out.println("Removing cycles...");
		
		Set<EquivalenceClass> toDelete = new HashSet<EquivalenceClass>();
		for (EquivalenceClass e : edgeList.keySet()) {
			Set<EquivalenceClass> visited = new HashSet<EquivalenceClass>();
			if (hasCycle(e, visited)) {
				toDelete.add(e);
			}
		}
		
		Map<EquivalenceClass, Set<EquivalenceClass>> newEdgeList = null;
		newEdgeList = new HashMap<EquivalenceClass, Set<EquivalenceClass>>();
		
		for (EquivalenceClass e : edgeList.keySet()) {
			if (toDelete.contains(e)) continue;
			Set<EquivalenceClass> newEdges = edgeList.get(e);
			newEdges.removeAll(toDelete);
			newEdgeList.put(e, newEdges);
		}
		edgeList = newEdgeList;
	}
	
	public List<EquivalenceClass> getLeaves() {
		List<EquivalenceClass> leaves = new ArrayList<EquivalenceClass>();
		for (EquivalenceClass e : edgeList.keySet()) {
			if (isLeaf(e)) {
				leaves.add(e);
			}
		}
		return leaves;
	}
	
	private boolean isLeaf(EquivalenceClass node) {
		return edgeList.get(node).isEmpty();
	}

	private boolean hasCycle(
			EquivalenceClass startNode,
			Set<EquivalenceClass> visited) {
		for (EquivalenceClass child : edgeList.get(startNode)) {
			if (hasCycleInner(startNode, child, visited)) return true;
		}
		return false;
	}
	
	private boolean hasCycleInner(
			EquivalenceClass startNode,
			EquivalenceClass node, 
			Set<EquivalenceClass> visited) {
		if (node == startNode) return true;
		if (visited.contains(node)) return false;
		visited.add(node);
		
		for (EquivalenceClass child : edgeList.get(node)) {
			if (hasCycleInner(startNode, child, visited)) return true;
		}
		return false;
	}

	public int getSize() {
		return edgeList.size();
	}
	
	public Set<EquivalenceClass> getNodes() {
		return edgeList.keySet();
	}

	public void output() {
		for (EquivalenceClass node : edgeList.keySet()) {
			System.out.println("node: " + node.id);
			node.outputCode();
			List<Integer> edges = new ArrayList<Integer>();
			for(EquivalenceClass other: edgeList.get(node)) {
				edges.add(other.id);
			}
			System.out.println("edges: " + edges);
			System.out.println("\n");
		}
		
	}


	
}
