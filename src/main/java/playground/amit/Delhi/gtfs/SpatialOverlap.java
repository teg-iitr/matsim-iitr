package playground.amit.Delhi.gtfs;

import org.matsim.api.core.v01.Id;
import org.matsim.pt2matsim.gtfs.lib.StopTime;
import org.matsim.pt2matsim.gtfs.lib.Trip;

import java.util.*;

/**
 * Created by Amit on 23/04/2021
 */
public class SpatialOverlap {

    public SpatialOverlap(double timebinSize) {
        this.timebinSize = timebinSize;
    }

    private final double timebinSize;
    private final Map<String, TripOverlap> trip2tripOverlap = new HashMap<>();
    private final Map<Segment, Integer> collectedSegments = new HashMap<>();

    private int getTimeBin(double time_sec){
        return (int) (time_sec/timebinSize);
    }

    public void add(String trip_id, Trip trip) {
        TripOverlap to = new TripOverlap(Id.create(trip_id, Trip.class));
        NavigableSet<StopTime> stopTimes = trip.getStopTimes();
        StopTime prevStopTime = null;
        for (StopTime c : stopTimes){
            if (prevStopTime==null) prevStopTime = c;
            else {
                Segment seg = new Segment(prevStopTime.getStop(), c.getStop(), getTimeBin(prevStopTime.getDepartureTime()));
                to.getSegment2counts().put(seg, 1);
                int cnt = this.collectedSegments.getOrDefault(seg, 0);
                this.collectedSegments.put(seg, cnt+1); // cannot put back in TripOverlay already here because it's keep updating
                prevStopTime = c;
            }
        }
        this.trip2tripOverlap.put(trip_id, to);
    }
    
    public void collectOverlaps() {
    	for ( String tripId : this.trip2tripOverlap.keySet() ) {
            TripOverlap current = this.trip2tripOverlap.get(tripId);
            Set<Segment> segs = current.getSegment2counts().keySet();
            for (Segment seg : segs) {
                Integer cnt = collectedSegments.getOrDefault(seg, 1);
                current.getSegment2counts().put(seg, cnt);
            }
    	}
    }

    public Map<String, TripOverlap> getTrip2tripOverlap() {
        return trip2tripOverlap;
    }

    public static class TripOverlap {

        private final Map<Segment, Integer> segment2counts = new HashMap<>();
        private final Id<Trip> tripId;

        TripOverlap(Id<Trip> tripId) {
            this.tripId = tripId;
        }
        Map<Segment, Integer> getSegment2counts() {
            return this.segment2counts;
        }
        public Id<Trip> getTripId(){
            return this.tripId;
        }
    }
}
