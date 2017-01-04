import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;


public class Inject {
//  public static final String VARS = "vars";
//  public static final String IDX = "idx";
//  public static final String FID = "fid";
//  public static final String TMP = "tmp";
  public static final String VARS = "vars_salt32987602359124";
  public static final String IDX = "idx_salt32987602359124";
  public static final String FID = "fid_salt32987602359124";
  public static final String TMP = "tmp_salt32987602359124";
  
  public Inject(String fin, String fout, String output) throws Exception {
    ArrayList<String> code = new ArrayList<String>();
    Scanner in = new Scanner(new File(fin));
    while (in.hasNextLine()) {
      code.add(in.nextLine());
    }
    in.close();
    PrintWriter out = new PrintWriter(new FileWriter(fout));
    for (int i = 0; i < code.size() - 4; i++) {
      out.println(code.get(i));
      if (i == 0) {
        out.println(FID + " = fopen('" + output + "', 'w');");
        out.println(VARS + " = 0;");
        out.println(IDX + " = 0;");
        out.println(TMP + " = 0;");
        continue;
      }
      out.println(VARS + " = who;");
      out.println("fprintf(" + FID + ", '" + (i + 1) + " %d\\n', length(" + VARS + ") - 4);");
      out.println("for " + IDX + " = 1:length(" + VARS + ")");
      out.println("  if (~strcmp(" + VARS + "{" + IDX + "}, '" + FID + "') && ~strcmp(" + VARS + "{" + IDX + "}, '" + VARS + "') && ~strcmp(" + VARS + "{" + IDX + "}, '" + IDX + "') && ~strcmp(" + VARS + "{" + IDX + "}, '" + TMP + "'))");
      out.println("    eval(['" + TMP + " = disp(' " + VARS + "{" + IDX + "} ');']);");
      out.println("    fprintf(" + FID + ", '%s %s\\n', " + VARS + "{" + IDX + "}, md5sum(" + TMP + ", true));");
      out.println("  end");
      out.println("end");
    }
    
    out.println("fclose(" + FID + ");");
    out.println(code.get(code.size() - 4));
    out.close();
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("usage: Inject original.m injected.m output.txt");
      System.exit(1);
    }
    new Inject(args[0], args[1], args[2]);
  }
}
