package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

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


        System.out.println("Total number of person + driver= " + mnModeShareHandler.getPerson_MainMode().size());
//        System.out.println(mnModeShareHandler.getPerson_MainMode());
        int cntPt= mnModeShareHandler.getCntOfPtTrips();
        int cntWalk=mnModeShareHandler.getCntOfWalkTrips();
        System.out.println("Count of Pt trips= " + cntPt + ", Count of Walk trips= "+ cntWalk);


        int totalTrips=cntPt+cntWalk;
        double ptShare = (double) 100 * cntPt/totalTrips;
        double walkShare =(double) 100 * cntWalk/totalTrips;
        System.out.println("Share of Pt trips= " + ptShare + ", Share of Walk trips= "+ walkShare);


//


    }



}
