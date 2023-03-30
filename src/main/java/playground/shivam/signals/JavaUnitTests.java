package playground.shivam.signals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class JavaUnitTests {

    private EventHandlerLinks eventHandlerLinks;
//    private EventHandlerLanes eventHandlerLanes;
    private static EventHandlerLanes eventHandlerLanes;
    private final EventsManager eventsLinks = EventsUtils.createEventsManager();
//    private final EventsManager eventsLanes = EventsUtils.createEventsManager();

    public JavaUnitTests() {

    }

    @Test
    public void testTravelTimes1() { // Length diff: lane=500 & link=1000 capacityboth=1000
        String inputFile = "output/LengthDiff/output_events.xml.gz";
        eventHandlerLinks = new EventHandlerLinks();
        eventsLinks.addHandler(eventHandlerLinks);
        eventsLinks.initProcessing();

        MatsimEventsReader reader = new MatsimEventsReader(eventsLinks);
        reader.readFile(inputFile);
        eventsLinks.finishProcessing();

        eventHandlerLanes = new EventHandlerLanes();
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(eventHandlerLanes);

        //create the reader and read the file
        events.initProcessing();
        LanesEventReader reader2 = new LanesEventReader(events);

        reader2.readFile(inputFile);
        events.finishProcessing();

        double tt1 = eventHandlerLinks.getTravelTimeLinks("2_3");
        System.out.println(eventHandlerLanes.getTravelTimeLanes("2_3.l"));
        assertEquals(eventHandlerLanes.getTravelTimeLanes("2_3.l"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("2_3.s"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("2_3.r"), tt1, 1e-6);
    }
    @Test
    public void testTravelTimes2() { // When length same = 500 & capacityboth=1000
        String inputFile = "output/LengthSame/output_events.xml.gz";
        eventHandlerLinks = new EventHandlerLinks();
        eventsLinks.addHandler(eventHandlerLinks);
        eventsLinks.initProcessing();

        MatsimEventsReader reader = new MatsimEventsReader(eventsLinks);
        reader.readFile(inputFile);
        eventsLinks.finishProcessing();

        eventHandlerLanes = new EventHandlerLanes();
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(eventHandlerLanes);

        //create the reader and read the file
        events.initProcessing();
        LanesEventReader reader2 = new LanesEventReader(events);

        reader2.readFile(inputFile);
        events.finishProcessing();

        double tt1 = eventHandlerLinks.getTravelTimeLinks("4_3");

        assertEquals(eventHandlerLanes.getTravelTimeLanes("4_3.l"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("4_3.s"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("4_3.r"), tt1, 1e-6);
    }
    @Test
    public void testTravelTimes3(){ // capacity same = 1000 & length link=1000 lane = 500
        String inputFile = "output/case3/output_events.xml.gz";
        eventHandlerLinks = new EventHandlerLinks();
        eventsLinks.addHandler(eventHandlerLinks);
        eventsLinks.initProcessing();

        MatsimEventsReader reader = new MatsimEventsReader(eventsLinks);
        reader.readFile(inputFile);
        eventsLinks.finishProcessing();

        eventHandlerLanes = new EventHandlerLanes();
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(eventHandlerLanes);

        //create the reader and read the file
        events.initProcessing();
        LanesEventReader reader2 = new LanesEventReader(events);

        reader2.readFile(inputFile);
        events.finishProcessing();

        double tt1 = eventHandlerLinks.getTravelTimeLinks("8_3");

        assertEquals(eventHandlerLanes.getTravelTimeLanes("8_3.l"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("8_3.s"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("8_3.r"), tt1, 1e-6);
    }
    @Test
    public void testTravelTimes4(){ // capacity diff = 1000lane, 500link & length=500
        String inputFile = "output/CapacityDiff/output_events.xml.gz";
        eventHandlerLinks = new EventHandlerLinks();
        eventsLinks.addHandler(eventHandlerLinks);
        eventsLinks.initProcessing();

        MatsimEventsReader reader = new MatsimEventsReader(eventsLinks);
        reader.readFile(inputFile);
        eventsLinks.finishProcessing();

        eventHandlerLanes = new EventHandlerLanes();
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(eventHandlerLanes);

        //create the reader and read the file
        events.initProcessing();
        LanesEventReader reader2 = new LanesEventReader(events);

        reader2.readFile(inputFile);
        events.finishProcessing();

        double tt1 = eventHandlerLinks.getTravelTimeLinks("7_3");

        assertEquals(eventHandlerLanes.getTravelTimeLanes("7_3.l"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("7_3.s"), tt1, 1e-6);
        assertEquals(eventHandlerLanes.getTravelTimeLanes("7_3.r"), tt1, 1e-6);
    }
}
