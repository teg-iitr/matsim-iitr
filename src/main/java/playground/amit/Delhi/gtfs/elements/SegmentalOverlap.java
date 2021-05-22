package playground.amit.Delhi.gtfs.elements;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import playground.amit.Delhi.gtfs.GTFSOverlapOptimizer;

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
    private final List<String> overlappingRouteIds = new ArrayList<>();

    private final int max_warn_count = 1;
    private int warnCount = 0;

    public SegmentalOverlap (Segment segment){
        this.segment = segment;
    }

    void overlapWith(String tripId, String routeId){
        if(! routeId.equals(self_trip_routeId.getSecond())) {
            counter++;
            this.overlappingTripIds.add(tripId);
            this.overlappingRouteIds.add(routeId);
        }else{
            if (warnCount < max_warn_count) {
                warnCount++;
                GTFSOverlapOptimizer.LOG.warn("The trip id "+tripId+" belongs to own route "+this.self_trip_routeId.getSecond());
                if (warnCount == max_warn_count) {
                    GTFSOverlapOptimizer.LOG.warn(Gbl.FUTURE_SUPPRESSED);
                }
            }


        }
    }
    void self(String tripId, String routeId){
        if (this.self_trip_routeId!=null) throw new RuntimeException("The 'self' must be called only once.");
        this.self_trip_routeId = new Tuple<>(tripId, routeId);
    }
    void remove(String routeId, String tripId){
        this.counter--;
        this.overlappingRouteIds.remove(routeId);
        this.overlappingTripIds.remove(tripId);
    }

    public int getCount() {
        return counter;
    }

    public Tuple<String, String> getSelf_trip_routeId() {
        return self_trip_routeId;
    }

    public List<String> getOverlappingTripIds() {
        return overlappingTripIds;
    }

    public List<String> getOverlappingRouteIds() {
        return overlappingRouteIds;
    }

    public Segment getSegment() {
        return segment;
    }
}
