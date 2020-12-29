package playground.nidhi.practice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.amit.Chandigarh.ChandigarhConstants;

public class OverpassBusStop {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String matsim_net = "C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\Overpass_BusStop_matsim_net.xml.gz";
		{
            String osm_netw = "C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\Overpass_BusStop.osm";

            Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

            OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, ChandigarhConstants.CH_EPSG));
            reader.parse(osm_netw);

            new NetworkSimplifier().run(sc.getNetwork());
            new NetworkCleaner().run(sc.getNetwork());

            new NetworkWriter(sc.getNetwork()).write(matsim_net);
	}
	}

}
