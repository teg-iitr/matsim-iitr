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

    @Test
    public void testTravelTimes() {
        RunEventHandlerLanes.run(); // Call the run method to initialize the scenario and event handler

        Config config = ConfigUtils.loadConfig("output/RunFixedMixedTrafficSignalSimpleIntersection/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        config.network().getLaneDefinitionsFile();
        Network network = scenario.getNetwork();


        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()) {
            String linkId = l2l.getLinkId().toString();

            double linkTravelTime = eventHandlerLinks.getTravelTimeLinks(linkId);
            double lanesTravelTime = RunEventHandlerLanes.travelTimeLanesPerLink(linkId);
            // Test 1 for comparing the travel time of

            System.out.println("Link Id in test"+ linkId);
//            assertEquals(linkTravelTime, lanesTravelTime, 1e-6);

            assertNotEquals(linkTravelTime, lanesTravelTime, 1e-6);

        }
    }
}
