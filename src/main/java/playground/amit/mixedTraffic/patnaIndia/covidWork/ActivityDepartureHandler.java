package playground.amit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * @author amit
 */

public class ActivityDepartureHandler implements ActivityStartEventHandler, PersonDepartureEventHandler {

    private final SortedMap<String, SortedMap<Integer, Integer>> acts = new TreeMap<>();
    private final Map<Id<Person>,Integer> personId2DepartureTimeBin = new HashMap<>();
    private final Map<Id<Person>, String> personId2TripPurpose = new HashMap<>();
    private final double timebinsize = 3600.;
    private final Integer hours = 24;

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (personId2TripPurpose.get(event.getPersonId()) == null){
            // first act start is actual trip purpose for urban
            personId2TripPurpose.put(event.getPersonId(), event.getActType());
        }

        String actType = personId2TripPurpose.get(event.getPersonId());

        SortedMap<Integer, Integer> timebin2count = acts.get(actType);
        if(timebin2count==null) {
            timebin2count = IntStream.rangeClosed(1, 24).boxed().collect(Collectors.toMap(i -> i, i -> 0, (a, b) -> b, TreeMap::new));
            acts.put(event.getActType(),timebin2count);
        }

        Integer timeBin = personId2DepartureTimeBin.remove(event.getPersonId());
        if ( timeBin == null) {
            // person wont depart for work-home-type plans.
//            throw  new RuntimeException("Person Id "+event.getPersonId()+ " is not departed yet. Aborting...");
        } else {
            int countSoFar = timebin2count.getOrDefault(timeBin,0); // getOrDefault for timeBin>24h
            timebin2count.put(timeBin, countSoFar+1);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        int timebin = (int)  Math.ceil(event.getTime()/timebinsize);
        if(timebin==0) timebin=1;
        personId2DepartureTimeBin.put(event.getPersonId(), timebin);
    }

    @Override
    public void reset(int iteration) {
        this.personId2DepartureTimeBin.clear();
        this.acts.clear();
    }

    public SortedMap<String, SortedMap<Integer, Integer>> getActivityDepartureCounter(){
        return this.acts;
    }
}
