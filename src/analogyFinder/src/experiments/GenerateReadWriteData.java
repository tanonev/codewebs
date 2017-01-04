package experiments;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Assignment;
import models.Program;
import models.ast.Node;
import util.FileSystem;

public class GenerateReadWriteData {
  private static final int NUM_TO_LOAD = 10000;

  // The assignment object that stores all programs loaded and the index.
  private Assignment assn;
  
  private PrintWriter out;
  
  public void run() throws Exception {
    FileSystem.setAssignment("7_1");
    assn = Assignment.loadFromFile(NUM_TO_LOAD, false);
    String base = "../../data/Instrumentation/7_1/ast_";
    for (Program p : assn.getPrograms()) {
      out = new PrintWriter(new File(base + p.getId() + ".access"));
      Set<Integer> identIds = new HashSet<Integer>();
      Map<Integer, Integer> identLines = new HashMap<Integer, Integer>();
      
      for (Node n : p.getTree().getPostorder()) {
        if (n.getType().equals("IDENT")) identIds.add(n.getId());
      }
      
      int[][] map = p.getMap();
      for (int i = 0; i < map.length; i++) {
        for (int j = 0; j < map[i].length; j++) {
          if (identIds.contains(map[i][j])) {
            identLines.put(map[i][j], i + 1);
          }
        }
      }
      
      Set<Integer> identWrites = new HashSet<Integer>();
      Set<Integer> identPartials = new HashSet<Integer>();
      Set<Integer> identIgnores = new HashSet<Integer>();
      
      for (Node n : p.getTree().getPostorder()) {
        if (n.getType().equals("ASSIGN")) {
          mark(n.getChildren().get(0), identWrites, identPartials);
        } else if (n.getType().equals("MULTI_ASSIGN")) {
          Node child = n.getChildren().get(0);
          if (child.getType().equals("ARGUMENT_LIST")) {
            for (Node c : child.getChildren()) {
              mark(c, identWrites, identPartials);
            }
          } else {
            System.err.println("unexpected left child of MULTI_ASSIGN: " + child.getType());
          }
        } else if (n.getType().equals("SIMPLE_FOR")) {
          mark(n.getChildren().get(0), identWrites, identPartials);
        } else if (n.getType().equals("COMPLEX_FOR")) {
          Node child = n.getChildren().get(0);
          if (child.getType().equals("ARGUMENT_LIST")) {
            for (Node c : child.getChildren()) {
              mark(c, identWrites, identPartials);
            }
          } else {
            System.err.println("unexpected left child of COMPLEX_FOR: " + child.getType());
          }
        } else if (n.getType().equals("USER_FCN")) {
          Node outputs = n.getChildren().get(0);
          for (Node c : outputs.getChildren()) {
            identIgnores.add(c.getChildren().get(0).getId());
          }
          Node inputs = n.getChildren().get(1);
          for (Node c : inputs.getChildren()) {
            identWrites.add(c.getChildren().get(0).getId());
          }
        }
      }
      
      for (Node n : p.getTree().getPostorder()) {
        if (n.getType().equals("IDENT")) {
          if (identWrites.contains(n.getId())) {
            recordAccess(n, true, identLines);
          } else if (identPartials.contains(n.getId())) {
            recordAccess(n, true, identLines);
            recordAccess(n, false, identLines);
          } else if (!identIgnores.contains(n.getId())) {
            recordAccess(n, false, identLines);
          }
        }
      }
      
      out.close();
    }
  }
  
  private void mark(Node n, Set<Integer> identWrites, Set<Integer> identPartials) {
    if (n.getType().equals("IDENT")) {
      identWrites.add(n.getId());
    } else if (n.getType().equals("INDEX_EXP")) {
      Node child = n.getChildren().get(0);
      if (child.getType().equals("IDENT")) {
        identPartials.add(child.getId());
      } else {
        System.err.println("INDEX_EXP without IDENT left child: " + child.getType());
      }
    } else {
      System.err.println("unexpected l-value node type: " + n.getType());
    }
  }
  
  private void recordAccess(Node n, boolean write, Map<Integer, Integer> identLines) {
    out.println(identLines.get(n.getId()) + " " + n.getTrueName() + (write ? " W" : " R"));
  }

  public static void main(String[] args) throws Exception {
    new GenerateReadWriteData().run();
  }
}
