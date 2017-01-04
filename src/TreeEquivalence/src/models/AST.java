package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import util.FileSystem;


public class AST {
  public static final AST NULL = new AST(Collections.singletonList(Node.NULL));
  static {
    NULL.setHashCode(1);
  }
  
  private static final boolean ANONYMIZE_IDENTIFIERS = true;
  private static final boolean INCLUDE_EMPTY_TREE = true;
  private static Set<String> keywords = null;
  
  private final List<Node> postorder;
  
  private boolean isHashCached = false;
  private int hashCode;
  
  public void setHashCode(int hashCode) {
    if (this == NULL) return;
    assert(!isHashCached);
    isHashCached = true;
    this.hashCode = hashCode;
  }
  
  public Node getNode(int postOrderIndex){
    return postorder.get(postOrderIndex);
  }
    
  public int getNodeId(int postOrderIndex){
    return postorder.get(postOrderIndex).id;
  }
  
  public  String toString() {
  	String s = "";
  	for (Node node : postorder) {
  		s = s + node + "\n";
  	}
  	return s;
  }
  
  private static int postorder(AST ast, JSONObject node, Set<String> keywords) {
    int size = 1;
    JSONArray children = node.getJSONArray("children");
    for (int i = 0; i < children.length(); i++) {
      size += postorder(ast, children.getJSONObject(i), keywords);
    }
    ast.postorder.add(Node.makeNode(node, size, keywords));
    return size;
  }

  /*public static AST makeAst(JSONObject root) {
    AST ast = new AST(new ArrayList<Node>());
    postorder(ast, root, null);
    return ast;
  }*/
  
  
  public static AST makeAst(JSONObject root) {

	AST ast = new AST(new ArrayList<Node>());
	if (ANONYMIZE_IDENTIFIERS){
		if (keywords == null) {
			keywords = FileSystem.loadKeywords();
		}
	    
	    postorder(ast, root, keywords);
	} else {
		postorder(ast, root, null);
	}
    return ast;
  }
    
  public AST(List<Node> postorder) {
    this.postorder = postorder;
  }
  
  public AST(List<Node> postorder, int hashCode) {
    this(postorder);
    setHashCode(hashCode);
  }
  
  public AST getSubtree(int root) {
    return new AST(postorder.subList(root - postorder.get(root).size + 1, root + 1));
  }
  
  public boolean isSubtreeOf(AST other) {
	  if (other.equals(this)) return true;
	  for (int i = 0; i < other.size(); i++) {
		  AST otherChild = other.getSubtree(i);
		  if (otherChild.equals(this)) return true;
	  }
	  return false;
  }
  
  public Node getRoot() {
    return postorder.get(postorder.size() - 1);
  }
  
  public Collection<Node> getNodes() {
    return Collections.unmodifiableList(postorder);
  }
  
  public String rootType() {
    return getRoot().type;
  }
  
  // subforests of size 0 or 2+
  public List<ASTWithComplement> subforests(int i) {
    List<ASTWithComplement> ans = new ArrayList<ASTWithComplement>();
    if (postorder.get(i).type.equals("STATEMENT_LIST")) {
      ArrayList<Integer> partialSums = new ArrayList<Integer>();
      int partial = 0;
      partialSums.add(partial);
      while (1 + partial < postorder.get(i).size) {
        partial += postorder.get(i-1-partial).size;
        partialSums.add(partial);
      }
      for (int j = 0; j < partialSums.size(); j++) {
        for (int k = j + 2; k < partialSums.size(); k++) {
          int start = i - partialSums.get(k);
          int end = i - partialSums.get(j);
          ans.add(new ASTWithComplement(new AST(postorder.subList(start, end)), getComplement(start, end), start, end));
          if (!ans.get(ans.size() - 1).ast.rootType().equals("STATEMENT")) {
            //System.out.println("PROBLEM");
          }
        }
        
        int loc = i - partialSums.get(j);
        if (INCLUDE_EMPTY_TREE) {
        	ans.add(new ASTWithComplement(AST.NULL, getComplement(loc, loc), loc, loc));
        }
      }
      
    }
    return ans;
  }
  
  public int size() {
    return postorder.size();
  }
  
  public Context getContext(int upperRoot, int lowerRoot) {
    return getContext(upperRoot, lowerRoot - postorder.get(lowerRoot).size + 1, lowerRoot + 1);
  }
  
  public Context getContext(int upperRoot, int subStart, int subEnd) {
    return new Context(postorder.subList(upperRoot - postorder.get(upperRoot).size + 1, subStart), postorder.subList(subEnd, upperRoot + 1), subEnd - subStart);
  }
  
  public Context getComplement(int root) {
    return getComplement(root - postorder.get(root).size + 1 , root + 1);
  }
  
  private Context getComplement(int start, int end) {
    return new Context(postorder.subList(0, start), postorder.subList(end, postorder.size()), end - start);
  }

  @Override
  public int hashCode() {
    if (isHashCached) return hashCode;
    isHashCached = true;
    return hashCode = postorder.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AST other = (AST) obj;
    if (postorder == null) {
      if (other.postorder != null)
        return false;
    } else if (!postorder.equals(other.postorder))
      return false;
    return true;
  }

  public AST getReducedAst(Map<Integer, Integer> changeList) {
    System.out.println("what gets changed:");
    List<Node> newPostorder = new ArrayList<Node>();
    for (Node node : this.postorder) {
      if (changeList.containsKey(node.id)) {
        Integer equivalenceId = changeList.get(node.id);
        if (equivalenceId != null) {
          System.out.println(node.id + " -> " + equivalenceId);
          
          String name = "" + equivalenceId;
          Node newNode = new Node("EQUIVALENCE", name, 1, node.id);
          newPostorder.add(newNode);
        }
      } else {
        newPostorder.add(node);
      }
    }
    return new AST(newPostorder);
  }
}
