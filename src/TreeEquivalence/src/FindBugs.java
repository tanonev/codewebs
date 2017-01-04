import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import util.FileSystem;

import engine.BugFinder;
import engine.Factorizer;

import models.ASTID;
import models.Output;
import models.Program;

/**
 * Runnable: FindBugs
 * ---------------
 * A Runnable Java program. Creates and runs a BugFinder engine.
 */
public class FindBugs {

	private static final int NUM_ASTS = 300;

	public void run() {
		clearPreviousFactorization();
		Map<ASTID, Program> programs = loadStudentPrograms();
		
		//Factorizer factorizer = new Factorizer(programs);
		//programs = factorizer.factorize();
		
		BugFinder bugFinder = new BugFinder(programs);
		bugFinder.findBugs();
		
	}
	
	/**
	 * Method: Clear Previous Factorization
	 * ------------------------------------
	 * Delete anything from the filestystem in the factors folder.
	 */
	protected void clearPreviousFactorization() {
		FileSystem.clearEquivalences();
	}
	
	/**
	 * Method: Load Student Programs
	 * -----------------------------
	 * Does what it says. If the constant NUM_ASTS is smaller than the 
	 * number of student programs we have on file, we will only load a
	 * subset of programs.
	 */
	protected Map<ASTID, Program> loadStudentPrograms() {
		System.out.println("loading student programs...");
		int numAsts = FileSystem.getNumAsts();
		System.out.println("number of asts on file: " + numAsts);
		if (NUM_ASTS < FileSystem.getNumAsts()) {
			System.out.println("loading first " + NUM_ASTS + " asts...");
		} else {
			System.out.println("loading all programs on file...");
		}

		return FileSystem.loadAllPrograms(NUM_ASTS);
	}
	
	/**
	 * Method: Main 
	 * ------------
	 * The entry point into the program. Creates a new FactorAsts instance 
	 * and runs it.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("running FindBugs.java");
		new FindBugs().run();
		System.out.println("finished.");
	}

}
