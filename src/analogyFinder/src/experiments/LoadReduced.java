package experiments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import models.Program;

import util.FileSystem;

public class LoadReduced {

	public void run() {
		Set<String> keywords = FileSystem.loadKeywords();
		
		System.out.println("loading all programs...");
		int numAsts = FileSystem.getNumAsts();
		
		
		Set<Integer> corrupts = FileSystem.getCorrupts();
		ArrayList<Integer> outputList = FileSystem.loadOutputs();

		Set<Program> allPrograms = new HashSet<Program>();
		Set<Integer> emptyLinePrograms = FileSystem.loadToIgnore();
		
		FileSystem.setProgramPath(FileSystem.getReducedOutDir());
		for (int i = 0; i < numAsts; i++) {
			
			if (i % 100 == 0) {
				//System.out.println("num loaded: " + i);
				int size = allPrograms.size();
				double percent = size * 100.0 / i;
				//System.out.println(" percent: " + percent);
				System.out.println(i + "\t" + percent);
			} 
			
			/*if(emptyLinePrograms.contains(i)) {
				continue;
			}*/
			
			if (corrupts.contains(i)) {
				continue;
			} 
			Program current = Program.loadProgram(i, outputList.get(i), keywords);
			allPrograms.add(current);
			
		}
		System.out.println(allPrograms.size());
	}
	
	public static void main(String[] args) {
		new LoadReduced().run();
	}
}
