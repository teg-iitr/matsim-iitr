package playground.shivam.signals;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class RunEventHandlerLinks {
    public static void main(String[] args){
        // give the path of events file. for this you first need to run a simulation

        String inputfile = "output/RunFixedMixedTrafficSignalSimpleIntersection/output_events.xml.gz"; // events file

        //create an events object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it
        EventHandlerLinks eventHandler = new EventHandlerLinks();
        events.addHandler(eventHandler);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputfile);
        events.finishProcessing();


        System.out.println("Travel Time in link 2_3: " + eventHandler.getTravelTimeLinks());
    }
}
