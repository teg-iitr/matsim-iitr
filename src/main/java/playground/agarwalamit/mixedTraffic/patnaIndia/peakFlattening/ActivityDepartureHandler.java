package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

public class ActivityDepartureHandler implements ActivityStartEventHandler, PersonDepartureEventHandler {

    private final SortedMap<Tuple<String, Integer>, Integer> acts = new TreeMap<>(new Comparator<Tuple<String, Integer>>() {
        @Override
        public int compare(Tuple<String, Integer> o1, Tuple<String, Integer> o2) {
            return o1.getFirst().compareTo(o2.getFirst()) + o1.getSecond().compareTo(o2.getSecond());
        }
    });
    private final Map<Id<Person>,Integer> personId2DepartureTimeBin = new HashMap<>();
    private final double timebinsize = 3600.;

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Integer timeBin = personId2DepartureTimeBin.remove(event.getPersonId());
        if (! event.getActType().equals("home")) {
            if ( timeBin == null) throw  new RuntimeException("Person Id "+event.getPersonId()+ " is not departed yet. Aborting...");
            Tuple<String, Integer> ad = new Tuple<>(event.getActType(), timeBin);
            int countSoFar = acts.getOrDefault(ad,0);
            acts.put(ad, countSoFar+1);
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

    public SortedMap<Tuple<String, Integer>, Integer> getActivityDepartureCounter(){
        return this.acts;
    }
}
