package models;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import util.FileSystem;
import util.IdCounter;

import minions.Jasonizer;

/**
 * Class: Equivalence
 * This class represents a set of forests that are equivalent to one another.
 * It also contains a set of contexts where we know that the student needed
 * to put a piece of code that is analogous to this equivalence class (and
 * some problematic subtrees that we have seen in place of good ones).
 */
public class Equivalence {

	// Static ID counter
	private static int idCounter = 0;
	
	// Pointers to all the underlying subforests
	private Set<Subforest> subforests;
	
	// Contexts where you need an instance of this equivalence class
	private Set<Context> necessaryContexts;
	
	// Versions of the equivalence class that don't work
	private Set<Subforest> bugs;
	
	// A unique ID
	private int id;
	
	// A name given by a human
	private String name;
	
	// The priority of the equivalence class
	private int priority;

	private String type;
	
	// Subtree Id counter
	private IdCounter subtreeIdCounter;
	
	public Equivalence(Set<Subforest> analogies, String name) {
		this.subforests = analogies;
		this.name = name.split("_")[1];
		this.priority = Integer.parseInt(name.split("_")[0]);
		this.necessaryContexts = new HashSet<Context>();
		this.id = idCounter;
		idCounter++;
		subtreeIdCounter = new IdCounter();
		this.type = "";
	}
	
	public Equivalence(Set<Subforest> analogies, String name, String type) {
		this.subforests = analogies;
		this.name = name.split("_")[1];
		this.priority = Integer.parseInt(name.split("_")[0]);
		this.necessaryContexts = new HashSet<Context>();
		this.id = idCounter;
		idCounter++;
		subtreeIdCounter = new IdCounter();
		this.type = type;
	}
	
	public void addSubforests(Set<Subforest> analogies) {
		for(Subforest f : analogies) {
			subforests.add(f);
		}
	}

	public void outputCodeStrings() {
		for(Subforest f : subforests) {
			System.out.println(f.getCodeString());
			System.out.println("-----");
		}
	}
	
	public static Equivalence loadFromFile(String name,
			Set<String> keywords, String dir) {
		String eqDir = dir + "/" + name;
		Equivalence eq = new Equivalence(null, name);
		eq.loadSubforests(eqDir, keywords);
		eq.loadMetadata(eqDir);
		return eq;
	}
	
	public void setSubtreeIdCounter(int value)  {
		subtreeIdCounter.setNextId(value);
	}

	public void saveToFile(String dir) {
		updateCounter(dir);
		writeSubforests(dir);
		writeMetadata(dir);
		//writeContexts(equivalenceDirName);
		//writeBugs(equivalenceDirName);
	}

	private void updateCounter(String dir) {
		// These next 8 lines look up what id to save the
		// next subtree so as to not clobber any previous
		// subtrees
		String dirName = getEquivalenceDirName(dir);
		String subforestDirName = dirName + "/subforest";
		File folder = new File(subforestDirName);
		File[] listOfFiles = folder.listFiles();
		int nextId = 0;
		if (listOfFiles != null) {
			nextId = listOfFiles.length/2;
		}
		subtreeIdCounter.setNextId(nextId);
	}

	private String getEquivalenceDirName(String eqDir) {
		String equivalenceDirName = eqDir + "/" + priority + "_"+ name;
		return equivalenceDirName;
	}

	public String getCodeString() {
		return "{" + id + "}";
	}
	
	public String getName() {
		return name;
	}

	public Set<Subforest> getSubforests() {
		return subforests;
	}
	
	public Set<Context> getContexts() {
	  return necessaryContexts;
	}

	public int getId() {
		throw new RuntimeException("not done");
	}
	
	public String getType() {
		return type;
	}
	
	public boolean containsSubforest(Subforest subforest) {
		return subforests.contains(subforest);
	}
	
	public boolean contextRequiresInstance(Context context) {
		return necessaryContexts.contains(context);
	}
	
	public boolean addNecessaryContext(Context context) {
		return necessaryContexts.add(context);
	}
	
	public boolean addSubforest(Subforest subforest) {
		return subforests.add(subforest);
	}
	
	public void addBug(Subforest subforest) {
		//bugs.add(subforest);
	}
	
	public void saveSubforest(Subforest subforest, String dir) {
		updateCounter(dir);
		writeSubforest(subforest, dir);
	}
	
	public Set<Context> getNecessaryContexts() {
		return necessaryContexts;
	}

	public int getPriority() {
		return priority;
	}
	
	//---------------------- Private parts ---------------//
	
	private void writeSubforests(String dir) {
		for(Subforest forest : subforests) {
			writeSubforest(forest, dir);
		}
	}
	
	// This code is a little repeated with programs save to file...
	private void writeSubforest(Subforest f, String dir) {
		String dirName = getEquivalenceDirName(dir);
		String subforestDirName = dirName + "/subforest";
		new File(subforestDirName).mkdirs();
		int subtreeId = subtreeIdCounter.getNextId();
		JSONObject json = Jasonizer.jsonify(f);
		String code = f.getCodeString();
		writeJsonToFile(subforestDirName, json, subtreeId);
		writeCodeToFile(subforestDirName, code, subtreeId);
	}
	
	private void writeMapToFile(String dirName, String map, int id) {
		FileSystem.createFile(dirName, id + ".map", map);
		throw new RuntimeException("deprecated");
	}
	
	private void writeCodeToFile(String dirName, String code, int id) {
		FileSystem.createFile(dirName, id + ".txt", code);
	}

	private void writeJsonToFile(String dirName, JSONObject json, int id) {
		FileSystem.createFile(dirName, id + ".json", json.toString(4));
	}

	private void loadSubforests(String eqDirName, Set<String> keywords) {
		String subforestDirName = eqDirName + "/subforest";
		File subforestDir = new File(subforestDirName);
		subforests = new HashSet<Subforest>();
		int maxId = 0;
		for(File f : subforestDir.listFiles()) {
			String fileName = f.getName();
			String idString = fileName.split("\\.")[0];
			String type = fileName.split("\\.")[1];
			if(!type.equals("json")) continue;
			int id = Integer.parseInt(idString);
			maxId = Math.max(id, maxId);
			String path = f.getAbsolutePath();
			Subforest t = Subforest.loadFromFile(path, keywords);
			subforests.add(t);
		}
		//subtreeIdCounter.setNextId(maxId + 1);
	}

	private void writeMetadata(String dir) {
		String dirName = getEquivalenceDirName(dir);
		JSONObject metadata = new JSONObject();
		metadata.put("type", type);
		FileSystem.createFile(dirName, "metadata.txt", metadata.toString());
	}

	private void loadMetadata(String dirName) {
		JSONObject metadata = FileSystem.loadJson(dirName+"/"+"metadata.txt");
		this.type = metadata.getString("type");
	}
}
