package models;

import java.util.HashMap;
import java.util.Map;

import models.ast.Forest;
import models.ast.Node;

/**
 * Class: Context
 * This is a piece of code around a subforest. By definition it is the original
 * tree (from which the subforest came) with a symbolic node replacing the
 * subforest. In practice, we represent it as the list of postorder nodes
 * before and after the subforest. This representation is not perfect in that
 * it is possible for two different contexts to have the same left and right
 * forests.
 */
public class Context {

    // To save memory these are pointers.
    private Forest left;
    private Forest right;

    // Cache this. But be wary
    private Integer hash;
    private int size;

    public Context(Forest left, Forest right, Program program,
            int start, int end) {
        this.left = left;
        this.right = right;
        this.size = end - start;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (hashCode() != obj.hashCode())
            return false;

        Context other = (Context) obj;

        if (!left.equals(other.left))
            return false;
        return right.shiftEquals(other.right, size, other.size);
    }

    public boolean isIsomorphic(Context other) {
        if (!this.equals(other)) return false;
        Map<String, String> association = new HashMap<String, String>();
        if (!left.checkValidIdentifierMap(other.left, association)) return false;
        return right.checkValidIdentifierMap(other.right, association);
    }

    public void setHashCode(int hash) {
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        if (hash != null) {
            return hash;
        }

        int hashCode = left.hashCode();
        // This is a way to include a symbolic epsilon node in between left
        // and right.
        for (int i = 0; i < right.getSize(); i++) {
            hashCode *= 31;
        }
        int hash = 31 * hashCode + right.hashCode();
        this.hash = hash;
        return hash;
    }

    public Node getRoot() {return right.getNode(right.getSize() - 1);}

    public String toString() {
        return size + " " + left + " " + right;
    }

    public Context reduce(Equivalence equivalence) {
    	// We can do this safely because we *know* that reduce on a forest does NOT mutate the original forest.
    	// Do not do this with other things like Subforest.
    	Context reduced = new Context(left.reduce(equivalence), right.reduce(equivalence), null, 0, size);
        return reduced;
    }
}
