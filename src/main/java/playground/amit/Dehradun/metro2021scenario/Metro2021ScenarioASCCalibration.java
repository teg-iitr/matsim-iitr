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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Amit
 *
 * <b>The utility parameters for the modes car, motorbike, IPT, bus, metro are given in the Table 6-7 of metro report.
 * However, the ASCs are not given. </b>
 * <b>The ASCs for the base case (2017; car, motorbike, IPT, bus, bicycle, walk) can be estimated by creating a simulation model using the OD matrix and the modal share. </b>
 * <b> We use the ASCs for the car, motorbike, IPT, bus, bicycle, walk from the base case and we calibrate the ASC for the metro using the given OD matrix for all trips and metro trips in 2021.</b>
 * <b> While applying MNL for merto OD matrix with respect to the overall OD matrix, only ASC for metro is unknown.</b>
 * <b> After this stage, we simply create the ring road scenario (probably use graphhoper routing engine) and travel time between Haridwar-Rishikesh connectivity using integrated graphhoper routing engine and here maps API.</b>
 * <b> This should give the new metro ridership, i.e., impact of ring road as well as the connectivity of NH between Haridwar and Rishikesh.</b>
 */
public class Metro2021ScenarioASCCalibration {

    private static final String OD_all_2021_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/OD_2021_all.txt";
    private static final String OD_metro_2021_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/OD_2021_metro.txt";

    private final DMAZonesProcessor dmaZonesProcessor;
    private final GHNetworkDistanceCalculator ghNetworkDistanceCalculator;

    public Metro2021ScenarioASCCalibration(DMAZonesProcessor dmaZonesProcessor, GHNetworkDistanceCalculator ghNetworkDistanceCalculator){
        this.dmaZonesProcessor = new DMAZonesProcessor();
        this.ghNetworkDistanceCalculator = ghNetworkDistanceCalculator;
    }

    public static void main(String[] args) {
        String outFile = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/OD_2021_metro_trips_comparison_28-11-2021.txt";
        new Metro2021ScenarioASCCalibration(new DMAZonesProcessor(), new GHNetworkDistanceCalculator(new MetroStopsQuadTree())).run(outFile, OD2MetroTripCharsWriter.readMetroData(HaridwarRishikeshScenarioRunner.OD_2_metro_trips_characteristics));
    }

    void run(String outputFile, Map<Id<OD>, TripChar> od2metroTripChar){
        Map<Id<OD>, OD> od_2021_all = generateOD(OD_all_2021_file);
        Map<Id<OD>, OD> od_2021_metro = generateOD(OD_metro_2021_file);

        //process od_all and od_metro, store everything in attributes.
        for(OD od : od_2021_all.values()){

            double metroTrips = od_2021_metro.get(od.getId()).getNumberOfTrips();
            od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old, metroTrips);

            double metroShare = metroTrips/ od.getNumberOfTrips();
            TripChar metroTC = od2metroTripChar.get(od.getId());

            if (metroTrips ==0. || od.getNumberOfTrips()==0.){
                od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC, Double.NaN);
            } else if (metroTC==null) {
                od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old, 0.0); //also update the metro trips in OD matrix so that it is not carried forward
                od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC, Double.NaN);
            } else{
                List<Coord> origin = this.dmaZonesProcessor.getRandomCoords(od.getOrigin(),HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);
                List<Coord> destination = this.dmaZonesProcessor.getRandomCoords(od.getDestination(), HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone);

                List<Double> sum_exp_util_except_metro = new ArrayList<>();
                for (int i = 0; i<origin.size(); i ++) {
                    double sum_util_per_OD = 0;
                for (DehradunUtils.TravelModesBaseCase2017 tMode : DehradunUtils.TravelModesBaseCase2017.values()) {
                        TripChar tc = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
                                DehradunUtils.Reverse_transformation.transform(destination.get(i)), tMode.name());
                        double tripDist = tc.tripDist;
                        double tripTime = tc.tripTime;
                    sum_util_per_OD += Math.exp(UtilityComputation.getUtilExceptMetro(tMode, tripDist, tripTime));
                    }
                    sum_exp_util_except_metro.add(sum_util_per_OD);
                }

                boolean keepMetroTrips = false;
                List<Double> ascs_metro = new ArrayList<>();
                for (int i = 0; i<HaridwarRishikeshScenarioRunner.numberOfPoints2DrawInEachZone; i ++){
                    if(sum_exp_util_except_metro.get(i)==0.) {
//                        LogManager.getLogger(Metro2021ScenarioASCCalibration.class).warn("The sum of exponential of utility of all modes except metro is zero for OD " + od.getId() + ". This means everyone will use metro. This should not happen.");
//                        od.getAttributes().putAttribute(METRO_ASC, Double.NaN);
                    } else {
//                        TripChar metroTC = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
//                                DehradunUtils.Reverse_transformation.transform(destination.get(i)), DehradunUtils.TravelModesMetroCase2021.metro.name());

                      if( metroTC.accessDist <= HaridwarRishikeshScenarioRunner.threshold_access_egress_distance && metroTC.egressDist  <= HaridwarRishikeshScenarioRunner.threshold_access_egress_distance) {
                            keepMetroTrips = true;
                            double util_metro_no_asc = UtilityComputation.getUtilMetroWithoutASC(metroTC.tripDist + metroTC.accessDist + metroTC.egressDist, metroTC.tripTime + metroTC.accessTime + metroTC.egressTime);
                            double asc_metro = getMetroASC(metroShare, sum_exp_util_except_metro.get(i), util_metro_no_asc);
                            ascs_metro.add(asc_metro);
                        }
                    }
                }
                if(keepMetroTrips){
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC, ListUtils.doubleMean(ascs_metro));
                } else{
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old, 0.0); //also update the metro trips in OD matrix so that it is not carried forward
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC, Double.NaN);
                }
            }
        }

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)){
            writer.write("origin\tdestination\ttotalTrips\tmetroTrips" +
                    "\tASC_metro\n");
            for(OD od : od_2021_all.values()){
                writer.write(od.getOrigin()+"\t");
                writer.write(od.getDestination()+"\t");
                writer.write(od.getNumberOfTrips()+"\t");
                writer.write(od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old)+"\t");
                writer.write(od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.METRO_ASC)+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+outputFile+". Possible reason "+e);
        }
        HaridwarRishikeshScenarioRunner.LOG.info("Finished processing and writing.");
    }

    private double getMetroASC(double metroShare, double sum_exp_util_all_modes_except_metro, double util_metro_except_ASC){
        // metroShare = exp(U_metro) / sum_exp(U_modes)
        //exp(u_metro) = metroShare * sum_exp(U_modes_except_metro) + metroShare * exp(U_metro)
        //(1-metroShare) * exp(U_metro) = metroShare * sum_exp(U_modes_except_metro)
        // ASC_metro+util_metro_except_ASC = ln ( (metroShare * sum_exp(U_modes_except_metro)) / (1-metroShare) )
        double a = (metroShare * sum_exp_util_all_modes_except_metro) / (1-metroShare);
        return Math.log(a) - util_metro_except_ASC;
    }

    private Map<Id<OD>, OD> generateOD(String inputFile){
        Map<Id<OD>, OD> odMap = new HashMap<>();
        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        try {
            String line = reader.readLine();
            List<String> destinations = null;
            while (line!=null){
                String [] parts = line.split("\t");
                if (destinations == null ){
                    destinations = Arrays.asList(parts);
                } else {
                    String origin = parts[0];
                    for (int index = 1; index<destinations.size()-2;index++){ // first column is origin number, last column is row sum --> no need to store them
                        String desti = destinations.get(index);
                        if (origin.equalsIgnoreCase("Total")) continue; // last row is column sum --> no need to store it.
                        else if(this.dmaZonesProcessor.excludeTrip(origin, desti)) {
                            //--> excluding any trip which starts/ ends in dehradun zones
                            continue;
                        }

                        OD od = new OD(origin, desti);
                        od.setNumberOfTrips( (int) Math.round(Integer.parseInt(parts[index]) ) );
                        odMap.put(od.getId(), od);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return odMap;
    }
}



