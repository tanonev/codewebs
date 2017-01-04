package experiments;

import java.util.*;

import models.Assignment;
import models.Program;
import models.ast.Node;

import util.FileSystem;

public class ReductionPlot2 {

	public void run() {
		Set<String> keywords = FileSystem.loadKeywords();
		
		System.out.println("loading all programs...");
		int numAsts = FileSystem.getNumAsts();
		FileSystem.setProgramPath(FileSystem.getReducedOutDir());
		Assignment assn = Assignment.loadFromFile(numAsts);
		
		// Build up a map from equivalent programs -> student counts
		final Map<Program, Integer> studentCount = new HashMap<Program, Integer>();
		for(Program p : assn.getPrograms()) {
			if(!studentCount.containsKey(p)) {
				studentCount.put(p, 0);
			}
			
			int pStudents = p.getStudents();
			int oldCount = studentCount.get(p);
			int newCount = oldCount + pStudents;
			studentCount.put(p, newCount);
		}
		
		// Add in the empty line programs
		assn.detectProgramsWithFunctionallyEmptyLines();
		Map<Integer, Integer> emptyLineMap = assn.getEmptyLineMap();
		Set<Program> superfluousConsidered = new HashSet<Program>();
		for(Integer superfluousAstId : emptyLineMap.keySet()) {
			int reasonableAstId = emptyLineMap.get(superfluousAstId);
			Program superfluous = assn.getProgram(superfluousAstId);
			Program reasonable = assn.getProgram(reasonableAstId);
			if(superfluousConsidered.contains(superfluous)) {
				// this prevents you from double counting.
				continue;
			}
			if(reasonable.equals(superfluous)) {
				System.out.println("im a spikey porqupine.");
				continue;
			}
			
			int superfluousCount = studentCount.get(superfluous);
			int reasonableCount = studentCount.get(reasonable);
			int sum = superfluousCount + reasonableCount;
			if(!studentCount.containsKey(reasonable)) {
				studentCount.put(reasonable, 0);
			}
			//studentCount.put(superfluous, sum);
			studentCount.put(reasonable, sum);
			superfluousConsidered.add(superfluous);
		}
		
		// Sort programs by their counts
		List<Program> programList = new ArrayList<Program>(studentCount.keySet());
		Collections.sort(programList, new Comparator<Program>() {
			@Override
			public int compare(Program a, Program b) {
				return studentCount.get(b) - studentCount.get(a);
			}
		});
		
		outputCoverage(programList, studentCount, 25);
		outputCoverage(programList, studentCount, 50);
		outputCoverage(programList, studentCount, 100);
		outputCoverage(programList, studentCount, 200);
	}
	
	private void outputCoverage(List<Program> programList,
			Map<Program, Integer> studentCount, int numAsts) {
		int sum = 0;
		for(int i = 0; i < numAsts; i++) {
			Program curr = programList.get(i);
			int count = studentCount.get(curr);
			sum += count;
		}
		System.out.println("COVERED BY " + numAsts +": " + sum);
	}

	public static void main(String[] args) {
		new ReductionPlot2().run();
	}
}
