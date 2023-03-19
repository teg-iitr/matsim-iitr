package playground.shivam.Dadar.evacuation.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import playground.shivam.Dadar.evacuation.DadarUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

import static playground.shivam.Dadar.evacuation.DadarUtils.*;

public class CreateNetworkFromOSM {
    /**
     * Creates a Network from OSM
     *
     * @return path of MATSIm network
     */
    public static void createDadarNetworkFromOSM() {
        String inputOSMFile = INPUT_FILES_PATH + "dadar.osm.pbf";

        Geometry wholeGeometry = (Geometry) ShapeFileReader.getAllFeatures(BOUNDARY_SHAPEFILE).iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            if (hierarchyLevel <= 4)
                return true; //keep all roads upto level 4.
            else
                return (hierarchyLevel <= 5 && wholeGeometry.contains(MGC.coord2Point(DadarUtils.TRANSFORMATION_FROM_WSG_84.transform(cord))));
        };
        Set<String> modes = new HashSet<>(MAIN_MODES);

        Network network = (new SupersonicOsmNetworkReader.Builder())
                .setCoordinateTransformation(DadarUtils.TRANSFORMATION_FROM_WSG_84)
//                .addOverridingLinkProperties(OsmTags.MOTORWAY, new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 2, 120.0 / 3.6, 2000, true))
//                .addOverridingLinkProperties(OsmTags.MOTORWAY_LINK, new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 2, 120.0 / 3.6, 1800, true))
//                .addOverridingLinkProperties(OsmTags.TRUNK, new LinkProperties(LinkProperties.LEVEL_TRUNK, 2, 120.0 / 3.6, 2000, false))
//                .addOverridingLinkProperties(OsmTags.TRUNK_LINK, new LinkProperties(LinkProperties.LEVEL_TRUNK, 2, 80.0 / 3.6, 1800, false))
//                .addOverridingLinkProperties(OsmTags.PRIMARY, new LinkProperties(LinkProperties.LEVEL_PRIMARY, 2, 80.0 / 3.6, 1800, false))
//                .addOverridingLinkProperties(OsmTags.PRIMARY_LINK, new LinkProperties(LinkProperties.LEVEL_PRIMARY, 2, 80.0 / 3.6, 1800, false))
//                .addOverridingLinkProperties(OsmTags.SECONDARY, new LinkProperties(LinkProperties.LEVEL_SECONDARY, 2, 4, 800, false))
//                .addOverridingLinkProperties(OsmTags.SECONDARY_LINK, new LinkProperties(LinkProperties.LEVEL_SECONDARY, 2, 4, 800, false))
//                .addOverridingLinkProperties(OsmTags.TERTIARY, new LinkProperties(LinkProperties.LEVEL_TERTIARY, 2, 4, 600, false))
//                .addOverridingLinkProperties(OsmTags.TERTIARY_LINK, new LinkProperties(LinkProperties.LEVEL_TERTIARY, 2, 4, 600, false))
                .setIncludeLinkAtCoordWithHierarchy(includeLinkAtCoordWithHierarchy)
                .setAfterLinkCreated((link, osmTags, isReversed) -> {
                    link.setAllowedModes(modes);
                })
                .build()
                .read(inputOSMFile);
        // adding the missingLink
        /*{
            Node n1 = network.getNodes().get(Id.createNodeId("6165604798"));
            Node n2 = network.getNodes().get(Id.createNodeId("7371092120"));
            Link link = network.getFactory().createLink(Id.createLinkId("missingLink"), n1, n2);
            link.setCapacity(1500.0);
            link.setFreespeed(22.22222222222222);
            link.setAllowedModes(modes);
            link.setLength(1000);
            network.addLink(link);
        }*/
        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(MATSIM_NETWORK);
    }
}
