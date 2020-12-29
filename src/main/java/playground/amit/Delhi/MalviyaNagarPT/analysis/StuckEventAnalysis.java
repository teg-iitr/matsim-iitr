package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class StuckEventAnalysis {
    public static void main(String[] args) {
        String inputFile = "C:\\Users\\Nidhi\\Documents\\GitHub\\matsim-iitr\\output\\output_events.xml.gz";

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();
        StuckEventsHandler handler1 = new StuckEventsHandler();
        events.addHandler(handler1);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
       events.finishProcessing();

        System.out.println("Events file read!");

    }
}
