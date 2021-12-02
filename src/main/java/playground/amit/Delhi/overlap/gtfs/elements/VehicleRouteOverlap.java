package playground.amit.Delhi.overlap.gtfs.elements;

import org.matsim.api.core.v01.Id;
import org.matsim.pt2matsim.gtfs.lib.Trip;

import java.util.*;

/**
 * Created by Amit on 10/05/2021.
 */
public class VehicleRouteOverlap {
    private final String vehicleNumber;
    private final Map<String, List<Id<Trip>>> routeId2Trips = new HashMap<>(); // a vehicle may be serving multiple routes (eg., up and down)
    private final Map<Id<Trip>, Map<SigmoidFunction, Double>> tripId2Probs = new HashMap<>();
    private final Map<SigmoidFunction, Double> routeProb = new HashMap<>();

    /**
     * Use if vehicle information is unavailable, a vehicle will be created with number same as that of route.
     * @param routeId
     */
//    public VehicleRouteOverlap(String routeId){
//        this.vehicleNumber = routeId;
//        addRouteToVehicle(routeId);
//    }

    public VehicleRouteOverlap(String vehicleNumber, String routeId){
        this.vehicleNumber = vehicleNumber;
        addRouteToVehicle(routeId);
    }

    public void addRouteToVehicle(String routeId){
        this.routeId2Trips.put(routeId, new ArrayList<>());
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

    public void addProbsToTrip(String routeId, Id<Trip> tripId, Map<SigmoidFunction, Double> probs ){
        if (this.routeId2Trips.get(routeId)==null){
            throw new RuntimeException("Route Id "+routeId+" is not found.");
        } else{
            this.routeId2Trips.get(routeId).add(tripId);
        }
        this.tripId2Probs.put(tripId, probs);
    }

    public int getNumberOfTrips(){
        return this.tripId2Probs.size();
    }

//    /**
//     * Trips ids of the route.
//     * @return
//     */
//    public Map<Id<Trip>, Map<SigmoidFunction, Double>> getTripId2Probs() {
//        return tripId2Probs;
//    }

    public Set<String> getRoutes(){
        return this.routeId2Trips.keySet();
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }
}
