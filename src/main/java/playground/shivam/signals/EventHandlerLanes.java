package playground.shivam.signals;

import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;

public class EventHandlerLanes implements LaneEnterEventHandler, LaneLeaveEventHandler, LinkLeaveEventHandler {

    @Override
    public void handleEvent(LaneEnterEvent laneEnterEvent) {
        String s = laneEnterEvent.getEventType().toString();
        System.out.println("hey");
    }

    @Override
    public void handleEvent(LaneLeaveEvent laneLeaveEvent) {
        System.out.println("madhu");
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        System.out.println("in Link Eneter Event");
    }
}
