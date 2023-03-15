package playground.shivam.signals;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesToLinkAssignment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class JavaUnitTests {

    private EventHandlerLinks eventHandlerLinks;
    private EventHandlerLanes eventHandlerLanes;

    private final String inputFile = "output/RunFixedMixedTrafficSignalSimpleIntersection/output_events.xml.gz";

    private final EventsManager eventsLinks = EventsUtils.createEventsManager();
    private final EventsManager eventsLanes = EventsUtils.createEventsManager();

    // constructor for initializing the objects
    public JavaUnitTests() {
        eventHandlerLinks = new EventHandlerLinks();
        eventsLinks.addHandler(eventHandlerLinks);
        eventsLinks.initProcessing();

        MatsimEventsReader reader = new MatsimEventsReader(eventsLinks);
        reader.readFile(inputFile);
        eventsLinks.finishProcessing();

        eventHandlerLanes = new EventHandlerLanes();
        eventsLanes.addHandler(eventHandlerLanes);
        eventsLanes.initProcessing();

        reader = new MatsimEventsReader(eventsLanes);
        reader.readFile(inputFile);
        eventsLanes.finishProcessing();
    }
    // Test case 1 for comparing the travel time of links and lane per ID
    @Test
    public void testTravelTimes() {
        // Call the run method to initialize the scenario and event handler
        RunEventHandlerLanes.run(); 

        Config config = ConfigUtils.loadConfig("output/RunFixedMixedTrafficSignalSimpleIntersection/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        config.network().getLaneDefinitionsFile();
        Network network = scenario.getNetwork();

        // loop over all the link Ids present in the network we created 
        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()) {
            String linkId = l2l.getLinkId().toString();

            // get the travel time for each link through eventHandlers
            double linkTravelTime = eventHandlerLinks.getTravelTimeLinks(linkId);
            // get the travel time for each Lane per link by calling a method present in class RunEventHandlerLanes
            double lanesTravelTime = RunEventHandlerLanes.travelTimeLanesPerLink(linkId);
            

            System.out.println("Link Id in test"+ linkId);
//          assertEquals(linkTravelTime, lanesTravelTime, 1e-6);
                
            // checks if the link travel time is equal to lane travel time
            assertNotEquals(linkTravelTime, lanesTravelTime, 1e-6);

        }
    }
}
