package models;


/**
 * Class: CodeBlock
 * A code block is a pairing between a subforest and a context. It represents
 * a chunck of code from a program (and there is exactly one for every chunk
 * of code in every program).
 */
public class CodeBlock {

	// The codeblock id
	private int id;
	
	// Pointers to the two parts that make up a code block
	private Subforest subforest;
	private Context complement;
	
	// A pointer to the program where this code block exists.
	private Program program;
	
	// Code blocks can either be buggy or not buggy 
	private boolean buggy = false;

	
	public CodeBlock(Program program, Subforest subforest, Context context, int id) {
		this.program = program;
		this.subforest = subforest;
		this.complement = context;
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public Context getContext() {
		return complement;
	}
	
	public Subforest getSubforest() {
		return subforest;
	}
	
	public Program getProgram() {
		return program;
	}

	public int size() {
		return subforest.size();
	}

	public void markAsBuggy(Equivalence eq) {
		buggy = true;
		program.markAsBuggy(this, eq);
		System.out.println("bug found!");
	}

	public void markEquivalence(Equivalence eq) {
		subforest.markEquivalence(eq);
	}
	
	public CodeBlock getReduced(Equivalence eq) {
		return new CodeBlock(program, subforest.getReduced(eq), complement, id);
	}

}
