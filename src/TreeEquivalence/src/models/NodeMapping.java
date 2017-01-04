package models;

import java.util.List;

public class NodeMapping {

	public List<Integer> nodes;
	public int equivalenceId;
	
	public NodeMapping(List<Integer> nodes, int equivalenceId) {
		this.nodes = nodes;
		this.equivalenceId = equivalenceId;
	}
	
	@Override
	public String toString() {
		return nodes + ": " + equivalenceId;
	}
	
}
