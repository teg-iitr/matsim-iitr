package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 20-11-2021
 */

public class MetroStopsCloseToODWriter {

    private static final String metro_trips_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_trips_comparison_gh-router_19-10-2021.txt";
    private static final String nearest_metro_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/nearest_metro_stops_ODs.txt";

    private final Map<Id<OD>, OD> od2OD = new HashMap<>();
    private final DMAZonesProcessor zonesProcessor = new DMAZonesProcessor();

    public static void main(String[] args) {
        MetroStopsCloseToODWriter metroTripsCollector = new MetroStopsCloseToODWriter();
        metroTripsCollector.readODFile();
        metroTripsCollector.run();
        metroTripsCollector.writeFile();
    }

    private void writeFile(){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(nearest_metro_file)){
            writer.write("odID\torigin\tdestination\torigin_metro\tdetination_metro\tmetroLine\n");
            List<Id<OD>> ods = new ArrayList<>(od2OD.keySet());
            Collections.sort(ods);
            for(Id<OD> id : ods){
                OD od = this.od2OD.get(id);
                writer.write(id+"\t");
                writer.write(od.getOrigin()+"\t");
                writer.write(od.getDestination()+"\t");
                writer.write(od.getOrigin_metro_stop()+"\t");
                writer.write(od.getDestination_metro_stop()+"\t");
                writer.write(od.getOrigin_metro_stop().getAttributes().getAttribute(MetroStopsQuadTree.metro_line_name)+"\t");
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+nearest_metro_file+". Possible reason "+e);
        }
    }

    private void run(){
        MetroStopsQuadTree metroStopsQuadTree = new MetroStopsQuadTree();
        for(OD od : this.od2OD.values()){
            Coord origin = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(od.getOrigin(), 1).get(0));
            Coord destination = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(od.getDestination(), 1).get(0));

            Node [] nearestMetroStops_origin = metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(origin);
            Node [] nearestMetroStops_destination = metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(destination);
            nearestMetroStops_destination = MetroStopsQuadTree.arrangeMetroStopsAsPerOriginLines(nearestMetroStops_origin, nearestMetroStops_destination);

            double shortestDist = Double.POSITIVE_INFINITY;
            Node nearest_origin = null;
            Node nearest_destination = null;
            for (int i = 0; i<nearestMetroStops_origin.length; i++) {
                double dist =  GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(nearestMetroStops_origin[i].getCoord(), nearestMetroStops_destination[i].getCoord(),"metro",null).tripDist;
                if (dist < shortestDist) {
                    nearest_origin = nearestMetroStops_origin[i];
                    nearest_destination = nearestMetroStops_destination[i];
                    shortestDist = dist;
                }
            }

            od.setOrigin_metro_stop(nearest_origin);
            od.setDestination_metro_stop(nearest_destination);
        }
    }

    private void readODFile(){
        try(BufferedReader reader = IOUtils.getBufferedReader(metro_trips_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(!header){
                    String [] parts = line.split("\t");
                    Id<OD> odID = OD.getID(parts[0],parts[1]);

                    OD od = this.od2OD.getOrDefault(odID, new OD(parts[0],parts[1]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.total_trips,Double.parseDouble(parts[2]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old,Double.parseDouble(parts[3]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_new,Double.parseDouble(parts[5]));

                    od2OD.put(odID, od);
                } else{
                    header=false;
                }
                line = reader.readLine();
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }
}
