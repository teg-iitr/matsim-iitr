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
    // Value: Set of unique persons who had a rest activity at this activityFacility during this period.
    public Map<Id<ActivityFacility>, Set<Id<Person>>> uniqueRestActivityPersonsPerFacility = new HashMap<>();
    // Derived: Count of unique persons who had a rest activity at this facility
    public Map<Id<ActivityFacility>, Integer> totalRestActivityUsesPerFacilityCount = new HashMap<>();


    // --- Dham Activity Metrics (per Dham type) ---
    // Value: Set of unique persons who had a Dham activity of this type during this period.
    public Map<String, Set<Id<Person>>> uniqueDhamActivityPersons = new HashMap<>();
    // Derived: Count of unique persons who had a Dham activity of this type
    public Map<String, Integer> totalDhamActivityUsesCount = new HashMap<>();


    // --- Overall Travel Metrics (now calculated per day/period of this result object) ---
    public int totalAgentsSimulated; // Total number of unique agents who completed at least one leg in this period
    public double totalTravelTime_s; // Sum of all leg travel times across all agents in this period
    public double averageTravelTime_s; // Average travel time per agent (totalTravelTime_s / totalAgentsSimulated) in this period
    public double minTravelTime_s = Double.MAX_VALUE; // Minimum total travel time for any agent in this period
    public double maxTravelTime_s = Double.MIN_VALUE; // Maximum total travel time for any agent in this period
    public int totalLegs; // Total number of legs completed by all agents in this period
    public double averageLegTravelTime_s; // Average travel time per leg (totalTravelTime_s / totalLegs) in this period

    // --- Nighttime Travel Metrics ---
    public double totalNightTravelTime_s;
    public int numAgentsNightTravel;

    // --- Other potentially useful metrics ---
    // This map now holds total travel time per person for the specific 'day' or 'all_days' this result object represents.
    public Map<Id<Person>, Double> totalTravelTimePerPerson = new HashMap<>();
    public Map<String, Integer> totalLegsPerMode = new HashMap<>();
    public Map<String, Double> totalTravelTimePerMode = new HashMap<>();
    public Map<String, Double> avgTravelTimePerMode = new HashMap<>();
    public Map<String, Double> percentageLegsPerMode = new HashMap<>();

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
        // Calculate unique person counts for rest activities
        for (Map.Entry<Id<ActivityFacility>, Set<Id<Person>>> entry : uniqueRestActivityPersonsPerFacility.entrySet()) {
            totalRestActivityUsesPerFacilityCount.put(entry.getKey(), entry.getValue().size());
        }

        // Calculate unique person counts for Dham activities
        for (Map.Entry<String, Set<Id<Person>>> entry : uniqueDhamActivityPersons.entrySet()) {
            totalDhamActivityUsesCount.put(entry.getKey(), entry.getValue().size());
        }

        // Calculate overall travel metrics for THIS period (day or all_days)
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

        // Calculate total non-walk legs for percentage calculation
        int totalNonWalkLegs = 0;
        for (Map.Entry<String, Integer> entry : totalLegsPerMode.entrySet()) {
            if (!"walk".equalsIgnoreCase(entry.getKey())) {
                totalNonWalkLegs += entry.getValue();
            }
        }

        // Calculate average travel time per mode and percentage of legs per mode
        for (String mode : totalLegsPerMode.keySet()) {
            int numLegs = totalLegsPerMode.getOrDefault(mode, 0);
            double totalTime = totalTravelTimePerMode.getOrDefault(mode, 0.0);
            if (numLegs > 0) {
                avgTravelTimePerMode.put(mode, totalTime / numLegs);
            } else {
                avgTravelTimePerMode.put(mode, 0.0);
            }

            // NEW: Calculate percentage of legs per mode, excluding 'walk' from the total base
            if ("walk".equalsIgnoreCase(mode)) {
                percentageLegsPerMode.put(mode, 0.0); // Walk mode explicitly gets 0% for this metric
            } else {
                if (totalNonWalkLegs > 0) {
                    percentageLegsPerMode.put(mode, (double) numLegs / totalNonWalkLegs * 100.0);
                } else {
                    percentageLegsPerMode.put(mode, 0.0);
                }
            }
        }

        // Calculate number of unique agents traveling at night
        this.numAgentsNightTravel = this.agentsTravelingAtNight.size();
    }

    /**
     * Aggregates data from another SimulationAnalysisResult into this one.
     * Useful for summing up day-wise results into an "all_days" result.
     *
     * @param other The other SimulationAnalysisResult to aggregate.
     */
    public void aggregate(SimulationAnalysisResult other) {
        // Aggregate unique rest activity persons (merge sets)
        other.uniqueRestActivityPersonsPerFacility.forEach((facilityId, personsSet) ->
                this.uniqueRestActivityPersonsPerFacility.computeIfAbsent(facilityId, k -> new HashSet<>()).addAll(personsSet));

        // Aggregate unique Dham activity persons (merge sets)
        other.uniqueDhamActivityPersons.forEach((dhamType, personsSet) ->
                this.uniqueDhamActivityPersons.computeIfAbsent(dhamType, k -> new HashSet<>()).addAll(personsSet));

        // Aggregate travel time metrics (raw data for recalculation)
        // This merges the total travel time per person from the 'other' daily result into 'this' (all_days) result.
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