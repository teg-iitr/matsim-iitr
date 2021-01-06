package playground.nidhi.examJan.ques4;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import playground.amit.Delhi.MalviyaNagarPT.analysis.MNmodeShareHandler;

public class CongestionAnalysis {
    public static void main(String[] args) {
        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();

        CongestionHandler congestionHandler= new CongestionHandler();
        events.addHandler(congestionHandler);

        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();



    }

}
