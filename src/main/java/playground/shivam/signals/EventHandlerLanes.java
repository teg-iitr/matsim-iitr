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
import java.util.HashSet;

public class EventHandlerLanes implements LaneEnterEventHandler, LaneLeaveEventHandler {
    private HashMap<String, Double> entryTime = new HashMap<>();
    private HashMap<String, Double>exitTime = new HashMap<>();
    private static Config config = ConfigUtils.loadConfig("output/RunFixedMixedTrafficSignalSimpleIntersection/config.xml");
    private static Scenario scenario = ScenarioUtils.loadScenario(config);
    private static Network network;

    private static HashMap<String, Integer> numVehicles = new HashMap<>();
    private static HashSet<String>allLanes = new HashSet<>();

    @Override
    public void handleEvent(LaneEnterEvent event) {
        config.network().getLaneDefinitionsFile();
        network = scenario.getNetwork();

        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()){
            for (Lane lane : l2l.getLanes().values()) {
                String laneId = lane.getId().toString();
                allLanes.add(laneId);
            }
        }
        String currentLaneId = event.getLaneId().toString();

        for (String lanes : allLanes) {
            if (currentLaneId.equals(lanes)) {
                double travelTime = entryTime.getOrDefault(currentLaneId, 0.0);
                entryTime.put(currentLaneId, travelTime + event.getTime());
            }
        }
    }

    @Override
    public void handleEvent(LaneLeaveEvent event) {
        config.network().getLaneDefinitionsFile();
        network = scenario.getNetwork();

        for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()){
            for (Lane lane : l2l.getLanes().values()) {
                String laneId = lane.getId().toString();
                allLanes.add(laneId);
            }
        }
        String currentLaneId = event.getLaneId().toString();
        for (String lanes : allLanes) {
            if (currentLaneId.equals(lanes)) {
                double travelTime = exitTime.getOrDefault(currentLaneId, 0.0);
                exitTime.put(currentLaneId, travelTime + event.getTime());

                int count = numVehicles.getOrDefault(currentLaneId, 0);
                numVehicles.put(currentLaneId, count + 1);
            }
        }
    }

    public double getTravelTimeLanes(String laneId){
        double totalTravelTime = Math.abs(exitTime.get(laneId) - entryTime.get(laneId));
        int totalNumVehicles = numVehicles.get(laneId);
        return totalTravelTime / totalNumVehicles;
    }
}
