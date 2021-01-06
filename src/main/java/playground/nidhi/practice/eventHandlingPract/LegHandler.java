package playground.nidhi.practice.eventHandlingPract.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LegHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
    private Set<Id<Person>> personIds;
    private final Map<Id<Person>, Map<Integer, LegInfo>> person2leg = new HashMap<>();
    private final Map<Id<Person>, Integer> person2legNumber = new HashMap<>();
    private Map<Integer, LegInfo> legNumber2LegInfo = new HashMap<>();

    public LegHandler() {
        this.personIds = personIds;
    }

    @Override
    public void reset(int iteration) {
        this.person2leg.clear();
        this.person2legNumber.clear();
    }



    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (this.personIds.contains(event.getPersonId())) {

            if (this.person2legNumber.get(event.getPersonId()) == null) {
                this.person2legNumber.put(event.getPersonId(), 0);

                this.person2leg.put(event.getPersonId(), legNumber2LegInfo);

            } else {
                int legNr = this.person2legNumber.get(event.getPersonId()) + 1;
                this.person2legNumber.put(event.getPersonId(),legNr);
            }

         this.person2leg.get(event.getPersonId()).put(person2legNumber.get(event.getPersonId()),
                 new LegInfo(event.getLegMode(), event.getTime()));
        }else {
            //skip
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if(this.personIds.contains(event.getPersonId())){
            this.person2leg.get(event.getPersonId()).get(person2legNumber.get(event.getPersonId())).setArrivalTime(event.getTime());
        }else {
            //skip
        }
    }

    public Map<Id<Person>, Map<Integer, LegInfo>> getPerson2legInfo() {
        return person2leg;
    }

    private class LegInfo {
        String legMode;
        double departureTime;
        double arrivalTime;

        public LegInfo(String legMode, double departureTime) {
        }

        public String getLegMode() {
            return legMode;
        }

        public double getDepartureTime() {
            return departureTime;
        }

        public double getArrivalTime() {
            return arrivalTime;
        }

        public void setArrivalTime(double time) {
            this.arrivalTime = time;
        }

        public double getTravelTime() {
            return arrivalTime - departureTime;
        }
    }
}
