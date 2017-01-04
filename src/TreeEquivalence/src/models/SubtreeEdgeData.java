package models;

import java.util.ArrayList;

public class SubtreeEdgeData {

	private static final int ANALOGTHRESHOLD = 10;
	private static final float EQUIVALENCETHRESHOLD = (float).9;
	
	public Subtree A;
	public Subtree B;
	ArrayList<Context> complements = new ArrayList<Context>();
	ArrayList<Boolean> equivalentOutputs = new ArrayList<Boolean>();
		
	public SubtreeEdgeData(Subtree A, Subtree B) {
		this.A = A;
		this.B = B;
	}
		
	// maybe not so obvious that A and B would share the same complement since
	// we could be adding the edge due to a transitive relation...
	public void insert(Context complement) {
		complements.add(complement);
	
		assert(A.info.outputMap.containsKey(complement));
		assert(B.info.outputMap.containsKey(complement));
		
		int outputA = A.info.outputMap.get(complement);
		int outputB = B.info.outputMap.get(complement);
		equivalentOutputs.add(outputA == outputB);
	}
	
	public boolean areAnalogous() {
		return (complements.size() >= ANALOGTHRESHOLD);
	}
	
	public boolean areEquivalent() {
		int count = 0;
		for (boolean equiv : equivalentOutputs) {
			if (equiv == true) count += 1;
		}
		float ratio = (float)(count) / equivalentOutputs.size();
		return (ratio >= EQUIVALENCETHRESHOLD) && areAnalogous();
	}
}