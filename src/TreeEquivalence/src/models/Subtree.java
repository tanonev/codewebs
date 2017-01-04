package models;

public class Subtree {

	public AST ast;
	public SubtreeInfo info;
	private int id;
	
	public Subtree(int id, AST ast, SubtreeInfo info) {
		this.id = id;
		this.ast = ast;
		this.info = info;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return ast.hashCode();
	}

	public boolean isSubtreeOf(Subtree other) {
		if (info.root.size > other.info.root.size) return false;
		
		return ast.isSubtreeOf(other.ast);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subtree other = (Subtree) obj;
		if (ast == null) {
			if (other.ast != null)
				return false;
		} else if (!ast.equals(other.ast))
			return false;
		return true;
	}

	public boolean hasOverlap(Subtree second) {
		return info.getOverlapCount(second.info) > 0;
	}

	public int getAnalogyScore(Subtree second) {
		return info.getAnalogyScore(second.info);
	}
	
}
