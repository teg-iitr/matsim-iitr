package playground.amit.mixedTraffic.patnaIndia.heat;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.amit.utils.LoadMyScenarios;
import playground.amit.utils.NumberUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Amit on 15/09/2020.
 */
public class AverageBicycleDurationCollector
        implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

    public static final String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
    private final PatnaPersonFilter filter = new PatnaPersonFilter();

    private final Map<Id<Person>, Double> personId2TotalTripTime = new HashMap<>();
    private final String mode_filter= TransportMode.bike;

    public final SortedMap<Double, Double> income2AvgCyclingDur = new TreeMap<>();

    public static void main(String[] args) {
        String parentPath = "C:\\Users\\Amit\\Google Drive\\iitr_gmail_drive\\project_data\\patna\\0.15pcu_TUB-work\\sensitivity\\";

        String [] runCases = {"makeBSHLength_25pct","makeBSHLength_33.33pct","makeBSHLength_50pct","makeBSHLength_66.66pct","makeBSHLength_75pct","makeBSHLength_90pct","makeBSHLength_100pct"};

        for (String runCase : runCases){
            String plansFile  = parentPath+runCase+"\\output_plans.xml.gz";
            String attributesFile  = parentPath+runCase+"\\output_personAttributes.xml.gz"; // for income per person; required for older MATSim files
            String eventsFile  = parentPath+runCase+"\\output_events.xml.gz";

            String outFile = "C:/Users/Amit/Documents/git-repos/workingPapers/iitr/2020/papers/economicBenefitsCycling/graphics/"+runCase+"_incomeAvgCyclingDuration.txt";

            AverageBicycleDurationCollector cyclingDurationTripCollector = new AverageBicycleDurationCollector();
            EventsManager eventsManager = EventsUtils.createEventsManager();
            eventsManager.addHandler(cyclingDurationTripCollector);
            new MatsimEventsReader(eventsManager).readFile(eventsFile);

            Scenario scenario = LoadMyScenarios.loadScenarioFromPlansAndAttributes(plansFile,attributesFile);
            cyclingDurationTripCollector.processData(scenario);
            SortedMap<Double, Double> out = cyclingDurationTripCollector.income2AvgCyclingDur;

            try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
                writer.write("income\taverageCyclingDurationSec\n");
                for (double inc : out.keySet()){
                    writer.write(inc+"\t"+out.get(inc)+"\n");
                }
            } catch (IOException e) {
                throw new RuntimeException("Data is not written. Reason "+e);
            }
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        Id<Person> personId = personArrivalEvent.getPersonId();
        if ( filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup)&&
                personArrivalEvent.getLegMode().equals(mode_filter)) {
            double tripTimeSoFar = this.personId2TotalTripTime.getOrDefault(personId,0.);
            this.personId2TotalTripTime.put(personId, tripTimeSoFar + personArrivalEvent.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        Id<Person> personId = personDepartureEvent.getPersonId();
        if (filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup) &&
                personDepartureEvent.getLegMode().equals(mode_filter)) {
            double tripTimeSoFar = this.personId2TotalTripTime.getOrDefault(personId,0.);
            this.personId2TotalTripTime.put(personId, tripTimeSoFar - personDepartureEvent.getTime());
        }
    }

    @Override
    public void handleEvent(PersonStuckEvent personStuckEvent) {
        Logger.getLogger(AverageBicycleDurationCollector.class).error("Stuck and abort event :"+personStuckEvent.toString());
    }

    private void processData(Scenario scenario){
        Map<Double, Integer> income2counter = new HashMap<>();

        for (Id<Person> personId : this.personId2TotalTripTime.keySet()) {
            double inc = (Double) scenario.getPopulation().getPersons()
                    .get(personId)
                    .getAttributes().getAttribute(PatnaUtils.INCOME_ATTRIBUTE);
            int countSoFar = income2counter.getOrDefault(inc,0);
            income2counter.put(inc, countSoFar+1);

            double durSoFar = this.income2AvgCyclingDur.getOrDefault(inc,0.);
            this.income2AvgCyclingDur.put(inc, durSoFar+this.personId2TotalTripTime.get(personId));
        }

        for(double inc : income2counter.keySet()){
            this.income2AvgCyclingDur.put(inc,
                    NumberUtils.round(this.income2AvgCyclingDur.get(inc)/income2counter.get(inc),2));
        }
    }

}
