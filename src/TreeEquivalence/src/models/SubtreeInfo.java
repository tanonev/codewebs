package models;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SubtreeInfo {
	public String code;
	public Node root;
	public Subtree parent;
	
	private ArrayList<SubtreeLocation> subtreeLocations = new ArrayList<SubtreeLocation>();
	private ArrayList<Context> contexts = new ArrayList<Context>();
	private HashMap<Context, Integer> outputMap = new HashMap<Context, Integer>();

	public SubtreeInfo(Node root, String code, Subtree parent) {
		this.root = root;
		this.code = code;
		this.parent = parent;
	}

	public void add(Context complement, SubtreeLocation loc, int output) {
		this.subtreeLocations.add(loc);
		this.contexts.add(complement);
		this.outputMap.put(complement, output);
	}
	
	public int getAnalogyScore(SubtreeInfo o) {
		int unoverlap = 0;
		int correctOverlap = 0;
		int incorrectOverlap = 0;
		
		for (Map.Entry<Context, Integer> e : o.outputMap.entrySet()) {
			if (this.outputMap.containsKey(e.getKey())) {
				int contextOutputA = this.outputMap.get(e.getKey());
				int contextOutputB = e.getValue();
				if (contextOutputA == contextOutputB) {
					if(Output.isCorrect(contextOutputA)) {
						correctOverlap++;
					} else {
						incorrectOverlap++;
					}
				} else {
					unoverlap++;
				}
			}
		}

		// Chris invented this combination of details..
		int score = 0;
		score += incorrectOverlap;
		score += correctOverlap * 2;
		score += unoverlap * -6;
		
		return score;
	}

	
	/**
	 * Method: Get Overlap Score
	 * -------------------------
	 * Computes a value which we call "overlap". Overlap is zero if two subtrees
	 * have equivalent complements across different output classes. Otherwise
	 * it is the number of different complements where the output was the same.
	 */
	public int getOverlapScore(SubtreeInfo o) {
		int count = 0;
		for (Map.Entry<Context, Integer> e : o.outputMap.entrySet()) {
			if (this.outputMap.containsKey(e.getKey())) {
				int contextOutputA = this.outputMap.get(e.getKey());
				int contextOutputB = e.getValue();
				if (contextOutputA == contextOutputB) {
					count++;
				} else {
					return 0;
				}
			}
		}
		return count;
	}
	

	public int getOverlapCount(SubtreeInfo o) {
		int count = 0;
		for (Map.Entry<Context, Integer> e : o.outputMap.entrySet()) {
			if (this.outputMap.containsKey(e.getKey())) {
				int contextOutputA = this.outputMap.get(e.getKey());
				int contextOutputB = e.getValue();
				if (contextOutputA == contextOutputB) {
					count++;
				} 
			}
		}
		return count;
	}
	
	public int getCorrectOverlapCount(SubtreeInfo o) {
		int count = 0;
		for (Map.Entry<Context, Integer> e : o.outputMap.entrySet()) {
			if (this.outputMap.containsKey(e.getKey())) {
				int contextOutputA = this.outputMap.get(e.getKey());
				int contextOutputB = e.getValue();
				if(Output.isCorrect(contextOutputB)) {
					if (contextOutputA == contextOutputB) {
						count++;
					} 
				}
			}
		}
		return count;
	}
	
	public int getUnoverlapCount(SubtreeInfo o) {
		int count = 0;
		for (Map.Entry<Context, Integer> e : o.outputMap.entrySet()) {
			if (this.outputMap.containsKey(e.getKey())) {
				int contextOutputA = this.outputMap.get(e.getKey());
				int contextOutputB = e.getValue();
				if (contextOutputA != contextOutputB) {
					count++;
				} 
			}
		}
		return count;
	}

	
	/**
	 * Method: getBugScore
	 * -------------------
	 * Like getOverlapScore except that it returns the probability that
	 * the two subtrees are bug/solution pairs.
	 */
	public int getBugScore() {
		return outputMap.size();
		
	}
	
	public boolean containsComplement(Context c) {
		return outputMap.containsKey(c);
	}
	
	public int getOutput(Context c) {
		return outputMap.get(c);
	}
	
	public List<Context> getComplements() {
		return Collections.unmodifiableList(contexts);
	}
	
	public List<SubtreeLocation> getSubtreeLocations() {
	  return Collections.unmodifiableList(subtreeLocations);
	}
	
	public int getCount() {
		return subtreeLocations.size();
	}

}