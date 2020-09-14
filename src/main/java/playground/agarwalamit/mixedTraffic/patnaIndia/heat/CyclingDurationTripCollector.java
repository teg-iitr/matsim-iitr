package playground.agarwalamit.mixedTraffic.patnaIndia.heat;

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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CyclingDurationTripCollector implements  PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

    public final double timebinSize = 1800.;
    public static final String mode_filter = TransportMode.bike;
    public static final String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();

    private final PatnaPersonFilter filter = new PatnaPersonFilter();
    private final Map<Id<Person>, Double> personId2TotalTripTime = new HashMap<>();
    private final Map<Tuple<Double, Double>, Integer> monthlyIncTimeBinCount = new HashMap<>();

    public static void main(String[] args) {
        String runCase = "run2020_19_BSH";
        String outFile = "C:/Users/Amit Agarwal/Documents/git/workingPapers/iitr/2020/papers/economicBenefitsCycling/graphics/"+runCase+"_incCyclingTime_count.txt";
        String plansFile  = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/results/policy/"+runCase+"/"+runCase+".output_plans.xml.gz"; // for income per person
        String eventsFile = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/results/policy/"+runCase+"/"+runCase+".output_events.xml.gz";

        CyclingDurationTripCollector cyclingDurationTripCollector = new CyclingDurationTripCollector();

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(cyclingDurationTripCollector);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(plansFile);
        cyclingDurationTripCollector.processData(scenario);
        Map<Tuple<Double, Double>, Integer> out = cyclingDurationTripCollector.monthlyIncTimeBinCount;

        try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
            writer.write("incomeCategory\tcyclingTimeBinSec\tnumberOfPerson\n");
            for(Tuple<Double, Double> tup : out.keySet()) {
                writer.write(tup.getFirst()+"\t"+tup.getSecond()+"\t"+out.get(tup)+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        Id<Person> personId = personArrivalEvent.getPersonId();
        if (personArrivalEvent.getLegMode().equals(mode_filter) &&
                filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup)) {
            double tripTimeSoFar = this.personId2TotalTripTime.getOrDefault(personId,0.);
            this.personId2TotalTripTime.put(personId, tripTimeSoFar + personArrivalEvent.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        //register
        Id<Person> personId = personDepartureEvent.getPersonId();
        if (personDepartureEvent.getLegMode().equals(mode_filter) &&
                filter.getUserGroupAsStringFromPersonId(personId).equals(userGroup)) {
            double tripTimeSoFar = this.personId2TotalTripTime.getOrDefault(personId,0.);
            this.personId2TotalTripTime.put(personId, tripTimeSoFar - personDepartureEvent.getTime());
        }
    }

    @Override
    public void reset(int iteration) {
        this.personId2TotalTripTime.clear();
        this.monthlyIncTimeBinCount.clear();
    }

    @Override
    public void handleEvent(PersonStuckEvent personStuckEvent) {
        Logger.getLogger(CyclingDurationTripCollector.class).error("Stuck and abort event :"+personStuckEvent.toString());
    }

    private void processData(Scenario scenario){
        this.personId2TotalTripTime.entrySet().stream().forEach(e-> {
            double timeBin = getTimeBin(e.getValue());

            double inc = (Double) scenario.getPopulation().getPersons()
                    .get(e.getKey())
                    .getAttributes().getAttribute(PatnaUtils.INCOME_ATTRIBUTE);
            Tuple<Double, Double> inc_timeBin = new Tuple<>(inc, timeBin);
            int countSoFar = this.monthlyIncTimeBinCount.getOrDefault(inc_timeBin, 0);
            this.monthlyIncTimeBinCount.put(inc_timeBin, countSoFar+1);
        });
    }

    private double getTimeBin(double tripTime){
        if (tripTime == 0.) return 0.; // do not want to include them.

        double timeBin = Math.ceil(tripTime/ timebinSize);
        return timeBin * timebinSize;
    }
}
