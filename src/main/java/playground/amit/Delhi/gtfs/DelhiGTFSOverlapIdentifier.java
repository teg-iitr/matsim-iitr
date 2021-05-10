package playground.amit.Delhi.gtfs;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import org.matsim.pt2matsim.gtfs.lib.Route;
import playground.amit.Delhi.gtfs.elements.Segment;
import playground.amit.Delhi.gtfs.elements.SegmentalOverlap;
import playground.amit.Delhi.gtfs.elements.SpatialOverlap;
import playground.amit.Delhi.gtfs.elements.TripOverlap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Amit on 23/04/2021
 */
public class DelhiGTFSOverlapIdentifier {

    private static final String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_DIMTS_18042021.zip";
    private static final double timebinSize = 24*60*60.; // 2min
	private final String date = "10052021";
    private static final String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\";

	public static void main(String[] args) {
		new DelhiGTFSOverlapIdentifier().segmentalOverlapCountsProbs();
		new DelhiGTFSOverlapIdentifier().routeProbs();
	}

	/**
	 * Writing the probabilities of every trips for different Sigmoidal functions
	 */
	public void routeProbs(){
		GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);
		SpatialOverlap spatialOverlap = new SpatialOverlap(timebinSize);
		// go through with trips because a trip is an instance of a vehicle
		gtfsFeed.getTrips().forEach(spatialOverlap::add);

		System.out.println("Evaluating overlaps and overlaps probabilities to a file ...");
		spatialOverlap.collectOverlaps();
		Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();

		System.out.println("Writing overlaps to a file ...");
		String filename = outFilePath+"routeTripProbs_24h-timebin_"+date+".txt";
		BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
		try {
			writer.write("routeId\ttripId\tsigmoidFunction\tprob\n");
			for (Route route : gtfsFeed.getRoutes().values()){
				for (String t : route.getTrips().keySet()){
					TripOverlap to = trip2tripOverlap.get(t);
					for (SigmoidFunction sg : SigmoidFunction.values()) {
						writer.write(route.getId()+"\t"+t+"\t"+sg.toString()+"\t"+to.getSigmoidFunction2Probs().get(sg)+"\n");
					}
				}
			}
		}catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
		System.out.println("Writing overlaps to "+filename+" completed.");
	}

	/**
	 * Writing segmental counts and prob for each segment in all trips.
	 */
    public void segmentalOverlapCountsProbs() {
		GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);
		SpatialOverlap spatialOverlap = new SpatialOverlap(timebinSize);
		// go through with trips because a trip is an instance of a vehicle
		gtfsFeed.getTrips().forEach(spatialOverlap::add);

		System.out.println("Evaluating overlaps and overlaps probabilities to a file ...");
		spatialOverlap.collectOverlaps();
		Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();

		System.out.println("Writing overlaps to a file ...");
		String filename = outFilePath+"segments_probs_24h-timebin_"+date+".txt";
		BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
		try {
			writer.write("tripId\tstopA_lat\tstopA_lon\t_stopB_lat\tstopB_lon\ttimebin\toverlapcount" +
					"\tstopA_seq\tstopB_seq\ttimeSpentOnSegment_sec\tlegnthOfSegment_m" +
					"\tlogisticSigmoidProb\tbipolarSigmoidProb\ttanhProb\talgebraicSigmoidProb\n");
			for (TripOverlap to : trip2tripOverlap.values()) {
				for (java.util.Map.Entry<Segment, SegmentalOverlap> val: to.getSeg2overlaps().entrySet()) {
					writer.write(to.getTripId()+"\t");
					writer.write(val.getKey().getStopA().getLat()+"\t");
					writer.write(val.getKey().getStopA().getLon()+"\t");
					writer.write(val.getKey().getStopB().getLat()+"\t");
					writer.write(val.getKey().getStopB().getLon()+"\t");
					writer.write(val.getKey().getTimebin()+"\t");
					writer.write(val.getValue().getCount()+"\t");
					writer.write(val.getKey().getStopSequence().getFirst()+"\t"+val.getKey().getStopSequence().getSecond()+"\t");
					writer.write(val.getKey().getTimeSpentOnSegment()+"\t");
					writer.write(val.getKey().getLength()+"\t");
					for (SigmoidFunction sg : SigmoidFunction.values()) {
						writer.write(SigmoidFunctionUtils.getValue(sg,val.getValue().getCount())+"\t");
					}
					writer.write("\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
		System.out.println("Writing overlaps to "+filename+" completed.");
    }
}
