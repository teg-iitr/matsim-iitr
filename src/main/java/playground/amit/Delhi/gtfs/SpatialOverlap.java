package playground.amit.Delhi.gtfs;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt2matsim.gtfs.lib.Stop;
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
    private final Map<Segment, SegmentalOverlap> collectedSegments = new HashMap<>();

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
                Stop prevStop = wrapper(prevStopTime.getStop());
                Stop stop = wrapper(c.getStop());
                Segment seg = new Segment(prevStop,
                        stop, getTimeBin(prevStopTime.getDepartureTime()));

                seg.setTimeSpentOnSegment(c.getArrivalTime()- prevStopTime.getDepartureTime());
                seg.setStopSequence(new Tuple<>(prevStopTime.getSequencePosition(), c.getSequencePosition()));
                seg.setLength(GeometryUtils.getGeoDaticDistance(prevStop.getLat(), prevStop.getLon(),
                        stop.getLat(), stop.getLon()));

                to.getSegments().add(seg); // must be unique for the trip

                SegmentalOverlap soverlap = this.collectedSegments.get(seg);
                if (soverlap==null){
                    soverlap = new SegmentalOverlap(seg);
                    soverlap.self(trip_id, trip.getRoute().getId());
                } else{
                    soverlap.overlapWith(trip_id, trip.getRoute().getId());
                }
                this.collectedSegments.put(seg, soverlap); // cannot put back in TripOverlay already here because segments are keep updating
                prevStopTime = c;
            }
        }
        this.trip2tripOverlap.put(trip_id, to);
    }

    private MyStopImpl wrapper(Stop stop){
        return new MyStopImpl(stop.getId(), stop.getName(), stop.getLon(), stop.getLat());
    }

    public void collectOverlaps() {
    	for ( String tripId : this.trip2tripOverlap.keySet() ) {
            TripOverlap current = this.trip2tripOverlap.get(tripId);
            for (Segment seg : current.getSegments()) {
                SegmentalOverlap so = this.collectedSegments.get(seg); //must noe be null
                if (so == null){
                    throw new RuntimeException("There must be segmental overlap against segment "+seg);
                }
                current.getSeg2overlaps().put(seg,so);
            }
    	}
    }

    public Map<String, TripOverlap> getTrip2tripOverlap() {
        return trip2tripOverlap;
    }

    public static class SegmentalOverlap {
        private final Segment segment;
        private int counter = 0;

        private Tuple<String, String> self_trip_routeId = null;
        private Set<String> overlappingTripIds = new HashSet<>();
        private Set<String> overlappingRouteIds = new HashSet<>();

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
    }

    public static class TripOverlap {

        private final List<Segment> segments = new ArrayList<>();
        private final Id<Trip> tripId;
        private final Map<Segment, SegmentalOverlap> seg2overlaps = new LinkedHashMap<>();

        TripOverlap(Id<Trip> tripId) {
            this.tripId = tripId;
        }
        List<Segment> getSegments() {
            return this.segments;
        }
        Id<Trip> getTripId(){
            return this.tripId;
        }

        public Map<Segment, SegmentalOverlap> getSeg2overlaps() {
            return seg2overlaps;
        }
    }
}
