package util;

public class IdCounter {

	public int nextId;
	
	public IdCounter() {
		nextId = 0;
	}
	
	public int getNextId() {
		int toReturn = nextId;
		nextId += 1;
		return toReturn;
	}
	
	public void setNextId(int nextId) {
		this.nextId = nextId;
	}
	
}
