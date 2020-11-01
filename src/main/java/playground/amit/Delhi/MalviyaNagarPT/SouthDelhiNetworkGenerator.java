package playground.amit.Delhi.MalviyaNagarPT;

import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.amit.utils.FileUtils;

public class SouthDelhiNetworkGenerator {
//		 private static final String roadShapeFile = "C:\\Users\\Nidhi\\Desktop\\MalviyaNagar_PT\\Files\\planet_SouthDelhi_shp\\shape\\roads.shp";
		 private static final String inputPBFFile = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/planet_77.183,28.513_77.247,28.55.osm.pbf";
		 private static final String matsimNetworkFile =  FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/south_delhi_matsim_network.xml.gz";

		    public static void main(String[] args) {
		        CoordinateTransformation transformation = TransformationFactory
		                .getCoordinateTransformation(TransformationFactory.WGS84, MN_TransitDemandGenerator.toCoordinateSystem);
		        CoordinateTransformation reverse_transformation = TransformationFactory
		                .getCoordinateTransformation(MN_TransitDemandGenerator.toCoordinateSystem, TransformationFactory.WGS84);

//		        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(roadShapeFile);
//		        Geometry geometry = GeometryUtils.getGeometryFromListOfFeatures(features); // --> WGS:84

//		        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
//		            //cord in EPSG:32643
//		            if (hierarchyLevel<=4) return true; //keep all roads upto level 4.
//		            else return ( hierarchyLevel<=6 && geometry.contains(MGC.coord2Point(reverse_transformation.transform(cord))) );
//		        };

		        Network network = new SupersonicOsmNetworkReader.Builder()
		                .setCoordinateTransformation(transformation)
//		                .setIncludeLinkAtCoordWithHierarchy(includeLinkAtCoordWithHierarchy)   // since for south delhi, no need to filter the network
		                .build()
		                .read(Paths.get(inputPBFFile));
				new NetworkCleaner().run(network);
		        new NetworkWriter(network).write(matsimNetworkFile);

	}

}
