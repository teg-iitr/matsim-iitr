package playground.agarwalamit.mixedTraffic.patnaIndia.heat;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class CyclingDurationTripCollector implements  PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler, TeleportationArrivalEventHandler {

    public static final SortedSet<String> URBAN_ALL_MODES = new TreeSet<>(PatnaUtils.URBAN_ALL_MODES);
    public final double timebinSize = 1800.;
    public final double [] incomeCategories = {500,750,2000,4000,6250,20000};
    public final double [] cyclingDurationBins = {1800,3600,5400,7200,9000,10800,12600};

    public static final String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();

    private final PatnaPersonFilter filter = new PatnaPersonFilter();
    private final SortedMap<String, SortedMap<Id<Person>, Double>> personId2TotalTripTime = new TreeMap<>();
    private final SortedMap<String, Map<Tuple<Double, Double>, Integer>> monthlyIncTimeBinCount = new TreeMap<>();
    private final Map<Id<Person>,String> personId2Mode = new HashMap<>();

    CyclingDurationTripCollector () {
        for (String mode : URBAN_ALL_MODES){
            this.personId2TotalTripTime.put(mode, new TreeMap<>());
            Map<Tuple<Double, Double>, Integer> map = new HashMap<>();
            for (double incomeCategory : incomeCategories) {
                for (double cyclingDurationBin : cyclingDurationBins) {
                    map.put(new Tuple<>(incomeCategory, cyclingDurationBin), 0);
                }
            }
            this.monthlyIncTimeBinCount.put(mode, map);
        }
    }

    public static void main(String[] args) {
        String runCase = "run2020_19_BSH";
//        String runCase = "run2020_17_BAU";
        String outFile = "C:/Users/Amit Agarwal/Documents/git/workingPapers/iitr/2020/papers/economicBenefitsCycling/graphics/"+runCase+"_incTripTime_count.txt";
        String plansFile  = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/results/policy/"+runCase+"/"+runCase+".output_plans.xml.gz"; // for income per person
        String eventsFile = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/results/policy/"+runCase+"/"+runCase+".output_events.xml.gz";

        CyclingDurationTripCollector cyclingDurationTripCollector = new CyclingDurationTripCollector();

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(cyclingDurationTripCollector);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(plansFile);
        cyclingDurationTripCollector.processData(scenario);
        SortedMap<String, Map<Tuple<Double, Double>, Integer>> out = cyclingDurationTripCollector.monthlyIncTimeBinCount;

        try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
            for (String mode : URBAN_ALL_MODES){
                for(Tuple<Double, Double> tup : out.get(mode).keySet()) {
                    writer.write(mode+"\t"+tup.getFirst()+"\t"+tup.getSecond()+"\t"+out.get(mode).get(tup)+"\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        Id<Person> personId = personArrivalEvent.getPersonId();
        if ( filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup)&&
                URBAN_ALL_MODES.contains(personArrivalEvent.getLegMode())) {
            double tripTimeSoFar = this.personId2TotalTripTime.get(personArrivalEvent.getLegMode()).getOrDefault(personId,0.);
                    this.personId2TotalTripTime.get(personArrivalEvent.getLegMode()).put(personId, tripTimeSoFar + personArrivalEvent.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        Id<Person> personId = personDepartureEvent.getPersonId();
        if (filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup) &&
                URBAN_ALL_MODES.contains(personDepartureEvent.getLegMode())) {
            double tripTimeSoFar = this.personId2TotalTripTime.get(personDepartureEvent.getLegMode()).getOrDefault(personId,0.);
            this.personId2TotalTripTime.get(personDepartureEvent.getLegMode()).put(personId, tripTimeSoFar - personDepartureEvent.getTime());
        }
        personId2Mode.put(personId,personDepartureEvent.getLegMode());
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent teleportationArrivalEvent) {
        Id<Person> personId = teleportationArrivalEvent.getPersonId();
        if ( filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup)&&
                URBAN_ALL_MODES.contains(personId2Mode.get(teleportationArrivalEvent.getPersonId()))) {
            double tripTimeSoFar = this.personId2TotalTripTime.get(personId2Mode.get(teleportationArrivalEvent.getPersonId())).getOrDefault(personId,0.);
            this.personId2TotalTripTime.get(personId2Mode.get(teleportationArrivalEvent.getPersonId())).put(personId, tripTimeSoFar + teleportationArrivalEvent.getTime());
        }
    }

    @Override
    public void reset(int iteration) {
        this.personId2TotalTripTime.clear();
        this.monthlyIncTimeBinCount.clear();
        this.personId2Mode.clear();
    }

    @Override
    public void handleEvent(PersonStuckEvent personStuckEvent) {
        Logger.getLogger(CyclingDurationTripCollector.class).error("Stuck and abort event :"+personStuckEvent.toString());
    }

    private void processData(Scenario scenario){
        for (String mode : this.personId2TotalTripTime.keySet()) {
            for (Id<Person> personId : this.personId2TotalTripTime.get(mode).keySet()) {
                double timeBin = getTimeBin(this.personId2TotalTripTime.get(mode).get(personId));
                double inc = (Double) scenario.getPopulation().getPersons()
                        .get(personId)
                        .getAttributes().getAttribute(PatnaUtils.INCOME_ATTRIBUTE);
                Tuple<Double, Double> inc_timeBin = new Tuple<>(inc, timeBin);
                int countSoFar = this.monthlyIncTimeBinCount.get(mode).get(inc_timeBin); //NPE
                this.monthlyIncTimeBinCount.get(mode).put(inc_timeBin, countSoFar+1);
            }
        }
    }

    private double getTimeBin(double tripTime){
        if (tripTime == 0.) return 0.; // do not want to include them.

        double timeBin = Math.ceil(tripTime/ timebinSize);
        return timeBin * timebinSize;
    }
}
