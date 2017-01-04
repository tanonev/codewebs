package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import util.Helper;

import models.AST;
import models.ASTID;
import models.EquivalenceClass;
import models.Program;
import models.Subtree;

/**
 * Class: Manual Factorization
 * ---------------------------
 * Factorize a set of student programs, but have a human make the final
 * decision as to what code analogies are correct.
 */
public class ManualFactorizer extends Factorizer {
	
	private Set<EquivalenceClass> ignoreList;

	public ManualFactorizer(Map<ASTID, Program> programs) {
		super(programs);
		ignoreList = new HashSet<EquivalenceClass>();
	}

	@Override
	protected void runFactorizationIteration(int factorIteration){
		

		List<Subtree> subtrees = getSubtreesFromPrograms(factorIteration);
		EquivalenceClass pair = getBestEquivalence(subtrees);
		
		String pairName = userValidates(pair);
		if(pairName == null) {
			addToIgnoreList(pair);
		} else {
			pair.setName(pairName);
			List<EquivalenceClass> equivalenceClasses = Collections.singletonList(pair);
			///saveEquivalenceClasses(equivalenceClasses, factorIteration);
			factorPrograms(equivalenceClasses);
			Helper.saveReducedPrograms(programs, astCounts, factorIteration);
		}
	}
	
	@Override
	/**
	 * Method: Get Best Equivalence
	 * --------------------------------
	 * Chooses the best equivalence pair of subtrees. The best pair is 
	 * determined to be the two subtrees that have maximal overlapScore (they 
	 * preserve program output in many different contexts and never change 
	 * output).
	 */
	protected EquivalenceClass getBestEquivalence(List<Subtree> subtrees) {
		System.out.println("\n\nLooking for a new equivalence class...");
		EquivalenceSelector selector = new EquivalenceSelector(subtrees);
		EquivalenceClass theChosenOne = selector.chose(ignoreList);
		
		if(theChosenOne == null) {
			throw new RuntimeException("no pair found.");
		}
		
		return theChosenOne;
	}

	private void addToIgnoreList(EquivalenceClass pair) {
		System.out.println("add to ignore list");
		ignoreList.add(pair);
		
	}

	/**
	 * Method: User Validates
	 * ----------------------
	 * Has the user decide if a pair is correct. This method returns null
	 * if the user says the pair is not valid and a name for the pair if the
	 * user decides it is valid.
	 */
	private String userValidates(EquivalenceClass pair) {
		System.out.println("Potential match found:");
		Scanner in = new Scanner(System.in);
		pair.outputCode();
		pair.outputStats();
		System.out.print("Enter a name (or \"false\" to ignore): ");
		String s = in.nextLine();
		if(s.equals("false")) return null;
		System.out.println("Pair name: "+s);
		return s;
	}
	

}
