package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.Dehradun.ODWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Amit, created on 22-10-2021
 */

public class MetroStop2StopDistanceMatrix {

    private final Map<Id<OD>,OD> odMap = new HashMap<>();
    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String outputFile = SVN_repo+"atIITR/metro_stops_distance_matrix.txt";
    private final GHNetworkDistanceCalculator ghNetworkDistanceCalculator = new GHNetworkDistanceCalculator();

    public static void main(String[] args) {
        new MetroStop2StopDistanceMatrix().run();
    }

    public void run() {
       getStopsODDistances();
       ODWriter.writeOD(odMap, outputFile);
    }

    private void getStopsODDistances(){
        Network metroStopsNet = new MetroStopsQuadTree().getMetroStopsNetwork();
        for (Node o : metroStopsNet.getNodes().values()){
            for (Node d : metroStopsNet.getNodes().values()){
                OD od = new OD((String)o.getAttributes().getAttribute(MetroStopsQuadTree.node_name),
                        (String)d.getAttributes().getAttribute(MetroStopsQuadTree.node_name));
                double dist = ghNetworkDistanceCalculator.getTripDistanceInKmTimeInHrFromAvgSpeeds(o.getCoord(), d.getCoord(), "metro").getFirst();
                od.setNumberOfTrips(dist); // using dist instead of number of trips, so that matrix writer can be used.
                odMap.put(od.getId(), od);
            }
        }
    }
}
