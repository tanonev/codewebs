package models;

import java.util.Collections;
import java.util.List;

public class SubtreeLocation {

	public ASTID astId;
	public List<Integer> nodeIds;
	
	public SubtreeLocation(ASTID astId, List<Integer> nodeIds) {
		this.astId = astId;
		this.nodeIds = nodeIds;

		Collections.sort(this.nodeIds );
	}
	
	@Override
	public String toString() {
		return astId + ": " + nodeIds.toString();
	}
	
}
