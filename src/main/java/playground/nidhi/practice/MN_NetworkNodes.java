package playground.nidhi.practice;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Delhi.MalviyaNagarPT.MN_TransitDemandGenerator;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static org.matsim.api.core.v01.Id.createLinkId;

public class MN_NetworkNodes {
    private static double numberOfLanes =1;
    public static String coordinatesFile = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/PT_stops_coordinates_links.csv";
    public static String outputNetworkNodesMN = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/matsimFiles/nodes_matsim_network.xml.gz";
    private static Scenario scenario;
    private static Network network;
    private static NetworkFactory networkFactory;


    MN_NetworkNodes (Network  network) {
        this.network = network;
        this.networkFactory = this.network.getFactory();
    }

    public static void main(String[] args) {

        scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();
        networkFactory = network.getFactory();
        MN_NetworkNodes.addNode();
        MN_NetworkNodes.addLink();
        new NetworkWriter(network).write(outputNetworkNodesMN);
    }





    private static void addNode() {
        Map<String, Coord> busStopToCoordinate = getStopsCoordinates();

        for (Map.Entry<String, Coord> entry : busStopToCoordinate.entrySet()) {
            String k = entry.getKey();
            Coord v = entry.getValue();
            Node nodeAtBusStop = networkFactory.createNode(Id.create(k, Node.class), v);
            network.addNode(nodeAtBusStop);
        }

    }


    public static void addLink(){
        createLink("1", "22");
        createLink("22", "2");
        createLink("2", "16");
        createLink("16", "26");
        createLink("26", "19");
        createLink("18", "22");
        createLink("22", "15");
        createLink("15", "23");
        createLink("17", "23");
        createLink("23", "4");
        createLink("4", "24");
        createLink("24", "25");
        createLink("24", "21");
        createLink("21", "13");
        createLink("13", "12");
        createLink("12", "10");
        createLink("10", "11");
        createLink("20", "26");
        createLink("26", "8");
        createLink("8", "28");
        createLink("28", "9");
        createLink("9", "10");
        createLink("2", "3");
        createLink("3", "25");
        createLink("25", "5");
        createLink("5", "14");
        createLink("14", "13");
        createLink("5", "6");
        createLink("6", "7");
        createLink("7", "28");
        createLink("14", "27");
        createLink("27", "9");

    }

    public static void createLink(String fromNodeID, String toNodeID) {
        Node n1 = network.getNodes().get(Id.createNodeId(fromNodeID));
        Node n2 = network.getNodes().get(Id.createNodeId(toNodeID));

        Coord fromCoord = n1.getCoord();
        Coord toCoord = n2.getCoord();
        double linkLength = NetworkUtils.getEuclideanDistance(fromCoord, toCoord);

        Link link12 = networkFactory.createLink(createLinkId(n1.getId()+"_"+n2.getId()), n1, n2);
        link12.setNumberOfLanes(numberOfLanes);
        link12.setCapacity(1500);
        link12.setLength(linkLength);
        network.addLink(link12);


        Link link21 = networkFactory.createLink(createLinkId(n2.getId()+"_"+n1.getId()), n1, n2);
        link21.setNumberOfLanes(numberOfLanes);
        link21.setCapacity(1500);
        link21.setLength(linkLength);
        network.addLink(link21);
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



