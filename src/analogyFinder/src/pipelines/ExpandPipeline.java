package pipelines;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import models.Assignment;
import models.CodeBlock;
import models.Context;
import models.Equivalence;
import models.Program;
import models.Subforest;

import util.FileSystem;


public class ExpandPipeline {

	String[] equivalenceNames = {
			"1_alphaOverM",
			"2_hypothesis",
			"3_residual",
			"4_gradTrans",
			"6_grad",
			//"6_gradTimeM-i1",
			//"6_gradTimesM-i",
			//"6_gradTimesM-i2",
			//"6_gradTimesM1",
			//"6_gradTimesM2",
			//"8_scaledGrad1",
			//"8_scaledGrad2",
			"10_scaledGradient",
			"11_negScaledGrad"
	};

	Set<String> keywords;
	Equivalence eq;
	
	Assignment assn;
	Set<Integer> toIgnore;

	public void run() {
		toIgnore = new HashSet<Integer>();
		keywords = FileSystem.loadKeywords();
		
		List<Integer> sizes = new ArrayList<Integer>();
		FileSystem.setProgramPath(FileSystem.getReducedOutDir());
		int numAsts =  FileSystem.getNumAsts();
		assn = Assignment.loadFromFile(numAsts);
		int numPrograms = assn.getNumUniquePrograms();

		for(String name : equivalenceNames) {
			assn.detectProgramsWithFunctionallyEmptyLines();
			System.out.println(name);
			copyFromSeedToExpand(name);
			expand(name);
			verifyEquivalence(name);
			copyFromExpandToEquivalence(name);
			Equivalence eq = Equivalence.loadFromFile(name, keywords, FileSystem.getEquivalenceOutDir());
			assn.reduce(eq);
			
			numPrograms = assn.getNumUniquePrograms();
			System.out.println("!@#!@#!@#!@#");
			System.out.println("SIZE: " + numPrograms);
			System.out.println("!@#!@#!@#!@#");
			sizes.add(numPrograms);
			FileSystem.createFile(".", "size_"+name, "" + numPrograms);
		}
	}

	private int reduceAllPrograms(String eqName) {
		System.out.println("reducing all programs...");

		int numAsts = FileSystem.getNumAsts();
		Set<Integer> corrupts = FileSystem.getCorrupts();
		Set<Program> allPrograms = new HashSet<Program>();
		for (int i = 0; i < numAsts; i++) {
			if (i % 100 == 0) {
				System.out.println("num reduced: " + i);
				//System.out.println("contexts: " + eq.getNecessaryContexts().size());
			} 

			if(!shouldProcessProgram(i)) continue;
			Program current = assn.getProgram(i);
			Program reduced = reduceProgram(current,eqName);
			allPrograms.add(reduced);
		}
		return allPrograms.size();
	}

	private boolean shouldProcessProgram(int i) {
		if(!assn.hasProgram(i)) return false;
		if(toIgnore.contains(i)) return false;
		return true;
	}

	private Program reduceProgram(Program current, String eqName) {
		eq = Equivalence.loadFromFile(eqName, keywords, FileSystem.getExpandedDir());
		for(CodeBlock block : current.getCodeBlocks()){
			Subforest forest = block.getSubforest();

			if(eq.containsSubforest(forest)) {
				forest.markEquivalence(eq);
			}
		}
		current = current.reduce();
		current.saveToFile(FileSystem.getReducedOutDir());
		return current;
	}

	private void verifyEquivalence(String name) {
		Scanner keyboard = new Scanner(System.in);
		System.out.println("veryfiy: " + name +" and hit enter");
		keyboard.nextLine();
	}

	private void expand(String name) {
		eq = Equivalence.loadFromFile(name, keywords, FileSystem.getExpandedDir());

		for(Subforest f : eq.getSubforests()) {
			System.out.println(f.hashCode());
		}
		System.out.println("----");
		expandAllPrograms(keywords);
	}

	private int expandAllPrograms(Set<String> keywords) {
		int numAsts = FileSystem.getNumAsts();
		Set<Integer> corrupts = FileSystem.getCorrupts();
		int count = 0;
		for (int i = 0; i < numAsts; i++) {
			if (i % 100 == 0) {
				System.out.println("num expanded: " + i);
				//System.out.println("contexts: " + eq.getNecessaryContexts().size());
			} 
			if (corrupts.contains(i)) {
				continue;
			} 
			if(!assn.hasProgram(i)) {
				continue;
			}
			
			Program current = assn.getProgram(i);
			count += expandProgram(current);
		}
		return count;
	}
	
	

	private int expandProgram(Program current) {
		List<CodeBlock> blocks = current.getCodeBlocks();
		int count = 0;
		for(CodeBlock block : blocks) {
			if(current.getId() < 1000) {
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
	
	private void copyFromExpandToEquivalence(String name) {
		String fromDirPath = FileSystem.getExpandedDir() + "/" + name + "/subforest";
		String toDirPath = FileSystem.getEquivalenceOutDir() + "/" + name + "/subforest";
		File toDir = new File(toDirPath);
		toDir.mkdirs();
		File fromDir = new File(fromDirPath);
		File[] listOfFiles = fromDir.listFiles(); 
		for(File fromFile : listOfFiles) {
			String fileName = fromFile.getName();
			String fromFilePath = fromDirPath + "/" + fileName;
			String toFilePath = toDirPath + "/" + fileName;
			FileSystem.copyFile(fromFilePath, toFilePath);
		}
	}

	private void copyFromSeedToExpand(String name) {
		String fromDirPath = FileSystem.getSeedDir() + "/" + name + "/subforest";
		String toDirPath = FileSystem.getExpandedDir() + "/" + name + "/subforest";
		File toDir = new File(toDirPath);
		toDir.mkdirs();
		File fromDir = new File(fromDirPath);
		File[] listOfFiles = fromDir.listFiles(); 
		for(File fromFile : listOfFiles) {
			String fileName = fromFile.getName();
			String fromFilePath = fromDirPath + "/" + fileName;
			String toFilePath = toDirPath + "/" + fileName;
			FileSystem.copyFile(fromFilePath, toFilePath);
		}
	}

	public static void main(String[] args) {
		new ExpandPipeline().run();
	}

}
