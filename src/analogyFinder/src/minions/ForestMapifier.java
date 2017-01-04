package minions;

import java.util.*;

import models.Program;
import models.ast.Forest;

public class ForestMapifier extends ForestStringifier{

	public static int[][] mapify(Program program, Forest forest) {
		return new ForestMapifier().run(program, forest);
	}
	
	private List<List<Integer>> answer;
	private List<Integer> currLine;
	
	private int[][] run(Program program, Forest forest) {
		getAnswer(program, forest);
		return mapFromAnswer();
	}
	
	protected void init() {
		answer = new ArrayList<List<Integer>>();
	}
	
	protected void equivAdded(int nodeId, String toAdd) {
		for(int i = 0; i < toAdd.length(); i++) {
			currLine.add(nodeId);
		}
	}

	protected void oldCharAdded(int nodeId) {
		currLine.add(nodeId);
	}

	protected void newLine() {
		currLine = new ArrayList<Integer>();
	}

	protected void whitespaceAdded(int nodeId) {
		currLine.add(nodeId);
	}

	protected void setNullAnswer() {
		answer = null;
	}
	
	protected void addLineToAns(String line, int lineNum) {
		if(!currLine.isEmpty()) {
			answer.add(currLine);
		}
	}
	
	//--------- Private ------------------//
	
	private int[][] mapFromAnswer() {
		int[][] map = new int[answer.size()][];
		for(int i = 0; i < answer.size(); i++) {
			List<Integer> intList = answer.get(i);
			map[i] = new int[intList.size()];
			for(int j = 0; j < intList.size(); j++) {
				map[i][j] = intList.get(j);
			}
		}
		return map;
	}
	
}
