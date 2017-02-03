package models.ast;

import java.util.ArrayList;
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

  protected void findRoots() {
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

  public Forest reduce(Equivalence eq) {
    for (int i = 0; i < postorder.size(); i++) {
        Forest subtree = new Forest();
        subtree.makePostorder(postorder.get(i));
        if (eq.containsSubforest(new Subforest(null, subtree))) {
            postorder.get(i).markEquivalence(eq);
        }
    }
    Forest reduced = new Forest();
    for (Node root : roots) {
        reduced.makePostorder(root);
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
  
  protected Node makePostorder(Node toCopy) {
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
