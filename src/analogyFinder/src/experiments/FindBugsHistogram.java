package experiments;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import util.FileSystem;

import models.Assignment;
import models.Context;
import models.Profile;
import models.Program;
import models.ast.Node;

public class FindBugsHistogram {
	private static final int NUM_TO_LOAD = 5000;

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;
	private Map<Context, Map<Integer, Integer>> contextHistograms;
	private static String assnStr;

	public void run() {
	  FileSystem.setAssignment(assnStr);
//    FileSystem.setProgramPath(FileSystem.getReducedOutDir());
    assn = Assignment.loadFromFile(NUM_TO_LOAD, false);
    contextHistograms = new HashMap<Context, Map<Integer, Integer>>();
    for (Program p : assn.getPrograms()) addProgramToHistograms(p);
    System.gc();
    
    long start = System.nanoTime();
    int correctWithNoBug = 0, correctWithBug = 0, incorrectWithNoBug = 0, incorrectWithBug = 0;
    for (int id = 0; id < NUM_TO_LOAD; id++) {
      Program toExamine = assn.getProgram(id);
      if (toExamine != null) {
      removeProgramFromHistograms(toExamine);
      
      boolean[] mark = new boolean[toExamine.getRoot().getSize()];
      int count = 0;
    
      for (Context c : toExamine.getLocalContexts()) {
        Profile p = new Profile(contextHistograms.get(c), 0);
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
      }
      
      if (id % 100 == 99) {
        System.err.println("SUMMARY (" + (id + 1) + ")");
        System.err.println("Correct output class, 0 bugs: " + correctWithNoBug);
        System.err.println("Correct output class, 1+ bugs: " + correctWithBug);
        System.err.println("Incorrect output class, 0 bugs: " + incorrectWithNoBug);
        System.err.println("Incorrect output class, 1+ bugs: " + incorrectWithBug);
      }
    }
    long end = System.nanoTime();
    System.out.println("Elapsed query time: " + (end - start) / 1e9);
	}

  public void addProgramToHistograms(Program program) {
    for (Context c : program.getLocalContexts()) {
      if (!contextHistograms.containsKey(c)) contextHistograms.put(c, new TreeMap<Integer, Integer>());
      Map<Integer, Integer> histogram = contextHistograms.get(c);
      if (!histogram.containsKey(program.getOutput())) histogram.put(program.getOutput(), 0);
      histogram.put(program.getOutput(), histogram.get(program.getOutput()) + program.getStudents());
    }
  }
  
  // MAKE SURE THAT THE PROGRAM WAS PREVIOUSLY ADDED FIRST!
  public void removeProgramFromHistograms(Program program) {
    for (Context c : program.getLocalContexts()) {
      Map<Integer, Integer> histogram = contextHistograms.get(c);
      histogram.put(program.getOutput(), histogram.get(program.getOutput()) - program.getStudents());
    }
  }
  
	public static void main(String[] args) {
	  assnStr = args[0];
		new FindBugsHistogram().run();
	}
}
