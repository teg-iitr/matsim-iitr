package playground.amit.Delhi.gtfs;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt2matsim.gtfs.lib.StopTime;
import org.matsim.pt2matsim.gtfs.lib.Trip;
import playground.amit.utils.geometry.GeometryUtils;

import java.util.*;

/**
 * Created by Amit on 23/04/2021
 */
public class SpatialOverlap {

    public SpatialOverlap(double timebinSize) {
        this.timebinSize = timebinSize;
    }

    private final double timebinSize;
    private final Map<String, TripOverlap> trip2tripOverlap = new LinkedHashMap<>();
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
                MyStop prevStop = (MyStop) prevStopTime.getStop();
                MyStop stop = (MyStop) c.getStop();
                Segment seg = new Segment(prevStop, stop, getTimeBin(prevStopTime.getDepartureTime()));

                seg.setTimeSpentOnSegment(c.getArrivalTime()- prevStopTime.getDepartureTime());
                seg.setStopSequence(new Tuple<>(prevStopTime.getSequencePosition(), c.getSequencePosition()));
                seg.setLength(GeometryUtils.getGeoDaticDistance(prevStop.getLat(), prevStop.getLon(),
                        stop.getLat(), stop.getLon()));

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
            for (Segment seg : current.getSegment2counts().keySet()) {
                Integer cnt = collectedSegments.getOrDefault(seg, 1);
                current.getSegment2counts().put(seg, cnt);
            }
    	}
    }

    public Map<String, TripOverlap> getTrip2tripOverlap() {
        return trip2tripOverlap;
    }

    public static class TripOverlap {

        private final Map<Segment, Integer> segment2counts = new LinkedHashMap<>();
        private final Id<Trip> tripId;

        TripOverlap(Id<Trip> tripId) {
            this.tripId = tripId;
        }
        Map<Segment, Integer> getSegment2counts() {
            return this.segment2counts;
        }
        Id<Trip> getTripId(){
            return this.tripId;
        }
    }
}
