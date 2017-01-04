package experiments;

import models.Assignment;

public class TreeCountScaling {
	private static final int NUM_TO_LOAD = 25000;

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;

	public void run() {
	  for (int i = 1000; i <= NUM_TO_LOAD; i += 1000) {
	    assn = Assignment.loadFromFile(i);
	    assn = null;
	    System.gc();
	  }
	}

	public static void main(String[] args) {
		new TreeCountScaling().run();
	}
}
