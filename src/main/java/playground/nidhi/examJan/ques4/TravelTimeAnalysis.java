package playground.nidhi.examJan.ques4;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.util.List;

public class TravelTimeAnalysis {
    public static void main(String[] args) {
        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();

        TravelTimeHandler travelTimeHandler= new TravelTimeHandler();
        events.addHandler(travelTimeHandler);

        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        System.out.println("Total travel time of all traveller: "+ travelTimeHandler.getTotalTravelTime());

    }
}
