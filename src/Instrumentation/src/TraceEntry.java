import java.util.HashMap;
import java.util.Map;


public class TraceEntry {
  Map<String, String> vars = new HashMap<String, String>();
  int line;
  
  public TraceEntry(int line) {this.line = line;}
}
