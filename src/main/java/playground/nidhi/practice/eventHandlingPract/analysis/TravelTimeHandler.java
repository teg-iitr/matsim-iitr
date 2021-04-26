package playground.nidhi.practice.eventHandlingPract.analysis;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;

//transiteventhandler

public class TravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler{
    private Map<Id<Person>, Double> departureTimes= new HashMap<>();
    private double travelTimeSum =0.0;
    private int travelTimeCount =0;



    @Override
    public void handleEvent(PersonArrivalEvent event) {
     this.departureTimes.put(event.getPersonId(), event.getTime());
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        double departureTimeMatch = this.departureTimes.get(event.getPersonId());
        double travelTime = event.getTime()-departureTimeMatch;

        this.travelTimeSum+=travelTime;
        this.travelTimeCount++;
    }

    public double getAverageTravelTime(){
        double averageTravelTime = this.travelTimeSum/this.travelTimeCount;

        return averageTravelTime;

    }
}