package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Amit on 28/11/2020
 */
public class PTLinesAsNetworkGenerator {

    private final String coordinatesFile = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/PT_stops_coordinates_links.csv";
    private static final String outNet = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/PTLines_as_matsimPlainNetwork.xml.gz";
    private final Network network;
    private final NetworkFactory networkFactory;

    PTLinesAsNetworkGenerator(Network  network) {
        this.network = network;
        this.networkFactory = this.network.getFactory();
    }

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        PTLinesAsNetworkGenerator ptLinesAsNetworkGenerator = new PTLinesAsNetworkGenerator(network);
        ptLinesAsNetworkGenerator.addNodes();
        ptLinesAsNetworkGenerator.addLinks();

        new NetworkWriter(network).write(outNet);

    }

    private void addLinks(){
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

    private void createLink(String fromNodeId, String toNodeId){
        Node n1 = network.getNodes().get(Id.createNodeId(fromNodeId));
        Node n22 = network.getNodes().get(Id.createNodeId(toNodeId));

        Link link = networkFactory.createLink(Id.createLinkId(n1.getId()+"_"+n22.getId()), n1, n22);
        link.setLength(NetworkUtils.getEuclideanDistance(n1.getCoord(), n22.getCoord()));
        link.setCapacity(1000.);
        link.setNumberOfLanes(1);

        network.addLink(link);
    }

    private void addNodes(){
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
                   Node node = networkFactory.createNode(Id.createNodeId(index), transformCoord);
                   network.addNode(node);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }
}
