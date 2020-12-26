package playground.nidhi.practice.eventHandlingPract;

import java.util.SortedMap;
import java.util.SortedSet;

public interface ModeShare {
    SortedSet<String> getUsedModes();
    SortedMap<String, Integer> getModeToNumberOfLegs();
    SortedMap<String, Double> getModeToPercentOfLegs();
    void writeResults(String outputFile);
}
