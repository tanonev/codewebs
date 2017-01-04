
import java.util.Map;

import engine.Factorizer;
import engine.ManualFactorizer;
import models.ASTID;
import models.Program;
import util.FileSystem;

/**
 * Runnable: Factor Asts 
 * ----------------- 
 * This runnable class takes in a set of
 * asts, finds analogous code blocks and creates reduced ast representations.
 * See FindBugs.java for another experiment you may want to run.
 */
public class FactorAsts {

	// Constants
	private static final int NUM_ASTS = 200; //5000; //25398
	

	/**
	 * Method: Run 
	 * ----------- 
	 * The main entry point for the program which is
	 * called on a FactorAsts instance.
	 */
	protected void run() {
		clearPreviousFactorization();
		Map<ASTID, Program> programs = loadStudentPrograms();
		Factorizer factorizer = new ManualFactorizer(programs);
		programs = factorizer.factorize();
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
		System.out.println("running FactorAsts.java");
		new FactorAsts().run();
		System.out.println("finished.");
	}

}