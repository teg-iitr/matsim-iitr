package playground.amit.Dehradun.metro2021scenario;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;
import playground.amit.utils.ListUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Amit
 */

public class TravelTimeMatrixWriter {

    private static final String zone_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/zones_update_11092021/zones_updated.shp";
    private static final int numberOfPoints2DrawInEachZone = 10;
    private static final String outFolder = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/TravelTimeMatrix/";
    private static final String suffix = "16-10-2021.txt";
    private final Collection<SimpleFeature> features;
    private final DMAZonesProcessor dmaZonesProcessor;

    private final List<String> zones_with_forest_areas = List.of("100","120","121","99","123","98","122","25","108","125","131","132","133","134","182");

    public TravelTimeMatrixWriter(){
        this.features = ShapeFileReader.getAllFeatures(zone_file);
        this.dmaZonesProcessor = new DMAZonesProcessor();
    }

    public static void main(String[] args) {
        new TravelTimeMatrixWriter().run();
    }

    private void run(){
        List<OD> modal_od_travelTimes = new ArrayList<>();
        GHNetworkDistanceCalculator ghNetworkDistanceCalculator = new GHNetworkDistanceCalculator();

        for(SimpleFeature origin_feature : features) {
            String origin = (String) origin_feature.getAttribute("Zone"); // a unique key
            if (origin.equals("181")) origin ="135"; // cannot distinguish between 181 and 135

            for (SimpleFeature destination_feature : features) {
                String destination = (String) destination_feature.getAttribute("Zone"); // a unique key
                if (destination.equals("181")) destination ="135"; // cannot distinguish between 181 and 135

                System.out.println("Getting travel time and distances for origin and destination "+origin+", "+destination);

                OD od = new OD(origin, destination);

                List<Coord> origins;

                if(zones_with_forest_areas.contains(origin)) {
                    origins = this.dmaZonesProcessor.getRandomPointsInsideFeature_excludedForestArea(origin_feature,numberOfPoints2DrawInEachZone);
                }
                else origins = this.dmaZonesProcessor.getRandomPointsInsideFeature(origin_feature,numberOfPoints2DrawInEachZone);

                List<Coord> destinations;
                if(zones_with_forest_areas.contains(destination)) {
                    destinations = this.dmaZonesProcessor.getRandomPointsInsideFeature_excludedForestArea(destination_feature, numberOfPoints2DrawInEachZone);
                }
                else destinations = this.dmaZonesProcessor.getRandomPointsInsideFeature(destination_feature,numberOfPoints2DrawInEachZone);

                for (DehradunUtils.TravelModesMetroCase2021 mode : DehradunUtils.TravelModesMetroCase2021.values()) {
                    List<Double> distances = new ArrayList<>();
                    List<Double> times = new ArrayList<>();

                    for (int index =0 ; index < numberOfPoints2DrawInEachZone; index ++) {
                        Tuple<Double, Double> dist_time = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromGHRouter(origins.get(index),destinations.get(index),mode.toString());
                        distances.add(dist_time.getFirst());
                        times.add(dist_time.getSecond());
                    }
                    od.getAttributes().putAttribute(mode+"_distance_Km", ListUtils.doubleMean(distances));
                    od.getAttributes().putAttribute(mode+"_times_h", ListUtils.doubleMean(times));
                }
                modal_od_travelTimes.add(od);
            }
        }
        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFolder + "/modal_od_summary"+suffix)){
            writer.write("origin\tdestination\tmode\tdistanceInKm\ttravelTimeInHr\n");
            for (OD od : modal_od_travelTimes) {
                for (DehradunUtils.TravelModesMetroCase2021 mode : DehradunUtils.TravelModesMetroCase2021.values()) {
                    writer.write(od.getOrigin()+"\t");
                    writer.write(od.getDestination()+"\t");
                    writer.write(mode+"\t");
                    writer.write(od.getAttributes().getAttribute(mode+"_distance_Km")+"\t");
                    writer.write(od.getAttributes().getAttribute(mode+"_times_h")+"\t");
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+ outFolder +". Possible reason "+e);
        }
    }


}