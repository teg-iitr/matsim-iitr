package playground.amit.Dehradun.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
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
import java.util.function.BiPredicate;

/**
 *
 * @author Amit
 *
 */

public class DMANetworkFromOSM {

    private static final String boundaryShapeFile = "C:/Users/Amit/Downloads/UKMRC_Dehradun_Metropolitan_Area/boundary/single_boundary_DMA.shp";
    private static final String inputPBFFile = "C:/Users/Amit/Downloads/UKMRC_Dehradun_Metropolitan_Area/road-network-osm/planet_77.734,29.841_78.327,30.369.osm.pbf";
    private static final String matsimNetworkFile = "C:/Users/Amit/Downloads/UKMRC_Dehradun_Metropolitan_Area/road-network-osm/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned.xml.gz";

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

        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(DehradunUtils.transformation)
                .setIncludeLinkAtCoordWithHierarchy(includeLinkAtCoordWithHierarchy)
                .setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.bike, "motorbike"))))
                .build()
                .read(inputPBFFile);

        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(matsimNetworkFile);
    }
}
