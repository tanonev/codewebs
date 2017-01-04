package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class BaseLine {
  private static String assnStr;
  
  private static final int NUM_TO_LOAD = 5000;
  
  private static final int K = 7;
  
  private static final int[][] reserve = new int[NUM_TO_LOAD][NUM_TO_LOAD];
  
  private static final String baseDir = "/home/andy/codeweb/data/";
  
  private void update(int i, int dist, TreeSet<Integer> neighbors) {
    if (dist == -1) return;
    neighbors.add(i + (dist << 20));
    if (neighbors.size() > K) neighbors.pollLast();
  }
  
  public void run() throws Exception {
    Set<Integer> corrects = new HashSet<Integer>();
    Scanner s = new Scanner(new File(baseDir + "incorrects/corrects_" + assnStr + ".txt"));
    while (s.hasNextInt()) corrects.add(s.nextInt());
    s.close();
    
    BufferedReader matrix = new BufferedReader(new FileReader(baseDir + "matching/dist_" + assnStr + ".txt"));
    int correctWithNoBug = 0, correctWithBug = 0, incorrectWithNoBug = 0, incorrectWithBug = 0;
    for (int i = 0; i < NUM_TO_LOAD; i++) {
      String line = matrix.readLine();
      if (line == null || line.length() <= 1) {
        break;
      }
      line = line.trim();
      StringTokenizer st = new StringTokenizer(line);
      TreeSet<Integer> neighbors = new TreeSet<Integer>();
      for (int j = 0; j < i; j++) {
        update(j, reserve[j][i], neighbors);
        st.nextToken();
      }
      st.nextToken();
      for (int j = i + 1; j < NUM_TO_LOAD; j++) {
        if (!st.hasMoreTokens()) break;
        reserve[i][j] = Integer.parseInt(st.nextToken());
        update(j, reserve[i][j], neighbors);
      }
      
      int numCorrect = 0;
      for (int num : neighbors) {
        if (corrects.contains(num & ((1 << 20) - 1))) numCorrect++;
      }
      
      if (neighbors.size() == K) {
        if (2 * numCorrect < K) {
          if (corrects.contains(i)) {
            correctWithBug++;
          } else {
            incorrectWithBug++;
          }
        } else {
          if (corrects.contains(i)) {
            correctWithNoBug++;
          } else {
            incorrectWithNoBug++;
          }
        }
      }
      
      if (i % 100 == 99) {
        System.err.println("SUMMARY (" + (i + 1) + ")");
        System.err.println("Correct output class, 0 bugs: " + correctWithNoBug);
        System.err.println("Correct output class, 1+ bugs: " + correctWithBug);
        System.err.println("Incorrect output class, 0 bugs: " + incorrectWithNoBug);
        System.err.println("Incorrect output class, 1+ bugs: " + incorrectWithBug);
      }
    }
    System.err.println("FINAL SUMMARY");
    System.err.println("Correct output class, 0 bugs: " + correctWithNoBug);
    System.err.println("Correct output class, 1+ bugs: " + correctWithBug);
    System.err.println("Incorrect output class, 0 bugs: " + incorrectWithNoBug);
    System.err.println("Incorrect output class, 1+ bugs: " + incorrectWithBug);
    matrix.close();
  }
  
  public static void main(String[] args) throws Exception {
    assnStr = args[0];
    new BaseLine().run();
  }
}
