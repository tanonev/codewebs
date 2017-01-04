package models;

public class ASTWithComplement {
	public AST ast;
	public Context complement;
	public int start, end;

	public ASTWithComplement(AST ast, Context complement, int start, int end) {
		this.ast = ast;
		this.complement = complement;
		this.start = start;
		this.end = end;
	}
}