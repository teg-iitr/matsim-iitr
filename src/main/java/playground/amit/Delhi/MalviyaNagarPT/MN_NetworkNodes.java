package playground.amit.Delhi.MalviyaNagarPT;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static org.matsim.api.core.v01.Id.createLinkId;
import static org.matsim.api.core.v01.Id.get;

public class MN_NetworkNodes {
    private static double numberOfLanes =1;
    public static String coordinatesFile = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/PT_stops_coordinates_links.csv";
    public static String outputNetworkNodesMN = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/matsimFiles/nodes_matsim_network.xml.gz";


    public static void main(String[] args) {

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory networkFactory = network.getFactory();

        Map<String, Coord> busStopToCoordinate = getStopsCoordinates();


////        List<Id<Node>> nodeList = new ArrayList<>();
//        List<Node> nodeList = new ArrayList<>();
//      nodeList.add(nodeAtBusStop);

        for (Map.Entry<String, Coord> entry : busStopToCoordinate.entrySet()) {
            String k = entry.getKey();
            Coord v = entry.getValue();

            Node nodeAtBusStop = networkFactory.createNode(Id.create(k, Node.class), v);
            network.addNode(nodeAtBusStop);

        }


        Node node = (Node) network.getNodes().values();
        Node fromNode = null;
        Node toNode = null;

        Coord fromCoord = fromNode.getCoord();
        Coord toCoord = toNode.getCoord();
        double linkLength = NetworkUtils.getEuclideanDistance(fromCoord, toCoord);

        Link link1 = networkFactory.createLink(createLinkId("a"), fromNode, toNode);
        link1.setNumberOfLanes(numberOfLanes);
        link1.setCapacity(1500);
        link1.setLength(linkLength);
        network.addLink(link1);


        Link link2 = networkFactory.createLink(createLinkId("b"), toNode, fromNode);
        link2.setNumberOfLanes(numberOfLanes);
        link2.setCapacity(1500);
        link2.setLength(linkLength);
        network.addLink(link2);

//            NetworkUtils.createAndAddLink(network,id, fromNode, toNode, length, freespeed, capacity, numLanes );


        new NetworkWriter(network).write(outputNetworkNodesMN);
    }


    public static Map<String, Coord> getStopsCoordinates() {
        Map<String, Coord> busStopToCoordLink = new HashMap<>();
        BufferedReader reader = IOUtils.getBufferedReader(coordinatesFile);
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, MN_TransitDemandGenerator.toCoordinateSystem);
        try {
            String line = reader.readLine();
            boolean isHeader = true;
            while(line!=null) {
                if (isHeader) {
                    isHeader = false;
                } else {
                    String [] parts = line.split(",");
                    String index = parts[0];
                    Coord cord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    Coord transformCoord = ct.transform(cord);
                    busStopToCoordLink.put(index, transformCoord);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
        return busStopToCoordLink;
    }
}


//Unique lnks from node
//     1-22
//        22-18,15,2
//        15-23
//        23-17,4
//        4-24
//        24-25,21
//        21-13
//        13-5,14,12
//        14-5,27
//        12-27,10
//        10-9,11
//        9-27,28
//        28-8,7
//        7-6
//        6-5,27
//        25-5,3
//        3-2
//        2-5,16
//        16-26
//        26-8,20,19

