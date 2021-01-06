package playground.nidhi.practice.eventHandlingPract.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class TravelTimeAnalysis {
    public static void main(String[] args) {

        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";

        EventsManager events = EventsUtils.createEventsManager();
        TravelTimeHandler ttHandler = new TravelTimeHandler();
        events.addHandler(ttHandler);

        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);

//		StuckEventHandler se = new StuckEventHandler();
//		se.writeResults("C:\\Users\\Nidhi\\Documents\\iitr_gmail_drive\\project_data\\delhiMalviyaNagar_PT\\matsimFiles\\StuckEvent.txt");

    }
}
