package playground.anuj.charDham.network;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import playground.amit.Dehradun.DehradunUtils;

import java.util.*;
import java.util.function.BiPredicate;

import static playground.anuj.charDham.runner.RunCharDhamSingleSimulation.*;

/**
 * This code generates the MATSim network file from a shapefile boundary and an OSM PBF file.
 * It now also embeds a 'nightToll' attribute into each link to facilitate night travel restrictions.
 */
public class CharDhamNetwork {

    public static final String matsimNetworkFile = "output/network_charDham.xml.gz"; // Updated filename
    public static final String boundaryShapeFile = "input/anuj/uttarakhand.shp";
    private static final String inputOSMFile = "input/anuj/uttarakhand_network.osm.pbf";
    public static final String linkAttributesCsvFile = "input/uk_link_attributes.csv";
    // --- NEW: Define the attribute name for the toll ---
    public static final String NIGHT_TOLL_ATTRIBUTE = "nightToll";
    private static final Logger log = LoggerFactory.getLogger(CharDhamNetwork.class);

    public static void main(String[] args) {
        CoordinateTransformation reverse_transformation = TransformationFactory
                .getCoordinateTransformation(DehradunUtils.Dehradun_EPGS, TransformationFactory.WGS84);

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShapeFile);
        Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            if (hierarchyLevel <= 4) return true;
            else return (hierarchyLevel <= 5 && geometry.contains(MGC.coord2Point(reverse_transformation.transform(cord))));
        };
        Set<String> modes = new HashSet<>(Arrays.asList(CAR_MODE, TAXI_MODE, MOTORBIKE_MODE, BUS_MODE, MINI_BUS_MODE));

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
                .setAfterLinkCreated((link, osmTags, isReversed) -> {
                    link.setFreespeed(60.00 / 3.6);
                    link.setCapacity(2500);
                    link.setAllowedModes(modes);
                })
                .build()
                .read(inputOSMFile);

        new NetworkCleaner().run(network);

        //none of the link length should be smaller than Euclidean distance
        for (Link l : network.getLinks().values()) {
            double beelineDist = org.matsim.core.network.NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord());
            if (l.getLength() < beelineDist) {
                l.setLength(Math.ceil(beelineDist));
            }
        }

        System.out.println("Writing network with toll attributes to " + matsimNetworkFile);
        new NetworkWriter(network).write(matsimNetworkFile);
        System.out.println("Done.");
    }
}