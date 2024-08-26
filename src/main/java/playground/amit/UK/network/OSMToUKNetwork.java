package playground.amit.UK.network;

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
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DehradunUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * @author Amit
 * Aug 26, 2024
 */

public class OSMToUKNetwork {

    public static final String matsimNetworkFile = "C:\\Users\\amit2\\Downloads\\UK-ChaarDham\\Uk-road-network.xml.gz";
    public static final String boundaryShapeFile = "C:\\Users\\amit2\\Downloads\\UK-ChaarDham\\uttarakhand-state-boundary.shp";
    //    private static final String dma_boundariesShape = SVN_repo+"atIITR/boundary/OSMB-DMA-Boundary_no-smoothening.shp";
    private static final String inputOSMFile = "C:\\Users\\amit2\\Downloads\\UK-ChaarDham\\planet_77.71,28.923_80.603,30.943.osm.pbf";

    public static void main(String[] args) {
        CoordinateTransformation reverse_transformation = TransformationFactory
                .getCoordinateTransformation(DehradunUtils.Dehradun_EPGS, TransformationFactory.WGS84);

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
                .read(inputOSMFile);

        new NetworkCleaner().run(network);

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
