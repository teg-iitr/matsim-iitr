package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Amit, created on 21-10-2021
 */

public class MetroTripsCollector {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String outFile = SVN_repo + "atIITR/metro_trips_comparison_gh-router_19-10-2021.txt";

    private final Map<String, Zone> zoneId2Zone = new HashMap<>();
    private final DMAZonesProcessor zonesProcessor = new DMAZonesProcessor();

    public static void main(String[] args) {
        MetroTripsCollector metroTripsCollector = new MetroTripsCollector();
        metroTripsCollector.readODFile();
        metroTripsCollector.run();
    }

    private void run(){
        QuadTree<Node> qt = new MetroStopsQuadTree().getQuadTree();
        for(Zone z : this.zoneId2Zone.values()){
            Coord coord = this.zonesProcessor.getRandomCoords(z.zoneId, 1).get(0);
            Node node = qt.getClosest(coord.getX(), coord.getY());
            z.setNearestMetroNode(node);
        }
    }

    private void readODFile(){
        try(BufferedReader reader = IOUtils.getBufferedReader(outFile)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(!header){
                    String [] parts = line.split("\t");
                    Zone origin = zoneId2Zone.getOrDefault(parts[0],new Zone(parts[0]));
                    Zone destination = zoneId2Zone.getOrDefault(parts[1],new Zone(parts[1]));

                    origin.addOutgoingTrips("total",Double.parseDouble(parts[2]));
                    destination.addIncomingTrips("total",Double.parseDouble(parts[2]));

                    origin.addOutgoingTrips("metro_old",Double.parseDouble(parts[3]));
                    destination.addIncomingTrips("metro_old",Double.parseDouble(parts[3]));

                    origin.addOutgoingTrips("metro_new",Double.parseDouble(parts[5]));
                    destination.addIncomingTrips("metro_new",Double.parseDouble(parts[5]));

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
