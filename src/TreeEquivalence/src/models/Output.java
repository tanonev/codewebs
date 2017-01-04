package models;

public class Output {

	private static final int PERFECT = 0;

	
	public static boolean isCorrect(int outputClass) {
		return outputClass == PERFECT;
	}

}
