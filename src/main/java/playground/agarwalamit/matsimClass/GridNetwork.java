package playground.agarwalamit.matsimClass;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.Map;

public class GridNetwork {

    GridNetwork () {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();
        networkFactory = network.getFactory();
    }

    private Network network;
    private Map<Coord, Id<Node>> coordToNodeId = new HashMap<>();
    private NetworkFactory networkFactory;

    public static void main(String[] args) {
        new GridNetwork().run();
    }

    public void run() {

        // major artrials at every 500m [nodes also at 250 to connect arterials with them
        createLinks(new Coord(0.,0.), new Coord[] {new Coord(250,0), new Coord(0,250)},
                1000.0, 3);
        createLinks(new Coord(1000.,1000.), new Coord[] {new Coord(-250,0), new Coord(0,-250)},
                1000.0, 3);
        createLinks(new Coord(500.,500.), new Coord[] {new Coord(250.0, 0.)},
                500.0, 3);
        createLinks(new Coord(500.,500.), new Coord[] { new Coord(0,250)},
                500.0, 3);
        createLinks(new Coord(500.,500.), new Coord[] {new Coord(-250,0)},
                500.0, 3);
        createLinks(new Coord(500.,500.), new Coord[] { new Coord(0,-250)},
                500.0, 3);

        //arterials at every 250m [nodes also at 100m to connect with sub-arterials]
        createLinks(new Coord(0.,250.), new Coord[] {new Coord(100,0)},
                1000.0, 2);
        createLinks(new Coord(0.,750.), new Coord[] {new Coord(100,0)},
                1000.0, 2);
        createLinks(new Coord(250.,0.), new Coord[] {new Coord(0,100)},
                1000.0, 2);
        createLinks(new Coord(750.,0.), new Coord[] {new Coord(0,100)},
                1000.0, 2);

        //sub-arterials at every 100m
        for (int i=1; i<=9; i++){
            if (i==5) continue;
            createLinks(new Coord(0.,100.*i), new Coord[] {new Coord(100,0)},
                    200.0, 1);
            createLinks(new Coord(200.,100.*i), new Coord[] {new Coord(50,0)},
                    100.0, 1);
            createLinks(new Coord(300.,100.*i), new Coord[] {new Coord(100,0)},
                    400.0, 1);
            createLinks(new Coord(700.,100.*i), new Coord[] {new Coord(50,0)},
                    100.0, 1);
            createLinks(new Coord(800.,100.*i), new Coord[] {new Coord(100,0)},
                    200.0, 1);

            createLinks(new Coord(100.*i,0), new Coord[] {new Coord(0,100)},
                    200.0, 1);
            createLinks(new Coord(100.*i,200), new Coord[] {new Coord(0,50)},
                    100.0, 1);
            createLinks(new Coord(100.*i,300), new Coord[] {new Coord(0,100)},
                    400.0, 1);
            createLinks(new Coord(100.*i,700), new Coord[] {new Coord(0,50)},
                    100.0, 1);
            createLinks(new Coord(100.*i,800), new Coord[] {new Coord(0,100)},
                    200.0, 1);
        }
        new NetworkWriter(network).write("C:\\Users\\Amit Agarwal\\Downloads\\gridNet.xml.gz");
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
                double linkLength = NetworkUtils.getEuclideanDistance(initialCoord, nextCoord);
                network.addLink(link);

                Link revLink = networkFactory.createLink(Id.createLinkId(network.getLinks().size()+1),toNode, fromNode);
                revLink.setNumberOfLanes(numberOfLanes);
                network.addLink(revLink);
                //set link attributes

                length += linkLength;
                initialCoord = nextCoord;
            }
        }
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
