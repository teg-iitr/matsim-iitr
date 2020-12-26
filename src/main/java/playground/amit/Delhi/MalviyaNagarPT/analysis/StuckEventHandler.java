package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class StuckEventHandler implements PersonStuckEventHandler {


    private static final Logger log = Logger.getLogger(String.valueOf(StuckEventHandler.class));

    private Set<Id<Person>> agentsStuck;

    public StuckEventHandler() {
        this.agentsStuck = new TreeSet<>();
       log.info("initialized");
//        System.out.println("initialized");
    }

    @Override
    public void reset(int iteration) {
        this.agentsStuck= new TreeSet<>();
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        System.out.println("Person id: " + event.getPersonId() +" , Link Id: " + event.getLinkId()+ " , legMode: " + event.getLegMode());
        System.out.println("  ");
        this.agentsStuck.add(event.getPersonId());
    }

    public Set<Id<Person>> getAgentsStuck() {
        log.info("Returning " + this.agentsStuck.size() + " agent ids");
        return this.agentsStuck;
    }


}
