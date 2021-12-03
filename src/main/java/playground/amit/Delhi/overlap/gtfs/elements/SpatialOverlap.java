package playground.amit.Delhi.overlap.gtfs.elements;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt2matsim.gtfs.lib.Stop;
import org.matsim.pt2matsim.gtfs.lib.StopTime;
import org.matsim.pt2matsim.gtfs.lib.Trip;
import playground.amit.Delhi.overlap.gtfs.MyStopImpl;
import playground.amit.utils.geometry.GeometryUtils;

import java.util.*;

/**
 * Created by Amit on 23/04/2021
 */
public class SpatialOverlap {

    public SpatialOverlap(int timebinSize) {
        this.timebinSize = timebinSize;
    }

    private final int timebinSize;
    private final Map<String, TripOverlap> trip2tripOverlap = new LinkedHashMap<>();
    private final Map<String, Set<String>> vehicleRoute2TripsIds = new HashMap<>();
    private final Map<Segment, SegmentalOverlap> collectedSegments = new HashMap<>();

    private int getTimeBin(double time_sec){
        // in matsim, pt works from 4 to 27, so need to be converted to 0 to 24.
        if (time_sec >= 86400) {
            time_sec = time_sec - 86400;
        }
        return (int) (time_sec/timebinSize);
    }

    public void add(Trip trip, String vehicleNumber) {
        String trip_id = trip.getId();
        TripOverlap to = new TripOverlap(Id.create(trip_id, Trip.class));
        to.setRouteLongName(trip.getRoute().getLongName());
        to.setVehicleNumber(vehicleNumber);

        // store route and trips
        Set<String> trips = this.vehicleRoute2TripsIds.getOrDefault(vehicleNumber, new HashSet<>());
        trips.add(trip_id);
        this.vehicleRoute2TripsIds.put(vehicleNumber, trips);

        // create and store segments
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
                    soverlap.self(trip_id, vehicleNumber);
                } else{
                    soverlap.overlapWith(trip_id, vehicleNumber);
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
                SegmentalOverlap so = this.collectedSegments.get(seg); //must not be null
                if (so == null){
                    throw new RuntimeException("There must be segmental overlap against segment "+seg);
                }
                current.getSeg2overlaps().put(seg,so);
            }
    	}
    }

    public void removeVehicle(String vehicleNumber){
        Set<String> trips2Remove = this.vehicleRoute2TripsIds.remove(vehicleNumber);
        for (String trip_id : trips2Remove) {
            TripOverlap removedTO = this.trip2tripOverlap.remove(trip_id);
            for (Segment removedSeg : removedTO.getSegments()) {
                this.collectedSegments.get(removedSeg).remove(vehicleNumber, removedTO.getTripId().toString());
            }
        }
    }

    public Map<Segment, SegmentalOverlap> getCollectedSegments() {
        return collectedSegments;
    }

    public Map<String, TripOverlap> getTrip2tripOverlap() {
        return trip2tripOverlap;
    }

    public Map<String, Set<String>> getVehicleRoute2TripsIds() {
        return vehicleRoute2TripsIds;
    }
}
