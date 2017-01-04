package minions;

import java.util.HashMap;
import java.util.List;

import models.Program;
import models.ast.Forest;
import models.ast.Node;

/**
 * Class ForestStringifier
 * The logic behind taking a forest (from a program) and returning its
 * codeString. The program is necessary because that is the object which
 * stores the node "map" and the original code.
 */
public class ForestStringifier {
	
	public static String stringify(Program program, Forest forest) {
		return new ForestStringifier().run(program, forest);
	}
	
	public static int getLineNumber(Program program, Forest forest) {
	  return new ForestStringifier().run2(program, forest);
	}
	
	private String answer;
	private String line;
	private int lineNum;
	
	protected void init() {
		answer = "";
		lineNum = -1;
	}

	private String run(Program program, Forest forest) {
		getAnswer(program, forest);
		return answer.toString();
	}
	
  private int run2(Program program, Forest forest) {
    getAnswer(program, forest);
    return lineNum;
  }
  
	protected void getAnswer(Program program, Forest forest) {
		List<String> originalCode = program.getCodeList();
		int[][] map = program.getMap();

		if (forest == Forest.NULL) {
			setNullAnswer();
			return;
		}

		HashMap<Integer, Node> ids = new HashMap<Integer, Node>();
		for (Node n : forest.getPostorder()) {
			ids.put(n.getId(), n);
		}

		init();

		// Keep track of the equivalence that you are printing out.
		String currEqName = null;

		// Loop over the codeMap
		for (int i = 0; i < map.length; i++) {
			line = "";
			newLine();
			for (int j = 0; j < map[i].length; j++) {
				int nodeId = map[i][j];
				
				// Make sure that its a node that you want to print.
				if (!ids.containsKey(nodeId)) {
					continue;
				}
				Node curr = ids.get(nodeId);
				
				// Not usre why this is happening.
				if(originalCode.size() <= i) {
					continue;
				}
				if(originalCode.get(i).length() <= j) {
					continue;
				}
				
				// Preserve whitespace
				if(originalCode.get(i).charAt(j) == ' ') {
					int length = line.length();
					boolean hasStarted = line.trim().length() > 0;
					if(!hasStarted || line.charAt(length - 1) != ' '){
						line += " ";
						whitespaceAdded(nodeId);
					}
				} else {
					if(curr.getType().equals("EQUIV")) {
						if(currEqName != curr.getName()) {
							String toAdd = "{" + curr.getName() + "}";
							line += toAdd;
							currEqName = curr.getName();
							equivAdded(curr.getId(), toAdd);
						}
					}else {
						line += originalCode.get(i).charAt(j);
						oldCharAdded(nodeId);
						currEqName = null;
					}
				}
			} 
			addLineToAns(line, i + 1);
		}
	}

	protected void equivAdded(int nodeId, String toAdd) {}

	protected void oldCharAdded(int nodeId) {}

	protected void newLine() {}

	protected void whitespaceAdded(int nodeId) {}

	protected void setNullAnswer() {
		answer = "{empty}";
	}

	protected void addLineToAns(String line, int lineNum) {
		if (line.trim().length() > 0) {
			// Append a carriage return to the previous line unless we
			// are adding the first line.
			if(!answer.isEmpty()) {
				answer += "\n";
			}
			
			// This adds the line without a carriage return
			answer += line;
			
			// Save the first line number
			if (this.lineNum == -1) this.lineNum = lineNum;
		}
	}
	
	
	
}
