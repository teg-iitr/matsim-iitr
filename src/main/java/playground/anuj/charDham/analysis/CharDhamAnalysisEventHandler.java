package playground.anuj.charDham.analysis;

import org.matsim.analysis.LegTimesModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

import static playground.anuj.charDham.analysis.CharDhamOutputAnalyzer.DHAM_ACTIVITY_TYPES;

/**
 * An event handler to collect various statistics from MATSim simulation events,
 * focusing on rest activities and travel times for Char Dham Yatra.
 */
public class CharDhamAnalysisEventHandler implements
        ActivityStartEventHandler, ActivityEndEventHandler,
        PersonDepartureEventHandler, PersonArrivalEventHandler, EventHandler {

    private final String runId;
    private final int iteration;

    // --- Data for rest activities ---
    // Stores the start time of a "rest" activity for each person at a specific facility (ActivityFacility ID).
    // Used to calculate the duration of the activity, even if not outputted.
    private final Map<Integer, Map<Id<ActivityFacility>, Map<Id<Person>, Double>>> restActivityStartTimes = new HashMap<>();
    // Tracks unique persons performing a "rest" activity at a given facility (ActivityFacility ID) on a specific day, based on activity END.
    private final Map<Integer, Map<Id<ActivityFacility>, Set<Id<Person>>>> uniqueRestActivityPersonsPerFacility = new HashMap<>();
    // Accumulates the total time (duration) spent by all agents at "rest" activities at a given facility (ActivityFacility ID).
    // Kept for internal calculation, but not passed to SimulationAnalysisResult as per request.
    private final Map<Integer, Map<Id<ActivityFacility>, Double>> totalRestDurationPerFacility = new HashMap<>();

    // --- Data for Dham activities ---
    private final Map<Integer, Map<String, Map<Id<Person>, Double>>> dhamActivityStartTimes = new HashMap<>();
    // Tracks unique persons performing a "Dham" activity of a specific type on a specific day, based on activity END.
    private final Map<Integer, Map<String, Set<Id<Person>>>> uniqueDhamActivityPersons = new HashMap<>();
    // Accumulates the total time (duration) spent by all agents at "Dham" activities.
    // Kept for internal calculation, but not passed to SimulationAnalysisResult as per request.
    private final Map<Integer, Map<String, Double>> totalDhamDuration = new HashMap<>();

    // --- Data for travel time ---
    private final Map<Id<Person>, Double> personLegStartTimes = new HashMap<>(); // Not day-indexed, tracks current leg
    // Changed to daily tracking for total travel time per person
    private final Map<Integer, Map<Id<Person>, Double>> totalTravelTimePerPersonDaily = new HashMap<>(); // Daily for person
    private final Map<Integer, Map<String, Integer>> totalLegsPerMode = new HashMap<>();
    private final Map<Integer, Map<String, Double>> totalTravelTimePerMode = new HashMap<>();

    // --- Data for nighttime travel ---
    private final Map<Integer, Double> totalNightTravelTime_s = new HashMap<>();
    private final Map<Integer, Set<Id<Person>>> agentsTravelingAtNight = new HashMap<>();

    private static final double NIGHT_WINDOW_START_HOUR = 22.0; // 10 PM
    private static final int MAX_SIM_DAYS_FOR_OVERLAP_CHECK = 10; // Consistent

    public CharDhamAnalysisEventHandler(String runId, int iteration) {
        this.runId = runId;
        this.iteration = iteration;
    }

    /**
     * Returns the current day, 1-indexed.
     * Day 1: 0:00 - 23:59:59
     * Day 2: 24:00:00 - 47:59:59
     * and so on.
     *
     * @param time The current simulation time in seconds.
     * @return The 1-indexed day.
     */
    private int getCurrentDay(double time) {
        return (int) Math.floor(time / (24 * 3600.0)) + 1;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        int day = getCurrentDay(event.getTime());
        Id<Person> personId = event.getPersonId();

        if ("rest".equals(event.getActType())) {
            Id<ActivityFacility> activityFacilityId = event.getFacilityId();
            // Store start time for duration calculation
            restActivityStartTimes.computeIfAbsent(day, k -> new HashMap<>())
                    .computeIfAbsent(activityFacilityId, k -> new HashMap<>()).put(personId, event.getTime());
        } else if (DHAM_ACTIVITY_TYPES.contains(event.getActType())) {
            String actType = event.getActType();
            // Store start time for duration calculation
            dhamActivityStartTimes.computeIfAbsent(day, k -> new HashMap<>())
                    .computeIfAbsent(actType, k -> new HashMap<>()).put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        int day = getCurrentDay(event.getTime());
        Id<Person> personId = event.getPersonId();

        if ("rest".equals(event.getActType())) {
            Id<ActivityFacility> activityFacilityId = event.getFacilityId();
            Map<Id<Person>, Double> personStartTimes = restActivityStartTimes.getOrDefault(day, Collections.emptyMap()).get(activityFacilityId);
            if (personStartTimes != null) {
                Double startTime = personStartTimes.remove(personId);
                if (startTime != null) {
                    double duration = event.getTime() - startTime;
                    // Accumulate total duration (kept for internal calculation)
                    totalRestDurationPerFacility.computeIfAbsent(day, k -> new HashMap<>())
                            .merge(activityFacilityId, duration, Double::sum);
                    // Track unique persons based on activity END
                    uniqueRestActivityPersonsPerFacility.computeIfAbsent(day, k -> new HashMap<>())
                            .computeIfAbsent(activityFacilityId, k -> new HashSet<>()).add(personId);
                }
            }
        } else if (DHAM_ACTIVITY_TYPES.contains(event.getActType())) {
            String actType = event.getActType();
            Map<Id<Person>, Double> personStartTimes = dhamActivityStartTimes.getOrDefault(day, Collections.emptyMap()).get(actType);
            if (personStartTimes != null) {
                Double startTime = personStartTimes.remove(personId);
                if (startTime != null) {
                    double duration = event.getTime() - startTime;
                    // Accumulate total duration (kept for internal calculation)
                    totalDhamDuration.computeIfAbsent(day, k -> new HashMap<>())
                            .merge(actType, duration, Double::sum);
                    // Track unique persons based on activity END
                    uniqueDhamActivityPersons.computeIfAbsent(day, k -> new HashMap<>())
                            .computeIfAbsent(actType, k -> new HashSet<>()).add(personId);
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        personLegStartTimes.put(event.getPersonId(), event.getTime());
        int day = getCurrentDay(event.getTime());
        totalLegsPerMode.computeIfAbsent(day, k -> new HashMap<>())
                .merge(event.getLegMode(), 1, Integer::sum);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Double legStartTime = personLegStartTimes.remove(event.getPersonId());
        if (legStartTime != null) {
            double travelTime = event.getTime() - legStartTime;
            int day = getCurrentDay(event.getTime()); // Get the day of arrival

            // Update daily travel time for this person
            totalTravelTimePerPersonDaily.computeIfAbsent(day, k -> new HashMap<>())
                    .merge(event.getPersonId(), travelTime, Double::sum);

            totalTravelTimePerMode.computeIfAbsent(day, k -> new HashMap<>())
                    .merge(event.getLegMode(), travelTime, Double::sum);

            double nightOverlap = calculateNightOverlap(legStartTime, event.getTime());
            if (nightOverlap > 0) {
                totalNightTravelTime_s.merge(day, nightOverlap, Double::sum);
                agentsTravelingAtNight.computeIfAbsent(day, k -> new HashSet<>())
                        .add(event.getPersonId());
            }
        }
    }

    @Override
    public void reset(int iteration) {
        restActivityStartTimes.clear();
        uniqueRestActivityPersonsPerFacility.clear();
        totalRestDurationPerFacility.clear(); // Clear internal map

        dhamActivityStartTimes.clear();
        uniqueDhamActivityPersons.clear();
        totalDhamDuration.clear(); // Clear internal map

        personLegStartTimes.clear();
        totalTravelTimePerPersonDaily.clear(); // Clear the daily map
        totalLegsPerMode.clear();
        totalTravelTimePerMode.clear();

        totalNightTravelTime_s.clear();
        agentsTravelingAtNight.clear();
    }

    /**
     * Aggregates the collected data into a map of SimulationAnalysisResult objects,
     * one for each day and one for "all_days".
     *
     * @return A Map where keys are "dayX" or "all_days", and values are SimulationAnalysisResult objects.
     */
    public Map<String, SimulationAnalysisResult> getAnalysisResults() {
        Map<String, SimulationAnalysisResult> results = new HashMap<>();
        SimulationAnalysisResult allDaysResult = new SimulationAnalysisResult(this.runId, "all_days", this.iteration);

        // Get all unique days for which data was collected
        Set<Integer> allDays = new HashSet<>();
        allDays.addAll(uniqueRestActivityPersonsPerFacility.keySet()); // Use unique persons map for day keys
        allDays.addAll(uniqueDhamActivityPersons.keySet()); // Use unique persons map for day keys
        allDays.addAll(totalLegsPerMode.keySet());
        allDays.addAll(totalNightTravelTime_s.keySet());
        allDays.addAll(totalTravelTimePerPersonDaily.keySet()); // Include days with only travel

        // Process each day
        for (Integer day : allDays) {
            SimulationAnalysisResult dailyResult = new SimulationAnalysisResult(this.runId, "day" + day, this.iteration);

            // Populate rest activity metrics for the day (pass the sets directly)
            dailyResult.uniqueRestActivityPersonsPerFacility.putAll(uniqueRestActivityPersonsPerFacility.getOrDefault(day, Collections.emptyMap()));
            // totalRestDurationPerFacility is NOT passed to dailyResult as per request

            // Populate Dham activity metrics for the day (pass the sets directly)
            dailyResult.uniqueDhamActivityPersons.putAll(uniqueDhamActivityPersons.getOrDefault(day, Collections.emptyMap()));
            // totalDhamDuration is NOT passed to dailyResult as per request

            // Populate travel time metrics for the day
            dailyResult.totalTravelTimePerPerson.putAll(totalTravelTimePerPersonDaily.getOrDefault(day, Collections.emptyMap()));
            dailyResult.totalLegsPerMode.putAll(totalLegsPerMode.getOrDefault(day, Collections.emptyMap()));
            dailyResult.totalTravelTimePerMode.putAll(totalTravelTimePerMode.getOrDefault(day, Collections.emptyMap()));

            // Populate nighttime travel metric for the day
            dailyResult.totalNightTravelTime_s = totalNightTravelTime_s.getOrDefault(day, 0.0);
            dailyResult.agentsTravelingAtNight.addAll(agentsTravelingAtNight.getOrDefault(day, Collections.emptySet()));

            dailyResult.calculateDerivedMetrics();
            results.put("day" + day, dailyResult);

            // Aggregate into allDaysResult
            allDaysResult.aggregate(dailyResult);
        }

        // Finalize allDaysResult's derived metrics (important for correct averages)
        allDaysResult.calculateDerivedMetrics();
        results.put("all_days", allDaysResult);

        return results;
    }

    /**
     * Calculates the duration of a leg that falls within the defined nighttime window (10 PM - 12 AM of the same day).
     * This function iterates through all relevant 24-hour cycles within the leg's duration.
     *
     * @param legStart The start time of the leg in seconds from simulation start.
     * @param legEnd   The end time of the leg in seconds from simulation start.
     * @return The total overlap duration in seconds.
     */
    private double calculateNightOverlap(double legStart, double legEnd) {
        double overlap = 0.0;
        double secondsPerDay = 24 * 3600.0;

        // Determine the first and last day (0-indexed) relevant to this leg
        int startDay0Indexed = (int) Math.floor(legStart / secondsPerDay);
        int endDay0Indexed = (int) Math.floor(legEnd / secondsPerDay);

        // Iterate through each 0-indexed day that the leg might overlap with the night window (22:00 - 24:00)
        // We go up to MAX_SIM_DAYS_FOR_OVERLAP_CHECK to cover all possible closure days
        for (int currentDay0Indexed = startDay0Indexed; currentDay0Indexed <= endDay0Indexed && currentDay0Indexed < MAX_SIM_DAYS_FOR_OVERLAP_CHECK; currentDay0Indexed++) {
            double nightWindowStart = (currentDay0Indexed * secondsPerDay) + (NIGHT_WINDOW_START_HOUR * 3600.0);
            double nightWindowEnd = (currentDay0Indexed * secondsPerDay) + (24 * 3600.0); // End of the current day (24:00)

            // Calculate the overlap between the leg and this specific night window
            double segmentStart = Math.max(legStart, nightWindowStart);
            double segmentEnd = Math.min(legEnd, nightWindowEnd);

            if (segmentStart < segmentEnd) { // There is an overlap
                overlap += (segmentEnd - segmentStart);
            }
        }
        return overlap;
    }
}