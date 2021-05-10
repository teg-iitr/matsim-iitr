package playground.amit.Delhi.gtfs.elements;

import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import playground.amit.Delhi.gtfs.SigmoidFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Amit on 10/05/2021.
 */
public class VehicleRouteOverlapCalculator {

    // need to have vehicleRoute to trips because segments are linked to trips
    private static final Map<String, Set<String>> vehicleRoute2Trips = new HashMap<>();
    private final Map<String, Double> vehicleRoute2probs = new HashMap<>();

    private static final String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_DIMTS_18042021.zip";
    private static final double timebinSize = 24*60*60.; // 2min
    private final String date = "08052021";
    private static final String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_overlap_24h-timebin.txt";

    public static void main(String[] args) {
        GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);
        SpatialOverlap spatialOverlap = new SpatialOverlap(timebinSize);
        // go through with trips because a trip is an instance of a vehicle
        gtfsFeed.getTrips().forEach(spatialOverlap::add);

        gtfsFeed.getRoutes().values().forEach(r->
            vehicleRoute2Trips.put(r.getId(), r.getTrips().keySet())
        );

        System.out.println("Evaluating overlaps and overlaps probabilities to a file ...");
        spatialOverlap.collectOverlaps();
        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        for (TripOverlap to: trip2tripOverlap.values()) {
            String routeId = to.getRouteId();
            VehicleRouteOverlap vrOverlap = new VehicleRouteOverlap(routeId);
            Map<SigmoidFunction, Double> sigmoidFunctionDoubleMap = vrOverlap.getTripId2Probs().get(to.getTripId());

        }




    }


}

