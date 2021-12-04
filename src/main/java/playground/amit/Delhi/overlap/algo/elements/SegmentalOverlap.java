package playground.amit.Delhi.overlap.algo.elements;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import playground.amit.Delhi.overlap.algo.optimizer.OverlapOptimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amit on 10/05/2021.
 */
public class SegmentalOverlap {
    private final Segment segment;
    private int counter = 0;

    private Tuple<String, String> self_trip_routeId = null;
    //using list to store duplicate IDs so that some elements are still there if a route/trip is removed.
    private final List<String> overlappingTripIds = new ArrayList<>();
    private final List<String> overlappingVehicleRouteIds = new ArrayList<>();

    private static final int max_warn_count = 1;
    private int warnCount = 0;

    public SegmentalOverlap (Segment segment){
        this.segment = segment;
    }

    void overlapWith(String tripId, String vehicleNumber){
        if(! vehicleNumber.equals(self_trip_routeId.getSecond())) {
            this.counter++;
            this.overlappingTripIds.add(tripId);
            this.overlappingVehicleRouteIds.add(vehicleNumber);
        }else{
            if (warnCount < max_warn_count) {
                warnCount++;
                OverlapOptimizer.LOG.warn("The trip id "+tripId+" belongs to own route "+this.self_trip_routeId.getSecond());
                if (warnCount == max_warn_count) {
                    OverlapOptimizer.LOG.warn(Gbl.FUTURE_SUPPRESSED);
                }
            }
        }
    }
    void self(String tripId, String vehicleNumber){
        if (this.self_trip_routeId!=null) throw new RuntimeException("The 'self' must be called only once.");
        this.counter++;
        this.self_trip_routeId = new Tuple<>(tripId, vehicleNumber);
    }
    void remove(String vehicleNumber, String tripId){
        if(! vehicleNumber.equals(self_trip_routeId.getSecond())) {
            // since overlap is not counted for self_routes, it should not be removed.
            this.counter--;
        }
        this.overlappingVehicleRouteIds.remove(vehicleNumber);
        this.overlappingTripIds.remove(tripId);
    }

    public int getCount() {
        return counter;
    }

    public Tuple<String, String> getSelfTripVehicleRoute() {
        return self_trip_routeId;
    }

    public List<String> getOverlappingTripIds() {
        return overlappingTripIds;
    }

    public List<String> getOverlappingVehicleRouteIds() {
        return overlappingVehicleRouteIds;
    }

    public Segment getSegment() {
        return segment;
    }
}
