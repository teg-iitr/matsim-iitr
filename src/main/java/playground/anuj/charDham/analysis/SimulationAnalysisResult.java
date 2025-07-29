package playground.anuj.charDham.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A data class to hold the aggregated analysis results for a single MATSim simulation run.
 */
public class SimulationAnalysisResult {
    public String runId;
    public int iteration;
    public String day;

    // --- Rest Activity Metrics (per activityFacility/link) ---
    // Key: ActivityFacilityId of the activityFacility where the rest activity occurred
    // Value: Number of unique agents who had a rest activity at this activityFacility
    public Map<Id<ActivityFacility>, Integer> totalRestActivityUsesPerFacility = new HashMap<>();
    // Value: Total accumulated duration of all rest activities at this activityFacility (in seconds)
    public Map<Id<ActivityFacility>, Double> totalRestDurationPerActivityFacility = new HashMap<>();
    // Value: Average duration of a rest activity at this activityFacility (in seconds)
    public Map<Id<ActivityFacility>, Double> avgRestDurationPerActivityFacility = new HashMap<>();

    // --- Dham Activity Metrics (per Dham type) ---
    // Total number of times a Dham activity was used (e.g., visit-Kedarnath)
    public Map<String, Integer> totalDhamActivityUses = new HashMap<>();
    // Total accumulated duration of all Dham activities (e.g., visit-Kedarnath)
    public Map<String, Double> totalDhamDuration = new HashMap<>();
    // Average duration of a Dham activity (e.g., visit-Kedarnath)
    public Map<String, Double> avgDhamDuration = new HashMap<>();

    // --- Overall Travel Metrics ---
    public int totalAgentsSimulated; // Total number of unique agents who completed at least one leg
    public double totalTravelTime_s; // Sum of all leg travel times across all agents
    public double averageTravelTime_s; // Average travel time per agent (totalTravelTime_s / totalAgentsSimulated)
    public double minTravelTime_s = Double.MAX_VALUE; // Minimum total travel time for any agent
    public double maxTravelTime_s = Double.MIN_VALUE; // Maximum total travel time for any agent
    public int totalLegs; // Total number of legs completed by all agents
    public double averageLegTravelTime_s; // Average travel time per leg (totalTravelTime_s / totalLegs)

    // --- Nighttime Travel Metrics ---
    public double totalNightTravelTime_s;
    public int numAgentsNightTravel;

    // --- Other potentially useful metrics ---
    public Map<Id<Person>, Double> totalTravelTimePerPerson = new HashMap<>();
    public Map<String, Integer> totalLegsPerMode = new HashMap<>();
    public Map<String, Double> totalTravelTimePerMode = new HashMap<>();
    public Map<String, Double> avgTravelTimePerMode = new HashMap<>();
    public Set<Id<Person>> agentsTravelingAtNight = new HashSet<>();

    // Constructor to initialize with run details
    public SimulationAnalysisResult(String runId, String day, int iteration) {
        this.runId = runId;
        this.day = day;
        this.iteration = iteration;
    }


    /**
     * Calculates derived metrics after all raw data has been collected/aggregated.
     */
    public void calculateDerivedMetrics() {
        // Calculate average rest duration per activityFacility
        for (Map.Entry<Id<ActivityFacility>, Double> entry : totalRestDurationPerActivityFacility.entrySet()) {
            Id<ActivityFacility> activityFacilityId = entry.getKey();
            Double totalDuration = entry.getValue();
            Integer totalUses = totalRestActivityUsesPerFacility.get(activityFacilityId);
            if (totalUses != null && totalUses > 0) {
                avgRestDurationPerActivityFacility.put(activityFacilityId, totalDuration / totalUses);
            } else {
                avgRestDurationPerActivityFacility.put(activityFacilityId, 0.0);
            }
        }

        // Calculate average Dham activity duration per type
        for (Map.Entry<String, Double> entry : totalDhamDuration.entrySet()) {
            String dhamType = entry.getKey();
            Double totalDuration = entry.getValue();
            Integer totalUses = totalDhamActivityUses.get(dhamType);
            if (totalUses != null && totalUses > 0) {
                avgDhamDuration.put(dhamType, totalDuration / totalUses);
            } else {
                avgDhamDuration.put(dhamType, 0.0);
            }
        }

        // Calculate overall travel metrics
        this.totalAgentsSimulated = (int) totalTravelTimePerPerson.keySet().stream()
                .filter(p -> totalTravelTimePerPerson.get(p) != null && totalTravelTimePerPerson.get(p) > 0)
                .count();

        this.totalTravelTime_s = totalTravelTimePerPerson.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalAgentsSimulated > 0) {
            this.averageTravelTime_s = this.totalTravelTime_s / this.totalAgentsSimulated;
            this.minTravelTime_s = totalTravelTimePerPerson.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            this.maxTravelTime_s = totalTravelTimePerPerson.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        } else {
            this.averageTravelTime_s = 0.0;
            this.minTravelTime_s = 0.0;
            this.maxTravelTime_s = 0.0;
        }

        this.totalLegs = totalLegsPerMode.values().stream().mapToInt(Integer::intValue).sum();
        if (this.totalLegs > 0) {
            this.averageLegTravelTime_s = this.totalTravelTime_s / this.totalLegs;
        } else {
            this.averageLegTravelTime_s = 0.0;
        }

        // Calculate average travel time per mode
        for (String mode : totalLegsPerMode.keySet()) {
            int numLegs = totalLegsPerMode.getOrDefault(mode, 0);
            double totalTime = totalTravelTimePerMode.getOrDefault(mode, 0.0);
            if (numLegs > 0) {
                avgTravelTimePerMode.put(mode, totalTime / numLegs);
            } else {
                avgTravelTimePerMode.put(mode, 0.0);
            }
        }

        // Calculate number of unique agents traveling at night
        this.numAgentsNightTravel = this.agentsTravelingAtNight.size();
    }

    /**
     * Aggregates data from another SimulationAnalysisResult into this one.
     * Useful for summing up day-wise results into an "all_days" result.
     * @param other The other SimulationAnalysisResult to aggregate.
     */
    public void aggregate(SimulationAnalysisResult other) {
        // Aggregate rest activity metrics
        other.totalRestActivityUsesPerFacility.forEach((facilityId, count) ->
                this.totalRestActivityUsesPerFacility.merge(facilityId, count, Integer::sum));
        other.totalRestDurationPerActivityFacility.forEach((facilityId, duration) ->
                this.totalRestDurationPerActivityFacility.merge(facilityId, duration, Double::sum));

        // Aggregate Dham activity metrics
        other.totalDhamActivityUses.forEach((dhamType, count) ->
                this.totalDhamActivityUses.merge(dhamType, count, Integer::sum));
        other.totalDhamDuration.forEach((dhamType, duration) ->
                this.totalDhamDuration.merge(dhamType, duration, Double::sum));

        // Aggregate travel time metrics (raw data for recalculation)
        other.totalTravelTimePerPerson.forEach((personId, time) ->
                this.totalTravelTimePerPerson.merge(personId, time, Double::sum));
        other.totalLegsPerMode.forEach((mode, count) ->
                this.totalLegsPerMode.merge(mode, count, Integer::sum));
        other.totalTravelTimePerMode.forEach((mode, time) ->
                this.totalTravelTimePerMode.merge(mode, time, Double::sum));

        // Aggregate nighttime travel
        this.totalNightTravelTime_s += other.totalNightTravelTime_s;
        this.agentsTravelingAtNight.addAll(other.agentsTravelingAtNight); // Add all unique agents
    }
}