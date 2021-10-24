package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.utils.FileUtils;
import playground.amit.utils.NumberUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 21-10-2021
 */

public class MetroTripsComparator {

    private static final String metro_trips_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_trips_comparison_gh-router_22-10-2021.txt";
    private static final String stop_metro_share = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_share_change_at_stop_23-10-2021.txt";

    private final Map<String, Zone> zoneId2Zone = new HashMap<>();
    private final DMAZonesProcessor zonesProcessor = new DMAZonesProcessor();

    private final Map<Id<Node>, MetroStopDetails> stop_details = new HashMap<>();
    private static final String metro_trips_before = "metro_trips_before";
    private static final String metro_trips_after = "metro_trips_after";

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
                writer.write(this.stop_details.get(stop).getStop().getAttributes().getAttribute(MetroStopsQuadTree.node_line_name)+"\t");
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
        QuadTree<Node> qt = new MetroStopsQuadTree().getQuadTree();
        for(Zone z : this.zoneId2Zone.values()){
            Coord coord = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(z.zoneId, 1).get(0));
            Node node = qt.getClosest(coord.getX(), coord.getY());
            z.setNearestMetroNode(node);

            MetroStopDetails metroStopDetails = this.stop_details.getOrDefault(node.getId(), new MetroStopDetails(node));
            metroStopDetails.addAlighting_before(z.getIncomingTrips(metro_trips_before));
            metroStopDetails.addAlighting_after(z.getIncomingTrips(metro_trips_after));

            metroStopDetails.addBoarding_before(z.getOutgoingTrips(metro_trips_before));
            metroStopDetails.addBoarding_after(z.getOutgoingTrips(metro_trips_after));
            stop_details.put(node.getId(),metroStopDetails);
        }
    }

    private void readODFile(){
        try(BufferedReader reader = IOUtils.getBufferedReader(metro_trips_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(!header){
                    String [] parts = line.split("\t");
                    Zone origin = zoneId2Zone.getOrDefault(parts[0],new Zone(parts[0]));
                    Zone destination = zoneId2Zone.getOrDefault(parts[1],new Zone(parts[1]));

                    origin.addOutgoingTrips("total",Double.parseDouble(parts[2]));
                    destination.addIncomingTrips("total",Double.parseDouble(parts[2]));

                    origin.addOutgoingTrips(metro_trips_before,Double.parseDouble(parts[3]));
                    destination.addIncomingTrips(metro_trips_before,Double.parseDouble(parts[3]));

                    origin.addOutgoingTrips(metro_trips_after,Double.parseDouble(parts[5]));
                    destination.addIncomingTrips(metro_trips_after,Double.parseDouble(parts[5]));

                    zoneId2Zone.put(origin.getZoneId(), origin);
                    zoneId2Zone.put(origin.getZoneId(), destination);
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
