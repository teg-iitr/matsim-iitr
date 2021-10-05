package playground.amit.Dehradun.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.utils.NetworkUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 *
 * @author Amit
 *
 */

public class DMANetworkFromOSM {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String matsimNetworkFile = SVN_repo + "atIITR/matsim/road-network-osm/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned_20092021.xml.gz";
    private static final String boundaryShapeFile = SVN_repo+"atIITR/boundary/single_boundary_DMA.shp";
//    private static final String dma_boundariesShape = SVN_repo+"atIITR/boundary/OSMB-DMA-Boundary_no-smoothening.shp";
    private static final String inputPBFFile = SVN_repo+"atIITR/matsim/road-network-osm/planet_77.734,29.841_78.327,30.369.osm.pbf";

    public static void main(String[] args) {
        CoordinateTransformation reverse_transformation = TransformationFactory
                .getCoordinateTransformation(DehradunUtils.EPSG, TransformationFactory.WGS84);

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShapeFile);
        Geometry geometry = (Geometry)features.iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            //cord in EPSG:32644; so need to transform
            if (hierarchyLevel<=4) return true; //keep all roads upto level 4.
            else return ( hierarchyLevel<=5 && geometry.contains(MGC.coord2Point(reverse_transformation.transform(cord))) );
        };
        Set<String> modes = new HashSet<>(Arrays.asList(DehradunUtils.TravelModesBaseCase2017.car.name(),
                DehradunUtils.TravelModesBaseCase2017.motorbike.name()));

        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(DehradunUtils.transformation)
                .addOverridingLinkProperties(OsmTags.MOTORWAY, new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 2, 120.0 / 3.6, 2000, true))
                .addOverridingLinkProperties(OsmTags.MOTORWAY_LINK, new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 1, 120.0 / 3.6, 1800, true))
                .addOverridingLinkProperties(OsmTags.TRUNK, new LinkProperties(LinkProperties.LEVEL_TRUNK, 1, 120.0 / 3.6, 2000, false))
                .addOverridingLinkProperties(OsmTags.TRUNK_LINK, new LinkProperties(LinkProperties.LEVEL_TRUNK, 1, 80.0 / 3.6, 1800, false))
                .addOverridingLinkProperties(OsmTags.PRIMARY, new LinkProperties(LinkProperties.LEVEL_PRIMARY, 1, 80.0 / 3.6, 1800, false))
                .addOverridingLinkProperties(OsmTags.PRIMARY_LINK, new LinkProperties(LinkProperties.LEVEL_PRIMARY, 1, 80.0 / 3.6, 1800, false))
                .addOverridingLinkProperties(OsmTags.SECONDARY, new LinkProperties(LinkProperties.LEVEL_SECONDARY, 1, 80.0 / 3.6, 1500, false))
                .addOverridingLinkProperties(OsmTags.SECONDARY_LINK, new LinkProperties(LinkProperties.LEVEL_SECONDARY, 1, 80.0 / 3.6, 1500, false))
                .addOverridingLinkProperties(OsmTags.TERTIARY, new LinkProperties(LinkProperties.LEVEL_TERTIARY, 1, 80.0 / 3.6, 1200, false))
                .addOverridingLinkProperties(OsmTags.TERTIARY_LINK, new LinkProperties(LinkProperties.LEVEL_TERTIARY, 1, 80.0 / 3.6, 1200, false))
                .setIncludeLinkAtCoordWithHierarchy(includeLinkAtCoordWithHierarchy)
                .setAfterLinkCreated((link, osmTags, isReversed)-> {
                    link.setFreespeed(80.00/3.6);
                    link.setAllowedModes(modes);
                })
                .build()
                .read(inputPBFFile);

        new NetworkCleaner().run(network);

        // some corrections based on experience in the simulations by NK, Sep 21
        {// there is no link in the opposite direction of 7266273210051f (fromNode = 681085758; toNode = 6810857603
            Link link = network.getLinks().get(Id.createLinkId("7266273210051f"));
            Link reverseLink = network.getFactory().createLink(Id.createLinkId("7266273210051r"), link.getToNode(), link.getFromNode());
            NetworkUtils.copy(link, reverseLink);
            network.addLink(reverseLink);
        }
        {// a bridge links are missing connecting roads (1071588440020r, 1071588430005r) and opposite direction
            Node from = network.getLinks().get(Id.createLinkId("1071588440020r")).getToNode();
            Node to = network.getLinks().get(Id.createLinkId("1071588430005r")).getFromNode();
            Link link = network.getFactory().createLink(Id.createLinkId("bridge_10715884_f"), from, to);
            link.setLength(50.0);
            link.setFreespeed(80/3600.);
            link.setCapacity(600.);
            link.setAllowedModes(modes);
            link.setNumberOfLanes(1);
            network.addLink(link);

            Link reverseLink = network.getFactory().createLink(Id.createLinkId("bridge_10715884_r"), to, from);
            NetworkUtils.copy(link, reverseLink);
            network.addLink(reverseLink);
        }
        {// the roads 1052863010008f and 1052863010008r must go via node 2358611670
            Node n = network.getNodes().get(Id.createNodeId("2358611670"));
            {
                // first split 1052863010008f (260 m and  remaining)
                Link existingLink = network.getLinks().get(Id.createLinkId("1052863010008f"));

                Node old_from_node = existingLink.getFromNode();
                double old_length = existingLink.getLength();
                existingLink.setFromNode(n);
                existingLink.setLength(260.);

                Link newLink = network.getFactory().createLink(Id.createLinkId("1052863010008f_split"), old_from_node, n);
                NetworkUtils.copy(existingLink, newLink);
                newLink.setLength(old_length - existingLink.getLength());
                network.addLink(newLink);
            }
            {
                // now split 1052863010008r (260 m and  remaining)
                Link existingLink = network.getLinks().get(Id.createLinkId("1052863010008r"));
                Node old_to_node = existingLink.getToNode();
                double old_length = existingLink.getLength();
                existingLink.setToNode(n);
                existingLink.setLength(260.);

                Link newLink = network.getFactory().createLink(Id.createLinkId("1052863010008r_split"), n, old_to_node);
                NetworkUtils.copy(existingLink, newLink);
                newLink.setLength(old_length - existingLink.getLength());
                network.addLink(newLink);
            }
        }
        {// road 2271164390005f and in reverse direction need to be removed since there is a dead-end in the end.
            network.removeLink(Id.createLinkId("2271164390005f"));
            network.removeLink(Id.createLinkId("2271164390005r"));
        }
        {// road 1818202350014f and in reverse direction need to be removed since there is a dead-end in the end.
            network.removeLink(Id.createLinkId("1818202350014f"));
            network.removeLink(Id.createLinkId("1818202350014r"));
        }
        {// missing connection between nodes 1217127858 and 448467317
            Node f = network.getNodes().get(Id.createNodeId("1217127858"));
            Node t = network.getNodes().get(Id.createNodeId("448467317"));
            Link l = network.getFactory().createLink(Id.createLinkId("1217127858-448467317"),f,t);
            l.setLength(50.);
            l.setCapacity(600.);
            l.setAllowedModes(modes);
            l.setFreespeed(80/3.6);
            l.setNumberOfLanes(1);
            network.addLink(l);

            Link r = network.getFactory().createLink(Id.createLinkId("448467317-1217127858"),t,f);
            NetworkUtils.copy(l,r);
            network.addLink(r);
        }
        {// the roads 4985360820017f and 5274335800006f must be connected
            Node existingNode = network.getNodes().get(Id.createNodeId("3633950979"));
            Node newNode = network.getFactory().createNode(Id.createNodeId("close_to_3633950979"), new Coord(215208,3358613));
            network.addNode(newNode);
            {
                // first split 4985360820017f (90 m and  remaining)
                Link existingLink = network.getLinks().get(Id.createLinkId("4985360820017f"));
                Node old_from_node = existingLink.getFromNode();
                double old_length = existingLink.getLength();
                existingLink.setFromNode(newNode);
                existingLink.setLength(old_length-90.);

                Link newLink = network.getFactory().createLink(Id.createLinkId("4985360820017f_split"), old_from_node, newNode);
                NetworkUtils.copy(existingLink, newLink);
                newLink.setLength(90.0);
                network.addLink(newLink);

                Link crossLink = network.getFactory().createLink(Id.createLinkId("4985360820017f_cross"),newNode, existingNode);
                crossLink.setAllowedModes(modes);
                crossLink.setNumberOfLanes(1);
                crossLink.setFreespeed(80/3.6);
                crossLink.setLength(55.0);
                crossLink.setCapacity(600.);
                network.addLink(crossLink);
            }
            {
                // now split 7266055660026f (90 m and  remaining)
                Link existingLink = network.getLinks().get(Id.createLinkId("7266055660026f"));
                Node old_to_node = existingLink.getToNode();
                double old_length = existingLink.getLength();
                existingLink.setToNode(newNode);
                existingLink.setLength(old_length-90.);

                Link newLink = network.getFactory().createLink(Id.createLinkId("7266055660026f_split"), newNode, old_to_node);
                NetworkUtils.copy(existingLink, newLink);
                newLink.setLength(90.0);
                network.addLink(newLink);

                Link crossLink = network.getFactory().createLink(Id.createLinkId("7266055660026f_cross"), existingNode, newNode);
                crossLink.setAllowedModes(modes);
                crossLink.setNumberOfLanes(1);
                crossLink.setFreespeed(80/3.6);
                crossLink.setLength(55.0);
                crossLink.setCapacity(600.);
                network.addLink(crossLink);
            }
        }

        //allow autos in Dehradun only
//        Collection<SimpleFeature> features_boundaries = ShapeFileReader.getAllFeatures(dma_boundariesShape);
//        Geometry dehradunGeom = null;
//        for (SimpleFeature feature: features_boundaries) {
//            if (feature.getAttribute("name").equals("Dehradun")){
//                dehradunGeom = (Geometry) feature.getDefaultGeometry();
//            }
//        }
//
//        if (dehradunGeom==null) throw new RuntimeException("Dehradun Geometry should not be null. Check the CRS.");

//        Set<String> d_modes = modes;
//        d_modes.add(DehradunUtils.TravelModesBaseCase2017.auto.name());
//        for(Link link : network.getLinks().values()){
//            if (GeometryUtils.isLinkInsideGeometry(dehradunGeom, link)){
//                link.setAllowedModes(d_modes);
//            }
//        }

        //none of the link length should be smaller than Euclidean distance
        for (Link l : network.getLinks().values()) {
            double beelineDist = org.matsim.core.network.NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord());
            if (l.getLength() < beelineDist) {
                l.setLength(Math.ceil(beelineDist));
            }
        }

        new NetworkWriter(network).write(matsimNetworkFile);
    }


}
