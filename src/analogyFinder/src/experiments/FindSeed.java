package experiments;

import java.util.*;

import util.FileSystem;

import models.Assignment;
import models.CodeBlock;
import models.Equivalence;
import models.Context;
import models.Program;
import models.Subforest;

/**
 * Class Decompose
 * This is an executable program that has the teacher note decision points
 * in the first few asts. It then creates a "seed" of an equivalence class 
 * (ie, not a complete list of all the equivalences, just a few). It has a 
 * companion program called expand which can take the "seed" and find all
 * of the other equivalences.
 */
public class FindSeed {

	// Don't worry about loading all ASTS. Decompose only needs to create
	// a seed. This number only needs to be larger than the number of ASTs
	// one wishes to annotate.
	private static final int NUM_TO_LOAD = 500;
	
	private static final boolean RECURSE = false;

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;
	private int astId = 0;
	
	private Set<Program> visitedPrograms;

	public void run() {
		
		assn = Assignment.loadFromFile(NUM_TO_LOAD);
		int numSubforests = assn.getUniqueSubforests().size();
		System.out.println("Unique subtrees: " +numSubforests);
		int numComplements = assn.getUniqueContexts().size();
		System.out.println("Unique complements: " + numComplements);

		Map<Integer, Program> programs = assn.getProgramIdMap();
		visitedPrograms = new HashSet<Program>();
		//visitedPrograms.add(programs.get(0));
		
		CodeBlock decisionPoint = null;
		while(decisionPoint == null) {
			choseNextAstId();
			Program first = programs.get(astId);
			decisionPoint = selectCodeBlock(first);
			astId++;
		}
		Equivalence eq = learnEquivalence(decisionPoint);
		eq.saveToFile(FileSystem.getSeedDir());
	}

	private void choseNextAstId() {
		Map<Integer, Program> programs = assn.getProgramIdMap();
		while(true) {
			Program program = programs.get(astId);
			boolean isCorrect = program.isCorrect();
			boolean isVisited = visitedPrograms.contains(program);
			if(isCorrect && !isVisited) {
				break;
			}
			astId++;
		}
	}

	private Equivalence learnEquivalence(CodeBlock decisionPoint) {
		Context decisionContext = decisionPoint.getContext();
	
		Set<Subforest> analogies = new HashSet<Subforest>();
		Set<Context> decisionContexts = new HashSet<Context>();
		decisionContexts.add(decisionContext);
		
		boolean sizeChanged = true;
		while(sizeChanged) {
			System.out.println("\nequivalence finding iteration...");
			int oldSize = analogies.size() + decisionContexts.size();
			Set<CodeBlock> fits = assn.codeBlocksFromContext(decisionContexts);
			for(CodeBlock fit : fits) {
				if(fit.getProgram().isCorrect()) {
					addFit(analogies, decisionContexts, fit);
				}
			}
			sizeChanged = oldSize != analogies.size() + decisionContexts.size();
		}

		String name = getEquivalenceName();

		return new Equivalence(analogies, name);
	}

	private void addFit(Set<Subforest> analogies,
			Set<Context> decisionContexts, CodeBlock fit) {
		Subforest analogy = fit.getSubforest();
		
		if(!analogies.contains(analogy)){
			System.out.println(analogy.getCodeString());
			System.out.println("----");
		}
		analogies.add(analogy);
		
		// Warning: this "recursive" application might be 
		// dangerous. 
		if(RECURSE) {
			Set<CodeBlock> slots = assn.codeBlocksFromSubforest(analogy);
			for(CodeBlock block : slots) {
				decisionContexts.add(block.getContext());
			}
		}
	}

	private String getEquivalenceName() {
		Scanner in = new Scanner(System.in);
		System.out.print("\nEnter a name: ");
		return in.nextLine();
	}

	// Assumes that you are annotating a "correct" program.
	private CodeBlock selectCodeBlock(Program program) {
		List<CodeBlock> codeBlocks = program.getCodeBlocks();
		System.out.println("\n");

		for(int i = 0; i < codeBlocks.size(); i++) {
			System.out.println("Codeblock num: " + i);
			CodeBlock block = codeBlocks.get(i);
			Subforest forest = block.getSubforest();
			String code = forest.getCodeString();
			System.out.println(code);
			System.out.println("--------");
		}
		
		System.out.println("Program is correct? " + program.isCorrect());
		System.out.println("AST id: " + astId);

		Scanner in = new Scanner(System.in);
		System.out.print("Enter the code block to decompose: ");
		String s = in.nextLine();
		if(s.equals("next")) return null;
		
		int codeBlockId = Integer.parseInt(s);
		CodeBlock selected = codeBlocks.get(codeBlockId);

		String selectedCode = selected.getSubforest().getCodeString();
		System.out.println("you selected:");
		System.out.println(selectedCode);

		return selected;
	}
	
	// ----------------------- Old code. Not used ---------------------//
	

	
	private void calculateBugUnderstanding() {
		Collection<Program> programs = assn.getPrograms();
		int bugSize = 0;
		for(Program program : programs) {
			if(!program.isCorrect()) {
				int smallestBug = program.getSmallestBugSize();
				bugSize += smallestBug;
			}
		}
		System.out.println("Bug size: " + bugSize);
	}

	private void findBugs(CodeBlock decisionPoint) {
		throw new RuntimeException("not done");
		/*System.out.println("finding bugs...");
		Context decisionContext = decisionPoint.getContext();
		Set<CodeBlock> fits = assn.codeBlocksFromContext(decisionContext);

		for(CodeBlock fit : fits) {
			if(!fit.getProgram().isCorrect()) {
				fit.markAsBuggy();
			}
		}*/
	}

	private void pressEnterToContinue() {
		Scanner keyboard = new Scanner(System.in);
		System.out.println("press enter to continue");
		keyboard.nextLine();
	}

	public static void main(String[] args) {
		new FindSeed().run();
	}
}
