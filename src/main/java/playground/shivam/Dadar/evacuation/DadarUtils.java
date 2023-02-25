package playground.shivam.Dadar.evacuation;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.amit.jaipur.JaipurUtils;

public class DadarUtils {

    public static final String Dadar_EPSG = "EPSG:7767"; // same is used for Mumbai.

    public static final CoordinateTransformation reverse_transformation = TransformationFactory
            .getCoordinateTransformation(Dadar_EPSG, TransformationFactory.WGS84);

    public static final CoordinateTransformation transformation = TransformationFactory
            .getCoordinateTransformation(TransformationFactory.WGS84, Dadar_EPSG);

    public enum DadarTrafficCountMode2023 {
        motorbike, bus, car, lcv, truck, cycle, cart;
    }
}
