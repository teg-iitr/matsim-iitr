package playground.nidhi.examJan.ques4;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;


public class TravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
    private final Map<Id<Person>, Double> depatureTimes= new HashMap<>();
    private double TotalTravelTime = 0.0;

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.depatureTimes.put(event.getPersonId(), event.getTime());
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
       double departTime= this.depatureTimes.get(event.getPersonId());
       double travelTime= event.getTime()-departTime;
       this.TotalTravelTime+=travelTime;
    }

    public double getTotalTravelTime() {
        return TotalTravelTime;
    }
}
