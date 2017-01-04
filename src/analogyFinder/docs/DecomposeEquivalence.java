package experiments;

import java.util.Map;
import java.util.Scanner;

import models.Assignment;
import models.CodeBlock;
import models.Equivalence;
import models.Program;

public class DecomposeEquivalence {

	private static final int NUM_TO_LOAD = 29000;
	
	Assignment assn;
	
	public void run() {
		assn = Assignment.loadFromFile(NUM_TO_LOAD);
		int numSubforests = assn.getUniqueSubforests().size();
		System.out.println("Unique subtrees: " +numSubforests);
		int numComplements = assn.getUniqueContexts().size();
		System.out.println("Unique complements: " + numComplements);

		//Map<Integer, Program> programs = assn.getProgramIdMap();
		//Program first = programs.get(0);
		//CodeBlock decisionPoint = selectCodeBlock(first);

		//calculateBugUnderstanding();

		/*Map<Integer, Program> programs = assn.getProgramIdMap();
		Program first = programs.get(0);
		CodeBlock decisionPoint = selectCodeBlock(first);
		Equivalence eq = learnEquivalence(decisionPoint);
		eq.saveToFile();*/
	}
	
	private String getEquivalenceName() {
		Scanner in = new Scanner(System.in);
		System.out.print("\nEnter a name: ");
		return in.nextLine();
	}
	
	public static void main(String[] args) {
		new DecomposeEquivalence().run();
	}
	
}
