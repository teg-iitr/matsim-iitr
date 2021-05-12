package playground.amit.Delhi.gtfs;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import playground.amit.Delhi.gtfs.elements.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Amit on 10/05/2021.
 */
public class GTFSOverlapOptimizer {

    private final SpatialOverlap spatialOverlap;
    private final String outputPath;
    private final String suffix;
    private int iteration = 0;
    private final BufferedWriter writer;
    // required for optimization
    private final SigmoidFunction sigmoidFunction;

    public GTFSOverlapOptimizer(int timebinSize, String outputPath, SigmoidFunction sigmoidFunction){
      this.spatialOverlap = new SpatialOverlap(timebinSize);
      this.outputPath = outputPath;
      this.sigmoidFunction = sigmoidFunction;
      String date = new SimpleDateFormat("dd-mm-yy").format(Calendar.getInstance().getTime());
      this.suffix = timebinSize+"_"+date;
      this.writer = IOUtils.getBufferedWriter(this.outputPath+"/overall_summary_stats.txt");
    }

    private void writeToFile(String s) {
        try{
            this.writer.write(s);
        } catch (IOException e){
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    public void initialize(String gtfs_file){
        writeToFile("iterationNr\tremovedRoute\t\troutes\ttrips\toverlappingLengthRatio\n");
        GtfsFeed gtfsFeed = new GtfsFeedImpl(gtfs_file);
        // go through with trips because a trip is an instance of a vehicle
        gtfsFeed.getTrips().forEach(spatialOverlap::add);

        System.out.println("Evaluating overlaps and overlaps probabilities to a file ...");
        spatialOverlap.collectOverlaps();
        writeStatsToSummaryFile("-");
    }

    private void writeStatsToSummaryFile(String removedRoute){
        String out = this.iteration+"\t"+
                removedRoute+"\t"+
                this.spatialOverlap.getRoute2TripsIds().size()+"\t"+
                this.spatialOverlap.getTrip2tripOverlap().size()+"\t"+
                getOverlappingRatio()+"\n";
        writeToFile(out);
    }

    public void remove(String routeId){
        this.iteration++;
        this.spatialOverlap.removeRoute(routeId);
        spatialOverlap.collectOverlaps();
        writeStatsToSummaryFile(routeId);
    }

    public List<String> getLeastProbRoute(){
        Map<String, VehicleRouteOverlap> route2VROverlpas = new HashMap<>();

        for (TripOverlap to: spatialOverlap.getTrip2tripOverlap().values()) {
            String routeId = to.getRouteId();
            VehicleRouteOverlap vrOverlap = route2VROverlpas.getOrDefault(routeId, new VehicleRouteOverlap(routeId));
            vrOverlap.getTripId2Probs().put(to.getTripId(), to.getSigmoidFunction2Probs());
            route2VROverlpas.put(routeId, vrOverlap);
        }

        // identify which routes have highest prob
        List<VehicleRouteOverlap> vros = new ArrayList<>(route2VROverlpas.values());
        vros.sort(Comparator.comparing(o -> o.getVRProb().get(sigmoidFunction)));
        Collections.reverse(vros);

        List<VehicleRouteOverlap> highestProbRoutes = new ArrayList<>();
        double highestProb = vros.get(0).getVRProb().get(sigmoidFunction);
        for (VehicleRouteOverlap vro : vros) {
            if (vro.getVRProb().get(sigmoidFunction)==highestProb) highestProbRoutes.add(vro);
            else break;
        }

        //now find the routes which has highest number of trips
        highestProbRoutes.sort(Comparator.comparingInt(o -> o.getTripId2Probs().size()));
        Collections.reverse(highestProbRoutes);
        int highestTrips = highestProbRoutes.get(0).getTripId2Probs().size();
        List<String> removedRoutes = new ArrayList<>();
        for (VehicleRouteOverlap vro : highestProbRoutes) {
            if (vro.getTripId2Probs().size() == highestTrips) removedRoutes.add(vro.getId());
            else break;
        }
        return removedRoutes;
    }

    public double getOverlappingRatio(){
        double overlappingRatio = 0;
//        for (VehicleRouteOverlap vro : this.id2vehicleRouteOverlap.values()) {
////            for (TripOverlap to :)
//// for each trip: calculate overlapping length ratio (i.e., sum of length of the segments which have overlap count >= 1 / length of trip)
//// for vehicle route, average it across trips
//        }
        return 0.;
    }

    private String getItrDir(){
        String path = outputPath+"ITERS/it."+iteration+"/";
        if (! new File(path).exists()) {
            new File(path).mkdirs();
        }
        return path;
    }

    /**
     * Writing segmental counts and prob for each segment in all trips.
     */
    public void writeSegmentalOverlapCountsProbs(String outputFolder) {
        System.out.println("Writing overlaps to a file ...");
        String filename = outputFolder+"segmentsProbs_"+suffix+"_"+"it-"+iteration+".txt";
        BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
        try {
            writer.write("tripId\tstopA_lat\tstopA_lon\t_stopB_lat\tstopB_lon\ttimebin\toverlapcount" +
                    "\tstopA_seq\tstopB_seq\ttimeSpentOnSegment_sec\tlegnthOfSegment_m" +
                    "\tlogisticSigmoidProb\tbipolarSigmoidProb\ttanhProb\talgebraicSigmoidProb\n");
            for (TripOverlap to : spatialOverlap.getTrip2tripOverlap().values()) {
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

    public void writeVehicleRouteProbs(String outputFolder){
        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        Map<String, VehicleRouteOverlap> route2VROverlpas = new HashMap<>();

        for (TripOverlap to: trip2tripOverlap.values()) {
            String routeId = to.getRouteId();
            VehicleRouteOverlap vrOverlap = route2VROverlpas.getOrDefault(routeId, new VehicleRouteOverlap(routeId));
            vrOverlap.getTripId2Probs().put(to.getTripId(), to.getSigmoidFunction2Probs());
            route2VROverlpas.put(routeId, vrOverlap);
        }

        System.out.println("Writing vehicle-route probs to a file ...");
        String filename = outputFolder+"vehicleRouteProbs_"+suffix+"_"+"it-"+iteration+".txt";
        try (BufferedWriter writer  = IOUtils.getBufferedWriter(filename)) {
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

    /**
     * Writing the probabilities of every trips for different Sigmoidal functions
     */
    public void writeTripProbs(String outputFolder){
        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        Map<String, Set<String>> route2Trips = spatialOverlap.getRoute2TripsIds();

        System.out.println("Writing overlaps to a file ...");
        String filename = outputFolder+"routeTripProbs_"+suffix+"_"+"it-"+iteration+".txt";
        BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
        try {
            writer.write("routeId\ttripId\tsigmoidFunction\tprob\n");
            for (String routeId : route2Trips.keySet()){
                for (String t : route2Trips.get(routeId)){
                    TripOverlap to = trip2tripOverlap.get(t);
                    for (SigmoidFunction sg : SigmoidFunction.values()) {
                        writer.write(routeId+"\t"+t+"\t"+sg.toString()+"\t"+to.getSigmoidFunction2Probs().get(sg)+"\n");
                    }
                }
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
        System.out.println("Writing overlaps to "+filename+" completed.");
    }

//    private void writeSpecificRouteOverlapDetails(String outFilePath, String suffix){
//        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
//        Map<String, VehicleRouteOverlap> route2VROverlpas = new HashMap<>();
//
//        for (TripOverlap to: trip2tripOverlap.values()) {
//            String routeId = to.getRouteId();
//            VehicleRouteOverlap vrOverlap = route2VROverlpas.getOrDefault(routeId, new VehicleRouteOverlap(routeId));
//            vrOverlap.getTripId2Probs().put(to.getTripId(), to.getSigmoidFunction2Probs());
//            route2VROverlpas.put(routeId, vrOverlap);
//        }
//
//        System.out.println("Wrinting complete details for route IDs 362, 281 (prob =1, overlapping trips = 19)...");
//        List<String> routeIds = Arrays.asList("362","281");
//        for (String routeId : routeIds) {
//            String file2 = outFilePath+"completeDetails_route_"+routeId+"_"+suffix+".txt";
//            try (BufferedWriter writer  = IOUtils.getBufferedWriter(file2)) {
//                writer.write("routeId\ttripIds\t" +
//                        "seg_stopA_lat\tseg_stopA_lon\t" +
//                        "seg_stopB_lat\tseg_stopB_lon\t" +
//                        "overlap_count\toverlappingTrips\toverlappingRoutes\n");
//                VehicleRouteOverlap vr = route2VROverlpas.get(routeId);
//                for(Id<Trip> tripId : vr.getTripId2Probs().keySet()) {
//                    TripOverlap tripOverlap = trip2tripOverlap.get(tripId.toString());
//                    for (Segment seg: tripOverlap.getSegments()){
//                        SegmentalOverlap so = tripOverlap.getSeg2overlaps().get(seg);
//                        writer.write(routeId+"\t"+tripId+"\t"+seg.getStopA().getLat()+"\t"+seg.getStopA().getLon()+"\t");
//                        writer.write(seg.getStopB().getLat()+"\t"+seg.getStopB().getLon()+"\t");
//                        writer.write(so.getCount()+"\t");
//                        writer.write(so.getOverlappingTripIds()+"\t");
//                        writer.write(so.getOverlappingRouteIds()+"\n");
//                    }
//                }
//            }catch (IOException e) {
//                throw new RuntimeException("Data is not written. Reason "+e);
//            }
//        }
//    }

    public void writeIterationFiles() {
        String outputFolder = getItrDir();
        writeSegmentalOverlapCountsProbs(outputFolder);
		writeTripProbs(outputFolder);
		writeVehicleRouteProbs(outputFolder);
    }

    public void done() {
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException("The writer cannot be closed. Reason "+e);
        }
    }
}

