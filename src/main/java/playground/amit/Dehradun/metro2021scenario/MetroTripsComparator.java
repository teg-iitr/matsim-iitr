package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;
import playground.amit.utils.NetworkUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 21-10-2021
 */

public class MetroTripsComparator {

    private static final String metro_trips_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_trips_comparison_gh-router_10-11-2021.txt";
    private static final String stop_metro_share = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_share_change_at_stops_10-11-2021.txt";

    private final Map<Id<OD>, OD> odId2OD = new HashMap<>();
    private final DMAZonesProcessor zonesProcessor = new DMAZonesProcessor();

    private final Map<Id<Node>, MetroStopDetails> stop_details = new HashMap<>();
//    private static final String metro_trips_before = "metro_trips_before";
//    private static final String metro_trips_after = "metro_trips_after";

    public static void main(String[] args) {
        MetroTripsComparator metroTripsCollector = new MetroTripsComparator();
        metroTripsCollector.readODFile();
        metroTripsCollector.setNearestStopToZone();
        metroTripsCollector.writeFile();
    }

    private void writeFile(){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(stop_metro_share)){
            writer.write("stopId\tmetroStopName\tmetroLine" +
                    "\tboarding_before\tboarding_after\tboarding_pct_change" +
                    "\talighting_before\talighting_after\talighting_pct_change\n");
            List<Id<Node>> stops = new ArrayList<>(stop_details.keySet());
            Collections.sort(stops);
            for(Id<Node> stop : stops){
                writer.write(stop+"\t");
                writer.write(this.stop_details.get(stop).getStop().getAttributes().getAttribute(MetroStopsQuadTree.node_name)+"\t");
                writer.write(this.stop_details.get(stop).getStop().getAttributes().getAttribute(MetroStopsQuadTree.metro_line_name)+"\t");
                writer.write(this.stop_details.get(stop).getBoarding_before()+"\t");
                writer.write(this.stop_details.get(stop).getBoarding_after()+"\t");
                writer.write(this.stop_details.get(stop).getBoarding_pct_change()+"\t");
                writer.write(this.stop_details.get(stop).getAlighting_before()+"\t");
                writer.write(this.stop_details.get(stop).getAlighting_after()+"\t");
                writer.write(this.stop_details.get(stop).getAlighting_pct_change()+"\t");
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+stop_metro_share+". Possible reason "+e);
        }
    }

    private void setNearestStopToZone(){

        MetroStopsQuadTree metroStopsQuadTree = new MetroStopsQuadTree();
        for(OD od : this.odId2OD.values()){
            Coord origin = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(od.getOrigin(), 1).get(0));
            Coord destination = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(od.getDestination(), 1).get(0));

            Node [] nearestMetroStops_origin = metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(origin);
            Node [] nearestMetroStops_destination = metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(destination);
            nearestMetroStops_destination = MetroStopsQuadTree.arrangeMetroStopsAsPerOriginLines(nearestMetroStops_origin, nearestMetroStops_destination);

            double shortestDist = Double.POSITIVE_INFINITY;
            Node nearest_origin = null;
            Node nearest_destination = null;
            for (int i = 0; i<nearestMetroStops_origin.length; i++) {
                double dist =  GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(nearestMetroStops_origin[i].getCoord(), nearestMetroStops_destination[i].getCoord(),"metro",null).getFirst();
                if (dist < shortestDist) {
                    nearest_origin = nearestMetroStops_origin[i];
                    nearest_destination = nearestMetroStops_destination[i];
                    shortestDist = dist;
                }
            }

            double access_dist = NetworkUtils.haversineDistanceKm(origin.getY(), origin.getX(), nearest_origin.getCoord().getY(), nearest_origin.getCoord().getX());
            double egress_dist = NetworkUtils.haversineDistanceKm(destination.getY(), destination.getX(), nearest_destination.getCoord().getY(), nearest_destination.getCoord().getX());
            if (access_dist < 2. && egress_dist < 2.) {
                od.setOrigin_metro_stop(nearest_origin);
                od.setDestination_metro_stop(nearest_destination);

                {
                    MetroStopDetails metroStopDetails_origin = this.stop_details.getOrDefault(nearest_origin.getId(), new MetroStopDetails(nearest_origin));
                    metroStopDetails_origin.addBoarding_before((Double) od.getAttributes().getAttribute(OD.metro_old));
                    metroStopDetails_origin.addBoarding_after((Double) od.getAttributes().getAttribute(OD.metro_new));
                    stop_details.put(nearest_origin.getId(),metroStopDetails_origin);
                }
                {
                    MetroStopDetails metroStopDetails_destination = this.stop_details.getOrDefault(nearest_destination.getId(), new MetroStopDetails(nearest_destination));
                    metroStopDetails_destination.addAlighting_before((Double) od.getAttributes().getAttribute(OD.metro_old));
                    metroStopDetails_destination.addAlighting_after((Double) od.getAttributes().getAttribute(OD.metro_new));
                    stop_details.put(nearest_origin.getId(),metroStopDetails_destination);
                }
            }
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

                    OD od = this.odId2OD.getOrDefault(odID, new OD(parts[0],parts[1]));
                    od.getAttributes().putAttribute(OD.total_trips,Double.parseDouble(parts[2]));
                    od.getAttributes().putAttribute(OD.metro_old,Double.parseDouble(parts[3]));
                    od.getAttributes().putAttribute(OD.metro_new,Double.parseDouble(parts[5]));

                    odId2OD.put(odID, od);
                } else{
                    header=false;
                }
                line = reader.readLine();
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }

    private static class Zone{
        private final String zoneId;
        private final Map<String, Double> modeToIncomingTrips = new HashMap<>();
        private final Map<String, Double> modeToOutgoingTrips = new HashMap<>();
        private Node nearestMetroNode;

        Zone(String zoneId){
            this.zoneId = zoneId;
        }

        String getZoneId() {
            return zoneId;
        }

        void addIncomingTrips(String mode, double numberOfTrips){
            this.modeToIncomingTrips.put(mode, getIncomingTrips(mode)+numberOfTrips);
        }

        void addOutgoingTrips(String mode, double numberOfTrips){
            this.modeToOutgoingTrips.put(mode, getOutgoingTrips(mode)+numberOfTrips);
        }

        double getIncomingTrips(String mode){
            return this.modeToIncomingTrips.getOrDefault(mode,0.);
        }

        double getOutgoingTrips(String mode){
            return this.modeToOutgoingTrips.getOrDefault(mode,0.);
        }

        public Node getNearestMetroNode() {
            return nearestMetroNode;
        }

        public void setNearestMetroNode(Node nearestMetroNode) {
            this.nearestMetroNode = nearestMetroNode;
        }
    }
}
