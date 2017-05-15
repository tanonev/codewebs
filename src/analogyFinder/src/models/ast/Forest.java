package models.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minions.ForestMapifier;
import minions.ForestStringifier;
import models.Program;
import models.Equivalence;
import models.Subforest;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class Forest
 * A forest is a subtree or a list of statement subtrees from a program. It
 * encapsulates the idea of a collection of postorder nodes. Of note, the
 * hash values for forests should always be computed by the parent AST in a
 * dynamic programming way and TOLD to the forest instance.
 */
public class Forest {

  public static final Forest NULL = new Forest(Collections.singletonList(Node.NULL), 0);

  // Essential
  protected List<Node> postorder;

  // Computed
  protected List<Node> roots;

  // This class is told its hash code so that we can compute all hashes
  // in a dynamic programming algorithm
  Integer hash;

  // This is only here so that AST can override forest.
  protected Forest() {
	postorder = new ArrayList<Node>();
    hash = null;
  }

  public Forest(List<Node> postorder, int hash) {
    this.hash = hash;
    this.postorder = postorder;
    findRoots();
  }

  public Forest(List<Node> postorder) {
    this.postorder = postorder;
    findRoots();
  }

  public List<Node> getPostorder() {
    return postorder;
  }

  public void findRoots() {
    roots = new LinkedList<Node>();
    int currIndex = postorder.size() - 1;
    while(currIndex >= 0) {
      Node curr = postorder.get(currIndex);
      roots.add(curr);
      int size = curr.getSize();
      currIndex -= size;
    }

    Collections.sort(roots, new Comparator<Node>() {
      @Override
      public int compare(Node a, Node b) {
        return b.getPostorderIndex() - a.getPostorderIndex();
      }
    });
  }

  public Node getNode(int index) {
    return postorder.get(index);
  }

  public Node getARoot() {
    return postorder.get(postorder.size() - 1);
  }

  public String rootType() {
    return getARoot().getType();
  }

  public int getSize() {
    return postorder.size();
  }

  @Override
  public int hashCode() {
    // This should not be true in the long run.
    if (hash == null) {
      int hashCode = 1;
      for (Node n : postorder) {
        hashCode = 31 * hashCode + n.hashCode();
      }
      //throw new RuntimeException("should not call");
      hash = hashCode;
      return hash;
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    Forest other = (Forest) obj;
    if(hashCode() != obj.hashCode())
      return false;

    return postorder.equals(other.postorder);
  }
  
  public boolean shiftEquals(Forest other, int size, int otherSize) {
    if (hashCode() != other.hashCode())
      return false;
    if (getSize() != other.getSize())
      return false;
    
    for (int index = 0; index < getSize(); index++) {
      int shift = index - postorder.get(index).getSize() < -1 ? size : 0;
      int otherShift = index - other.postorder.get(index).getSize() < -1 ? otherSize : 0;
      if (!postorder.get(index).shiftEquals(other.postorder.get(index), shift, otherShift))
        return false;
    }
    return true;
  }
  
  public boolean checkValidIdentifierMap(Forest other, Map<String, String> association) {
    for (int i = 0; i < getSize(); i++) {
      if (!postorder.get(i).checkValidIdentifierMap(other.postorder.get(i), association)) return false;
    }
    return true;
  }

  public List<Node> getRoots() {
    return this.roots;
  }

  public String getCodeString(Program program) {
    return ForestStringifier.stringify(program, this);
  }
  
  public int getLineNumber(Program program) {
    return ForestStringifier.getLineNumber(program, this);
  }
  
  public int[][] getMap(Program program) {
    return ForestMapifier.mapify(program, this);
  }
  
  public String toString() {
    return postorder + "";
  }

  public void setHashCode(int hash) {
    this.hash = hash;
  }

  public static Forest loadFromJson(JSONObject json, Set<String> keywords) {
    Forest forest = new Forest();
    forest.postorder = new ArrayList<Node>();
    if (json.has("statements")) {
      JSONArray arr = json.getJSONArray("statements");
      for (int i = 0; i < arr.length(); i++) {
        forest.makePostorder(arr.getJSONObject(i), keywords);
      }
    } else {
      forest.makePostorder(json, keywords);
    }
    for(int i = 0; i < forest.postorder.size(); i++) {
      Node n = forest.postorder.get(i);
      n.setPostorderIndex(i);
    }
    forest.findRoots();
    return forest;
  }
  
  // This method is kind of clunky. It takes a root node and copies all the nodes that are
  // in the subtree rooted at the node. However, it only adds a node to the postorder of our
  // current tree if it is also in the postorder of `original`.
  
  // It is strange but we have to "maintain" the inconsistency here that a node in the postorder can have
  // children that are not in the postorder. This weird inconsistency is why we need the `shiftEquals` function
  // and unfortunately, fixing it would break everything that uses shiftEquals... 
  // when we port this, we should fix this once and for all everywhere
  
  public Node appendSubtreeRootedAtNode(Forest original, Node root) {
	  Set<Integer> nodeIds = new HashSet<Integer>();
	  
	  for (Node n : original.postorder) {
		  nodeIds.add(n.getId());
	  }
	  
	  List<Node> children = new ArrayList<Node>();
	  Node newNode = null;
	  
	  // Only reduce this subtree if the entire subtree is included in the forest
	  if(root.hasEquivalence() && original.postorder.indexOf(root) >= root.getSize() - 1) {
	      String type = "EQUIV";
	      String name = root.getEquivalence().getName();
	      newNode = new Node(type, name, children, root.getId());
	      postorder.add(newNode);
	  } else {
		 for (Node oldChild : root.getChildren()) {
			 Node newChild = appendSubtreeRootedAtNode(original, oldChild);
			 children.add(newChild);	            
	      }
	      String type = root.getType();
	      String name = root.getName();
	      newNode = new Node(type, name, children, root.getId());
	    
	  	  if (nodeIds.contains(newNode.getId())) {
	  		  postorder.add(newNode);
	  	  }
	  }
	  
	  return newNode;
  }
  
  // This is a really really bad function...
  // We first make a deep copy of a forest because we want to be able to mark equivalences on nodes
  // without altering the nodes in the original forest. This is important!!
  
  // After that, we "reduce" the forest by calling `appendSubtreeRootedAtNode` which has the side-effect
  // of reducing nodes that have marked equivalences if the full subtree rooted at that node is in
  // the postorder of our forest.
  
  // Honestly, this is an ugly hack to hack around the way the original codewebs research code was set-up
  // We shouldn't have to do this once we refactor the rest of the codebase.
  
  public Forest getReduced(Equivalence eq) {
	// We make a deep copy here to maintain the invariant that methods that
	// return "new" objects must never mutate the original.
	Forest copy = new Forest();
	
    for (Node root : roots) {
        copy.appendSubtreeRootedAtNode(this, root);
    }
    
    for (int i = 0; i < copy.getPostorder().size(); i++) {
        Forest subtree = new Forest();
        subtree.appendSubtreeRootedAtNode(copy, copy.getNode(i));
        
        if (eq.containsSubforest(new Subforest(null, subtree))) {
            copy.getPostorder().get(i).markEquivalence(eq);
        }
    }
    
    for(int i = 0; i < copy.postorder.size(); i++) {
        Node n = copy.postorder.get(i);
        n.setPostorderIndex(i);
     }
    
    copy.findRoots();
   
    Forest reduced = new Forest();
    for (Node root : copy.getRoots()) {
        reduced.appendSubtreeRootedAtNode(copy, root);
    }
    
    for(int i = 0; i < reduced.postorder.size(); i++) {
        Node n = reduced.postorder.get(i);
        n.setPostorderIndex(i);
     }
    
    reduced.findRoots();
    return reduced;
  }
  
  public Forest getReduced(List<Equivalence> eqs) {
	// We make a deep copy here to maintain the invariant that methods that
	// return "new" objects must never mutate the original.
	Forest copy = new Forest();
	
    for (Node root : roots) {
        copy.appendSubtreeRootedAtNode(this, root);
    }

	for(int i = 0; i < copy.postorder.size(); i++) {
		Node n = copy.postorder.get(i);
		n.setPostorderIndex(i);
	}
	
	copy.findRoots();
	
	if (eqs.size() == 0) {
		return copy;
	}
	
	Forest reduced = new Forest();
	
	// Reduce the forest
    for (Equivalence eq : eqs) {
    	reduced = new Forest();
    	
    	for (int i = 0; i < copy.getPostorder().size(); i++) {
    		Forest subtree = new Forest();
    		subtree.appendSubtreeRootedAtNode(copy, copy.getNode(i));        
	        if (eq.containsSubforest(new Subforest(null, subtree))) {
	            copy.getPostorder().get(i).markEquivalence(eq);
	        }
        }

    	for (Node root : copy.getRoots()) {
    		reduced.appendSubtreeRootedAtNode(copy, root);
    	}
    
    	for(int i = 0; i < reduced.postorder.size(); i++) {
    		Node n = reduced.postorder.get(i);
    		n.setPostorderIndex(i);
    	}
    	reduced.findRoots();
    	
    	copy = reduced;
    }
    	
    return reduced;
  }  

  //----------------------- Private ---------------------------------//

  protected Node makePostorder(JSONObject node, Set<String> keywords) {
    JSONArray jsonChildren = node.getJSONArray("children");
    List<Node> children = new ArrayList<Node>();
    for (int i = 0; i < jsonChildren.length(); i++) {
      Node child = makePostorder(jsonChildren.getJSONObject(i), keywords);
      children.add(child);
    }
    Node newNode = Node.makeNode(node, children, keywords);
    postorder.add(newNode);
    return newNode;
  }
  
  public Node makePostorder(Node toCopy) {
    List<Node> children = new ArrayList<Node>();
    Node newNode = null;
    
    if(!toCopy.hasEquivalence()) {
      for (Node oldChild : toCopy.getChildren()) {
        Node newChild = makePostorder(oldChild);
        children.add(newChild);
      }
      String type = toCopy.getType();
      String name = toCopy.getName();
      newNode = new Node(type, name, children, toCopy.getId());
    } else {
      String type = "EQUIV";
      String name = toCopy.getEquivalence().getName();
      newNode = new Node(type, name, children, toCopy.getId());
    }
    
    postorder.add(newNode);
    return newNode;
  }

  public boolean containsId(int id) {
    for (Node n : postorder) if (n.getId() == id) return true;
    return false;
  }
}
