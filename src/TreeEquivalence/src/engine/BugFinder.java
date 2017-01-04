package engine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.AST;
import models.ASTID;
import models.Bug;
import models.EquivalenceClass;
import models.Program;
import models.Subtree;

/**
 * Class: Bug Finder
 * -----------------
 * Takes in a set of programs and finds common bugs! Doesn't quite work and
 * is largely untested.
 */
public class BugFinder extends Factorizer {

	public BugFinder(Map<ASTID, Program> programs) {
		super(programs);
	}

	public void findBugs() {
		System.out.println("finding bugs...");
		
		List<Subtree> subtrees = getSubtreesFromPrograms();
		Bug aBug = findBug(subtrees);
		
	}

	private Bug findBug(List<Subtree> subtrees) {
		int bestSoFar = 0;
		Subtree buggySubtree = null;
		/*for (int idx1 = 0; idx1 < subtrees.size(); idx1++) {
			Subtree first = subtrees.get(idx1);
			if(first.info.isBug()) {
				
				int bugScore = first.info.getBugScore();
				if (bugScore <= 1) {
					continue;
				}
				
				int bestSolnScore = 0;
				Subtree bestSoln = null;
				for (int idx2 = idx1 + 1; idx2 < subtrees.size(); idx2++) {
					Subtree second = subtrees.get(idx2);
					

					int unoverlapScore = first.info.getUnoverlapScore(second.info);
					
					if (unoverlapScore > bestSolnScore) {
						bestSoln = second;
						bestSolnScore = unoverlapScore;
					}
				}
				if(bestSoln == null) continue;
				
				System.out.println("\n\n");
				System.out.println("Bug:");
				System.out.println(first.info.code);
				//System.out.println(first.info.isBug());
				System.out.println("Soln:");
				System.out.println(bestSoln.info.code);
				System.out.println("Bug score: " + bugScore);
				System.out.println("Soln score: " + bestSolnScore);
				buggySubtree = first;
				
				
			}
		}*/
		throw new RuntimeException("Not done");
	}

}
