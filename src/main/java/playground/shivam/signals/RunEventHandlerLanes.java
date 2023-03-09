package playground.shivam.signals;


import org.matsim.api.core.v01.events.EventsManagerImpl;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;

public class RunEventHandlerLanes{
    public static void main(String[] args){
        // Give the path of events file. for this you first need to run a simulation

        String inputfile = "output/RunFixedMixedTrafficSignalSimpleIntersection/output_events.xml.gz"; // events file

        //create an events object
//        EventsManager events = new EventsManagerImpl();

        // Create the handler and add it
        EventHandlerLanes eventHandler = new EventHandlerLanes();
//        events.addHandler(eventHandler);

        EventsManager events = new EventsManagerImpl();
        events.addHandler(eventHandler);
//        eventHandler.handleEvent();


        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputfile);
        events.finishProcessing();


        System.out.println("Travel Time in link 2_3: ");
    }
}

