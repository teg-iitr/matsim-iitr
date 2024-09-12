package playground.shivam.Dadar.evacuation.SafePoints;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import playground.shivam.Dadar.evacuation.DadarUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static playground.shivam.Dadar.evacuation.DadarUtils.*;

public class SafePoints {
    public static void safePoints() {

        Collection<SimpleFeature> safePoints = ShapeFileReader.getAllFeatures(SAFE_POINT_SHAPEFILE);

        safePoints.removeIf(e -> (e.getDefaultGeometry() == null));

        Collection<SimpleFeature> duplicate = new ArrayList<>(safePoints);
        int c = 0;

        for (SimpleFeature simpleFeature : duplicate) {
            safePoints.removeIf(e -> (Objects.equals(e.getID(), simpleFeature.getID())));

            c++;

            if (c == (duplicate.size() - numberOfSafePointsNeeded))
                break;
        }

        String fromSafePointSystem = "EPSG:32643";


        for (SimpleFeature safePoint : safePoints) {
            Geometry safePointDefaultGeometry = (Geometry) safePoint.getDefaultGeometry();

            try {
                Geometry transformedSafePoint = JTS.transform(safePointDefaultGeometry, CRS.findMathTransform(MGC.getCRS(fromSafePointSystem), MGC.getCRS(DadarUtils.Dadar_EPSG), true));
                SAFE_POINTS.put(Id.createLinkId(safeLinkId.toString() + safePoint.getID()), transformedSafePoint);
            } catch (TransformException | FactoryException e) {
                throw new RuntimeException("Transformation isn't successful" + e);
            }
        }
    }
}
