package playground.amit.Delhi.gtfs.elements;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import org.matsim.pt2matsim.gtfs.lib.Route;
import playground.amit.Delhi.gtfs.SigmoidFunction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Amit on 10/05/2021.
 */
public class VehicleRouteOverlapCalculator {

    private static final String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_DIMTS_18042021.zip";
    private static final double timebinSize = 24*60*60.; // 2min
    private static final String date = "10052021";
    private static final String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\";

    public static void main(String[] args) {
        GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);
        SpatialOverlap spatialOverlap = new SpatialOverlap(timebinSize);
        // go through with trips because a trip is an instance of a vehicle
        gtfsFeed.getTrips().forEach(spatialOverlap::add);

        System.out.println("Evaluating overlaps and overlaps probabilities to a file ...");
        spatialOverlap.collectOverlaps();
        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        Map<String, VehicleRouteOverlap> route2VROverlpas = new HashMap<>();

        for (TripOverlap to: trip2tripOverlap.values()) {
            String routeId = to.getRouteId();
            VehicleRouteOverlap vrOverlap = route2VROverlpas.getOrDefault(routeId, new VehicleRouteOverlap(routeId));
            vrOverlap.getTripId2Probs().put(to.getTripId(), to.getSigmoidFunction2Probs());
            route2VROverlpas.put(routeId, vrOverlap);
        }

        System.out.println("Writing vehicle-route probs to a file ...");
        String filename = outFilePath+"routeProbs_24h-timebin_"+date+".txt";
        BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
        try {
            writer.write("routeId\tnumberOfTrips\tsigmoidFunction\tprob\n");
            for(VehicleRouteOverlap vr : route2VROverlpas.values()) {
                for(SigmoidFunction sf : SigmoidFunction.values()) {
                    writer.write(vr.getId() + "\t" + vr.getTripId2Probs().size() + "\t"+sf+"\t"+vr.getVRProb().get(sf)+"\n");
                }
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
        System.out.println("Writing vehicle-route probs to "+filename+" completed.");
    }
}

