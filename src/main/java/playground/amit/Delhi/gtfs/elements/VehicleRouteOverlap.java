package playground.amit.Delhi.gtfs.elements;

import org.matsim.api.core.v01.Id;
import org.matsim.pt2matsim.gtfs.lib.Trip;
import playground.amit.Delhi.gtfs.SigmoidFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amit on 10/05/2021.
 */
public class VehicleRouteOverlap {
    private final String id;
    private final Map<Id<Trip>, Map<SigmoidFunction, Double>> tripId2Probs = new HashMap<>();
    private final Map<SigmoidFunction, Double> routeProb = new HashMap<>();

    public VehicleRouteOverlap(String id){
        this.id = id;
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

    /**
     * Trips ids of the route.
     * @return
     */
    public Map<Id<Trip>, Map<SigmoidFunction, Double>> getTripId2Probs() {
        return tripId2Probs;
    }

    public String getId() {
        return id;
    }
}
