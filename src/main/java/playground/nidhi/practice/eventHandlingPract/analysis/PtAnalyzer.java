package playground.nidhi.practice.eventHandlingPract.analysis;


import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;

public class PtAnalyzer {

    private PtHandler ptHandler;
    private MutableScenario scenario;

public void init(MutableScenario scenario){
    this.scenario=scenario;
    this.ptHandler=new PtHandler();
}

    public static void main(String[] args) {
        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();
        PtHandler handlerPt = new PtHandler();
        events.addHandler(handlerPt);
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        System.out.println("Events file read!");
    }


}
