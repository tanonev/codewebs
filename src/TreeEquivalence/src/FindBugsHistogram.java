import java.util.Map;

import models.AST;
import models.ASTID;
import models.Context;
import models.Profile;
import models.Program;
import util.FileSystem;
import util.Helper;


public class FindBugsHistogram implements Runnable {
  private static final int NUM_ASTS = 1000; // 1000; //5000;
  
  private static final ASTID TEST_ID = new ASTID(2944);
  
  private Map<ASTID, Program> programs;
  private Map<Context, Map<Integer, Integer>> contextHistograms;
  
  /**
   * Method: Run 
   * ----------- 
   * The main entry point for the program which is
   * called on a FindBugsHistogram instance.
   */
  public void run() {
    programs = loadStudentPrograms();
    contextHistograms = Helper.getContextHistograms(programs);
    int total = 0;
    for (Map<Integer, Integer> map : contextHistograms.values()) for (int i : map.values()) total += i;
    System.out.println(total);
    System.out.println(contextHistograms.size());


      int correctWithNoBug = 0, correctWithBug = 0, incorrectWithNoBug = 0, incorrectWithBug = 0;
      for (int id = 0; id < 1000; id++) {
        Program toExamine = programs.get(new ASTID(id));
        Helper.removeProgramFromHistograms(toExamine, contextHistograms);
  
        boolean[] mark = new boolean[toExamine.ast.size()];
        int count = 0;
      
        for (int i = 0; i < toExamine.ast.size() - 1; i++) {
          Profile p = new Profile(contextHistograms.get(toExamine.contexts[toExamine.parents[i]][i]), 0);
//          Profile p = new Profile(contextHistograms.get(toExamine.getSubtree(i)), 0);
          if (p.isBugSpike()) {
            count += mark[toExamine.parents[i]] ? 0 : 1;
            mark[toExamine.parents[i]] = true;
//              if (id == 501) {
//                System.out.println(p);
  //              System.out.println(i + " (" + toExamine.ast.getNodeId(i) + ") " + toExamine.ast.getNode(i));
  //              System.out.println(Helper.getCode(toExamine.code, toExamine.map, toExamine.ast.getSubtree(i)));
//              }
          }
        }
        
        for (int i = 0; i < toExamine.ast.size(); i++) {
          if (mark[i]) {
            int cur = i;
            while (toExamine.parents[cur] != -1) {
              cur = toExamine.parents[cur];
              count -= mark[cur] ? 1 : 0;
              mark[cur] = false;
            }
          }
        }
        
//          if (count > 0 && toExamine.output == 0) {
//            System.out.println("Examining program " + id);
//            System.out.println("bugs found: " + count);
//          }
        
        if (count > 0) {
          if (toExamine.output == 0) {
            correctWithBug++;
          } else {
            incorrectWithBug++;
          }
        } else {
          if (toExamine.output == 0) {
            correctWithNoBug++;
          } else {
            incorrectWithNoBug++;
          }
        }
        
        Helper.addProgramToHistograms(toExamine, contextHistograms);
        
        if (id == 999) {
          System.out.println("SUMMARY (" + Profile.THRESHOLD + ")");
          System.out.println("Correct output class, 0 bugs: " + correctWithNoBug);
          System.out.println("Correct output class, 1+ bugs: " + correctWithBug);
          System.out.println("Incorrect output class, 0 bugs: " + incorrectWithNoBug);
          System.out.println("Incorrect output class, 1+ bugs: " + incorrectWithBug);
        }
      }
  }

  /**
   * Method: Load Student Programs
   * -----------------------------
   * Does what it says. If the constant NUM_ASTS is smaller than the 
   * number of student programs we have on file, we will only load a
   * subset of programs.
   */
  private Map<ASTID, Program> loadStudentPrograms() {
    System.out.println("loading student programs...");
    int numAsts = FileSystem.getNumAsts();
    System.out.println("number of asts on file: " + numAsts);
    if (NUM_ASTS < FileSystem.getNumAsts()) {
      System.out.println("loading first " + NUM_ASTS + " asts...");
    } else {
      System.out.println("loading all programs on file...");
    }

    return FileSystem.loadAllPrograms(NUM_ASTS);
  }
  
  /**
   * Method: Main 
   * ------------
   * The entry point into the program. Creates a new FindBugsHistogram instance 
   * and runs it.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("running FindBugsHistogram.java");
    new FindBugsHistogram().run();
    System.out.println("finished.");
  }
}
