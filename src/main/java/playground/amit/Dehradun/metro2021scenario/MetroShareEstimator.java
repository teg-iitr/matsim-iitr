package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;
import playground.amit.utils.ListUtils;
import playground.amit.utils.NumberUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 17-10-2021
 */

public class MetroShareEstimator {

    private static final String production_nearby_zone = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/production_nearby_zone.txt";
    private static final String attraction_nearby_zone = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/attraction_nearby_zone.txt";

    private final Map<Id<OD>, OD> odMap = new HashMap<>();
    private final HaridwarRishikeshScenarioRunner.HRScenario hrScenarios;

    private final DMAZonesProcessor dmaZonesProcessor;
    private final GHNetworkDistanceCalculator ghNetworkDistanceCalculator;

    public static final String new_metro_trips = "new_metro_trips";

    public MetroShareEstimator(DMAZonesProcessor dmaZonesProcessor, GHNetworkDistanceCalculator ghNetworkDistanceCalculator, HaridwarRishikeshScenarioRunner.HRScenario hrScenarios){
        this.dmaZonesProcessor = dmaZonesProcessor;
        this.ghNetworkDistanceCalculator = ghNetworkDistanceCalculator;
        this.hrScenarios = hrScenarios;
    }

    public static void main(String[] args) {
        String OD_merged_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/OD_2021_metro_trips_comparison_28-11-2021.txt";
        String outFile = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_trips_comparison_gh-router_NH-only_28-11-2021.txt";
        new MetroShareEstimator(new DMAZonesProcessor(), new GHNetworkDistanceCalculator(new MetroStopsQuadTree()), HaridwarRishikeshScenarioRunner.HRScenario.Integrated).run(OD_merged_file, outFile, OD2MetroTripCharsWriter.readMetroData(HaridwarRishikeshScenarioRunner.OD_2_metro_trips_characteristics));
    }

    public void run(String OD_merged_file, String outputFile, Map<Id<OD>, TripChar> od2metroTripStats){
        readODFile(OD_merged_file);
        readNearByZoneFiles();
        computeMetroShare(od2metroTripStats);
        writeData(outputFile);
    }

    private void readNearByZoneFiles(){
        Map<String, String> productionActual2NearbyZone = readAndStore(production_nearby_zone);
//        Map<String, String> attractionActual2NearbyZone = readAndStore(attraction_nearby_zone);

        for (OD od : this.odMap.values()) {
            String nearbyZone = productionActual2NearbyZone.get(od.getOrigin());
            if (nearbyZone==null) continue;

            double metroTrips = (double) od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old);
            if (metroTrips==0.){
                OD newOD = this.odMap.get(OD.getID(nearbyZone, od.getDestination()));
                double newMetroTrips = (double) newOD.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old);
                if(newMetroTrips!=0.){
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC, newOD.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC));
                }/*else{
                    OD anotherOD = this.odMap.get(OD.getID(nearbyZone, attractionActual2NearbyZone.get(od.getDestination())));
                    if(anotherOD==null) continue;
                    od.getAttributes().putAttribute(Metro2021ScenarioASCCalibration.METRO_ASC, anotherOD.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_ASC));
                }*/
            }
        }

    }

    private Map<String, String> readAndStore(String file){
        Map<String, String> outMap = new HashMap<>();
        try(BufferedReader reader = IOUtils.getBufferedReader(file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null) {
                if (!header) {
                    String[] parts = line.split("\t");
                    outMap.put(parts[0], parts[1]);
                } else {
                    header = false;
                }
                line = reader.readLine();
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
        return outMap;
    }

    private void writeData(String outFile){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)){
            writer.write("origin\tdestination\ttotalTrips\tmetroTrips_old" +
                    "\tASC_metro\tmetroTrips_new\n");
            for(OD od : odMap.values()){
                writer.write(od.getOrigin()+"\t");
                writer.write(od.getDestination()+"\t");
                writer.write(od.getNumberOfTrips()+"\t");
                writer.write(od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old)+"\t");
                writer.write(od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC)+"\t");
                writer.write(od.getAttributes().getAttribute(MetroShareEstimator.new_metro_trips)+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+outFile+". Possible reason "+e);
        }
    }

    private void readODFile(String OD_merged_file){
        try(BufferedReader reader = IOUtils.getBufferedReader(OD_merged_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(!header){
                    String [] parts = line.split("\t");
                    OD od = new OD(parts[0], parts[1]);
//                    System.out.println(od.getId());
                    od.setNumberOfTrips(Double.parseDouble(parts[2]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old, Double.parseDouble(parts[3]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC, Double.parseDouble(parts[4]));
                    odMap.put(od.getId(), od);
                } else{
                    header=false;
                }
                line = reader.readLine();
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }

    private void computeMetroShare( Map<Id<OD>, TripChar> od2metroTripStats){
        for(OD od : this.odMap.values()) {
//            double metroTrips = (double) od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS);
            if ( od.getNumberOfTrips()==0.){
                od.getAttributes().putAttribute(new_metro_trips, 0.);
            } else if ( od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC).equals(Double.NaN) ) {
                od.getAttributes().putAttribute(new_metro_trips, 0.);
            } else if (od2metroTripStats.get(od.getId())==null) {
                od.getAttributes().putAttribute(new_metro_trips, 0.);
            } else {
                List<Coord> origins = this.dmaZonesProcessor.getRandomCoords(od.getOrigin(), HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
                List<Coord> destinations = this.dmaZonesProcessor.getRandomCoords(od.getDestination(), HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
                double asc_metro =(double) od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC);

                List<Double> metroShareList = new ArrayList<>();
                for (int i = 0; i< HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone; i++) {
                    double util_rest_modes = 0.;
                    for (DehradunUtils.TravelModesBaseCase2017 tMode : DehradunUtils.TravelModesBaseCase2017.values()) {

                        TripChar distTime;

                        switch(this.hrScenarios){
                            case RingRoadOnly:
                                distTime  = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origins.get(i)),
                                        DehradunUtils.Reverse_transformation.transform(destinations.get(i)), tMode.name());
                                break;
                            case NHOnly:
                            case Integrated:
                                distTime  = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromGHRouter(DehradunUtils.Reverse_transformation.transform(origins.get(i)),
                                        DehradunUtils.Reverse_transformation.transform(destinations.get(i)), tMode.name());
                                break;
                            default: throw new RuntimeException("HR scenario undefined.");
                        }
                        double tripDist = distTime.tripDist;
                        double tripTime = distTime.tripTime;
                        util_rest_modes += Math.exp(UtilityComputation.getUtilExceptMetro(tMode, tripDist, tripTime));
                    }

                    TripChar tc = od2metroTripStats.get(od.getId());
//                            = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origins.get(i)),
//                    DehradunUtils.Reverse_transformation.transform(destinations.get(i)), DehradunUtils.TravelModesMetroCase2021.metro.name());
                    double util_metro = UtilityComputation.getUtilMetroWithoutASC(tc.tripDist+tc.accessDist+tc.egressDist, tc.tripTime+tc.accessTime+tc.egressTime)+asc_metro;
                    double exp_util_metro = Math.exp(util_metro);
                    double metroShare = exp_util_metro/ (exp_util_metro+util_rest_modes);
                    metroShareList.add(metroShare);
                }
                od.getAttributes().putAttribute(MetroShareEstimator.new_metro_trips, NumberUtils.round(ListUtils.doubleMean(metroShareList) *od.getNumberOfTrips(), 1));
            }
        }
    }
}
