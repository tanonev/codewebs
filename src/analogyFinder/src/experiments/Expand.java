package experiments;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import util.FileSystem;

import models.CodeBlock;
import models.Context;
import models.Equivalence;
import models.Program;
import models.Subforest;

/**
 * Class Expand
 * Take the seed of an equivalence class and find all the different ways of
 * writing that equivalence. Mark all codeBlocks from programs that have
 * an instance of that equivalence (or an attempt). Marks are saved to file.
 */
public class Expand {
	
	// The equivalence to expand
	Equivalence eq;
	
	public void run() {
		System.out.println("Expand!");
		
		Set<String> keywords = FileSystem.loadKeywords();
		
		String name = getEquivalenceName();
		boolean fromReduced = shouldLoadFromReduced();
		if(fromReduced) {
			FileSystem.setProgramPath(FileSystem.getReducedOutDir());
		}
		
		eq = Equivalence.loadFromFile(name, keywords, FileSystem.getExpandedDir());
		
		for(Subforest f : eq.getSubforests()) {
			System.out.println(f.hashCode());
		}
		System.out.println("----");
		while(true) {
			expandAllPrograms(keywords);
		}
	}

	private int expandAllPrograms(Set<String> keywords) {
		int numAsts = FileSystem.getNumAsts();
		Set<Integer> corrupts = FileSystem.getCorrupts();
		ArrayList<Integer> outputList = FileSystem.loadOutputs();
		int count = 0;
		for (int i = 0; i < numAsts; i++) {
			if (i % 100 == 0) {
				System.out.println("num expanded: " + i);
				//System.out.println("contexts: " + eq.getNecessaryContexts().size());
			} 
			if (corrupts.contains(i)) {
				continue;
			} 
			Program current = Program.loadProgram(i, outputList.get(i), keywords);
			count += expandProgram(current);
		}
		return count;
	}
	
	private int expandProgram(Program current) {
		List<CodeBlock> blocks = current.getCodeBlocks();
		int count = 0;
		for(CodeBlock block : blocks) {
			if(current.getId() < 4000) {
				checkForSubtreeMatch(block);
			}
			boolean newSubtree = checkForContextMatch(block);
			if(newSubtree) count++;
		}
		return count;
	}
	
	private void checkForSubtreeMatch(CodeBlock block) {
		Subforest subforest = block.getSubforest();
		Context context = block.getContext();
		
		// First, check if there is a positive match.
		if(eq.containsSubforest(subforest)) {
			if(block.getProgram().isCorrect()) {
				eq.addNecessaryContext(context);
			}
		} 
	}
	
	private boolean checkForContextMatch(CodeBlock block){
		Subforest subforest = block.getSubforest();
		Context context = block.getContext();
		
		// Then, you check if this fits our idea of where the eq exists.
		if(eq.contextRequiresInstance(context)) {
			if(block.getProgram().isCorrect()) {
				if(eq.addSubforest(subforest)) {
					System.out.println("new forest: " + subforest.getCodeString());
					eq.saveSubforest(subforest, FileSystem.getExpandedDir());
					return true;
				}
			} 
		}
		return false;
	}
	
	private boolean shouldLoadFromReduced() {
		Scanner in = new Scanner(System.in);
		System.out.print("Load from reduced (y = yes)? ");
		return in.nextLine().equals("y");
	}

	private String getEquivalenceName() {
		Scanner in = new Scanner(System.in);
		System.out.print("Enter a name: ");
		return in.nextLine();
	}
	
	private void pressEnterToContinue() {
		Scanner keyboard = new Scanner(System.in);
		System.out.println("press enter to continue");
		keyboard.nextLine();
	}

	public static void main(String[] args) {
		new Expand().run();
	}
	
}
