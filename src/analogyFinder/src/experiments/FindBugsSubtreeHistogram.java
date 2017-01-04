package experiments;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import models.Assignment;
import models.CodeBlock;
import models.Context;
import models.Profile;
import models.Program;
import models.Subforest;
import models.ast.Node;
import util.FileSystem;

public class FindBugsSubtreeHistogram {
	private static final int NUM_TO_LOAD = 5000;

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;
	private Map<Subforest, Map<Integer, Integer>> subtreeHistograms;

	public void run() {
    FileSystem.setProgramPath(FileSystem.getReducedOutDir());
    assn = Assignment.loadFromFile(NUM_TO_LOAD);
    subtreeHistograms = new HashMap<Subforest, Map<Integer, Integer>>();
    for (Program p : assn.getPrograms()) addProgramToHistograms(p);
    System.gc();
    
    long start = System.nanoTime();
    int correctWithNoBug = 0, correctWithBug = 0, incorrectWithNoBug = 0, incorrectWithBug = 0;
    for (int id = 0; id < 2000; id++) {
      Program toExamine = assn.getProgram(id);
      removeProgramFromHistograms(toExamine);
      
      boolean[] mark = new boolean[toExamine.getRoot().getSize()];
      int count = 0;
    
      for (Context c : toExamine.getLocalContexts()) {
        Profile p = new Profile(subtreeHistograms.get(c), 0);
        if (p.isBugSpike()) {
          count += mark[c.getRoot().getPostorderIndex()] ? 0 : 1;
          mark[c.getRoot().getPostorderIndex()] = true;
//            if (id == 501) {
//              System.out.println(p);
//              System.out.println(i + " (" + toExamine.ast.getNodeId(i) + ") " + toExamine.ast.getNode(i));
//              System.out.println(Helper.getCode(toExamine.code, toExamine.map, toExamine.ast.getSubtree(i)));
//            }
        }
      }
      
      for (int i = 0; i < toExamine.getRoot().getSize(); i++) {
        if (mark[i]) {
          Node cur = toExamine.getTree().getNode(i);
          while (cur.getParent() != null) {
            cur = cur.getParent();
            count -= mark[cur.getPostorderIndex()] ? 1 : 0;
            mark[cur.getPostorderIndex()] = false;
          }
        }
      }
      
//        if (count > 0 && toExamine.getOutput() == 0) {
//          System.out.println("Examining program " + id);
//          System.out.println("bugs found: " + count);
//        }
      
      if (count > 0) {
        if (toExamine.getOutput() == 0) {
          correctWithBug++;
        } else {
          incorrectWithBug++;
        }
      } else {
        if (toExamine.getOutput() == 0) {
          correctWithNoBug++;
        } else {
          incorrectWithNoBug++;
        }
      }
      
      addProgramToHistograms(toExamine);
      
      if (id % 100 == 99) {
        System.out.println("SUMMARY (" + (id + 1) + ")");
        System.out.println("Correct output class, 0 bugs: " + correctWithNoBug);
        System.out.println("Correct output class, 1+ bugs: " + correctWithBug);
        System.out.println("Incorrect output class, 0 bugs: " + incorrectWithNoBug);
        System.out.println("Incorrect output class, 1+ bugs: " + incorrectWithBug);
      }
    }
    long end = System.nanoTime();
    System.out.println("Elapsed query time: " + (end - start) / 1e9);
	}

  public void addProgramToHistograms(Program program) {
    for (CodeBlock c : program.getCodeBlocks()) {
      if (!subtreeHistograms.containsKey(c.getSubforest())) subtreeHistograms.put(c.getSubforest(), new TreeMap<Integer, Integer>());
      Map<Integer, Integer> histogram = subtreeHistograms.get(c.getSubforest());
      if (!histogram.containsKey(program.getOutput())) histogram.put(program.getOutput(), 0);
      histogram.put(program.getOutput(), histogram.get(program.getOutput()) + program.getStudents());
    }
  }
  
  // MAKE SURE THAT THE PROGRAM WAS PREVIOUSLY ADDED FIRST!
  public void removeProgramFromHistograms(Program program) {
    for (CodeBlock c : program.getCodeBlocks()) {
      Map<Integer, Integer> histogram = subtreeHistograms.get(c.getSubforest());
      histogram.put(program.getOutput(), histogram.get(program.getOutput()) - program.getStudents());
    }
  }
  
	public static void main(String[] args) {
		new FindBugsSubtreeHistogram().run();
	}
}
