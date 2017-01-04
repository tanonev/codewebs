import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class Experiment {
  static List<TraceEntry> trace1 = new ArrayList<TraceEntry>();
  static List<TraceEntry> trace2 = new ArrayList<TraceEntry>();
  
  static Map<Set<String>, Map<Set<String>, TraceEntry>> map1 = new HashMap<Set<String>, Map<Set<String>, TraceEntry>>();
  static Map<Set<String>, Map<Set<String>, TraceEntry>> map2 = new HashMap<Set<String>, Map<Set<String>, TraceEntry>>();
  
  static Map<TraceEntry, TraceEntry> align = new HashMap<TraceEntry, TraceEntry>();
  
  public static void main(String[] args) throws Exception {
    Scanner traceIn = new Scanner(new File("/home/andy/codeweb/data/Instrumentation/7_1/ast_10.merge"));
    while (traceIn.hasNext()) {
      TraceEntry entry = new TraceEntry(traceIn.nextInt());
      int numVars = traceIn.nextInt();
      for (int i = 0; i < numVars; i++) {
        entry.vars.put(traceIn.next(), traceIn.next());
      }
      trace1.add(entry);
    }
    traceIn.close();
    
    traceIn = new Scanner(new File("/home/andy/codeweb/data/Instrumentation/7_1/ast_4.merge"));
    while (traceIn.hasNext()) {
      TraceEntry entry = new TraceEntry(traceIn.nextInt());
      int numVars = traceIn.nextInt();
      for (int i = 0; i < numVars; i++) {
        entry.vars.put(traceIn.next(), traceIn.next());
      }
      trace2.add(entry);
    }
    traceIn.close();
    
    for (TraceEntry e : trace1) {
      Set<String> values = new HashSet<String>();
      values.addAll(e.vars.values());
      if (!map1.containsKey(values)) map1.put(values, new HashMap<Set<String>, TraceEntry>());
      map1.get(values).put(e.vars.keySet(), e);
    }
    
    for (TraceEntry e : trace2) {
      Set<String> values = new HashSet<String>();
      values.addAll(e.vars.values());
      if (!map2.containsKey(values)) map2.put(values, new HashMap<Set<String>, TraceEntry>());
      map2.get(values).put(e.vars.keySet(), e);
    }
    for (Set<String> values : map2.keySet()) {
      if (map1.containsKey(values)) {
        align.put(map1.get(values).values().iterator().next(), map2.get(values).values().iterator().next());
      }
    }
    
    Map<String, List<String>> var1 = new HashMap<String, List<String>>();
    Map<String, List<String>> var2 = new HashMap<String, List<String>>();
    
    int idx = 0;
    for (Map.Entry<TraceEntry, TraceEntry> e : align.entrySet()) {
      TraceEntry e1 = e.getKey();
      for (Map.Entry<String, String> entry : e1.vars.entrySet()) {
        if (!var1.containsKey(entry.getKey())) {
          var1.put(entry.getKey(), new ArrayList<String>());
          for (int i = 0; i < idx; i++) var1.get(entry.getKey()).add("");
        }
        var1.get(entry.getKey()).add(entry.getValue());
      }
      for (List<String> l : var1.values()) if (l.size() == idx) l.add("");
      TraceEntry e2 = e.getValue();
      for (Map.Entry<String, String> entry : e2.vars.entrySet()) {
        if (!var2.containsKey(entry.getKey())) {
          var2.put(entry.getKey(), new ArrayList<String>());
          for (int i = 0; i < idx; i++) var2.get(entry.getKey()).add("");
        }
        var2.get(entry.getKey()).add(entry.getValue());
      }
      for (List<String> l : var2.values()) if (l.size() == idx) l.add("");
      idx++;
      System.out.println(e1.line + " " + e2.line + " " + e1.vars.keySet());
    }
    
    System.out.println(var1);
    System.out.println(var1.size());
    System.out.println(var2);
    System.out.println(var2.size());
    
    Map<List<String>, String> invert = new HashMap<List<String>, String>();
    for (Map.Entry<String, List<String>> e : var1.entrySet()) invert.put(e.getValue(), e.getKey());
    for (Map.Entry<String, List<String>> e : var2.entrySet()) {
      if (invert.containsKey(e.getValue())) System.out.println(invert.get(e.getValue()) + " " + e.getKey());
    }
  }
}
