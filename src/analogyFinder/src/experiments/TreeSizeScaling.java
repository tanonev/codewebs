package experiments;

import models.Assignment;
import models.Program;
import util.FileSystem;

public class TreeSizeScaling {
	private static final int NUM_TO_LOAD = 1000;
	private static final String[] assnStrs = {//"1_1", "1_2", "1_3", "1_4", "1_5", "1_6", "1_7",
	                                          //"2_1", "2_2", "2_3", "2_4", "2_5"};//, "2_6",
	                                          //"3_1", "3_2", "3_3", "3_4",
	                                          //"4_1", "4_2", "4_3", "4_4", "4_5",
	                                          //"5_1", "5_2", "5_3", "5_4", "5_5",};
	                                          "6_1", "6_2", "6_3", "6_4",
	                                          "7_1", "7_2", "7_3", "7_4", "7_5",
	                                          "8_1", "8_2", "8_3", "8_4", "8_5", "8_6"};

	// The assignment object that stores all programs loaded and the index.
	private Assignment assn;

	public void run() {
	  for (String assnStr : assnStrs) {
	    FileSystem.setAssignment(assnStr);
	    System.err.println(assnStr);
	    assn = Assignment.loadFromFile(NUM_TO_LOAD);
	    int totalSize = 0;
	    for (Program p : assn.getPrograms()) {
	      totalSize += p.getRoot().getSize();
	    }
	    System.err.println(totalSize + "/" + assn.getPrograms().size() + " = " + ((double) totalSize) / assn.getPrograms().size());
	    System.err.println();
	    assn = null;
	    System.gc();
	  }
	}

	public static void main(String[] args) {
		new TreeSizeScaling().run();
	}
}
