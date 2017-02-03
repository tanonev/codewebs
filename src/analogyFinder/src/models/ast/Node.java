package models.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Equivalence;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class Node
 * A node from the underlying ast (there is exactly one instance for each ast
 * node in each program). The node is also the root of an underlying subtree.
 */
public class Node {

	// The type (from json).
	private String type;

	// The name (from json).
	private String name;

	// The size of the tree rooted at this node.
	private int size;

	// Each node has an id (from the json) which is unique to that node.
	private int id;

	// A node can be given an equivalence label.
	private Equivalence equivalence;

	// Nodes compute their hash value on creation and cache it.
	private int hash;

	// This node is also a tree. These are its children.
	private List<Node> children;

	// A back pointer to the parent of this node
	private Node parent;

	// Location in the postorder of the AST from which this node was created.
	private int postorderIndex = -1;

	// This may be useful later if we ever reduce a subtree...
	private int reducedSize;
	
	// User defined variable name, since name is nulled out for hash purposes
	private String trueName = null;

	public static final Node NULL = new Node("NO_OP", "{{empty}}", null, -1);
	static {
	  NULL.setPostorderIndex(Integer.MAX_VALUE);
	}

	public static Node makeNode(JSONObject node) {
		throw new RuntimeException("depricated");
		//String name = node.optString("name");
		//return new Node(node.getString("type"), name, null, node.getInt("id"));
	}

	public static Node makeNode(JSONObject node, List<Node> children,
			Set<String> keywords) {
		if (keywords == null) return makeNode(node);
		String name = node.optString("name");
		String trueName = null;
		if (node.getString("type").equals("IDENT") && !keywords.contains(name)){
		  trueName = name;
			name = null;
		}

		if (node.getString("type").equals("CONST")){
			String newName = null;
			try {
				newName = "" +Integer.parseInt(name.trim());
			} catch(NumberFormatException e) {}
			if (newName == null) {
				try {
					newName = "" +Double.parseDouble(name.trim());
				} catch(NumberFormatException e) {}
			}
			name = newName;
		}
		Node newNode = new Node(node.getString("type"), name, children, node.getInt("id"));
		for(Node c : children) {
			c.parent = newNode;
		}
		newNode.trueName = trueName;
		return newNode;
	}

	public Node(String type, String name, List<Node> children, int id) {
		this.type = type;
		this.name = name;
		if (name != null) {
			this.name = name.trim();
		}
		this.id = id;
		this.equivalence = null;
		this.children = children;
		if (this.children == null) {
			this.children = new ArrayList<Node>();
		}
		this.size = 1;
		for(Node child : this.children) {
			this.size += child.size;
            child.parent = this;
		}
		this.reducedSize = this.size;
		this.hash = calculateHash();
	}

	public void setPostorderIndex(int index) {
		this.postorderIndex = index;
	}

	public List<Node> getChildren() {
		return children;
	}

	public  String toString() {
		return "{ " +id +", "+ type + ", " + name + ", " + size + " }";
	}

	public int getSize() {
		return size;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}
	
	public String getTrueName() {
	  if (trueName != null) return trueName;
	  return name;
	}

	public int getId() {
		return id;
	}

	public void setId(int newId) {
		this.id = newId;
	}

	public void markEquivalence(Equivalence equivalence) {
		this.equivalence = equivalence;
		// This is the only time you have to recalculate the hash.
		this.hash = calculateHash();
	}

	public Equivalence getInheritedEquivalence() {
		Equivalence toReturn = equivalence;

		if(parent != null) {
			Equivalence parentsEq = parent.getInheritedEquivalence();
			if(parentsEq != null) {
				toReturn = parentsEq;
			}
		}
		return toReturn;
	}

	public JSONObject getJson() {
		JSONObject json = new JSONObject();
		if(name != null && !name.isEmpty()) {
			json.put("name", name);
		}
		json.put("type", type);
		json.put("id", id);

		JSONArray list = new JSONArray();
		if(!hasEquivalence()) {
			for(Node child : children) {
				JSONObject childJson = child.getJson();
				list.put(childJson);
			}
		} 
		json.put("children", list);

		return json;
	}

	@Override
	public int hashCode() {
		return this.hash;
	}

	public boolean shiftEquals(Node other, int shift, int otherShift) {
		this.size -= shift;
		other.size -= otherShift;
		boolean result = this.equals(other);
		this.size += shift;
		other.size += otherShift;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;

		if (hasEquivalence()) {
			return equivalence == other.equivalence;
		}

		if(hashCode() != obj.hashCode())
			return false;

		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;

		if (size != other.size)
			return false;
		return true;
	}

	public int getPostorderIndex() {
		if(postorderIndex == -1) {
			throw new RuntimeException("never set!");
		}
		return postorderIndex;
	}

	public boolean hasEquivalence() {
		return equivalence != null;
	}

	public Equivalence getEquivalence() {
		return equivalence;
	}

	public void calculateReducedSizes() {
		this.reducedSize = 1;
		for(Node child : this.children) {
			this.reducedSize += child.reducedSize;
		}
	}

	public Node getParent() {return parent;}
	
	// assumes this node is equal to Node o
	public boolean checkValidIdentifierMap(Node other, Map<String, String> associations) {
	  if (trueName != null) {
	    if (associations.containsKey(trueName)) {
	      return associations.get(trueName).equals(other.trueName);
	    } else {
	      associations.put(trueName, other.trueName);
	      return true;
	    }
	  }
	  return true;
	}

	// --------------------------- Private -------------------------------//

	/**
	 * The hash of a node is independent of its children.
	 */
	private int calculateHash(){
		if (this == NULL) {
			return 0;
		}

		// This code ignores any equivalence labels!

		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

}
