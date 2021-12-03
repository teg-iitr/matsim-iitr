package playground.amit.Delhi.overlap.gtfs.elements;

import org.matsim.api.core.v01.Id;
import org.matsim.pt2matsim.gtfs.lib.Trip;

import java.util.*;

/**
 * Created by Amit on 10/05/2021.
 */
public class VehicleRouteOverlap {
    private final String vehicleNumber;
    private final Map<String, List<Id<Trip>>> routeName2Trips = new HashMap<>(); // a vehicle may be serving multiple routes (eg., up and down)
    private final Map<Id<Trip>, Map<SigmoidFunction, Double>> tripId2Probs = new HashMap<>();
    private final Map<SigmoidFunction, Double> routeProb = new HashMap<>();

    public VehicleRouteOverlap(String vehicleNumber){
        this.vehicleNumber = vehicleNumber;
    }

    public Map<SigmoidFunction, Double> getVRProb(){
        if (routeProb.isEmpty()) {
            for(SigmoidFunction sigmoidFunction : SigmoidFunction.values()) {
                double prob = 1.0;
                for (Id<Trip> t : tripId2Probs.keySet()) {
                    prob *= tripId2Probs.get(t).get(sigmoidFunction);
                }
                routeProb.put(sigmoidFunction, prob);
            }
        }
        return  routeProb;
    }

    public void addProbsToTrip(String routeName, Id<Trip> tripId, Map<SigmoidFunction, Double> probs ){
        this.routeName2Trips.computeIfAbsent(routeName, k -> new ArrayList<>());

        this.routeName2Trips.get(routeName).add(tripId);
        this.tripId2Probs.put(tripId, probs);
    }

    public int getNumberOfTrips(){
        return this.tripId2Probs.size();
    }

    public Set<String> getRoutes(){
        return this.routeName2Trips.keySet();
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }
}
