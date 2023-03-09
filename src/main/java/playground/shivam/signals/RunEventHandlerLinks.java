package playground.shivam.signals;

import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import java.util.HashSet;

public class RunEventHandlerLinks {
    public static void main(String[] args){
        // give the path of events file. for this you first need to run a simulation
        String inputFile = "output/RunFixedMixedTrafficSignalSimpleIntersection/output_events.xml.gz"; // events file

        //create an events object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it
        EventHandlerLinks eventHandler = new EventHandlerLinks();
        events.addHandler(eventHandler);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
//        EmissionEventsReader reader1 = new EmissionEventsReader(events);
//        DrtEventsReaders readers2 = new
        reader.readFile(inputFile);
        events.finishProcessing();

        HashSet<String>linkId = new HashSet<>();
        linkId.add("2_3"); linkId.add("8_3"); linkId.add("4_3"); linkId.add("7_3");

        for (String linkIds: linkId) {
            System.out.println("Travel Time in link : " + linkIds + " " + eventHandler.getTravelTimeLinks(linkIds));
        }
    }
}
