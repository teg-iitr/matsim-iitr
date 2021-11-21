package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.network.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Amit
 */

/**
 * Probably, not using this class anywhere; it may be used to create productions and attractions at zonal levels.
 */
public class Zone{
    private final String zoneId;
    private final Map<String, Double> modeToIncomingTrips = new HashMap<>();
    private final Map<String, Double> modeToOutgoingTrips = new HashMap<>();
    private Node nearestMetroNode;

    Zone(String zoneId){
        this.zoneId = zoneId;
    }

    String getZoneId() {
        return zoneId;
    }

    void addIncomingTrips(String mode, double numberOfTrips){
        this.modeToIncomingTrips.put(mode, getIncomingTrips(mode)+numberOfTrips);
    }

    void addOutgoingTrips(String mode, double numberOfTrips){
        this.modeToOutgoingTrips.put(mode, getOutgoingTrips(mode)+numberOfTrips);
    }

    double getIncomingTrips(String mode){
        return this.modeToIncomingTrips.getOrDefault(mode,0.);
    }

    double getOutgoingTrips(String mode){
        return this.modeToOutgoingTrips.getOrDefault(mode,0.);
    }

    public Node getNearestMetroNode() {
        return nearestMetroNode;
    }

    public void setNearestMetroNode(Node nearestMetroNode) {
        this.nearestMetroNode = nearestMetroNode;
    }
}