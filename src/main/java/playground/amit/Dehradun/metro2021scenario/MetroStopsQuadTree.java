package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author Amit, created on 21-10-2021
 */

public class MetroStopsQuadTree {

    private static final String station_location_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_stop_locations_HR-region.txt";
    public static final String metro_line_name = "line_name";
    public static final String node_name = "stop_name";
    public static final String node_id_opposite_direction = "stop_id_opposite_direction";

    private final QuadTree<Node> qt;
    private final Network metroStopsNetwork;

    public MetroStopsQuadTree() {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        this.metroStopsNetwork = scenario.getNetwork();
        NetworkFactory networkFactory = metroStopsNetwork.getFactory();
        try(BufferedReader reader = IOUtils.getBufferedReader(station_location_file)){
            String line = reader.readLine();
            boolean header= true;
            while(line!=null){
                if(header){
                    header=false;
                    line = reader.readLine();
                } else {
                    String [] parts = line.split("\t");
                    Node n = networkFactory.createNode(Id.createNodeId(parts[1]),
                            new Coord(Double.parseDouble(parts[3]),Double.parseDouble(parts[2])));
                    n.getAttributes().putAttribute(node_name, parts[0]);
                    n.getAttributes().putAttribute(metro_line_name, parts[4]);
                    n.getAttributes().putAttribute(node_id_opposite_direction, switchCharacters(parts[1]));
                    metroStopsNetwork.addNode(n);
                    line = reader.readLine();
                }
            }
        }catch (IOException e){
            throw new RuntimeException("The file is not available.");
        }

        CalcBoundingBox calcBoundingBox = new CalcBoundingBox();
        calcBoundingBox.run(metroStopsNetwork);

        this.qt = new QuadTree<>(calcBoundingBox.getMinX(), calcBoundingBox.getMinY(), calcBoundingBox.getMaxX(), calcBoundingBox.getMaxY());
        metroStopsNetwork.getNodes().values().forEach(n -> this.qt.put(n.getCoord().getX(), n.getCoord().getY(), n));
    }

    /**
     * switches the places of first two characters
     * @param str
     */
    private String switchCharacters(String str){
       char first=str.charAt(0);
       char second=str.charAt(1);
       return String.valueOf(second) + first + str.substring(2);
    }

    public QuadTree<Node> getQuadTree(){
        return this.qt;
    }

    public Network getMetroStopsNetwork() {
        return metroStopsNetwork;
    }

    public Node [] getNearestNodeAndNodeInOppositeDirection(Coord cord){
        Node nearestMetroStop_origin = this.qt.getClosest(cord.getX(), cord.getY());
        Node node_oppDir = this.metroStopsNetwork.getNodes().get(Id.createNodeId((String) nearestMetroStop_origin.getAttributes().getAttribute(MetroStopsQuadTree.node_id_opposite_direction)));
        return new Node [] {nearestMetroStop_origin, node_oppDir};
    }

    public static Node [] arrangeMetroStopsAsPerOriginLines(Node [] nearestMetroStops_origin, Node [] nearestMetroStops_destination){
        if ( (nearestMetroStops_origin[0].getAttributes().getAttribute(MetroStopsQuadTree.metro_line_name)).equals(nearestMetroStops_destination[0].getAttributes().getAttribute(MetroStopsQuadTree.metro_line_name))) {
            return nearestMetroStops_destination;
        } else {
            return new Node [] {nearestMetroStops_destination[1], nearestMetroStops_destination[0]};
        }
    }
}
