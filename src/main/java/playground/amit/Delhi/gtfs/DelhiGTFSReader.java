package playground.amit.Delhi.gtfs;

import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;

import java.io.File;
import java.util.Map;

/**
 * Created by Amit on 23/04/2021
 */
public class DelhiGTFSReader {

    private static final String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_DIMTS_18042021.zip";
    private static final double timebinSize = 2*60.; // 2min

    public static void main(String[] args) {
        GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);
        SpatialOverlap spatialOverlap = new SpatialOverlap(timebinSize);
        // go through with trips because a trip is an instance of a vehicle
        gtfsFeed.getTrips().forEach((key, value) -> spatialOverlap.add(key, value));
        spatialOverlap.evaluate();
        Map<String, SpatialOverlap.TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
    }
}
