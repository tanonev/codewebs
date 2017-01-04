package experiments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import models.Assignment;
import models.CodeBlock;
import models.Subforest;
import util.FileSystem;

public class Zipf {
	private static final int NUM_TO_LOAD = 10000;

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;

	static class SubtreeEntry implements Comparable<SubtreeEntry> {
	  Subforest subtree;
	  int count;
	  public SubtreeEntry(Subforest subtree, Collection<CodeBlock> codeBlocks) {
	    this.subtree = subtree;
	    this.count = 0;
	    for (CodeBlock c : codeBlocks) count += c.getProgram().getStudents();
	  }
	  
	  public int compareTo(SubtreeEntry o) {
	    return o.count - count;
	  }
	}
	
	public void run() {
	  FileSystem.setProgramPath(FileSystem.getReducedOutDir());
    assn = Assignment.loadFromFile(NUM_TO_LOAD);
    ArrayList<SubtreeEntry> allSubtrees = new ArrayList<SubtreeEntry>();
    for (Subforest s : assn.getUniqueSubforests()) allSubtrees.add(new SubtreeEntry(s, assn.codeBlocksFromSubforest(s)));
    Collections.sort(allSubtrees);
    
    for (int i = 0; i < 50000; i++) {
      System.err.println(i + " " + allSubtrees.get(i).count);
    }
	}

	public static void main(String[] args) {
		new Zipf().run();
	}
}
