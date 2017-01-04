package minions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Assignment;
import models.CodeBlock;
import models.Context;
import models.Equivalence;
import models.Program;
import models.Subforest;
import models.ast.Node;

/**
 * Factorizer
 * Takes an assignment and an equivalence class and reduces all the programs
 * in the assignment based on the equivalence.
 */
public class Factorizer {

	public static void factor(Assignment assn, Equivalence eq) {
		new Factorizer().run(assn, eq);
	}

	private Assignment assn;
	private Equivalence equivalence;
	private Set<Program> affectedPrograms;

	private void run(Assignment assn, Equivalence eq) {
		//this.assn = assn;
		//this.equivalence = eq;
		//markEquivalence();
		//computeReducedSubtrees();
		//assn.reindex();
		throw new RuntimeException("reindexing is undefined at the moment.");
	}
	
	private void computeReducedSubtrees() {
		System.out.println("precomputing reduced subtrees...");
		for(Program p : affectedPrograms) {
			for(CodeBlock b : p.getCodeBlocks()) {
				//b.reduceComponents();
				throw new RuntimeException("not defined");
			}
		}
	}

	private void markEquivalence() {
		System.out.println("marking equivalence...");
		affectedPrograms = new HashSet<Program>();
		for(Subforest forest : equivalence.getSubforests()) {
			Set<CodeBlock> codeBlocks = assn.codeBlocksFromSubforest(forest);
			if(codeBlocks == null) continue;
			for(CodeBlock block : codeBlocks) {
				block.markEquivalence(equivalence);
				affectedPrograms.add(block.getProgram());
			}
		}
		
	}

}
