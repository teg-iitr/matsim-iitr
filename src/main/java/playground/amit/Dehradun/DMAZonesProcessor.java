package playground.amit.Dehradun;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.metro2021scenario.HaridwarRishikeshScenarioRunner;
import playground.amit.Dehradun.metro2021scenario.MetroShareEstimator;
import playground.amit.utils.FileUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Amit, created on 17-10-2021
 */

public class DMAZonesProcessor {

    private static final String zone_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/zones_update_11092021/zones_updated.shp";
    private final Collection<SimpleFeature> features ;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private static final String dehradunZonesFile = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/dehradunZones.txt";

    private final List<String> dehradunZones = new ArrayList<>();

    private final Geometry excludedGeom ;
    private final List<String> zones_with_forest_areas = List.of("100","120","121","99","123","98","122","25","108","125","131","132","133","134","182");
    private static final String forest_area_shape_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/area_to_be_excluded/haridwar_area_to_exclude.shp";

    public DMAZonesProcessor(){
        HaridwarRishikeshScenarioRunner.LOG.info("reading zone file.");
        this.features = ShapeFileReader.getAllFeatures(zone_file);
        HaridwarRishikeshScenarioRunner.LOG.info("reading forest area shape file.");
        this.excludedGeom = GeometryUtils.getGeometryFromListOfFeatures(ShapeFileReader.getAllFeatures(forest_area_shape_file));
        HaridwarRishikeshScenarioRunner.LOG.info("storing Dehradun zones.");
        storeDehradunZones();
    }

    private void storeDehradunZones() {
        try (BufferedReader reader = IOUtils.getBufferedReader(dehradunZonesFile)){
            String line = reader.readLine();
            while (line!=null){
                this.dehradunZones.add(line.split("\t")[0]);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not store the dehradun zones, reason "+e);
        }
    }

    public List<Coord> getRandomCoords(String zoneId, int numberOfPoints){
        if (zoneId.equals("181")) zoneId ="135"; // cannot distinguish between 181 and 135

        for (SimpleFeature feature : this.features){
            String handle = (String) feature.getAttribute("Zone"); // a unique key
            if (handle.equals(zoneId)){
                if(zones_with_forest_areas.contains(zoneId)) return getRandomPointsInsideFeature_excludedForestArea(feature, numberOfPoints);
                else return getRandomPointsInsideFeature(feature, numberOfPoints);
            }
        }
        throw new RuntimeException("The zone "+zoneId+ " is not found in the provided zone shape file.");
    }

    public List<Coord> getRandomPointsInsideFeature(SimpleFeature feature, int numberOfPoints){
        RandomPointsBuilder rnd = new RandomPointsBuilder(GEOMETRY_FACTORY);
        rnd.setNumPoints(numberOfPoints);
        rnd.setExtent((Geometry) feature.getDefaultGeometry());
        return Arrays.stream(rnd.getGeometry().getCoordinates()).map(MGC::coordinate2Coord).collect(Collectors.toList());
    }

    public List<Coord> getRandomPointsInsideFeature_excludedForestArea(SimpleFeature feature, int numberOfPoints){
        RandomPointsBuilder rnd = new RandomPointsBuilder(GEOMETRY_FACTORY);
        rnd.setNumPoints(numberOfPoints);
        Geometry final_geom = ((Geometry) feature.getDefaultGeometry()).difference(this.excludedGeom);
        rnd.setExtent(final_geom);
        return Arrays.stream(rnd.getGeometry().getCoordinates()).map(MGC::coordinate2Coord).collect(Collectors.toList());
    }

    public List<String> getDehradunZones() {
        return dehradunZones;
    }
}
