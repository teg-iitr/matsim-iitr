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

    public static final String new_metro_trips = "new_metro_trips";

    public MetroShareEstimator(DMAZonesProcessor dmaZonesProcessor, HaridwarRishikeshScenarioRunner.HRScenario hrScenarios){
        this.dmaZonesProcessor = dmaZonesProcessor;
        this.hrScenarios = hrScenarios;
    }

    public static void main(String[] args) {
        String OD_merged_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/OD_2021_metro_trips_comparison_28-11-2021.txt";
        String outFile = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_trips_comparison_gh-router_NH-only_28-11-2021.txt";
        new MetroShareEstimator(new DMAZonesProcessor(),HaridwarRishikeshScenarioRunner.HRScenario.Integrated).run(OD_merged_file,outFile);
    }

    public void run(String OD_merged_file, String outputFile){
        readODFile(OD_merged_file);
        readNearByZoneFiles();
        computeMetroShare();
        writeData(outputFile);
    }

    private void readNearByZoneFiles(){
        Map<String, String> productionActual2NearbyZone = readAndStore(production_nearby_zone);
//        Map<String, String> attractionActual2NearbyZone = readAndStore(attraction_nearby_zone);

        for (OD od : this.odMap.values()) {
            double metroTrips = (double) od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS);
            if (metroTrips==0.){
                String nearbyZone = productionActual2NearbyZone.get(od.getOrigin());
                if (nearbyZone==null) continue;
                OD newOD = this.odMap.get(OD.getID(nearbyZone, od.getDestination()));
                double newMetroTrips = (double) newOD.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS);
                if(newMetroTrips!=0.){
                    od.getAttributes().putAttribute(Metro2021ScenarioASCCalibration.METRO_ASC, newOD.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_ASC));
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
                writer.write(od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS)+"\t");
                writer.write(od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_ASC)+"\t");
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
                    od.getAttributes().putAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS, Double.parseDouble(parts[3]));
                    od.getAttributes().putAttribute(Metro2021ScenarioASCCalibration.METRO_ASC, Double.parseDouble(parts[4]));
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

    private void computeMetroShare(){
        GHNetworkDistanceCalculator ghNetworkDistanceCalculator = new GHNetworkDistanceCalculator();
        for(OD od : this.odMap.values()) {
            double metroTrips = (double) od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS);
            if ( od.getNumberOfTrips()==0.){
                od.getAttributes().putAttribute(new_metro_trips, 0.);
            } else if ( od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_ASC).equals(Double.NaN) ) {
                od.getAttributes().putAttribute(new_metro_trips, 0.);
            } else {
                List<Coord> origin = this.dmaZonesProcessor.getRandomCoords(od.getOrigin(), HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
                List<Coord> destination = this.dmaZonesProcessor.getRandomCoords(od.getDestination(), HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
                double asc_metro =(double) od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_ASC);

                List<Double> metroShareList = new ArrayList<>();
                for (int i = 0; i< HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone; i++) {
                    double util_rest_modes = 0.;
                    for (DehradunUtils.TravelModesBaseCase2017 tMode : DehradunUtils.TravelModesBaseCase2017.values()) {

                        Tuple<Double, Double> distTime;

                        switch(this.hrScenarios){
                            case RingRoadOnly:
                                distTime  = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
                                        DehradunUtils.Reverse_transformation.transform(destination.get(i)), tMode.name());
                                break;
                            case NHOnly:
                            case Integrated:
                                distTime  = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromGHRouter(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
                                        DehradunUtils.Reverse_transformation.transform(destination.get(i)), tMode.name());
                                break;
                            default: throw new RuntimeException("HR scenario undefined.");
                        }
                        double tripDist = distTime.getFirst();
                        double tripTime = distTime.getSecond();
                        util_rest_modes += Math.exp(UtilityComputation.getUtilExceptMetro(tMode, tripDist, tripTime));
                    }

                    Tuple<Double, Double> distTime = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
                    DehradunUtils.Reverse_transformation.transform(destination.get(i)), DehradunUtils.TravelModesMetroCase2021.metro.name());
                    double util_metro = UtilityComputation.getUtilMetroWithoutASC(distTime.getFirst(), distTime.getSecond())+asc_metro;
                    double exp_util_metro = Math.exp(util_metro);
                    double metroShare = exp_util_metro/ (exp_util_metro+util_rest_modes);
                    metroShareList.add(metroShare);
                }
                od.getAttributes().putAttribute(MetroShareEstimator.new_metro_trips, NumberUtils.round(ListUtils.doubleMean(metroShareList) *od.getNumberOfTrips(), 1));
            }
        }
    }
}
