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

import java.util.*;

import static playground.anuj.charDham.analysis.CharDhamOutputAnalyzer.DHAM_ACTIVITY_TYPES;

public class CharDhamAnalysisEventHandler implements
        ActivityStartEventHandler, ActivityEndEventHandler,
        PersonDepartureEventHandler, PersonArrivalEventHandler, EventHandler {

    private final String runId;
    private final int iteration;

    // --- Data for rest activities ---
    private final Map<Id<ActivityFacility>, Map<Id<Person>, Double>> restActivityStartTimes = new HashMap<>();
    private final Map<Integer, Map<Id<ActivityFacility>, Set<Id<Person>>>> uniqueRestActivityPersonsPerFacility = new HashMap<>();

    // --- Data for Dham activities ---
    private final Map<String, Map<Id<Person>, Double>> dhamActivityStartTimes = new HashMap<>();
    private final Map<Integer, Map<String, Set<Id<Person>>>> uniqueDhamActivityPersons = new HashMap<>();

    // --- Data for travel time ---
    private final Map<Id<Person>, Double> personLegStartTimes = new HashMap<>();
    private final Map<Integer, Map<Id<Person>, Double>> totalTravelTimePerPersonDaily = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> totalLegsPerMode = new HashMap<>();
    private final Map<Integer, Map<String, Double>> totalTravelTimePerMode = new HashMap<>();

    // --- Data for nighttime travel ---
    private final Map<Integer, Double> totalNightTravelTime_s = new HashMap<>();
    private final Map<Integer, Set<Id<Person>>> agentsTravelingAtNight = new HashMap<>();

    // MODIFICATION START: Define constants for the full night window.
    private static final double NIGHT_WINDOW_LATE_START_HOUR = 22.0; // 10 PM
    private static final double NIGHT_WINDOW_EARLY_END_HOUR = 4.0;   // 4 AM
    // MODIFICATION END
    private static final int MAX_SIM_DAYS_FOR_OVERLAP_CHECK = 10;

    public CharDhamAnalysisEventHandler(String runId, int iteration) {
        this.runId = runId;
        this.iteration = iteration;
    }

    private int getCurrentDay(double time) {
        return (int) Math.floor(time / (24 * 3600.0)) + 1;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Id<Person> personId = event.getPersonId();
        if ("rest".equals(event.getActType())) {
            restActivityStartTimes.computeIfAbsent(event.getFacilityId(), k -> new HashMap<>()).put(personId, event.getTime());
        } else if (DHAM_ACTIVITY_TYPES.contains(event.getActType())) {
            dhamActivityStartTimes.computeIfAbsent(event.getActType(), k -> new HashMap<>()).put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        int day = getCurrentDay(event.getTime());
        Id<Person> personId = event.getPersonId();
        if ("rest".equals(event.getActType())) {
            Map<Id<Person>, Double> personStartTimes = restActivityStartTimes.get(event.getFacilityId());
            if (personStartTimes != null && personStartTimes.remove(personId) != null) {
                uniqueRestActivityPersonsPerFacility.computeIfAbsent(day, k -> new HashMap<>())
                        .computeIfAbsent(event.getFacilityId(), k -> new HashSet<>()).add(personId);
            }
        } else if (DHAM_ACTIVITY_TYPES.contains(event.getActType())) {
            Map<Id<Person>, Double> personStartTimes = dhamActivityStartTimes.get(event.getActType());
            if (personStartTimes != null && personStartTimes.remove(personId) != null) {
                uniqueDhamActivityPersons.computeIfAbsent(day, k -> new HashMap<>())
                        .computeIfAbsent(event.getActType(), k -> new HashSet<>()).add(personId);
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
            int arrivalDay = getCurrentDay(event.getTime());

            totalTravelTimePerPersonDaily.computeIfAbsent(arrivalDay, k -> new HashMap<>())
                    .merge(event.getPersonId(), travelTime, Double::sum);

            totalTravelTimePerMode.computeIfAbsent(arrivalDay, k -> new HashMap<>())
                    .merge(event.getLegMode(), travelTime, Double::sum);

            // MODIFICATION START: Call the new, corrected method for night travel calculation.
            calculateAndAttributeNightOverlap(legStartTime, event.getTime(), event.getPersonId());
            // MODIFICATION END
        }
    }

    @Override
    public void reset(int iteration) {
        restActivityStartTimes.clear();
        uniqueRestActivityPersonsPerFacility.clear();
        dhamActivityStartTimes.clear();
        uniqueDhamActivityPersons.clear();
        personLegStartTimes.clear();
        totalTravelTimePerPersonDaily.clear();
        totalLegsPerMode.clear();
        totalTravelTimePerMode.clear();
        totalNightTravelTime_s.clear();
        agentsTravelingAtNight.clear();
    }

    public Map<String, SimulationAnalysisResult> getAnalysisResults() {
        Map<String, SimulationAnalysisResult> results = new HashMap<>();
        SimulationAnalysisResult allDaysResult = new SimulationAnalysisResult(this.runId, "all_days", this.iteration);

        Set<Integer> allDays = new HashSet<>();
        allDays.addAll(uniqueRestActivityPersonsPerFacility.keySet());
        allDays.addAll(uniqueDhamActivityPersons.keySet());
        allDays.addAll(totalLegsPerMode.keySet());
        allDays.addAll(totalNightTravelTime_s.keySet());
        allDays.addAll(totalTravelTimePerPersonDaily.keySet());

        for (Integer day : allDays) {
            SimulationAnalysisResult dailyResult = new SimulationAnalysisResult(this.runId, "day" + day, this.iteration);
            dailyResult.uniqueRestActivityPersonsPerFacility.putAll(uniqueRestActivityPersonsPerFacility.getOrDefault(day, Collections.emptyMap()));
            dailyResult.uniqueDhamActivityPersons.putAll(uniqueDhamActivityPersons.getOrDefault(day, Collections.emptyMap()));
            dailyResult.totalTravelTimePerPerson.putAll(totalTravelTimePerPersonDaily.getOrDefault(day, Collections.emptyMap()));
            dailyResult.totalLegsPerMode.putAll(totalLegsPerMode.getOrDefault(day, Collections.emptyMap()));
            dailyResult.totalTravelTimePerMode.putAll(totalTravelTimePerMode.getOrDefault(day, Collections.emptyMap()));
            dailyResult.totalNightTravelTime_s = totalNightTravelTime_s.getOrDefault(day, 0.0);
            dailyResult.agentsTravelingAtNight.addAll(agentsTravelingAtNight.getOrDefault(day, Collections.emptySet()));
            dailyResult.calculateDerivedMetrics();
            results.put("day" + day, dailyResult);
            allDaysResult.aggregate(dailyResult);
        }

        allDaysResult.calculateDerivedMetrics();
        results.put("all_days", allDaysResult);
        return results;
    }

    /**
     * MODIFICATION START: This method is completely rewritten.
     * Calculates the duration of a leg that falls within the defined nighttime windows
     * (00:00-04:00 and 22:00-24:00) and directly attributes the time and the agent
     * to the specific day the travel occurred on.
     *
     * @param legStart The start time of the leg in seconds.
     * @param legEnd   The end time of the leg in seconds.
     * @param personId The ID of the person traveling.
     */
    private void calculateAndAttributeNightOverlap(double legStart, double legEnd, Id<Person> personId) {
        double secondsPerDay = 24 * 3600.0;

        int startDay0Indexed = (int) Math.floor(legStart / secondsPerDay);
        int endDay0Indexed = (int) Math.floor(legEnd / secondsPerDay);

        for (int dayIdx = startDay0Indexed; dayIdx <= endDay0Indexed && dayIdx < MAX_SIM_DAYS_FOR_OVERLAP_CHECK; dayIdx++) {
            int currentDayForMap = dayIdx + 1; // Maps are 1-indexed

            // Window 1: Early morning of the current day (00:00 - 04:00)
            double earlyWindowStart = dayIdx * secondsPerDay;
            double earlyWindowEnd = earlyWindowStart + (NIGHT_WINDOW_EARLY_END_HOUR * 3600.0);
            double earlyOverlap = Math.max(0, Math.min(legEnd, earlyWindowEnd) - Math.max(legStart, earlyWindowStart));

            if (earlyOverlap > 0) {
                totalNightTravelTime_s.merge(currentDayForMap, earlyOverlap, Double::sum);
                agentsTravelingAtNight.computeIfAbsent(currentDayForMap, k -> new HashSet<>()).add(personId);
            }

            // Window 2: Late evening of the current day (22:00 - 24:00)
            double lateWindowStart = (dayIdx * secondsPerDay) + (NIGHT_WINDOW_LATE_START_HOUR * 3600.0);
            double lateWindowEnd = (dayIdx + 1) * secondsPerDay; // End of the day
            double lateOverlap = Math.max(0, Math.min(legEnd, lateWindowEnd) - Math.max(legStart, lateWindowStart));

            if (lateOverlap > 0) {
                totalNightTravelTime_s.merge(currentDayForMap, lateOverlap, Double::sum);
                agentsTravelingAtNight.computeIfAbsent(currentDayForMap, k -> new HashSet<>()).add(personId);
            }
        }
    }
    // MODIFICATION END
}