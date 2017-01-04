package experiments;

import java.util.*;

import util.FileSystem;
import models.*;

public class BottomUp {
	
	class SubforestPair {
		public Subforest a;
		public Subforest b;
		
		public SubforestPair(Subforest a, Subforest b) {
			this.a = a;
			this.b = b;
		}
		
		@Override
		public boolean equals(Object o) {
			SubforestPair other = (SubforestPair) o;
			if(!this.a.equals(other.a)) {
				return false;
			}
			return this.b.equals(other.b);
		}
		
		@Override
		public int hashCode() {
			// This way order doesn't matter
			return a.hashCode() + b.hashCode();
		}
	}
	
	
	private static final int NUM_TO_LOAD = 1000;
	
	Assignment assn;
	
	public void run() {
		//FileSystem.setProgramPath(FileSystem.getReducedOutDir());
		assn = Assignment.loadFromFile(NUM_TO_LOAD);
		int numSubforests = assn.getUniqueSubforests().size();
		System.out.println("Unique subtrees: " +numSubforests);
		int numComplements = assn.getUniqueContexts().size();
		System.out.println("Unique complements: " + numComplements);
		
		runIteration();

	}
	
	/**
	 * Method: Run Factorization Iteration
	 * -----------------------------------
	 * First, remove "empties" (programs with unsubstantial lines).
	 * Then, chose the best equivalence pair: two asts that we are most 
	 * convinced are the same and factor that equivalence out of all
	 * programs.
	 * @return 
	 */
	protected boolean runIteration(){

		Equivalence pair = getBestEquivalence();
		pair.outputCodeStrings();
		assn.reduce(pair);
		
		return true;
	}
	
	private Equivalence getBestEquivalence() {
		/*Map<SubforestPair, Integer> collisionCount = new HashMap<SubforestPair, Integer>();
		Set<Context> contexts = assn.getUniqueContexts();
		for(Context c : contexts) {
			Set<CodeBlock> blocks = assn.codeBlocksFromContext(c);
			
		}*/
		int mostImportantScore = 100;
		SubforestPair best = null;
		
		List<Subforest> sorted = assn.getSortedSubforests();
		for(Subforest a : sorted) {
			
			System.out.println(a.getCodeString());
			if(a.isEmpty()) continue;
			int aCount = assn.codeBlocksFromSubforest(a).size();
			if(aCount < mostImportantScore) continue;
			
			for(Subforest b : sorted) {
				if(a.equals(b)) continue;
				if(a.isEmpty()) continue;
				int bCount = assn.codeBlocksFromSubforest(b).size();
				if(bCount < mostImportantScore) continue;
	
				if(overlap(a, b)) {
					int score = Math.min(aCount, bCount);
					mostImportantScore = score;
					best = new SubforestPair(a, b);
				}
			}
			
		}
		System.out.println(best.a.getCodeString());
		System.out.println("----");
		System.out.println(best.b.getCodeString());
		return null;
	}

	/**
	 * Check that there are no "mismatches" and that contexts overlap.
	 */
	private boolean overlap(Subforest a, Subforest b) {
		Set<CodeBlock> aBlocks = assn.codeBlocksFromSubforest(a);
		Set<CodeBlock> bBlocks = assn.codeBlocksFromSubforest(b);
		int matches = 0;
		for(CodeBlock aBlock : aBlocks) {
			for(CodeBlock bBlock : bBlocks) {
				Context aContext = aBlock.getContext();
				Context bContext = bBlock.getContext();
				if(aContext.equals(bContext)) {
					Program aProgram = aBlock.getProgram();
					Program bProgram = bBlock.getProgram();
					if(aProgram.getOutput() == bProgram.getOutput()) {
						matches++;
						return true;
					} else {
						return false;
					}
				}
			}
		}
		return matches > 0;
	}

	public static void main(String[] args) {
		new BottomUp().run();
	}
}
