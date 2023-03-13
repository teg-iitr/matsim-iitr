package playground.shivam.signals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;

public class RunEventHandlerLanes{
    private static Scenario scenario;
    private static Network network;
    private static EventHandlerLanes eventHandler;

    public static void run(){
        // Give the path of events file. for this you first need to run a simulation

        String inputFile = "output/RunFixedMixedTrafficSignalSimpleIntersection/output_events.xml.gz"; // events file

        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it
        eventHandler = new EventHandlerLanes();
        events.addHandler(eventHandler);

        //create the reader and read the file
        events.initProcessing();
        LanesEventReader reader = new LanesEventReader(events);

        reader.readFile(inputFile);
        events.finishProcessing();

        Config config = ConfigUtils.loadConfig("output/RunFixedMixedTrafficSignalSimpleIntersection/config.xml");
        scenario = ScenarioUtils.loadScenario(config);
        config.network().getLaneDefinitionsFile();
        network = scenario.getNetwork();

        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()) {
            double travelTime = travelTimeLanes(l2l.getLinkId().toString());
            System.out.println(travelTimeLanesPerLink(l2l.getLinkId().toString()));
        }


    }

    public static double travelTimeLanes(String linkId){
        double travelTime = 0.0;
        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()) {
            for (Lane lane : l2l.getLanes().values()) {
                travelTime += eventHandler.getTravelTimeLanes(lane.getId().toString());
                System.out.println("Travel Time for lane Id is: "+lane.getId().toString());
                System.out.println("Travel Time for lane Id : "+ lane.getId().toString()+"=="+eventHandler.getTravelTimeLanes(lane.getId().toString()));
            }
        }
        return travelTime;
    }

    public static double travelTimeLanesPerLink(String linkId){
        double travelTime = 0.0;
        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()) {
            Link link = network.getLinks().get(l2l.getLinkId());
            System.out.println("travelTimeLanesPerLink for Link ID: " + l2l.getLinkId().toString());
            if(l2l.getLinkId().toString().equals(linkId)){
                travelTime = travelTimeLanes(l2l.getLinkId().toString());
            }
        }
        return travelTime;
    }
    public static void main(String[] args){
        run();
    }
}
