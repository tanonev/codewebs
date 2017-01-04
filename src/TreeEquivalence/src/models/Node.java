package models;

import java.util.Set;

import org.json.JSONObject;

public class Node {
	public String type;
	public String name;
	public int size;

	public int id;
	
	public static final Node NULL = new Node("NO_OP", "{{empty}}", 1, -1);
	
	public static Node makeNode(JSONObject node, int size) {
		return new Node(node.getString("type"), node.optString("name"), size,
				node.getInt("id"));
	}
	
	public static Node makeNode(JSONObject node, int size, Set<String> keywords) {
	  if (keywords == null) return makeNode(node, size);
	  String name = node.optString("name");
	  if (node.getString("type").equals("IDENT") && !keywords.contains(name)) name = null;
	  return new Node(node.getString("type"), name, size, node.getInt("id"));
	}

	public Node(String type, String name, int size, int id) {
		this.type = type;
		this.name = name;
		this.size = size;
		this.id = id;
	}

	public  String toString() {
		return "{ " + type + ", " + name + ", " + size + " }";
	}
	
	@Override
	public int hashCode() {
	  if (this == NULL) return 0;
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
//		result = prime * result + size;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (size != other.size)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}