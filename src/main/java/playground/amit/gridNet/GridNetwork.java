package playground.amit.gridNet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Amit.
 */

public class GridNetwork {

    public static final double LengthOfGrid = 10000.0;
    public static final String NETWORK_FILE = "C:\\Users\\amit2\\Documents\\svn-repos\\shared\\data\\project_data\\matsim_grid_example\\gridNet.xml.gz";

    public GridNetwork() {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();
        networkFactory = network.getFactory();
    }

    private final Network network;
    private final Map<Coord, Id<Node>> coordToNodeId = new HashMap<>();
    private final NetworkFactory networkFactory;

    public static void main(String[] args) {
        new GridNetwork().run();
    }

    public void run() {

        // major artrials at every 500m [nodes also at 250 to connect arterials with them
        createLinks(new Coord(0.,0.), new Coord[] {new Coord(LengthOfGrid/4,0), new Coord(0,LengthOfGrid/4)},
                LengthOfGrid, 3);
        createLinks(new Coord(LengthOfGrid,LengthOfGrid), new Coord[] {new Coord(-LengthOfGrid/4,0), new Coord(0,-LengthOfGrid/4)},
                LengthOfGrid, 3);
        createLinks(new Coord(LengthOfGrid/2,LengthOfGrid/2), new Coord[] {new Coord(LengthOfGrid/4, 0.)},
                LengthOfGrid/2, 3);
        createLinks(new Coord(LengthOfGrid/2,LengthOfGrid/2), new Coord[] { new Coord(0,LengthOfGrid/4)},
                LengthOfGrid/2, 3);
        createLinks(new Coord(LengthOfGrid/2,LengthOfGrid/2), new Coord[] {new Coord(-LengthOfGrid/4,0)},
                LengthOfGrid/2, 3);
        createLinks(new Coord(LengthOfGrid/2,LengthOfGrid/2), new Coord[] { new Coord(0,-LengthOfGrid/4)},
                LengthOfGrid/2, 3);

        //arterials at every 250m [nodes also at 100m to connect with sub-arterials]
        createLinks(new Coord(0.,LengthOfGrid/4.), new Coord[] {new Coord(LengthOfGrid/10,0)},
                LengthOfGrid, 2);
        createLinks(new Coord(0.,3*LengthOfGrid/4), new Coord[] {new Coord(LengthOfGrid/10,0)},
                LengthOfGrid, 2);
        createLinks(new Coord(LengthOfGrid/4,0.), new Coord[] {new Coord(0,LengthOfGrid/10)},
                LengthOfGrid, 2);
        createLinks(new Coord(3*LengthOfGrid/4.,0.), new Coord[] {new Coord(0,LengthOfGrid/10)},
                LengthOfGrid, 2);

        //sub-arterials at every 100m
        for (int i=1; i<=9; i++){
            if (i==5) continue;
            createLinks(new Coord(0.,LengthOfGrid*i/10), new Coord[] {new Coord(LengthOfGrid/10,0)},
                    LengthOfGrid/5, 1);
            createLinks(new Coord(LengthOfGrid/5,LengthOfGrid*i/10), new Coord[] {new Coord(LengthOfGrid/20,0)},
                    LengthOfGrid/10, 1);
            createLinks(new Coord(3*LengthOfGrid/10,LengthOfGrid*i/10), new Coord[] {new Coord(LengthOfGrid/10,0)},
                    4*LengthOfGrid/10, 1);
            createLinks(new Coord(7*LengthOfGrid/10,LengthOfGrid*i/10), new Coord[] {new Coord(LengthOfGrid/20,0)},
                    LengthOfGrid/10, 1);
            createLinks(new Coord(8*LengthOfGrid/10,LengthOfGrid*i/10), new Coord[] {new Coord(LengthOfGrid/10,0)},
                    LengthOfGrid/5, 1);

            createLinks(new Coord(LengthOfGrid/10*i,0), new Coord[] {new Coord(0,LengthOfGrid/10)},
                    LengthOfGrid/5, 1);
            createLinks(new Coord(LengthOfGrid*i/10,LengthOfGrid/5), new Coord[] {new Coord(0,LengthOfGrid/20)},
                    LengthOfGrid/10, 1);
            createLinks(new Coord(LengthOfGrid*i/10,3*LengthOfGrid/10), new Coord[] {new Coord(0,LengthOfGrid/10)},
                    4*LengthOfGrid/10, 1);
            createLinks(new Coord(LengthOfGrid*i/10,7*LengthOfGrid/10), new Coord[] {new Coord(0,LengthOfGrid/20)},
                    LengthOfGrid/10, 1);
            createLinks(new Coord(LengthOfGrid*i/10,8*LengthOfGrid/10), new Coord[] {new Coord(0,LengthOfGrid/10)},
                    LengthOfGrid/5, 1);
        }
        new NetworkWriter(network).write(GridNetwork.NETWORK_FILE);
    }

    private void createLinks(Coord initialCoord, Coord[] increaments, double lengthToMove, double numberOfLanes){
        for (Coord increm : increaments) {
            double length =0.;
            while(length < lengthToMove){
                Node fromNode = getOrCreateNode(initialCoord);

                Coord nextCoord = new Coord(initialCoord.getX()+increm.getX(), initialCoord.getY() +increm.getY() );
                Node toNode = getOrCreateNode(nextCoord);

                Link link = networkFactory.createLink(Id.createLinkId(network.getLinks().size()+1),fromNode, toNode);
                link.setNumberOfLanes(numberOfLanes);
                link.setFreespeed(getSpeed(numberOfLanes));
                link.setCapacity(getCapacity(numberOfLanes));
                link.setAllowedModes(getAllowedModes(numberOfLanes));
                double linkLength = NetworkUtils.getEuclideanDistance(initialCoord, nextCoord);
                network.addLink(link);

                Link revLink = networkFactory.createLink(Id.createLinkId(network.getLinks().size()+1),toNode, fromNode);
                revLink.setCapacity(getCapacity(numberOfLanes));
                revLink.setNumberOfLanes(numberOfLanes);
                revLink.setFreespeed(getSpeed(numberOfLanes));
                revLink.setAllowedModes(getAllowedModes(numberOfLanes));
                network.addLink(revLink);
                //set link attributes

                length += linkLength;
                initialCoord = nextCoord;
            }
        }
    }

    private double getCapacity(double noOfLanes){
        if (noOfLanes==1) return 1500;
        else if (noOfLanes==2) return 2*1600.;
        else if (noOfLanes==3) return 3*1800;
        else throw new RuntimeException("not implemented yet.");
    }

    private double getSpeed(double noOfLanes){
        if (noOfLanes==1) return 80/3.6;
        else if (noOfLanes==2) return 60/3.6;
        else if (noOfLanes==3) return 60/3.6;
        else throw new RuntimeException("not implemented yet.");
    }

    private Set<String> getAllowedModes(double noOfLanes){
        Set<String> allowedModes = new HashSet<>();
        allowedModes.add(TransportMode.car);
        allowedModes.add("motorcycle");

        if (noOfLanes==1 || noOfLanes ==2) {
            allowedModes.add("bicycle");
            return allowedModes;
        } else if (noOfLanes==3) return allowedModes;
        else throw new RuntimeException("not implemented yet.");
    }

    public  Node getOrCreateNode(Coord cord) {
        Id<Node> nodeId = coordToNodeId.get(cord);
        if (nodeId == null){
            nodeId = Id.createNodeId(network.getNodes().size()+1);
            Node node = networkFactory.createNode(nodeId, cord);
            network.addNode(node);
            coordToNodeId.put(cord, nodeId);
            return node;
        } else {
            return network.getNodes().get(nodeId);
        }
    }
}
