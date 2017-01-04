package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import models.CodeBlock;
import models.Equivalence;
import models.Program;
import models.Subforest;
import models.ast.Node;
import util.FileSystem;

public class Reduce {
	
	Set<String> keywords;
	List<Equivalence> eqs;
	
	private static final int TO_REDUCE = 30000000;
	
	
	public void run() {
		System.out.println("Reduce!");
		
		keywords = FileSystem.loadKeywords();
		
		
		eqs = loadEquivalences();
		
		
		reduceAllPrograms();
	}

	private List<Equivalence> loadEquivalences() {
		System.out.println("loading equivalences...");
		ArrayList<Equivalence> eqs = new ArrayList<Equivalence>();
		String allEquivalences = FileSystem.getEquivalenceOutDir();
		File folder = new File(allEquivalences);
		File[] listOfFiles = folder.listFiles();
		for(File eqDir : listOfFiles){
			String eqName = eqDir.getName();
			if(eqName.startsWith(".")) continue;
			Equivalence eq = Equivalence.loadFromFile(eqName, keywords, FileSystem.getEquivalenceOutDir());
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

	private void reduceAllPrograms() {
		System.out.println("reducing all programs...");
		
		/*ArrayList<Integer> outputList = FileSystem.loadOutputs();
		int i = 10;
		Program current = Program.loadProgram(i, outputList.get(i), keywords);
		System.out.println("reduce 10");
		reduceProgram(current);*/
		
		
		int numAsts = FileSystem.getNumAsts();
		Set<Integer> corrupts = FileSystem.getCorrupts();
		ArrayList<Integer> outputList = FileSystem.loadOutputs();
		int toReduce = Math.min(numAsts, TO_REDUCE);
		for (int i = 0; i < toReduce; i++) {
			if (i % 100 == 0) {
				System.out.println("num reduced: " + i);
				//System.out.println("contexts: " + eq.getNecessaryContexts().size());
			} 
			
			if (corrupts.contains(i)) {
				continue;
			} 
			Program current = Program.loadProgram(i, outputList.get(i), keywords);
			reduceProgram(current);
		}
	}

	private void reduceProgram(Program current) {
		for(Equivalence eq : eqs) {
			for(CodeBlock block : current.getCodeBlocks()){
				Subforest forest = block.getSubforest();
				
				if(eq.containsSubforest(forest)) {
					forest.markEquivalence(eq);
				}
			}
			current = current.reduce();
		}
		
		current.saveToFile(FileSystem.getReducedOutDir());
		
	}

	public static void main(String[] args) {
		new Reduce().run();
	}
}
