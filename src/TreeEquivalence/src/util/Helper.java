package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import models.AST;
import models.ASTID;
import models.ASTWithComplement;
import models.Context;
import models.Node;
import models.Output;
import models.Program;
import models.Subtree;
import models.SubtreeInfo;
import models.SubtreeLocation;

public class Helper {
	
  public static Map<Context, Map<Integer, Integer>> getContextHistograms(Map<ASTID, Program> programs) {
    HashMap<Context, Map<Integer, Integer>> ans = new HashMap<Context, Map<Integer, Integer>>();
    for (Program p : programs.values()) addProgramToHistograms(p, ans);
    return ans;
  }
  
  public static void addProgramToHistograms(Program program, Map<Context, Map<Integer, Integer>> contextHistograms) {
    for (Context c : program.getAllContexts()) {
      if (!contextHistograms.containsKey(c)) contextHistograms.put(c, new TreeMap<Integer, Integer>());
      Map<Integer, Integer> histogram = contextHistograms.get(c);
      if (!histogram.containsKey(program.output)) histogram.put(program.output, 0);
      histogram.put(program.output, histogram.get(program.output) + 1);
    }
  }
  
  // MAKE SURE THAT THE PROGRAM WAS PREVIOUSLY ADDED FIRST!
  public static void removeProgramFromHistograms(Program program, Map<Context, Map<Integer, Integer>> contextHistograms) {
    for (Context c : program.getAllContexts()) {
      Map<Integer, Integer> histogram = contextHistograms.get(c);
      histogram.put(program.output, histogram.get(program.output) - 1);
    }
  }
  
  public static Map<AST, Map<Integer, Integer>> getSubtreeHistograms(Map<ASTID, Program> programs) {
    HashMap<AST, Map<Integer, Integer>> ans = new HashMap<AST, Map<Integer, Integer>>();
    for (Program p : programs.values()) addProgramToSubtreeHistograms(p, ans);
    return ans;
  }
  
  public static void addProgramToSubtreeHistograms(Program program, Map<AST, Map<Integer, Integer>> subtreeHistograms) {
    for (int i = 0; i < program.ast.size(); i++) {
      AST c = program.getSubtree(i);
      if (!subtreeHistograms.containsKey(c)) subtreeHistograms.put(c, new TreeMap<Integer, Integer>());
      Map<Integer, Integer> histogram = subtreeHistograms.get(c);
      if (!histogram.containsKey(program.output)) histogram.put(program.output, 0);
      histogram.put(program.output, histogram.get(program.output) + 1);
    }
  }
  
  // MAKE SURE THAT THE PROGRAM WAS PREVIOUSLY ADDED FIRST!
  public static void removeProgramFromSubtreeHistograms(Program program, Map<AST, Map<Integer, Integer>> subtreeHistograms) {
    for (int i = 0; i < program.ast.size(); i++) {
      AST c = program.getSubtree(i);
      Map<Integer, Integer> histogram = subtreeHistograms.get(c);
      histogram.put(program.output, histogram.get(program.output) - 1);
    }
  }
  
	public static List<Subtree> getSubtrees(Map<ASTID, Program> programs, int maxNum, int iteration) {
		HashMap<AST, SubtreeInfo> subtreeMap = getSubtreeMap(programs);
		ArrayList<Subtree> subtrees = new ArrayList<Subtree>();
		int idCounter = 0;
		for (AST ast : subtreeMap.keySet()) {
			SubtreeInfo info = subtreeMap.get(ast);
			Subtree newSubtree = new Subtree(idCounter, ast, info);
			subtrees.add(newSubtree);
			idCounter++;
		}
		Collections.sort(subtrees, new Comparator<Subtree>() {
			@Override
			public int compare(Subtree a, Subtree b) {
				return b.info.getCount() - a.info.getCount();
			}
		});
		
		if (iteration >= 0) printSubtrees(subtrees, iteration);
		// Warning: There is a possibility of subtrees.size being larger than
		// maxInt.
		//System.out.println("subtrees.size: " + subtrees.size());
		//System.out.println("subtrees.size: " + maxNum);
		if(subtrees.size() > maxNum) {
			throw new RuntimeException("More subtrees than maxInt");
		}
		
		return subtrees.subList(0, Math.min(maxNum, subtrees.size()));
	}

	public static Map<Context, List<Subtree>> getComplements(Map<ASTID, Program> programs, int maxNum, int iteration) {
		List<Subtree> subtrees = getSubtrees(programs, maxNum, iteration);
		Map<Context, List<Subtree>> complementMap = new HashMap<Context, List<Subtree>>();
		
		for (Subtree tree : subtrees) {
			Collection<Context> complements = tree.info.getComplements();
			
			for (Context complement : complements) {
				if (!complementMap.containsKey(complement)) {
					complementMap.put(complement, new ArrayList<Subtree>());
				}
				complementMap.get(complement).add(tree);
			}
		}
		
		return complementMap;
		
	}
	
	public static HashMap<AST, SubtreeInfo> getSubtreeMap(Map<ASTID, Program> programs) {

		HashMap<AST, SubtreeInfo> subtrees = new HashMap<AST, SubtreeInfo>();
		for (ASTID astId : programs.keySet()) {
			Program program = programs.get(astId);
			AST current = program.ast;
			for (int j = 0; j < current.size(); j++) {
				int nodeId = current.getNodeId(j);
				//System.out.println(nodeId + " " + j);
				List<Integer> treeNodeList = new ArrayList<Integer>();
				treeNodeList.add(nodeId);
				SubtreeLocation treeLoc = new SubtreeLocation(astId, treeNodeList);
				
				Helper.recordSubforest(program.getSubtree(j), program.getComplement(j),
						program.code, program.map, program.getScore(), subtrees, treeLoc, null);
				
				for (ASTWithComplement cur : program.subforests(j)) {
					List<Integer> forestNodeList = new ArrayList<Integer>();
					for (int k = 0; k < cur.ast.size(); k++) {
						forestNodeList.add(cur.ast.getNodeId(k));
					}
					SubtreeLocation forestLoc = new SubtreeLocation(astId, forestNodeList);
					Helper.recordSubforest(cur.ast, cur.complement, program.code, program.map,
							program.getScore(), subtrees, forestLoc, null);
				}
			
			}
		}
		return subtrees;
	}
	
	public static HashMap<ASTWithComplement, ASTID> getASTidMap(Map<ASTID, Program> programs)
	{
		HashMap<ASTWithComplement, ASTID> astIdMap = new HashMap<ASTWithComplement, ASTID>();
		for(ASTID astId : programs.keySet()) {
			Program program = programs.get(astId);
			AST current = program.ast;
			for(int j = 0; j < current.size(); j++) {
				astIdMap.put(new ASTWithComplement(program.getSubtree(j), program.getComplement(j),-1,-1), astId);
				for(ASTWithComplement cur : program.subforests(j))
					astIdMap.put(cur, astId);
			}
		}
		return astIdMap;
	}
	
	// extracts a code sample from the ast subtree
	// put into util
	public static String getCode(ArrayList<String> code, int[][] map, AST ast) {
	  if (ast == AST.NULL) return "{{empty}}";
	  HashSet<Integer> ids = new HashSet<Integer>();
		for (Node n : ast.getNodes())
			ids.add(n.id);
		StringBuffer ans = new StringBuffer();
		int lastrow = -1;
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				if (ids.contains(map[i][j])) {
					if (lastrow != -1 && lastrow != i)
						ans.append('\n');
					lastrow = i;
					ans.append(code.get(i).charAt(j));
				}
			}
		}
		return ans.toString();
	}

	// builds out giant table of all trees
	// take subtrees as a parameter
	// util
	public static void recordSubforest(AST subforest, Context complement,
			ArrayList<String> code, int[][] map, int output,
			HashMap<AST, SubtreeInfo> subtrees, SubtreeLocation loc, Subtree parent) {
		if (!subtrees.containsKey(subforest)) {
			subtrees.put(
					subforest,
					new SubtreeInfo(
							subforest.getRoot(),
							getCode(code, map, subforest),
							parent));
		}
		SubtreeInfo info = subtrees.get(subforest);
		info.add(complement, loc, (output));
	}
	
	

	public static void printSubtrees(ArrayList<Subtree> subtreeList, int iteration){
		String fileDir = FileSystem.getSubtreeDir();
		File theDir = new File(fileDir);
		if (!theDir.exists()) {
			theDir.mkdir();  
		}
		String fileName = iteration + ".txt";
		String path = fileDir + fileName;
		try{
		PrintWriter writer = new PrintWriter(path, "UTF-8");
		int cnt = 0;
		for (Subtree subtree : subtreeList){
			writer.println("************************************************");
			writer.println("Index: " + cnt);
			writer.println("Subtree index: " + subtree.getId());
			writer.println("Count: " + subtree.info.getCount());
			writer.println(subtree.info.code);
			cnt++;
		}
		writer.close();
		} catch (Exception e){
			e.printStackTrace();
			throw new RuntimeException("Failed to write equivalence", e);
		}
	}
	
	public static void saveReducedPrograms(Map<ASTID, Program> corrects, 
					Map<AST, Integer> ASTCounts, int iteration){
		
		String fileDir = FileSystem.getReducedOutDir();
		File theDir = new File(fileDir);
		if (!theDir.exists()) {
			theDir.mkdir();  
		}
		String fileName = iteration + ".txt";
		String path = fileDir + fileName;
		final Map<AST,Integer> constCounts = ASTCounts;
		try{
			List<Program> list = new ArrayList<Program>(corrects.values());
			Collections.sort(list, new Comparator<Program>() {
				@Override
				public int compare(Program a, Program b) { // reverse sort
					return constCounts.get(b.ast) - constCounts.get(a.ast);
				}
			});
			
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			for (Program P : list){
				int count = ASTCounts.get(P.ast);
				writer.println("Count: " + count);
				for (String line : P.code) {
					writer.println(line);
				}
				writer.println("\n=================================\n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to write equivalence", e);
		}
		
	}
	
}
