package playground.nidhi.examJan.ques4;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class NetworkIITR {
    public static final String IITRCoordinate  = "EPSG:32643";
    private static final String inputOSMFile = "C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr.osm";
    private static final String outputNetworkFile = "C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_matsim_network.xml.gz";

    public static void main(String[] args) {
        Config config= ConfigUtils.createConfig();
        Scenario scenario= ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        CoordinateTransformation transformation= TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84 , IITRCoordinate);

        OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network,transformation);
        osmNetworkReader.parse(inputOSMFile);

        new NetworkWriter(network).write(outputNetworkFile);
    }
}
