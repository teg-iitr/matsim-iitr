package playground.anuj.locationChoice.toyScenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Collections;

public class CreateNetwork {

    private static final long CAPACITY = 100; // [veh/h]
    private static final double LINK_SPEED = 10.0; // [m/s]

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        NetworkFactory networkFactory = network.getFactory();

        // Creating nodes
        Node node1 = networkFactory.createNode(Id.createNodeId("1"), new Coord(0, 0));
        Node node2 = networkFactory.createNode(Id.createNodeId("2"), new Coord(1000, 0));
        Node node3 = networkFactory.createNode(Id.createNodeId("3"), new Coord(2000, 0));
        Node node4 = networkFactory.createNode(Id.createNodeId("4"), new Coord(0, 1000));
        Node node5 = networkFactory.createNode(Id.createNodeId("5"), new Coord(1000, 4000));
        Node node6 = networkFactory.createNode(Id.createNodeId("6"), new Coord(2000, 1000));
        Node node7 = networkFactory.createNode(Id.createNodeId("7"), new Coord(3000, 1000));

        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);
        network.addNode(node5);
        network.addNode(node6);
        network.addNode(node7);

        // Creating bidirectional links
        createBidirectionalLink(networkFactory, network, "1_2", "2_1", node1, node2, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "2_3", "3_2", node2, node3, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "1_4", "4_1", node1, node4, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "4_5", "5_4", node4, node5, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "5_6", "6_5", node5, node6, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "2_5", "5_2", node2, node5, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "3_6", "6_3", node3, node6, CAPACITY, LINK_SPEED, 1);
        createBidirectionalLink(networkFactory, network, "3_7", "7_3", node3, node7, CAPACITY, LINK_SPEED, 1);
        // Write network file
        new NetworkWriter(network).write("DestinationChoiceTest/input/network.xml");
        System.out.println("Network file written successfully!");
    }

    private static void createBidirectionalLink(NetworkFactory factory, Network network, String linkId,
                                                String reverseLinkId, Node fromNode, Node toNode,
                                                double capacity, double freespeed, int permlanes) {
        Link link = factory.createLink(Id.createLinkId(linkId), fromNode, toNode);
        setLinkAttributes(link, capacity, freespeed, permlanes);
        network.addLink(link);

        Link reverseLink = factory.createLink(Id.createLinkId(reverseLinkId), toNode, fromNode);
        setLinkAttributes(reverseLink, capacity, freespeed, permlanes);
        network.addLink(reverseLink);
    }

    private static void setLinkAttributes(Link link, double capacity, double freespeed, int permlanes) {
        link.setCapacity(capacity);
        link.setFreespeed(freespeed);
        link.setNumberOfLanes(permlanes);
        link.setAllowedModes(Collections.singleton("car")); // Ensure only car mode is used
    }
}