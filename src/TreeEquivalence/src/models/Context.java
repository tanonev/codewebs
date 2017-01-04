package models;

import java.util.*;

public class Context {
  private List<Node> leftPostorder;
  private List<Node> rightPostorder;
  private int size;
  
  private boolean isHashCached = false;
  private int hashCode;
  
//	public Set<Subtree> subtrees = new HashSet<Subtree>();
	//public HashMap<Subtree, Integer> outputMap = new HashMap<Subtree, Integer>();
	
	public Context(List<Node> leftPostorder, List<Node> rightPostorder, int size) {
	  this.leftPostorder = leftPostorder;
	  this.rightPostorder = rightPostorder;
	  this.size = size;
	}
	
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    if (hashCode() != obj.hashCode())
      return false;
    Context other = (Context) obj;
    if (!leftPostorder.equals(other.leftPostorder))
      {System.out.println("MISMATCH");return false;}
    if (rightPostorder.size() != other.rightPostorder.size())
    {System.out.println("MISMATCH");return false;}
    for (int index = 0; index < rightPostorder.size(); index++) {
      int oldThisSize = rightPostorder.get(index).size;
      int oldOtherSize = other.rightPostorder.get(index).size;
      if (index - oldThisSize < -1) rightPostorder.get(index).size -= size;
      if (index - oldOtherSize < -1) other.rightPostorder.get(index).size -= other.size;
      boolean result = rightPostorder.get(index).equals(other.rightPostorder.get(index));
      rightPostorder.get(index).size = oldThisSize;
      other.rightPostorder.get(index).size = oldOtherSize;
      if (!result) {System.out.println("MISMATCH");return false;}
    }
    return true;
  }

  public void setHashCode(int hashCode) {
    assert(!isHashCached);
    isHashCached = true;
    this.hashCode = hashCode;
  }
  
  @Override
  public int hashCode() {
    if (isHashCached) return hashCode;
    isHashCached = true;
    hashCode = leftPostorder.hashCode();
    for (int i = 0; i < rightPostorder.size(); i++) hashCode *= 31;
    return hashCode = 31 * hashCode + rightPostorder.hashCode();
  }
  
  public String toString() {
    return size + " " + leftPostorder + " " + rightPostorder;
  }
	
//	public void addSubtree(Subtree tree, int output) {
//		subtrees.add(tree);
//		//outputMap.put(tree, output);
//	}
//	
//	public Set<Subtree> getSubtrees() {
//		return subtrees;
//		//return (Set<Subtree>) outputMap.keySet();
//	}
//	
//	public int getOutput(Subtree tree) {
//		return tree.info.outputMap.get(this.hashCode);
//	}
}