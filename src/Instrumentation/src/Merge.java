import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class Merge {
  static class AccessEntry {
    Set<String> reads = new HashSet<String>();
    Set<String> writes = new HashSet<String>();
  }
  
  List<TraceEntry> trace = new ArrayList<TraceEntry>();
  List<AccessEntry> access = new ArrayList<AccessEntry>();
  
  public Merge(String traceFile, String accessFile, String mergeFile) throws Exception {
    Scanner traceIn = new Scanner(new File(traceFile));
    while (traceIn.hasNext()) {
      TraceEntry entry = new TraceEntry(traceIn.nextInt());
      int numVars = traceIn.nextInt();
      for (int i = 0; i < numVars; i++) {
        entry.vars.put(traceIn.next(), traceIn.next());
      }
      trace.add(entry);
    }
    traceIn.close();
    
    while (trace.get(trace.size() - 1).line >= access.size()) access.add(new AccessEntry());
    
    Scanner accessIn = new Scanner(new File(accessFile));
    while (accessIn.hasNext()) {
      int line = accessIn.nextInt();
      while (line >= access.size()) access.add(new AccessEntry());
      String name = accessIn.next();
      String type = accessIn.next();
      if (type.equals("R")) access.get(line).reads.add(name);
      else access.get(line).writes.add(name);
    }
    accessIn.close();
    
    Set<String> relevant = new HashSet<String>();
    relevant.add("idx");
    for (int i = trace.size() - 1; i >= 0; i--) {
      int line = trace.get(i).line;
      trace.get(i).vars.keySet().retainAll(relevant);
      Set<String> next = new HashSet<String>();
      for (String var : relevant) {
        if (access.get(line).writes.contains(var)) {
          next.addAll(access.get(line).reads);
        } else {
          next.add(var);
        }
      }
      relevant = next;
    }
    
    PrintWriter mergeOut = new PrintWriter(new FileWriter(mergeFile));
    for (TraceEntry e : trace) {
      mergeOut.println(e.line + " " + e.vars.size());
      for (Map.Entry<String, String> pair : e.vars.entrySet()) {
        mergeOut.println(pair.getKey() + " " + pair.getValue());
      }
    }
    mergeOut.close();
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("usage: Merge ast.trace ast.access ast.merge");
      System.exit(1);
    }
    try {
      new Merge(args[0], args[1], args[2]);
    } catch (Exception e) {
      System.err.println(args[0]);
      throw e;
    }
  }
}
