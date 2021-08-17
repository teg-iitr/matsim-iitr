package playground.amit.facultyHousingIITR;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * All roads are two links, speed is 40 kph.
 *
 * Created by Amit on 10/01/2021
 */
public class FHNetwork {
    private final Network network;
    private final NetworkFactory networkFactory ;

    public FHNetwork(){
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();
        networkFactory = network.getFactory();
    }

    public void generateNetwork() {

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, FHConstants.IITR_EPSG);

        //create nodes
        Node oth_circ = networkFactory.createNode(Id.createNodeId("oth_Gate"), ct.transform(new Coord(77.899703, 29.868290)));
        Node vn_gate = networkFactory.createNode(Id.createNodeId("VN_Gate"), ct.transform(new Coord(77.899476, 29.869158)));
        createAndAddLink(vn_gate,oth_circ);




    }

    public void createAndAddLink(Node from, Node to) {
        double length = NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord());
        Link up = networkFactory.createLink(Id.createLinkId(from.getId().toString()+"_"+to.getId().toString()),from, to);
        up.setNumberOfLanes(1);
        up.setFreespeed(40/3.6);
        up.setCapacity(1500);
        up.setLength(length);

        Link down = networkFactory.createLink(Id.createLinkId(to.getId().toString()+"_"+from.getId().toString()),to, from);
        down.setNumberOfLanes(1);
        down.setFreespeed(40/3.6);
        down.setCapacity(1500);
        down.setLength(length);


    }

}
