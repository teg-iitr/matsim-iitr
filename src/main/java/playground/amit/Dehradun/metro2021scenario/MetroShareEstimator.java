package playground.amit.Dehradun.metro2021scenario;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.ListUtils;
import playground.amit.utils.NumberUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Amit, created on 17-10-2021
 */

public class MetroShareEstimator {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String OD_merged_file = SVN_repo + "atIITR/OD_2021_metro_trips_comparison_17-10-2021.txt";
    private final Map<Id<OD>, OD> odMap = new HashMap<>();
    private static final int numberOfPoints2DrawInEachZone = 10;

    private final DMAZonesProcessor dmaZonesProcessor;

    public static final String new_metro_trips = "new_metro_trips";
    private static final String outFile = SVN_repo + "atIITR/metro_trips_comparison_17-10-2021.txt";

    MetroShareEstimator(){
        this.dmaZonesProcessor = new DMAZonesProcessor();
    }

    public static void main(String[] args) {
        new MetroShareEstimator().run();
    }

    private void run(){
        readODFile();
        computeMetroShare();
        writeData();
    }

    private void writeData(){
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

    private void readODFile(){
        try(BufferedReader reader = IOUtils.getBufferedReader(OD_merged_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(!header){
                    String [] parts = line.split("\t");
                    OD od = new OD(parts[0], parts[1]);
                    od.setNumberOfTrips(Integer.parseInt(parts[2]));
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
        for(OD od : this.odMap.values()) {
            double metroTrips = Double.parseDouble((String) od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_TRIPS));
            if (metroTrips == 0. || od.getNumberOfTrips()==0.){
                od.getAttributes().putAttribute(new_metro_trips, 0.);
            } else{
                List<Coord> origin = this.dmaZonesProcessor.getRandomCoords(od.getOrigin(), numberOfPoints2DrawInEachZone);
                List<Coord> destination = this.dmaZonesProcessor.getRandomCoords(od.getDestination(), numberOfPoints2DrawInEachZone);
                double asc_metro = Double.parseDouble((String) od.getAttributes().getAttribute(Metro2021ScenarioASCCalibration.METRO_ASC));

                List<Double> metroShareList = new ArrayList<>();
                for (int i = 0; i<MetroShareEstimator.numberOfPoints2DrawInEachZone; i++) {
                    double util_rest_modes = 0.;
                    for (DehradunUtils.TravelModesBaseCase2017 tMode : DehradunUtils.TravelModesBaseCase2017.values()) {

                        Tuple<Double, Double> distTime = GHNetworkDistanceCalculator.getTripDistanceInKmTimeInHr(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
                                DehradunUtils.Reverse_transformation.transform(destination.get(i)), tMode.name());
                        double tripDist = distTime.getFirst();
                        double tripTime = distTime.getSecond();
                        util_rest_modes += Math.exp(UtilityComputation.getUtilExceptMetro(tMode, tripDist, tripTime));
                    }

                    Tuple<Double, Double> distTime = GHNetworkDistanceCalculator.getTripDistanceInKmTimeInHr(DehradunUtils.Reverse_transformation.transform(origin.get(i)),
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
