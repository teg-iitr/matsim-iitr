package playground.agarwalamit.Delhi.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.jaipur.JaipurUtils;
import playground.agarwalamit.utils.geometry.GeometryUtils;

import java.util.Collection;

public class DelhiRoadNetworkFromOSM {

    private static final String boundaryShapeFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/data/shapeFiles/Delhi/datameet/Delhi_Boundary-SHP/Delhi_Boundary.shp";
    private static final String inputOSMFile = "C:/Users/Amit Agarwal/Downloads/Delhi_OSM/Delhi_OSM-all_map.osm";
    private static final String matsimNetworkFile = "C:/Users/Amit Agarwal/Downloads/Delhi_OSM/Delhi_net_insideDistrictBoundary.xml.gz";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network network = scenario.getNetwork();

        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32643");

        //TODO use bicycle OSM reader instead.
        OsmNetworkReader reader = new OsmNetworkReader(network, transformation);
        //filter is creating some issues. Need to check.
//        reader.addOsmFilter(new DelhiOSMFilter(boundaryShapeFile));
        reader.parse(inputOSMFile);

        new NetworkWriter(network).write(matsimNetworkFile);
    }

    static class DelhiOSMFilter implements OsmNetworkReader.OsmFilter {

        private final Geometry geometry;

        DelhiOSMFilter(String shapeFile){
            Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
            geometry = GeometryUtils.getGeometryFromListOfFeatures(features);
        }

        @Override
        public boolean coordInFilter(Coord coord, int hierarchyLevel) {
            if (hierarchyLevel<=4) return true; //keep all roads upto level 4.
            else return ( geometry.contains(MGC.coord2Point(coord)) && hierarchyLevel<=6 );
        }

    }

}
