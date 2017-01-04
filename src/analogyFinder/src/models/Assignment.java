package models;

import java.util.*;

import org.json.JSONObject;

import util.FileSystem;

import minions.Factorizer;

/**
 * Class: Assignment
 * Assignment is the collection of all student submissions. It also has
 * an index which makes it easy to look up codeBlocks given subforests or
 * contexts.
 */
public class Assignment {

	// All the student programs
	protected List<Program> programs;

	// A handy map which allows users to look up programs by id.
	protected Map<Integer, Program> programIdMap;

	// The assignment keeps the keywords
	protected Set<String> keywords;

	// Important: The index that allows one to lookup codeBlocks from subforests
	protected HashMap<Subforest, Set<CodeBlock>> subforestBlockMap;

	// Important: The index that allows one to lookup codeBlocks from contexts
	protected HashMap<Context, Set<CodeBlock>> contextBlockMap;

	// A map from the astId of programs with superflous lines to the astIds of their more reasonable counterparts
	protected Map<Integer, Integer> emptyLineMap;
	
	public static Assignment loadFromFile(int max) {
		return loadFromFile(max, true);
	}
	
	public static Assignment loadFromFile(int max, boolean makeIndex) {
		Assignment newAssn = new Assignment();
		newAssn.load(max, makeIndex);
		return newAssn;
	}

	public Collection<Program> getPrograms() {
		return programs;
	}
	
	public Map<Integer, Integer> getEmptyLineMap() {
		return emptyLineMap;
	}
	
	public void saveEmptyLinePrograms() {
		FileSystem.saveToIgnore(emptyLineMap.keySet());
	}

	public Map<Integer, Program> getProgramIdMap() {
		return programIdMap;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public Set<CodeBlock> codeBlocksFromContext(Context context) {
		if(!contextBlockMap.containsKey(context)) {
      return Collections.emptySet();
		}
		return contextBlockMap.get(context);
	}

	public Set<CodeBlock> codeBlocksFromSubforest(Subforest forest) {
		if(!subforestBlockMap.containsKey(forest)) {
			return Collections.emptySet();
		}
		return subforestBlockMap.get(forest);
	}

	public Set<Subforest> getUniqueSubforests() {
		return subforestBlockMap.keySet();
	}

	public Set<Context> getUniqueContexts() {
		return contextBlockMap.keySet();
	}

	public Program getProgram(int astId) {
		return programIdMap.get(astId);
	}

	public Set<CodeBlock> codeBlocksFromContext(Set<Context> decisionContexts) {
		Set<CodeBlock> blocks = new HashSet<CodeBlock>();
		for(Context c : decisionContexts) {
			Set<CodeBlock> contextBlocks = codeBlocksFromContext(c);
			blocks.addAll(contextBlocks);
		}
		return blocks;
	}

	public List<Subforest> getSortedSubforests() {
		Set<Subforest> forests = getUniqueSubforests();
		List<Subforest> forestList = new ArrayList<Subforest>();
		forestList.addAll(forests);
		Collections.sort(forestList, new Comparator<Subforest>() {
			@Override
			public int compare(Subforest a, Subforest b) {
				int aCount = subforestBlockMap.get(a).size();
				int bCount = subforestBlockMap.get(b).size();
				return bCount - aCount;
			}
		});
		return forestList;
	}

	public void reduce(Equivalence eq) {
		System.out.println("reducing...");
		
		List<Program> newPrograms = new ArrayList<Program>();
		for(Program p : programs) {
			for(CodeBlock block : p.getCodeBlocks()){
				Subforest forest = block.getSubforest();

				if(eq.containsSubforest(forest)) {
					forest.markEquivalence(eq);
				}
			}
			Program newProgram = p.reduce();
			newPrograms.add(newProgram);
			newProgram.saveToFile(FileSystem.getReducedOutDir());
		}
		
		programs = newPrograms;
		createProgramIdMap();
		createCodeBlockDataStructures();
	}
	
	/**
	 * Method: Remove Empty Subtrees
	 * -----------------------------.
	 */
	public void detectProgramsWithFunctionallyEmptyLines() {
		Subforest empty = getEmptySubtree();
		Set<CodeBlock> emptyBlocks = codeBlocksFromSubforest(empty);
		
		Map<Context, CodeBlock> emptyContexts = new HashMap<Context, CodeBlock>();
		for(CodeBlock emptyBlock : emptyBlocks) {
			emptyContexts.put(emptyBlock.getContext(), emptyBlock);
		}
		
		
		for (Subforest t : getUniqueSubforests()) {
			if (t == empty) {
				continue;
			}
			Set<CodeBlock> blocksSet = codeBlocksFromSubforest(t);
			
			List<CodeBlock> blocks = new ArrayList<CodeBlock>(blocksSet);
			Collections.sort(blocks, new Comparator<CodeBlock>() {
				@Override
				public int compare(CodeBlock a, CodeBlock b) {
					int aId = a.getProgram().getId();
					int bId = b.getProgram().getId();
					return aId - bId;
				}
			});
			
			for(CodeBlock b : blocks) {
				Context c = b.getContext();
				if(emptyContexts.containsKey(c)) {
					CodeBlock emptyBlock = emptyContexts.get(c);
					Program emptyProgram = emptyBlock.getProgram();
					Program superflousProgram = b.getProgram();
					
					if(emptyProgram.getOutput() == superflousProgram.getOutput()) {
						emptyLineMap.put(superflousProgram.getId(), emptyProgram.getId());
					} else {
						break;
					}
				}
			}
		}
		int sum = emptyLineMap.size();
		System.out.println("Asts deleted because they are empty: " + sum);
		saveEmptyLinePrograms();
	}
	
	public boolean hasProgram(int i) {
		return programIdMap.containsKey(i);
	}

	public int getNumUniquePrograms() {
		Set<Program> uniquePrograms = new HashSet<Program>();
		for(Program p : programs) {
			int programId = p.getId();
			if(emptyLineMap.containsKey(programId)) {
				continue;
			}
			uniquePrograms.add(p);
		}
		return uniquePrograms.size();
	}

	//------------------------ Private -------------------------------------//

	private Set<Context> contextsFromSubforest(Subforest empty) {
		// TODO Auto-generated method stub
		return null;
	}

	private Subforest getEmptySubtree() {
		for(Subforest f : getUniqueSubforests()) {
			if(f.isEmpty()) return f;
		}
		return null;
	}

	private void load(int maxPrograms, boolean makeCodeBlocks) {
		//emptyLinePrograms = FileSystem.loadToIgnore();
		emptyLineMap = new TreeMap<Integer, Integer>();
		
		// Before anything, load keywords
		keywords = FileSystem.loadKeywords();

		// First, load all program asts, code and maps and make codeblocks.
		loadPrograms(maxPrograms);
		createProgramIdMap();

		// Then create the awesome maps from contexts or subtree -> codeblocks
		if(makeCodeBlocks) {
			long start = System.nanoTime();
			createCodeBlockDataStructures();
			long end = System.nanoTime();
			System.err.println("Elapsed indexing time: " + (end - start) / 1e9);
		}
	}

	private void loadPrograms(int num) {
		System.out.println("loading programs...");
		int numAsts = FileSystem.getNumAsts();
		Set<Integer> corrupts = FileSystem.getCorrupts();
		ArrayList<Integer> outputList = FileSystem.loadOutputs();
		ArrayList<Integer> numSubmissionsList = FileSystem.loadNumSubmissions();
		programs = new ArrayList<Program>();
		for (int i = 0; i < Math.min(num, numAsts); i++) {
			if (i % 100 == 0) {
				System.out.println("num loaded: " + i);
			} 
			if (corrupts.contains(i)) {
				continue;
			} 
			Program current = loadProgram(i, outputList.get(i), numSubmissionsList.get(i) );
			programs.add(current);
		}
	}

	private Program loadProgram(int astId, int output, int numSubmissions) {
		JSONObject obj = FileSystem.loadAst(astId);
		ArrayList<String> code = FileSystem.loadCode(astId);
		int[][] map = FileSystem.loadMap(astId);
		JSONObject root = obj.getJSONObject("root");
		return new Program(keywords, astId, code, map, root, output, numSubmissions);
	}

	private void createProgramIdMap() {
		programIdMap = new TreeMap<Integer, Program>();
		for(Program p : programs) {
			int id = p.getId();
			programIdMap.put(id, p);
		}
	}

	private void createCodeBlockDataStructures() {
		System.out.println("indexing codeblock dbs...");
		subforestBlockMap = new HashMap<Subforest, Set<CodeBlock>>();
		contextBlockMap = new HashMap<Context, Set<CodeBlock>>();
		for(Program p : programs) {
			List<CodeBlock> blocks = p.getCodeBlocks();
			for(CodeBlock block : blocks) {
				addCodeBlockToDataStructures(block);
			}
		}
	}

	private void addCodeBlockToDataStructures(CodeBlock block) {
		Subforest forest = block.getSubforest();
		Context context = block.getContext();
		if (!subforestBlockMap.containsKey(forest)) {
			subforestBlockMap.put(forest, new HashSet<CodeBlock>());
		}
		if (!contextBlockMap.containsKey(context)) {
			contextBlockMap.put(context, new HashSet<CodeBlock>());
		}
		subforestBlockMap.get(forest).add(block);
		contextBlockMap.get(context).add(block);
	}


}

