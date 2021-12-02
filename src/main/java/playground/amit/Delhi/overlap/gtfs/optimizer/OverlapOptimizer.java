package playground.amit.Delhi.overlap.gtfs.optimizer;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import org.matsim.pt2matsim.gtfs.lib.Stop;
import playground.amit.Delhi.overlap.gtfs.elements.*;
import playground.amit.Delhi.overlap.vehicles.GTFSVehicleIntegrator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Amit on 10/05/2021.
 */
public class OverlapOptimizer {

    public static final Logger LOG = Logger.getLogger(OverlapOptimizer.class);

//    public enum OptimizingElements{vehicle, GTFS}

    private final SpatialOverlap spatialOverlap;
    private final String outputPath;
    private final String suffix;
    private int iteration = 0;
    private final BufferedWriter writer;
    // required for optimization
    private final SigmoidFunction sigmoidFunction;
    private double totalNetworkRouteLength= Double.NaN;

    public OverlapOptimizer(int timebinSize, String outputPath, SigmoidFunction sigmoidFunction){
      this.spatialOverlap = new SpatialOverlap(timebinSize);
      this.outputPath = outputPath;
      this.sigmoidFunction = sigmoidFunction;
      String date = new SimpleDateFormat("dd-MM-yy").format(Calendar.getInstance().getTime());
      this.suffix = timebinSize+"_"+date;
      this.writer = IOUtils.getAppendingBufferedWriter(this.outputPath+"\\overall_summary_stats.txt");
      try{
          OutputDirectoryLogging.initLoggingWithOutputDirectory(this.outputPath);
      }catch (IOException e){
          throw new RuntimeException("The log file cannot be written. Reason "+e);
      }
    }

    public void initializeWithGTFS(String gtfs_path){
        initializeWithGTFSAndVehicles(gtfs_path, null);
    }

    public void initializeWithGTFSAndVehicles(String gtfs_path, String vehicle_file){
        writeToSummaryFile("iterationNr\tremovedVehicle\tremovedVehicleProb\tnoOfRoutes\tnoOfTrips\toverlappingSegmentsLength_km\tallsegmentsLength_km\toverlappingLengthRatio\n");
        GtfsFeed gtfsFeed = new GtfsFeedImpl(gtfs_path);

        if(vehicle_file!=null){
            GTFSVehicleIntegrator gtfsVehicleIntegrator = new GTFSVehicleIntegrator(vehicle_file);
            // exclude the trips if they are not served by the required vehicle fleet.
            // exclude trips which have no departure time details.
            gtfsFeed.getTrips().values().stream().filter(e -> e.getStopTimes().size() > 0).forEach(e -> {
                String vehicleNumber = gtfsVehicleIntegrator.keepTheTrip(e.getStopTimes().first().getDepartureTime(),
                        e.getRoute().getLongName());
                if (vehicleNumber != null) {
                    spatialOverlap.add(e, vehicleNumber);
                }
            });
        } else{
            gtfsFeed.getTrips().values().forEach(e -> spatialOverlap.add(e,e.getRoute().getLongName()));
        }

        if(spatialOverlap.getTrip2tripOverlap().size()==0) throw new RuntimeException("No trips are added.");

        OverlapOptimizer.LOG.info("Evaluating overlaps and overlaps probabilities to a file ...");
        spatialOverlap.collectOverlaps();
        writeStatsToSummaryFile("-", 0);
        writeIterationFiles();
    }

    public void optimizeTillVehicles(int requiredVehicleRoutes){
        int vehiclesRemaining = 0;
        do {
            OverlapOptimizer.LOG.info("\t\tRunning iteration\t"+this.iteration);
            Map<String, Double> vehicles2Remove = getLeastProbVehicle();
            if (vehicles2Remove==null) break;
            for (String s : vehicles2Remove.keySet()) {
                OverlapOptimizer.LOG.info("Removing vehicle route "+s);
                remove(s, vehicles2Remove.get(s));
                writeIterationFiles();
                vehiclesRemaining = this.spatialOverlap.getVehicleRoute2TripsIds().size();
            }
        } while (vehiclesRemaining > requiredVehicleRoutes);
        done();
    }

    public void optimizeTillProb(double prob){
        double removedVehicleProb = 1.0;
        do {
            OverlapOptimizer.LOG.info("\t\tRunning iteration\t"+this.iteration);
            Map<String, Double> vehicles2Remove = getLeastProbVehicle();
            if (vehicles2Remove==null) break;
            for (String s : vehicles2Remove.keySet()) {
                OverlapOptimizer.LOG.info("Removing vehicle route "+s);
                remove(s, vehicles2Remove.get(s));
                writeIterationFiles();
                removedVehicleProb = vehicles2Remove.get(s);
            }
        } while (removedVehicleProb>=prob);
        done();
    }

//    public void run(int iteration){
//        for (int i = 1; i < iteration; i++) {
//            GTFSOverlapOptimizer.LOG.info("\t\tRunning iteration\t"+this.iteration);
//            Map<String, Double> route2Remove = getLeastProbRoute();
//            for (String s : route2Remove.keySet()) {
//                GTFSOverlapOptimizer.LOG.info("Removing vehicle route "+s);
//                remove(s, route2Remove.get(s));
//                writeIterationFiles();
//            }
//        }
//    }

    public void remove(String vehicleNumber, double removalProb){
        this.iteration++;
        this.spatialOverlap.removeVehicle(vehicleNumber);
        spatialOverlap.collectOverlaps();
        writeStatsToSummaryFile(vehicleNumber, removalProb);
    }

    public Map<String, Double> getLeastProbVehicle(){
        Map<String, VehicleRouteOverlap> vehicleRoute2VROverlpas = new HashMap<>();

        for (TripOverlap to: spatialOverlap.getTrip2tripOverlap().values()) {
            String vehicleNumber = to.getVehicleNumber();
            String routeId = to.getRouteId();
            VehicleRouteOverlap vrOverlap = vehicleRoute2VROverlpas.getOrDefault(vehicleNumber, new VehicleRouteOverlap(vehicleNumber, routeId));
            vrOverlap.addProbsToTrip(routeId, to.getTripId(), to.getSigmoidFunction2Probs());
//            vrOverlap.getTripId2Probs().put(to.getTripId(), to.getSigmoidFunction2Probs());
            vehicleRoute2VROverlpas.put(vehicleNumber, vrOverlap);
        }

        if (vehicleRoute2VROverlpas.size()==0) return null;

        // identify which routes have highest prob
        List<VehicleRouteOverlap> vros = new ArrayList<>(vehicleRoute2VROverlpas.values());
        vros.sort(Comparator.comparing(o -> o.getVRProb().get(sigmoidFunction)));
        Collections.reverse(vros);

        List<VehicleRouteOverlap> highestProbRoutes = new ArrayList<>();
        double highestProb = vros.get(0).getVRProb().get(sigmoidFunction);
        for (VehicleRouteOverlap vro : vros) {
            if (vro.getVRProb().get(sigmoidFunction)==highestProb) highestProbRoutes.add(vro);
            else break;
        }

        //now find the vehicle routes which has highest number of trips
        highestProbRoutes.sort(Comparator.comparingInt(VehicleRouteOverlap::getNumberOfTrips));
        Collections.reverse(highestProbRoutes);
        int highestTrips = highestProbRoutes.get(0).getNumberOfTrips();
        Map<String, Double> removedVehicles = new LinkedHashMap<>();
        for (VehicleRouteOverlap vro : highestProbRoutes) {
            if (vro.getNumberOfTrips() == highestTrips) removedVehicles.put(vro.getVehicleNumber(), vro.getVRProb().get(sigmoidFunction));
            else break;
        }
        return removedVehicles;
    }

    public double getOverlappingNetworkLength(){
        return spatialOverlap.getCollectedSegments().values()
                .stream()
                .filter(so -> so.getCount() > 1.)
                .mapToDouble(so -> so.getSegment().getLength()).sum();
    }

    /**
     * This represents the network route length (not total route length).
     */
    public double getAllSegmentsLength(){
        if (Double.isNaN(this.totalNetworkRouteLength)){
            this.totalNetworkRouteLength= spatialOverlap.getCollectedSegments().keySet().stream()
                    .mapToDouble(Segment::getLength).sum();
        }
        return this.totalNetworkRouteLength;
    }

    private String getItrDir(){
        String path = outputPath+"ITERS\\it."+iteration+"\\";
        if (! new File(path).exists()) {
            new File(path).mkdirs();
        }
        return path;
    }

    private void writeToSummaryFile(String s) {
        try{
            this.writer.write(s);
            this.writer.flush();
        } catch (IOException e){
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    private void writeStatsToSummaryFile(String removedVehicle, double removalProb){
        double overlapLen = getOverlappingNetworkLength()/1000.;
        double totalTripLength = getAllSegmentsLength()/1000.;
        String out = this.iteration+"\t"+
                removedVehicle+"\t"+
                removalProb+"\t"+
                this.spatialOverlap.getVehicleRoute2TripsIds().size()+"\t"+
                this.spatialOverlap.getTrip2tripOverlap().size()+"\t"+
                overlapLen+"\t"+
                totalTripLength + "\t" +
                overlapLen/totalTripLength +
                "\n";
        writeToSummaryFile(out);
    }

    private String getGeom(Stop stopA, Stop stopB){
        return "LINESTRING ("+stopA.getLon()+" "+stopA.getLat()+","+ stopB.getLon()+" "+stopB.getLat()+")";
    }

    /**
     * Writing segmental counts and prob for each segment in all trips.
     */
    public void writeSegmentalOverlapCountsProbs(String outputFolder) {
        OverlapOptimizer.LOG.info("Writing overlaps to a file ...");
        String filename = outputFolder+"segmentsProbs_"+suffix+"_"+"it-"+iteration+".txt.gz";
        BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
        try {
            writer.write("tripId\tstopA_lat\tstopA_lon\t_stopB_lat\tstopB_lon\tgeom\ttimebin\toverlapcount" +
                    "\tstopA_seq\tstopB_seq\ttimeSpentOnSegment_sec\tlegnthOfSegment_m" +
                    "\tlogisticSigmoidProb\tbipolarSigmoidProb\ttanhProb\talgebraicSigmoidProb\n");
            for (TripOverlap to : spatialOverlap.getTrip2tripOverlap().values()) {
                for (java.util.Map.Entry<Segment, SegmentalOverlap> val: to.getSeg2overlaps().entrySet()) {
                    writer.write(to.getTripId()+"\t");
                    Segment key = val.getKey();
                    writer.write(key.getStopA().getLat()+"\t");
                    writer.write(key.getStopA().getLon()+"\t");
                    writer.write(key.getStopB().getLat()+"\t");
                    writer.write(key.getStopB().getLon()+"\t");
                    writer.write(getGeom(key.getStopA(), key.getStopB())+ "\t");
                    writer.write(key.getTimebin()+"\t");
                    writer.write(val.getValue().getCount()+"\t");
                    writer.write(key.getStopSequence().getFirst()+"\t"+ key.getStopSequence().getSecond()+"\t");
                    writer.write(key.getTimeSpentOnSegment()+"\t");
                    writer.write(key.getLength()+"\t");
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
        OverlapOptimizer.LOG.info("Writing overlaps to "+filename+" completed.");
    }

    public void writeVehicleRouteProbs(String outputFolder){
        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        Map<String, VehicleRouteOverlap> vehicleRoute2VROverlpas = new HashMap<>();

        for (TripOverlap to: trip2tripOverlap.values()) {
            String vehicleNumber = to.getVehicleNumber();
            VehicleRouteOverlap vrOverlap = vehicleRoute2VROverlpas.getOrDefault(vehicleNumber, new VehicleRouteOverlap(to.getVehicleNumber(), vehicleNumber));
            vrOverlap.addProbsToTrip(vehicleNumber, to.getTripId(), to.getSigmoidFunction2Probs());
//            vrOverlap.getTripId2Probs().put(to.getTripId(), to.getSigmoidFunction2Probs());
            vehicleRoute2VROverlpas.put(vehicleNumber, vrOverlap);
        }

        OverlapOptimizer.LOG.info("Writing vehicle-route probs to a file ...");
        String filename = outputFolder+"vehicleRouteProbs_"+suffix+"_"+"it-"+iteration+".txt.gz";
        try (BufferedWriter writer  = IOUtils.getBufferedWriter(filename)) {
            writer.write("vehicleNumber\tnumberOfRoutes\t\tnumberOfTrips\tsigmoidFunction\tprob\n");
            for(VehicleRouteOverlap vr : vehicleRoute2VROverlpas.values()) {
                for(SigmoidFunction sf : SigmoidFunction.values()) {
                    writer.write(vr.getVehicleNumber() + "\t" + vr.getRoutes().size()+"\t" + vr.getNumberOfTrips() + "\t"+sf+"\t"+vr.getVRProb().get(sf)+"\n");
                }
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
        OverlapOptimizer.LOG.info("Writing vehicle-route probs to "+filename+" completed.");
    }

    /**
     * Writing the probabilities of every trips for different Sigmoidal functions
     */
    public void writeTripProbs(String outputFolder){
        Map<String, TripOverlap> trip2tripOverlap = spatialOverlap.getTrip2tripOverlap();
        Map<String, Set<String>> vehicleRoute2TripsIds = spatialOverlap.getVehicleRoute2TripsIds();

        OverlapOptimizer.LOG.info("Writing overlaps to a file ...");
        String filename = outputFolder+"vehicleRouteTripProbs_"+suffix+"_"+"it-"+iteration+".txt.gz";
        BufferedWriter writer  = IOUtils.getBufferedWriter(filename);
        try {
            writer.write("vehicleNumber\ttripId\tsigmoidFunction\tprob\n");
            for (String vehicleRouteId : vehicleRoute2TripsIds.keySet()){
                for (String t : vehicleRoute2TripsIds.get(vehicleRouteId)){
                    TripOverlap to = trip2tripOverlap.get(t);
                    for (SigmoidFunction sg : SigmoidFunction.values()) {
                        writer.write(vehicleRouteId+"\t"+t+"\t"+sg.toString()+"\t"+to.getSigmoidFunction2Probs().get(sg)+"\n");
                    }
                }
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
        OverlapOptimizer.LOG.info("Writing overlaps to "+filename+" completed.");
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

    private void done() {
        OutputDirectoryLogging.closeOutputDirLogging();
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException("The writer cannot be closed. Reason "+e);
        }
    }
}

