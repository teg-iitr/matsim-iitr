package playground.nidhi.practice.eventHandlingPract.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.IOException;

public class MN_EventAnalysis {

    public static void main(String[] args) throws IOException {

        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();

    //create the handler and add it

    StuckEventHandler handler2 = new StuckEventHandler();
    events.addHandler(handler2);

    //create the reader and read the file

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);

//		StuckEventHandler se = new StuckEventHandler();
//		se.writeResults("C:\\Users\\Nidhi\\Documents\\iitr_gmail_drive\\project_data\\delhiMalviyaNagar_PT\\matsimFiles\\StuckEvent.txt");

		System.out.println("Events file read!");
}

}
