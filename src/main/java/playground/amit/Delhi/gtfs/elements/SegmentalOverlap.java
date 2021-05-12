package playground.amit.Delhi.gtfs.elements;

import org.matsim.core.utils.collections.Tuple;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Amit on 10/05/2021.
 */
public class SegmentalOverlap {
    private final Segment segment;
    private int counter = 0;

    private Tuple<String, String> self_trip_routeId = null;
    private final Set<String> overlappingTripIds = new HashSet<>();
    private final Set<String> overlappingRouteIds = new HashSet<>();

    public SegmentalOverlap (Segment segment){
        this.segment = segment;
    }

    void overlapWith(String tripId, String routeId){
        counter++;
        this.overlappingTripIds.add(tripId);
        this.overlappingRouteIds.add(routeId);
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

    public Set<String> getOverlappingTripIds() {
        return overlappingTripIds;
    }

    public Set<String> getOverlappingRouteIds() {
        return overlappingRouteIds;
    }

    public Segment getSegment() {
        return segment;
    }
}
