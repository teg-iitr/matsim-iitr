package playground.shivam.signals;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;

import java.util.HashMap;

public class EventHandlerLanes implements LaneEnterEventHandler, LaneLeaveEventHandler {
    private HashMap<String, Double> entryTime = new HashMap<>();
    private HashMap<String, Double>exitTime = new HashMap<>();
    private HashMap<String, Integer> numVehicles = new HashMap<>();
    @Override
    public void handleEvent(LaneEnterEvent event) {
        String laneId = event.getLaneId().toString();

        if(laneId.equals("2_3.l") || laneId.equals("2_3.r") || laneId.equals("2_3.ol") || laneId.equals("2_3.s") ||
                laneId.equals("4_3.l") || laneId.equals("4_3.r") || laneId.equals("4_3.s") ||  laneId.equals("2_3.ol")
                || laneId.equals("7_3.l") || laneId.equals("7_3.r") || laneId.equals("7_3.s") ||  laneId.equals("7_3.ol")
                || laneId.equals("8_3.l") || laneId.equals("8_3.r") || laneId.equals("8_3.s") || laneId.equals("8_3.ol")){
            entryTime.put(laneId, event.getTime());
        }
    }

    @Override
    public void handleEvent(LaneLeaveEvent event) {
        String laneId = event.getLaneId().toString();
        if(laneId.equals("2_3.l") || laneId.equals("2_3.r") || laneId.equals("2_3.ol") || laneId.equals("2_3.s") ||
                laneId.equals("4_3.l") || laneId.equals("4_3.r") || laneId.equals("4_3.s") ||  laneId.equals("2_3.ol")
              || laneId.equals("7_3.l") || laneId.equals("7_3.r") || laneId.equals("7_3.s") ||  laneId.equals("7_3.ol")
              || laneId.equals("8_3.l") || laneId.equals("8_3.r") || laneId.equals("8_3.s") || laneId.equals("8_3.ol") ){
//            System.out.println("LaneId: " + event.getLaneId() + "  LaneLeaveEvent  " + "Time  " + event.getTime());
            double travelTime = exitTime.getOrDefault(laneId, 0.0);
            exitTime.put(laneId, travelTime+event.getTime());

            int count = numVehicles.getOrDefault(laneId, 0);
            numVehicles.put(laneId, count + 1);
        }
    }
    public double getTravelTimeLanes(String laneId){
        if(entryTime.get(laneId) == null && exitTime.get(laneId) == null){
            return 0.0;
        }
        double totalTravelTime = Math.abs(exitTime.get(laneId) - entryTime.get(laneId));
        int totalNumVehicles = numVehicles.get(laneId);
//        System.out.println("Inside lanes Event Handler" + totalTravelTime);

        return totalTravelTime / totalNumVehicles;
    }
}
