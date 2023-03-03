package playground.shivam.signals;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

import java.util.HashMap;

public class EventHandlerLinks implements LinkEnterEventHandler, LinkLeaveEventHandler {
    private double travelTime = 0.0;
    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        // Calculate travelTime of Link (2, 3)
        String linkId = linkEnterEvent.getLinkId().toString();
        if(linkId.equals("2_3")){
            System.out.println("LinkEnterEvent");
            System.out.println("Time: " + linkEnterEvent.getTime());
            travelTime += linkEnterEvent.getTime();
            System.out.println("LinkId: " + linkEnterEvent.getLinkId());
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        // Calculate travelTime of Link (2, 3)
        String linkId = linkLeaveEvent.getLinkId().toString();
        if(linkId.equals("2_3")){
            System.out.println("LinkLeaveEvent");
            System.out.println("Time: " + linkLeaveEvent.getTime());
            travelTime -= linkLeaveEvent.getTime();
            System.out.println("LinkId: " + linkLeaveEvent.getLinkId());
        }
    }
    public double getTravelTimeLinks(){
        return travelTime;
    }
}
