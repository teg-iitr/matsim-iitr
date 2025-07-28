package playground.anuj.charDham.analysis;

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
import org.matsim.facilities.Facility;

import java.util.*;

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
    // Used to calculate the duration of the activity.
    private final Map<Id<ActivityFacility>, Map<Id<Person>, Double>> restActivityStartTimes = new HashMap<>();
    // Counts how many unique agents visited a "rest" activity at a given facility (ActivityFacility ID).
    private final Map<Id<ActivityFacility>, Integer> totalRestActivityUsesPerFacility = new HashMap<>();
    // Accumulates the total time (duration) spent by all agents at "rest" activities at a given facility (ActivityFacility ID).
    private final Map<Id<ActivityFacility>, Double> totalRestDurationPerFacility = new HashMap<>();

    // --- Data for Dham activities ---
    // Stores the start time of a Dham activity for each person.
    private final Map<String, Map<Id<Person>, Double>> dhamActivityStartTimes = new HashMap<>();
    // Counts the total number of times a Dham activity was started.
    private final Map<String, Integer> totalDhamActivityUses = new HashMap<>();
    // Accumulates the total time (duration) spent on Dham activities.
    private final Map<String, Double> totalDhamDuration = new HashMap<>();
    // Set of Dham activity types to monitor
    private final Set<String> DHAM_ACTIVITY_TYPES = new HashSet<>(Arrays.asList(
            "Kedarnath", "Gangotri", "Yamunotri", "Badrinath"
    ));

    // --- Data for travel time ---
    // Stores the start time of the current leg for each person. Updated on DepartureEvent.
    private final Map<Id<Person>, Double> personLegStartTimes = new HashMap<>();
    // Accumulates the total travel time for each person across all their legs. Updated on ArrivalEvent.
    private final Map<Id<Person>, Double> totalTravelTimePerPerson = new HashMap<>();
    // Counts the number of legs per mode.
    private final Map<String, Integer> totalLegsPerMode = new HashMap<>();
    // Accumulates total travel time per mode.
    private final Map<String, Double> totalTravelTimePerMode = new HashMap<>();

    // --- Data for nighttime travel ---
    private double totalNightTravelTime_s = 0.0;
    private Set<Id<Person>> agentsTravelingAtNight = new HashSet<>();
    private static final double NIGHT_WINDOW_START_HOUR = 22.0; // 10 PM
    private static final double NIGHT_WINDOW_END_HOUR = 4.0;    // 4 AM (next day)
    private static final int MAX_SIM_DAYS_FOR_OVERLAP_CHECK = 10; // Consistent

    public CharDhamAnalysisEventHandler(String runId, int iteration) {
        this.runId = runId;
        this.iteration = iteration;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if ("rest".equals(event.getActType())) {
            Id<ActivityFacility> activityFacilityId = event.getFacilityId();
            Id<Person> personId = event.getPersonId();

            restActivityStartTimes.computeIfAbsent(activityFacilityId, k -> new HashMap<>()).put(personId, event.getTime());
            totalRestActivityUsesPerFacility.merge(activityFacilityId, 1, Integer::sum);
        } else if (DHAM_ACTIVITY_TYPES.contains(event.getActType())) { // Handle Dham activities
            String actType = event.getActType();
            Id<Person> personId = event.getPersonId();

            dhamActivityStartTimes.computeIfAbsent(actType, k -> new HashMap<>()).put(personId, event.getTime());
            totalDhamActivityUses.merge(actType, 1, Integer::sum);
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if ("rest".equals(event.getActType())) {
            Id<ActivityFacility> activityFacilityId = event.getFacilityId();
            Id<Person> personId = event.getPersonId();

            Map<Id<Person>, Double> personStartTimes = restActivityStartTimes.get(activityFacilityId);
            if (personStartTimes != null) {
                Double startTime = personStartTimes.remove(personId);
                if (startTime != null) {
                    double duration = event.getTime() - startTime;
                    totalRestDurationPerFacility.merge(activityFacilityId, duration, Double::sum);
                }
            }
        } else if (DHAM_ACTIVITY_TYPES.contains(event.getActType())) { // Handle Dham activities
            String actType = event.getActType();
            Id<Person> personId = event.getPersonId();

            Map<Id<Person>, Double> personStartTimes = dhamActivityStartTimes.get(actType);
            if (personStartTimes != null) {
                Double startTime = personStartTimes.remove(personId);
                if (startTime != null) {
                    double duration = event.getTime() - startTime;
                    totalDhamDuration.merge(actType, duration, Double::sum);
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        personLegStartTimes.put(event.getPersonId(), event.getTime());
        totalLegsPerMode.merge(event.getLegMode(), 1, Integer::sum);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Double legStartTime = personLegStartTimes.remove(event.getPersonId());
        if (legStartTime != null) {
            double travelTime = event.getTime() - legStartTime;
            totalTravelTimePerPerson.merge(event.getPersonId(), travelTime, Double::sum);
            totalTravelTimePerMode.merge(event.getLegMode(), travelTime, Double::sum);
            double nightOverlap = calculateNightOverlap(legStartTime, event.getTime());
            if (nightOverlap > 0) {
                totalNightTravelTime_s += nightOverlap;
                agentsTravelingAtNight.add(event.getPersonId());
            }
        }
    }

    @Override
    public void reset(int iteration) {
        restActivityStartTimes.clear();
        totalRestActivityUsesPerFacility.clear();
        totalRestDurationPerFacility.clear();

        dhamActivityStartTimes.clear();
        totalDhamActivityUses.clear();
        totalDhamDuration.clear();

        personLegStartTimes.clear();
        totalTravelTimePerPerson.clear();
        totalLegsPerMode.clear();
        totalTravelTimePerMode.clear();

        totalNightTravelTime_s = 0.0;
        agentsTravelingAtNight.clear();
    }

    /**
     * Aggregates the collected data into a SimulationAnalysisResult object.
     * This method should be called after all events for an iteration have been processed.
     *
     * @return A SimulationAnalysisResult object containing the aggregated metrics.
     */
    public SimulationAnalysisResult getAnalysisResults() {
        SimulationAnalysisResult result = new SimulationAnalysisResult(this.runId, this.iteration);

        // Populate rest activity metrics
        result.totalRestActivityUsesPerFacility.putAll(totalRestActivityUsesPerFacility);
        result.totalRestDurationPerActivityFacility.putAll(totalRestDurationPerFacility);

        // Dham activity metrics
        result.totalDhamActivityUses.putAll(totalDhamActivityUses);
        result.totalDhamDuration.putAll(totalDhamDuration);

        // Populate travel time metrics (raw data)
        result.totalTravelTimePerPerson.putAll(totalTravelTimePerPerson);
        result.totalLegsPerMode.putAll(totalLegsPerMode);
        result.totalTravelTimePerMode.putAll(totalTravelTimePerMode);

        // Populate nighttime travel metric
        result.totalNightTravelTime_s = totalNightTravelTime_s;
        result.agentsTravelingAtNight.addAll(agentsTravelingAtNight);

        // Calculate derived metrics
        result.calculateDerivedMetrics();

        return result;
    }

    /**
     * Calculates the duration of a leg that falls within the defined nighttime window (10 PM - 4 AM).
     * This function iterates through all relevant 24-hour cycles within the leg's duration.
     *
     * @param legStart The start time of the leg in seconds from simulation start.
     * @param legEnd The end time of the leg in seconds from simulation start.
     * @return The total overlap duration in seconds.
     */
    private double calculateNightOverlap(double legStart, double legEnd) {
        double overlap = 0.0;
        double secondsPerDay = 24 * 3600.0;

        // Determine the first and last day relevant to this leg
        int startDay = (int) Math.floor(legStart / secondsPerDay);
        int endDay = (int) Math.ceil(legEnd / secondsPerDay);

        // Iterate through each day that the leg might overlap with the night window
        // We go up to MAX_SIM_DAYS_FOR_OVERLAP_CHECK to cover all possible closure days
        for (int day = startDay; day <= endDay && day < MAX_SIM_DAYS_FOR_OVERLAP_CHECK; day++) {
            double nightWindowStart = (day * secondsPerDay) + (NIGHT_WINDOW_START_HOUR * 3600.0);
            double nightWindowEnd = ((day + 1) * secondsPerDay) + (NIGHT_WINDOW_END_HOUR * 3600.0);

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