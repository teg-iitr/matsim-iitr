package org.matsim.api.core.v01.events.handler;

import org.matsim.api.core.v01.events.LaneEnterEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.core.events.handler.EventHandler;

public interface LaneEnterEventHandler extends EventHandler {
    void handleEvent(LaneEnterEvent laneEnterEvent);
}
