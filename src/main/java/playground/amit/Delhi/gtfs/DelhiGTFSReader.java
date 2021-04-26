package playground.amit.Delhi.gtfs;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import playground.amit.Delhi.gtfs.SpatialOverlap.TripOverlap;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Amit on 23/04/2021
 */
public class DelhiGTFSReader {

    private static final String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_DIMTS_18042021.zip";
    private static final double timebinSize = 2*60.; // 2min

    private static final String outFile = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_overlap.txt";
    
    public static void main(String[] args) {
        GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);
        SpatialOverlap spatialOverlap = new SpatialOverlap(timebinSize);
        // go through with trips because a trip is an instance of a vehicle
        gtfsFeed.getTrips().forEach((key, value) -> spatialOverlap.add(key, value));
        System.out.println("Evaluating overlaps...");
        spatialOverlap.evaluate();
        Map<String, SpatialOverlap.TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        
        System.out.println("Writing overlaps to a file ...");
        BufferedWriter writer  = IOUtils.getBufferedWriter(outFile);
        try {
        	writer.write("tripId\tstopA_lat\tstopA_lon\t_stopB_lat\tstopB_lon\ttimebin\toverlapcount\n");
        	for (TripOverlap to : trip2tripOverlap.values()) {
        		writer.write(to.getTripId()+"\t");
        		for (java.util.Map.Entry<Segment, Integer> val: to.getSegment2counts().entrySet()) {
        			writer.write(val.getKey().getStopA().getLat()+"\t");
        			writer.write(val.getKey().getStopA().getLon()+"\t");
        			writer.write(val.getKey().getStopB().getLat()+"\t");
        			writer.write(val.getKey().getStopB().getLon()+"\t");
        			writer.write(val.getKey().getTimebin()+"\t");
        			writer.write(val.getValue()+"\n");
        		}
        	}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
 
        
        
        
    }
}
