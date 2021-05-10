package playground.amit.Delhi.gtfs.elements;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt2matsim.gtfs.lib.Stop;
import org.matsim.pt2matsim.gtfs.lib.StopTime;
import org.matsim.pt2matsim.gtfs.lib.Trip;
import playground.amit.Delhi.gtfs.MyStopImpl;
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
        to.setRouteId(trip.getRoute().getId());
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
}
