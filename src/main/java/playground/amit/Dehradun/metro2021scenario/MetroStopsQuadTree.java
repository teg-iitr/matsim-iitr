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
import playground.amit.Dehradun.DehradunUtils;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author Amit, created on 21-10-2021
 */

public class MetroStopsQuadTree {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String station_location_file = SVN_repo+"atIITR/metro_stop_locations.txt";
    public static final String node_line_name = "line_name";
    public static final String node_name = "stop_name";

    private final QuadTree<Node> qt;

    public MetroStopsQuadTree() {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory networkFactory = network.getFactory();
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
                    n.getAttributes().putAttribute(node_line_name, parts[4]);
                    network.addNode(n);
                    line = reader.readLine();
                }
            }
        }catch (IOException e){
            throw new RuntimeException("The file is not available.");
        }

        CalcBoundingBox calcBoundingBox = new CalcBoundingBox();
        calcBoundingBox.run(network);

        this.qt = new QuadTree<>(calcBoundingBox.getMinX(), calcBoundingBox.getMinY(), calcBoundingBox.getMaxX(), calcBoundingBox.getMaxY());
        network.getNodes().values().forEach(n -> this.qt.put(n.getCoord().getX(), n.getCoord().getY(), n));
    }

    public QuadTree<Node> getQuadTree(){
        return this.qt;
    }


}
