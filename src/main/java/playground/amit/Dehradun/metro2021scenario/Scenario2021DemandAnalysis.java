package playground.amit.Dehradun.metro2021scenario;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.OD;
import playground.amit.utils.geometry.GeometryUtils;

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
public class Scenario2021DemandAnalysis {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String zone_file = SVN_repo + "atIITR/zones_update_11092021/zones_updated.shp";
    private Collection<SimpleFeature> features ;

    private static final String OD_all_2021_file = SVN_repo + "atIITR/OD_2021_all.txt";
    private static final String OD_metro_2021_file = SVN_repo + "atIITR/OD_2021_metro.txt";

    private final Random random = MatsimRandom.getLocalInstance();
    private final int numberOfPoints2DrawInEachZone = 1;

    private static final String outFile = SVN_repo + "atIITR/OD_2021_metro_trips_comparison.txt";

    //key of attributes
    private static final String METRO_TRIPS = "metro_trips";
    private static final String TRIP_DIST = "trip_dist";
    private static final String TRIP_TIME = "trip_time";
    private static final String METRO_ASC = "metro_asc";

    public static void main(String[] args) {
        new Scenario2021DemandAnalysis().run();
    }

    private void run(){
        this.features = ShapeFileReader.getAllFeatures(zone_file);

        Map<Id<OD>, OD> od_2021_all = generateOD(OD_all_2021_file);
        Map<Id<OD>, OD> od_2021_metro = generateOD(OD_metro_2021_file);

        //process od_all and od_metro, store everything in attributes.
        for(OD od : od_2021_all.values()){
            double metroTrips = od_2021_metro.get(od.getId()).getNumberOfTrips();
            od.getAttributes().putAttribute(METRO_TRIPS, metroTrips);

            Coord origin = getRandomCoord(od.getOrigin());
            Coord destination = getRandomCoord(od.getDestination());

            double metroShare = metroTrips/ od.getNumberOfTrips();
            if (metroTrips ==0. || od.getNumberOfTrips()==0.){
                od.getAttributes().putAttribute(METRO_ASC, Double.NaN);
            } else{
                double sum_exp_util_except_metro = 0.;
                for (DehradunUtils.TravelModesBaseCase2017 tMode : DehradunUtils.TravelModesBaseCase2017.values()) {
                    double tripDist = getTripDistanceInKm(origin, destination, tMode.name());
                    double tripTime = getTripTimeInHour(origin, destination, tMode.name());
                    sum_exp_util_except_metro += Math.exp(UtilityComputation.getUtilExceptMetro(tMode, tripDist, tripTime));
                }

                if(sum_exp_util_except_metro==0.) {
                    Logger.getLogger(Scenario2021DemandAnalysis.class).warn("The sum of exponential of utility of all modes except metro is zero for OD " + od.getId() + ". This means everyone will use metro. This should not happen.");
                    od.getAttributes().putAttribute(METRO_ASC, Double.NaN);
                } else {
                    double util_metro_no_asc = UtilityComputation.getUtilMetroWithoutASC(getTripDistanceInKm(origin, destination, DehradunUtils.TravelModesMetroCase2021.metro.name()), getTripTimeInHour(origin, destination, DehradunUtils.TravelModesMetroCase2021.metro.name()));
                    double asc_metro = getMetroASC(metroShare, sum_exp_util_except_metro, util_metro_no_asc);
                    od.getAttributes().putAttribute(METRO_ASC, asc_metro);
                }
            }
        }

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)){
            writer.write("origin\tdestination\ttotalTrips\tmetroTrips" +
//                    "\ttripDistance_km\ttripTime_h" +
                    "\tASC_metro\n");
            for(OD od : od_2021_all.values()){
                writer.write(od.getOrigin()+"\t");
                writer.write(od.getDestination()+"\t");
                writer.write(od.getNumberOfTrips()+"\t");
                writer.write(od.getAttributes().getAttribute(METRO_TRIPS)+"\t");
//                writer.write(od.getAttributes().getAttribute(TRIP_DIST)+"\t");
//                writer.write(od.getAttributes().getAttribute(TRIP_TIME)+"\t");
                writer.write(od.getAttributes().getAttribute(METRO_ASC)+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+outFile+". Possible reason "+e);
        }
    }

    private double getMetroASC(double metroShare, double sum_exp_util_all_modes_except_metro, double util_metro_except_ASC){
        // metroShare = exp(U_metro) / sum_exp(U_modes)
        //exp(u_metro) = metroShare * sum_exp(U_modes_except_metro) + metroShare * exp(U_metro)
        //(1-metroShare) * exp(U_metro) = metroShare * sum_exp(U_modes_except_metro)
        // ASC_metro+util_metro_except_ASC = ln ( (metroShare * sum_exp(U_modes_except_metro)) / (1-metroShare) )
        double a = (metroShare * sum_exp_util_all_modes_except_metro) / (1-metroShare);
        return Math.log(a) - util_metro_except_ASC;
    }

    private double getTripDistanceInKm(Coord origin, Coord destination, String travelMode){
        //this should come from a rounting engine line graphhoper; however as of now, we can use the beeline distances
        double dist = NetworkUtils.getEuclideanDistance(origin, destination);

        switch (travelMode){
            case "car":
            case "motorbike":
            case "bicycle":
                dist = 1.3*dist;
                break;
            case "walk":
                dist = 1.1*dist;
                break;
            case "bus":
                dist = 1.5*dist;
                break;
            case "IPT":
                dist = 1.7*dist;
                break;
            case "metro":
                dist = 1.3*dist;
                break;
            default:
                throw new RuntimeException("Transport mode "+travelMode+" not known.");
        }
        return dist/1000.;
    }

    private double getTripTimeInHour(Coord origin, Coord destination, String travelMode){
        // technically, this should come from the simulation or a routing engine, however, we can try to use Here Maps API or Graphhoper routing engine.
        // in absence of the data, we can use the average speeds
        double dist = getTripDistanceInKm(origin,destination,travelMode);
        double tripTime = 0.;
        switch (travelMode){
            case "car":
                tripTime = dist/45;
                break;
            case "motorbike":
                tripTime = dist/70;
                break;
            case "bicycle":
                tripTime = dist/15;
                break;
            case "walk":
                tripTime = dist/5;
                break;
            case "bus":
                tripTime = dist/24;
                break;
            case "IPT":
                tripTime = dist/10.0;
                break;
            case "metro":
                tripTime = dist/50.0;
                break;
            default:
                throw new RuntimeException("Transport mode "+travelMode+" not known.");
        }
        return tripTime;
    }


//    private String getMode(){
//        //Table 6-4 of Metro report provides modal share by trips
//        // except, bus (18%) and rail, shares of 2W, car, shared IPT (&others), walk, cycles are 36, 17, 10, 6, 13
//        // making these to 100%; shares will be 44, 21, 12, 7, 16
//        //Sep. 2021: considering only ONE OD Matrix.
//        int rnd = this.random.nextInt(101);
////        if(rnd <= 44) return DehradunUtils.TravelModes.motorbike.toString();
////        else if (rnd <= 65) return DehradunUtils.TravelModes.car.toString();
////        else if (rnd <= 77) return DehradunUtils.TravelModes.IPT.toString();
////        else if (rnd <= 84) return DehradunUtils.TravelModes.walk.toString();
////        else  return DehradunUtils.TravelModes.bicycle.toString();
//        if(rnd <= 36) return DehradunUtils.TravelModesBaseCase2017.motorbike.toString();
//        else if (rnd <= 53) return DehradunUtils.TravelModesBaseCase2017.car.toString();
//        else if (rnd <= 63) return DehradunUtils.TravelModesBaseCase2017.IPT.toString();
//        else if (rnd <= 69) return DehradunUtils.TravelModesBaseCase2017.walk.toString();
//        else if (rnd <= 82) return DehradunUtils.TravelModesBaseCase2017.bicycle.toString();
//        else  return DehradunUtils.TravelModesBaseCase2017.bus.toString();
//    }


    private Coord getRandomCoord(String zoneId){
        if (zoneId.equals("181")) zoneId ="135"; // cannot distinguish between 181 and 135

        for (SimpleFeature feature : this.features){
            String handle = (String) feature.getAttribute("Zone"); // a unique key
            if (handle.equals(zoneId)){
                Point p = GeometryUtils.getRandomPointsInsideFeature(feature);
                return new Coord(p.getX(), p.getY());
            }
        }
        throw new RuntimeException("The zone "+zoneId+ " is not found in the provided zone shape file.");
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
                        if (origin.equalsIgnoreCase("Total")) continue; // last row is column sum --> no need to store it.

                        OD od = new OD(origin, destinations.get(index));
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



