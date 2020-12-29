package playground.nidhi.practice.eventHandlingPract.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class LegRun {
    public static void main(String[] args) {
        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it

        LegHandler handler2 = new LegHandler();
        events.addHandler(handler2);

        //create the reader and read the file

        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);

        System.out.println("Events file read!");
    }
}
