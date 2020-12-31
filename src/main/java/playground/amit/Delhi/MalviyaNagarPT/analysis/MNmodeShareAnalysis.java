package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MNmodeShareAnalysis {
    public static void main(String[] args) {
        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();

        MNmodeShareHandler mnModeShareHandler = new MNmodeShareHandler();
        events.addHandler(mnModeShareHandler);

        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        int cntOfPtTrips= 0;
        int cntOfWalkTrips =0;

        for (List<String> modes : mnModeShareHandler.getPerson_Modes().values()) {
            if (modes.contains("pt")) {
                cntOfPtTrips =cntOfPtTrips+1;
            } else if (modes.contains("car")) {
            } else {
                cntOfWalkTrips = cntOfWalkTrips+1;
            }
        }
        System.out.println(cntOfPtTrips);
        System.out.println(cntOfWalkTrips);


        int totalTrips=cntOfPtTrips+cntOfWalkTrips;
        double ptShare =  100.0 * cntOfPtTrips/totalTrips;
        double walkShare =100.0 * cntOfWalkTrips/totalTrips;
        System.out.println("Share of Pt trips= " + ptShare + ", Share of Walk trips= "+ walkShare);



    }



}
