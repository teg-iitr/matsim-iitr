package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import playground.nidhi.practice.eventHandlingPract.analysis.StuckEventHandler;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class StuckEventsHandler implements PersonStuckEventHandler {

    private static final Logger log = Logger.getLogger(String.valueOf(StuckEventHandler.class));

    private Set<Id<Person>> agentsStuck;

    public StuckEventsHandler(){
        this.agentsStuck=new TreeSet<>();
        log.info("initialized");
    }

    @Override
    public void reset(int iteration) {
        System.out.println("reset...");
        this.agentsStuck=new TreeSet<>();
    }


    @Override
    public void handleEvent(PersonStuckEvent event) {
        System.out.println("Person id: " + event.getPersonId() +" , Link Id: " + event.getLinkId()+ " , legMode: " + event.getLegMode());
        System.out.println("  ");
        this.agentsStuck.add(event.getPersonId());


    }
}
