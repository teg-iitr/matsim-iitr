package playground.amit.Chandigarh;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import playground.amit.utils.LoadMyScenarios;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkPrep {

    public static void main (String args []) {
        String matsim_net = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net.xml.gz";
        {
            String osm_netw = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/chandigarh_signalMap_fixed.osm";

            Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

            OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, ChandigarhConstants.CH_EPSG));
            reader.parse(osm_netw);

            new NetworkSimplifier().run(sc.getNetwork());
            new NetworkCleaner().run(sc.getNetwork());

            new NetworkWriter(sc.getNetwork()).write(matsim_net);
        }
        {
            String out_network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone_fixed.xml.gz";

            Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(matsim_net);

            List<String> nodes_to_keep = List.of("1425480847","1425480856", "1425480859", "1425480855",
                    "1428104566","1428104568","1425481090","3896472388","3896472383","3896472381","1428088071","1425481182",
                    "1428088074","1428088073","6329884498","6324797296","1425481032","1428119330","1425481067","6324516528",
                    "6330013306","6324702735","1425481134","6324702738","6329877435","6324702733","4833915521","4833915080",
                    "4833915118","4833915522","4833915083","4833915079","6163999707","5560188517","4833915525","4833915073",
                    "6474868668","4830855727","1425480821","1428119350","1428119365","4830811224","1425481037","1425480713",
                    "6241167504","4833915084","3896472377","-101867","-101866","-101865","-101864","-101863","-101862","-101861",
                    "-101852","-101853","-101854","-101855","-101856","-101857","-101858","-101859");

            List<Link> links_to_remove =
                    sc.getNetwork().getLinks().values().stream().filter(l -> !(
                            nodes_to_keep.contains(((Link) l).getToNode().getId().toString()) ||
                                    nodes_to_keep.contains(((Link) l).getFromNode().getId().toString()) ) ).collect(Collectors.toList());
            links_to_remove.forEach(l-> sc.getNetwork().removeLink(l.getId()));

            // manually remove links
            List<String> remove_links = List.of("990","991","911","912","570","569","4222","3581","1598","1599","4221");

            remove_links.forEach(l -> sc.getNetwork().removeLink(Id.createLinkId(l)));

            // remove with zero in- out-links
            List<Node> nodes_to_remove = sc.getNetwork().getNodes().values().stream().filter(n -> ( ((Node) n).getOutLinks().size()==0 && ((Node) n).getInLinks().size()==0) ).collect(Collectors.toList());
            nodes_to_remove.forEach(n -> sc.getNetwork().removeNode(n.getId()));

            new NetworkWriter(sc.getNetwork()).write(out_network);
        }

    }


}
