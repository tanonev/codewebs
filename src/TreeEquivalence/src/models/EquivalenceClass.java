package models;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import util.FileSystem;

public class EquivalenceClass {

	private static int idCounter = 0;
	
	public Set<Subtree> subtrees;
	public int id;
	
	private String name = null;
	
	public EquivalenceClass(Set<Subtree> subtrees) {
		if (subtrees == null) {
			throw new RuntimeException("bad subtrees");
		}
		this.id = idCounter;
		idCounter++;
		this.subtrees = subtrees;
	}

	public void saveToFile(int i) {
		String fileDir = FileSystem.getEquivalenceOutDir() + i +"/";
		File theDir = new File(fileDir);
		if (!theDir.exists()) {
			theDir.mkdir();  
		}
		String fileName = id + ".txt";
		String path = fileDir + fileName;
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			for (Subtree tree : subtrees) {
				String code = tree.info.code;
				writer.println("------------");
				writer.println(tree.getId() + " " + tree.info.getCount());
				writer.println(code);
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to write equivalence", e);
		} 
	}
	
	public void outputCode() {
		System.out.println("$$$$$$$$$$$$$");
		for (Subtree tree : subtrees) {
			String code = tree.info.code;
			System.out.println(code);
			System.out.println("------------");
		}
		System.out.println("$$$$$$$$$$$$$");
	}
	
	// is the "other" tree a subtree of any tree in the equivalence class?
	public boolean containsTree(Subtree other) {
		for (Subtree b : subtrees) {
			if (other.isSubtreeOf(b))  {
				return true;
			}
		}
		return false;
	}

	// returns true if any subtree of other is contained in any subtree in this.subtrees
	public boolean containsClass(EquivalenceClass other) {
		for (Subtree a : subtrees) {
			for (Subtree b : other.subtrees) {
				if (b.isSubtreeOf(a))  {
					return true;
				}
			}
		}
		return false;
	}
	
	// returns false if this contains any equivalence class in equivalenceClasses
	// i.e. returns false if 
	public boolean isLeafClass(List<EquivalenceClass> equivalenceClasses) {
		for (EquivalenceClass b : equivalenceClasses) {
			if (this == b) continue;
			if (this.containsClass(b)) return false;
		}
		return true;
	}

	public void addSubsumed(List<EquivalenceClass> equivalenceClasses,
			EquivalenceClassGraph graph) {
		graph.addNode(this);
		for (EquivalenceClass b : equivalenceClasses) {
			if (this == b) continue;
			if (this.containsClass(b)) {
				graph.addEdge(this, b);
			}
		}
		
	}
	
	public int getOverlap() {
		checkPair();
		Subtree first = (Subtree) subtrees.toArray()[0];
		Subtree second = (Subtree) subtrees.toArray()[1];
		return first.info.getOverlapCount(second.info);
	}
	
	public int getUnoverlap() {
		checkPair();
		Subtree first = (Subtree) subtrees.toArray()[0];
		Subtree second = (Subtree) subtrees.toArray()[1];
		return first.info.getUnoverlapCount(second.info);
	}
	
	public int getCorrectOverlap() {
		checkPair();
		Subtree first = (Subtree) subtrees.toArray()[0];
		Subtree second = (Subtree) subtrees.toArray()[1];
		return first.info.getCorrectOverlapCount(second.info);
	}
	
	public int getAnalogyScore() {
		checkPair();
		Subtree first = (Subtree) subtrees.toArray()[0];
		Subtree second = (Subtree) subtrees.toArray()[1];
		
		

		return first.info.getAnalogyScore(second.info);
	}

	private void checkPair() {
		if(subtrees.size() != 2) {
			throw new RuntimeException("only supported for pairs");
		}
	}
	
	public boolean containsEmpty() {
	  return subtrees.contains(new Subtree(-1, AST.NULL, null));
	}

	public Set<Subtree> getSubtrees() {
		return subtrees;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object o) {
		EquivalenceClass other = (EquivalenceClass)o;
		if(subtrees.size() != other.subtrees.size()) {
			return false;
		}
		for(Subtree a : subtrees) {
			if(!other.subtrees.contains(a)) {
				return false;
			}
		}
		return true;
		
	}
	
	@Override
	public int hashCode() {
		return subtrees.hashCode();
	}

	public String getCodeString() {
		if(name != null) {
			return "{"+name+"}";
		}
		return "{"+id+"}";
	}

	public void outputStats() {
		checkPair();
		Subtree first = (Subtree) subtrees.toArray()[0];
		Subtree second = (Subtree) subtrees.toArray()[1];
		
		int overlap = first.info.getOverlapCount(second.info);
		int unoverlap = first.info.getUnoverlapCount(second.info);
		int correctOverlap = first.info.getCorrectOverlapCount(second.info);
		int incorrectOverlap = overlap - correctOverlap;
		System.out.println("Correct Overlap: " + correctOverlap);
		System.out.println("Incorrect Overlap: " + incorrectOverlap);
		System.out.println("Unoverlap: " + unoverlap);
		System.out.println("Analogy Score: " + getAnalogyScore());
		int firstCount = first.info.getCount();
		int secondCount = second.info.getCount();
		System.out.println("First Count: " + firstCount);
		System.out.println("Second Count: " + secondCount);
		
	}
}
