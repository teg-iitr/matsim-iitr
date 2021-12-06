package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;
import playground.amit.utils.ListUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Amit, created on 05-12-2021
 */

public class OD2MetroTripCharsWriter {

    public static final String ACCESS_DISTANCE = "access_distance";
    public static final String EGRESS_DISTANCE = "egress_distance";

    public static final String ACCESS_TIME = "access_time";
    public static final String EGRESS_TIME = "egress_time";

    public static final String ACCESS_METRO_STOP = "access_metro_stop";
    public static final String EGRESS_METRO_STOP = "egress_metro_stop";
    public static final String METRO_TRIP_DISTANCE = "metro_trip_distance";
    public static final String METRO_TRIP_TIME = "metro_trip_time";

    private final Map<Id<OD>, TripChar> metroStats = new HashMap<>();
    private final MetroStopsQuadTree metroStopsQuadTree;
    private final GHNetworkDistanceCalculator ghNetworkDistanceCalculator ;
    private final DMAZonesProcessor dmaZonesProcessor;

    public OD2MetroTripCharsWriter(DMAZonesProcessor dmaZonesProcessor, GHNetworkDistanceCalculator ghNetworkDistanceCalculator, MetroStopsQuadTree metroStopsQuadTree){
        this.dmaZonesProcessor = dmaZonesProcessor;
        this.ghNetworkDistanceCalculator = ghNetworkDistanceCalculator;
        this.metroStopsQuadTree = metroStopsQuadTree;
    }

    public static void main(String[] args) {
        String metroStatsFile = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/OD2MetroTripChars.txt";
        MetroStopsQuadTree metroStopsQuadTree = new MetroStopsQuadTree();
        OD2MetroTripCharsWriter od2MetroStatsWriter = new OD2MetroTripCharsWriter(new DMAZonesProcessor(), new GHNetworkDistanceCalculator(metroStopsQuadTree), metroStopsQuadTree);
        od2MetroStatsWriter.run();
        od2MetroStatsWriter.writeMetroData(metroStatsFile);
    }

    public void run(){
        List<String> zones = this.dmaZonesProcessor.getZonesList();
        for (String o : zones) {
            for (String d : zones) {
                Id<OD> od = OD.getID(o,d);
                if (this.dmaZonesProcessor.excludeTrip(o,d)) {
                    this.metroStats.put(od, null);
                } else if (o.equals(d)){
                    this.metroStats.put(od, null);
                } else{
                    TripChar tripChar = getTripChar(o,d);
                    this.metroStats.put(od, tripChar);
                }
            }
        }
    }

    private TripChar getTripChar(String o, String d){
        List<Coord> origins = this.dmaZonesProcessor.getRandomCoords(o, HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
        List<Coord> destinations = this.dmaZonesProcessor.getRandomCoords(d, HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
        List<TripChar> tripChars = new ArrayList<>();

        for (int i=0; i<HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone; i++){
            TripChar tc =  ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origins.get(i)), DehradunUtils.Reverse_transformation.transform(destinations.get(i)),"metro");
            if(tc!=null) {
                if (tc.accessDist <= HaridwarRishikeshScenarioRunner.threshold_access_egress_distance && tc.egressDist <= HaridwarRishikeshScenarioRunner.threshold_access_egress_distance) {
                    tripChars.add(tc);
                } else {
                    return null;
                }
            }
        }

        if(tripChars.size()==0){
            return null;
        }

        double avgAccessDist = ListUtils.doubleMean(tripChars.stream().map(tc->tc.accessDist).collect(Collectors.toList()));
        double avgEgressDist = ListUtils.doubleMean(tripChars.stream().map(tc->tc.egressDist).collect(Collectors.toList()));

        double avgAccessTime = avgAccessDist/HaridwarRishikeshScenarioRunner.walk_speed;
        double avgEgressTime = avgEgressDist/HaridwarRishikeshScenarioRunner.walk_speed;

        String accessStop = getMostFrequent(tripChars.stream().map(tc->tc.access_stop).collect(Collectors.toList()));
        String egressStop = getMostFrequent(tripChars.stream().map(tc->tc.egress_stop).collect(Collectors.toList()));

        double metroDist = GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(this.metroStopsQuadTree.getMetroStopsNetwork().getNodes().get(Id.createNodeId(accessStop)).getCoord(),
                this.metroStopsQuadTree.getMetroStopsNetwork().getNodes().get(Id.createNodeId(egressStop)).getCoord(), "metro", null).tripDist;

        TripChar out = new TripChar(metroDist,metroDist/DehradunUtils.getSpeedKPHFromReport("metro"));
        out.access_stop = accessStop;
        out.egress_stop = egressStop;
        out.accessDist = avgAccessDist;
        out.egressDist = avgEgressDist;
        out.accessTime = avgAccessTime;
        out.egressTime = avgEgressTime;
        return out;
    }

    private String getMostFrequent(List<String> list){
        Set<String> distinct = new HashSet<>(list);
        int max = 0;
        String str = null;
        for (String s : distinct) {
            int f = Collections.frequency(list, s);
            if (f>max){
                max = f;
                str = s;
            }
        }
        return str;
    }

    public void writeMetroData(String outputFile){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)){
            writer.write("origin\tdestination\t");
            writer.write(ACCESS_METRO_STOP+"\t");
            writer.write(ACCESS_DISTANCE+"\t");
            writer.write(ACCESS_TIME+"\t");
            writer.write(METRO_TRIP_DISTANCE+"\t");
            writer.write(METRO_TRIP_TIME+"\t");
            writer.write(EGRESS_METRO_STOP+"\t");
            writer.write(EGRESS_DISTANCE+"\t");
            writer.write(EGRESS_TIME+"\t");
            writer.write("\n");
            for (Id<OD> od : metroStats.keySet()){
                Tuple<String, String> oNd = OD.getOriginAndDestination(od);
                writer.write(oNd.getFirst()+"\t"+oNd.getSecond()+"\t");
                TripChar stats = metroStats.get(od);
                if (stats==null) {
                    for (int i=0; i<8; i++) {
                        writer.write("null\t");
                    }
                } else {
                    writer.write(stats.access_stop+"\t");
                    writer.write(stats.accessDist+"\t");
                    writer.write(stats.accessTime+"\t");
                    writer.write(stats.tripDist+"\t");
                    writer.write(stats.tripTime+"\t");
                    writer.write(stats.egress_stop+"\t");
                    writer.write(stats.egressDist+"\t");
                    writer.write(stats.egressTime+"\t");
                }
                writer.write("\n");
            }
        } catch (IOException e){
            throw new RuntimeException("Data is not written to "+outputFile);
        }
    }

    public static Map<Id<OD>, TripChar> readMetroData(String inputFile){
        Map<Id<OD>, TripChar> od2metroTrip = new HashMap<>();
        try(BufferedReader reader = IOUtils.getBufferedReader(inputFile)){
            String line = reader.readLine();
            boolean header= true;
            while (line!=null){
                if (header) {
                    header = false;
                } else{
                    String [] parts = line.split("\t");
                    Id<OD> od = OD.getID(parts[0],parts[1]);
                    if(parts[2].equals("null")) {
                        od2metroTrip.put(od, null);
                    } else{
                        TripChar tc = new TripChar(Double.parseDouble(parts[5]), Double.parseDouble(parts[6]));
                        tc.access_stop = parts[2];
                        tc.accessDist = Double.parseDouble(parts[3]);
                        tc.accessTime = Double.parseDouble(parts[4]);

                        tc.egress_stop = parts[7];
                        tc.egressDist = Double.parseDouble(parts[8]);
                        tc.egressTime = Double.parseDouble(parts[9]);
                        od2metroTrip.put(od, tc);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e){
            throw new RuntimeException("Data is not read from "+inputFile);
        }
        return  od2metroTrip;
    }
}