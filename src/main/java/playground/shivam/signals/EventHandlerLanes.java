package playground.shivam.signals;

import org.matsim.api.core.v01.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;


public class EventHandlerLanes implements LaneLeaveEventHandler, LaneEnterEventHandler {
    @Override
    public void handleEvent(LaneLeaveEvent laneLeaveEvent) {
        System.out.println("hello");
    }

    @Override
    public void handleEvent(org.matsim.api.core.v01.events.LaneEnterEvent laneEnterEvent) {
        System.out.println(laneEnterEvent.getLaneId().toString());
    }
}
