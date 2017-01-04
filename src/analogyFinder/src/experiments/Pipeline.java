package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import models.Assignment;
import models.CodeBlock;
import models.Context;
import models.Equivalence;
import models.Program;
import models.Subforest;
import util.FileSystem;

public class Pipeline {

	// Don't worry about loading all ASTS. Decompose only needs to create
	// a seed. This number only needs to be larger than the number of ASTs
	// one wishes to annotate.
	private static final int NUM_TO_LOAD = 200;

	private static final boolean RECURSE = false;

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;

	private Set<Program> visitedPrograms;
	
	private int astId = 0;
	
	private Set<String> keywords;

	public void run() {
		
		keywords = FileSystem.loadKeywords();

		assn = Assignment.loadFromFile(NUM_TO_LOAD);
		
		// apply all the seed equivalences found so far...
		List<Equivalence> eqs = loadEquivalences();
		for(Equivalence eq : eqs) {
			assn.reduce(eq);
		}
		

		visitedPrograms = new HashSet<Program>();

		while(true) {

			Map<Integer, Program> programs = assn.getProgramIdMap();
			Program first = programs.get(astId);
			
			
			CodeBlock decisionPoint = selectCodeBlock(first);
			while(decisionPoint == null) {
				visitedPrograms.add(first);
				goToNextCorrectDifferentAstId();
				first = programs.get(astId);
				decisionPoint = selectCodeBlock(first);
				astId++;
			}
			
			Equivalence eq = learnEquivalence(decisionPoint);
			eq.saveToFile(FileSystem.getSeedDir());
			
			assn.reduce(eq);
			pressEnterToContinue();
		}
	}

	/*private void choseNextAstId() {
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
	}*/

	private void goToNextCorrectDifferentAstId() {
		while(true) {
			Program program = assn.getProgram(astId);
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
	
	private List<Equivalence> loadEquivalences() {
		System.out.println("loading equivalences...");
		ArrayList<Equivalence> eqs = new ArrayList<Equivalence>();
		String allEquivalences = FileSystem.getSeedDir();
		File folder = new File(allEquivalences);
		File[] listOfFiles = folder.listFiles();
		for(File eqDir : listOfFiles){
			String eqName = eqDir.getName();
			if(eqName.startsWith(".")) continue;
			Equivalence eq = Equivalence.loadFromFile(eqName, keywords, FileSystem.getSeedDir());
			eqs.add(eq);
		}
		sortEquivalences(eqs);
		return eqs;
	}
	
	private void sortEquivalences(ArrayList<Equivalence> eqs) {
		Collections.sort(eqs, new Comparator<Equivalence>() {
			@Override
			public int compare(Equivalence a, Equivalence b) {
				return a.getPriority() - b.getPriority();
			}
		});
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

	private void pressEnterToContinue() {
		Scanner keyboard = new Scanner(System.in);
		System.out.println("press enter to continue");
		keyboard.nextLine();
	}

	public static void main(String[] args) {
		new Pipeline().run();
	}

}
