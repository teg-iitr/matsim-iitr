package playground.amit.Delhi.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.jaipur.JaipurUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.BiPredicate;

public class DelhiNetworkFromPBF {

    private static final String boundaryShapeFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/data/shapeFiles/Delhi/datameet/Delhi_Boundary-SHP/Delhi_Boundary.shp";
    private static final String inputPBFFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/data/Delhi_Trilokpuri/matsimFiles/";
    private static final String matsimNetworkFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/data/Delhi_Trilokpuri/matsimFiles/Delhi_matsim_network_fromPBF_insideDelhiZone.xml.gz";

    public static void main(String[] args) {
        CoordinateTransformation transformation = TransformationFactory
                .getCoordinateTransformation(TransformationFactory.WGS84, JaipurUtils.EPSG);
        CoordinateTransformation reverse_transformation = TransformationFactory
                .getCoordinateTransformation(JaipurUtils.EPSG, TransformationFactory.WGS84);

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShapeFile);
        Geometry geometry = GeometryUtils.getGeometryFromListOfFeatures(features); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            //cord in EPSG:32643; so need to transform
            if (hierarchyLevel<=4) return true; //keep all roads upto level 4.
            else return ( hierarchyLevel<=6 && geometry.contains(MGC.coord2Point(reverse_transformation.transform(cord))) );
        };

        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(transformation)
                .setIncludeLinkAtCoordWithHierarchy(includeLinkAtCoordWithHierarchy)
                .build()
                .read(Paths.get(inputPBFFile).resolve("planet_76.76_28.298_f7cf0a91.osm.pbf"));

        new NetworkWriter(network).write(matsimNetworkFile);
    }
}
