/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.amit.utils.networkProcessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.geotools.api.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author amit
 */
public class EdgeNodeShapesToNetworkIndia {

	private final static String NETWORK_NODES_SHAPE_FILE = "C:/Users/amit2/Downloads/road_india/nodes.shp";
	private final static String NETWORK_EDGES_SHAPE_FILE = "C:/Users/amit2/Downloads/road_india/edges.shp";
	private final static String MATSIM_NETWORK = "C:/Users/amit2/Downloads/road_india/MATSim_net_clean.xml.gz";
	private final static CoordinateTransformation CT =TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:24378");
	private static final Map<String, Id<Node>> osmId2NodeId = new HashMap<>();
	private static final Logger log = LogManager.getLogger(EdgeNodeShapesToNetworkIndia.class);

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();

		Collection<SimpleFeature> features_nodes = new ShapeFileReader().readFileAndInitialize(NETWORK_NODES_SHAPE_FILE);
		String identifier = "osmid";

		for (SimpleFeature node: features_nodes){
			if(node.getFeatureType() instanceof SimpleFeatureType){
				Double fromNodeX=	(Double) node.getAttribute("lon");
				Double fromNodeY = (Double) node.getAttribute("lat");
				Coord fromCoord = new Coord(fromNodeX, fromNodeY);

				String osmId = String.valueOf(node.getAttribute(identifier));
				Id<Node> fromNodeId;
				if(!osmId2NodeId.containsKey(osmId)) {
					fromNodeId = Id.create(osmId,Node.class);
					osmId2NodeId.put(osmId,fromNodeId);
				} else {
					log.warn("Duplicate OSM ID: "+ osmId);
					fromNodeId = osmId2NodeId.get(osmId);
				}

				if(!network.getNodes().containsKey((fromNodeId))){
					Node node1 = NetworkUtils.createAndAddNode(network, fromNodeId, CT.transform(fromCoord));
					node1.getAttributes().putAttribute(identifier,osmId);
				}else{
					// do nothing
				}
			}
		}

		Collection<SimpleFeature> features_links = new ShapeFileReader().readFileAndInitialize(NETWORK_EDGES_SHAPE_FILE);

		for(SimpleFeature sf : features_links){
			// reads every feature here (corresponding to every line in attribute table)
			if(sf.getFeatureType() instanceof SimpleFeatureType){
				// get from Node
				String fromNode= String.valueOf(sf.getAttribute("from"));
				Id<Node> fNode = osmId2NodeId.get(fromNode);

				String toNode= String.valueOf(sf.getAttribute("to"));
				Id<Node> tNode = osmId2NodeId.get(toNode);

				if (fNode == null || tNode == null) {
					throw new RuntimeException("Either of from or to node OSM ID does not exist in the nodes shape file.");
				}

				// matsim have one way links thus create two links for both directions
				Id<Link> linkId1 = Id.create(fNode.toString()+"_"+tNode.toString(),Link.class);
				Id<Link> linkId2 =Id.create(tNode.toString()+"_"+fNode.toString(),Link.class);

				// following parameters are necessary for simulation, I just used some data to show how it works.
				double linkLength = (Double) sf.getAttribute("length");
				double capacity = 1800;// can be attributed to road type
				double numberOfLanes = 2;
				double freeSpeed = 60/3.6;

				// add links to network
				if (!network.getLinks().containsKey(linkId1)) {
					final Id<Link> id = linkId1;
					final Node f = network.getNodes().get(fNode);
					final Node t = network.getNodes().get(tNode);
					NetworkUtils.createAndAddLink(network, id, f, t, linkLength, freeSpeed, capacity, 2);
				}

				if (!network.getLinks().containsKey(linkId2)) {
					final Id<Link> id = linkId2;
					final Node f = network.getNodes().get(tNode);
					final Node t = network.getNodes().get(fNode);
					NetworkUtils.createAndAddLink(network,id, f, t, linkLength, freeSpeed, capacity, 2 );
				}
			}
		}
		// write network to a file
		new NetworkCleaner().run(network);
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(MATSIM_NETWORK);
	}
}