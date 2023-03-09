package playground.shivam.Dadar.evacuation;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class DadarUtils {

    public static final String Dadar_EPSG = "EPSG:7767"; // same is used for Mumbai.

    public static final CoordinateTransformation reverse_transformationWGS84 = TransformationFactory
            .getCoordinateTransformation(Dadar_EPSG, TransformationFactory.WGS84);

    public static final CoordinateTransformation transformationFromWSG84 = TransformationFactory
            .getCoordinateTransformation(TransformationFactory.WGS84, Dadar_EPSG);

    public enum DadarTrafficCountMode2023 {
        motorbike, car, auto,  bus, lcv, truck, bicycle, cart;
    }
}
