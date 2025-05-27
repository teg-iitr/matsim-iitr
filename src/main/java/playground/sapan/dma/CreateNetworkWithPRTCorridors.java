package playground.sapan.dma;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Sapan and Amit
 *
 */

public class CreateNetworkWithPRTCorridors {

    static class PRTStop {
        String stopId;
        String stopName;
        double lat;
        double lon;
        int sequence;

        public PRTStop(String stopId, String stopName, double lat, double lon, int sequence) {
            this.stopId = stopId;
            this.stopName = stopName;
            this.lat = lat;
            this.lon = lon;
            this.sequence = sequence;
        }
    }

    static class PRTCorridor{
        String id;
        List<PRTStop> stopList = new ArrayList<>();

        PRTCorridor(String id){
            this.id = id;
        }

    }

    public static void main(String[] args) throws IOException {
        String inputNetworkFile = "./input/haridwarPRT/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned_20092021.xml"; // Load the corrected network with bicycle on link
        String excelFilePath = "./input/haridwarPRT/proposed_stops.csv";
        String outputNetworkFile = "./input/haridwarPRT/network_combined.xml";

        // Load base network
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(inputNetworkFile);

        // Read PRT stops from Excel
        List<PRTStop> prtStops = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(excelFilePath))) {
            String line;
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String corridor = parts[0];
                String stopId = parts[1];
                String stopName = parts[2];
                double lat = Double.parseDouble(parts[3]);
                double lon = Double.parseDouble(parts[4]);
                int seq = Integer.parseInt(parts[5]);

                prtStops.add(new PRTStop(stopId, stopName, lat, lon, seq));
            }
        }

        // Sort stops by sequence
        prtStops.sort(Comparator.comparingInt(s -> s.sequence));

        // Convert lat/lon to EPSG:32644, which is the crs input network
        CoordinateTransformation transformation =
                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32644");


        NetworkFactory factory = network.getFactory();
        List<Id<Node>> nodeIds = new ArrayList<>();

        for (int i = 0; i < prtStops.size(); i++) {
            PRTStop stop = prtStops.get(i);
            Coord transformedCoord = transformation.transform(new Coord(stop.lon, stop.lat));
            Id<Node> nodeId = Id.createNodeId("prt_node_" + stop.stopId);
            Node node = factory.createNode(nodeId, transformedCoord);
            network.addNode(node);
            nodeIds.add(nodeId);
        }

        for (int i = 0; i < nodeIds.size() - 1; i++) {
            Id<Node> fromId = nodeIds.get(i);
            Id<Node> toId = nodeIds.get(i + 1);
            Node fromNode = network.getNodes().get(fromId);
            Node toNode = network.getNodes().get(toId);

            Id<Link> linkId = Id.createLinkId("prt_link_" + fromId.toString() + "_" + toId.toString());
            Link link = factory.createLink(linkId, fromNode, toNode);
            link.setAllowedModes(Set.of("prt"));
            link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
            link.setFreespeed(15.0);
            link.setCapacity(9999);
            network.addLink(link);
        }


        new NetworkWriter(network).write(outputNetworkFile);
        System.out.println("PRT network successfully added to car network and written to: " + outputNetworkFile);
    }
}
