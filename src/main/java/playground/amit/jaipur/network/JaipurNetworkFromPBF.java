package playground.amit.jaipur.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.geotools.api.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.jaipur.JaipurUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * @author Amit, created on 09-03-2022
 */

public class JaipurNetworkFromPBF {

    /**
     * following files are available at https://github.com/teg-iitr/jaipur-data
     */
    private static final String boundaryShapeFile = "..\\matsim-Indian-scenarios\\Jaipur\\shapeFile\\Jaipur\\arcGIS\\jaipur_boundary\\District_Boundary.shp";
    private static final String inputPBFFile = "C:\\Users\\Amit\\Documents\\git-repos\\matsim-Indian-scenarios\\Jaipur\\osm\\raw\\planet_75.497,26.699_76.182,27.078.osm.pbf";

    private static final String matsimNetworkFile = "..\\matsim-Indian-scenarios\\Jaipur\\matsimFiles\\jaipur_net_insideDistrictBoundary_09032022.xml.gz";

    public static void main(String[] args) {

        CoordinateTransformation reverse_transformation = TransformationFactory
                .getCoordinateTransformation(JaipurUtils.EPSG, TransformationFactory.WGS84);

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShapeFile);
        Geometry geometry = (Geometry)features.iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            //cord in EPSG:32643; so need to transform
            if (hierarchyLevel<=4) return true; //keep all roads upto level 4.
            else return ( hierarchyLevel<=5 && geometry.contains(MGC.coord2Point(reverse_transformation.transform(cord))) );
        };
        Set<String> modes = new HashSet<>(Arrays.asList(DehradunUtils.TravelModesBaseCase2017.car.name(),
                DehradunUtils.TravelModesBaseCase2017.motorbike.name(), "truck"));

        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(JaipurUtils.transformation)
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

        new NetworkWriter(network).write(matsimNetworkFile);

    }
}
