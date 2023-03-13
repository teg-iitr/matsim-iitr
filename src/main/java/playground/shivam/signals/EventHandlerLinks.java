package playground.shivam.signals;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import java.util.HashMap;

public class EventHandlerLinks implements LinkEnterEventHandler, LinkLeaveEventHandler {
    private HashMap<String, Double>entryTime = new HashMap<>();
    private HashMap<String, Double>exitTime = new HashMap<>();
    private HashMap<String, Integer> numVehicles = new HashMap<>();

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        String linkId = linkEnterEvent.getLinkId().toString();
        if(linkId.equals("2_3") || linkId.equals("8_3") || linkId.equals("4_3") || linkId.equals("7_3")){
//            System.out.println("LinkId: " + linkEnterEvent.getLinkId() + "  LinkEnterEvent  " + "Time  " + linkEnterEvent.getTime());
            double travelTime = entryTime.getOrDefault(linkId, 0.0);
            entryTime.put(linkId, travelTime + linkEnterEvent.getTime());
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        String linkId = linkLeaveEvent.getLinkId().toString();
        if(linkId.equals("2_3") || linkId.equals("8_3") || linkId.equals("4_3") || linkId.equals("7_3")){
//            System.out.println("LinkId: " + linkLeaveEvent.getLinkId() + "  LinkLeaveEvent  " + "Time  " + linkLeaveEvent.getTime());
            double travelTime = exitTime.getOrDefault(linkId, 0.0);
            exitTime.put(linkId, travelTime+linkLeaveEvent.getTime());

            int count = numVehicles.getOrDefault(linkId, 0);
            numVehicles.put(linkId, count + 1);
        }
    }
    public double getTravelTimeLinks(String linkId){
        double totalTravelTime = Math.abs(exitTime.get(linkId) - entryTime.get(linkId));
        int totalNumVehicles = numVehicles.get(linkId);
        return totalTravelTime / totalNumVehicles;
    }
}