package playground.amit.Dehradun;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import playground.amit.utils.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author Amit
 *
 */

public class ZoneCentroids {

    private static final String zone_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/zones_update_29082021/zone_data_update.shp";
    private static final String zone_centroid_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/flowMapVisualization/zone_cendtroid_details.txt";
    private static final CoordinateTransformation transformation = TransformationFactory
            .getCoordinateTransformation(DehradunUtils.Dehradun_EPGS,TransformationFactory.WGS84);

    public static void main(String[] args) {

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zone_file);

        try(BufferedWriter writer = IOUtils.getBufferedWriter(zone_centroid_file)) {
            writer.write("id\tname\tlat\tlon\n");
            for(SimpleFeature feature : features) {
                Point point = ((Geometry) feature.getDefaultGeometry()).getCentroid();
                Coord coord = new Coord(point.getX(), point.getY());
                Coord t_coord = transformation.transform(coord);
                String handle = (String) feature.getAttribute("Zone"); // a unique key

                writer.write(handle+"\t"+"zone_"+handle+"\t"+t_coord.getY()+"\t"+t_coord.getX()+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file. Reason "+ e);
        }
    }
}
