package playground.amit.Delhi.gtfs;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt2matsim.gtfs.lib.Route;
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
    /*
     * To ensure only one check for each segment.
     */
    private final Map<Segment, Integer> verifiedSegments = new HashMap<>();

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
                prevStopTime = c;
            }
        }
        this.trip2tripOverlap.put(trip_id, to);
    }

    // determine the overlap now, i.e., go through with each trip and check the segments with
    public void evaluate() {
        for ( String tripId : this.trip2tripOverlap.keySet() ) {
            TripOverlap current = this.trip2tripOverlap.get(tripId);
            Set<Segment> segs = current.getSegment2counts().keySet();
            for (Segment seg : segs) {
                Integer cnt = verifiedSegments.getOrDefault(seg, 1);
                if (cnt!=1) {
                    current.getSegment2counts().put(seg, cnt);
                } else {
                    // go through all trips and all segments
                    for (TripOverlap tripOverlap : this.trip2tripOverlap.values()) {
                        if (current!=tripOverlap) {
                           if (tripOverlap.getSegment2counts().containsKey(seg)) {
                               cnt++;
                           }
                        }
                    }
                    current.getSegment2counts().put(seg,cnt);
                    this.verifiedSegments.put(seg, cnt);
                }
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
