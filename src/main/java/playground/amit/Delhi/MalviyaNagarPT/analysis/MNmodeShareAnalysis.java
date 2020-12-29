package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class MNmodeShareAnalysis {
    public static void main(String[] args) {
        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();

        MNmodeShareHandler mnModeShareHandler = new MNmodeShareHandler();
        events.addHandler(mnModeShareHandler);

        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);

        System.out.println(mnModeShareHandler.modeShare());

        System.out.println("Events file read!");
    }
}
