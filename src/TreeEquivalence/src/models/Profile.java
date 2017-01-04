package models;

import java.util.Map;

import org.apache.commons.math3.distribution.BetaDistribution;

public class Profile {
  public final int correct;
  public final int incorrect;
  
  public static double THRESHOLD = 0.1;
  public static double QUANTILE = 0.5;
  public static double WEIGHT = 0.1;
  
  public Profile(int correct, int incorrect) {
    this.correct = correct;
    this.incorrect = incorrect;
  }
  
  public Profile(Map<Integer, Integer> histogram, int correctClass) {
    int correct = 0, incorrect = 0;
    if (histogram != null) {
      for (Map.Entry<Integer, Integer> e : histogram.entrySet()) {
        if (e.getKey() == correctClass) {
          correct += e.getValue();
        } else {
          incorrect += e.getValue();
        }
      }
    }
    this.correct = correct;
    this.incorrect = incorrect;
  }
  
  public boolean isBugSpike() {
    BetaDistribution beta = new BetaDistribution(correct + .726 * WEIGHT, incorrect + .274 * WEIGHT);
//    return beta.getNumericalMean() < THRESHOLD_PERCENTAGE;
    return beta.inverseCumulativeProbability(QUANTILE) < THRESHOLD;
  }
  
  public String toString() {
    return correct + ":" + incorrect + " (" + (100 * (correct + .726 * WEIGHT)) / (correct + incorrect + 1 * WEIGHT) + "%)";
  }
}
