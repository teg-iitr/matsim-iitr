package playground.shivam.Dadar.evacuation;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Arrays;
import java.util.Collection;

public class DadarUtils {

    public static final String Dadar_EPSG = "EPSG:7767"; // same is used for Mumbai.

    public static final CoordinateTransformation REVERSE_TRANSFORMATION_WGS_84 = TransformationFactory
            .getCoordinateTransformation(Dadar_EPSG, TransformationFactory.WGS84);

    public static final CoordinateTransformation TRANSFORMATION_FROM_WSG_84 = TransformationFactory
            .getCoordinateTransformation(TransformationFactory.WGS84, Dadar_EPSG);

    public static final Collection<String> ALL_MAIN_MODES =  Arrays.asList("motorbike", "car", "auto", "pt", "bicycle", "walk");
    public static final Collection<String> MAIN_MODES = Arrays.asList("motorbike", "car", "bicycle");
    public static final Collection<String> TELEPORTED_MODES = Arrays.asList("pt", "auto", "walk");

    public static double setMarginalUtilityOfTraveling(String mode) {
        double marginalUtilityOfTraveling;
        switch (mode) {
            case TransportMode.car:
                marginalUtilityOfTraveling = 95.18;
                break;
            case "bicycle":
            case "cycle":
            case TransportMode.bike:
                marginalUtilityOfTraveling = 20;
                break;
            case "motorbike":
                marginalUtilityOfTraveling = 53.17;
                break;
            case TransportMode.walk:
                marginalUtilityOfTraveling = 0;
                break;
            case "lcv":
                marginalUtilityOfTraveling = 0;
                break;
            case "bus":
                marginalUtilityOfTraveling = 44.14;
                break;
            case "cart":
                marginalUtilityOfTraveling = 0;
                break;
            case "truck":
                marginalUtilityOfTraveling = 0;
                break;
            case "auto":
                marginalUtilityOfTraveling = 38.5;
                break;
            case TransportMode.pt:
                marginalUtilityOfTraveling = 45.27;
                break;
            default:
                throw new RuntimeException("No marginal utility of traveling is set for travel mode " + mode + ".");
        }
        return marginalUtilityOfTraveling;
    }

    public static double setMarginalUtilityOfDistance(String mode) {
        double marginalUtilityOfDistance;
        switch (mode) {
            case TransportMode.car:
                marginalUtilityOfDistance = 9.09;
                break;
            case "bicycle":
            case "cycle":
            case TransportMode.bike:
                marginalUtilityOfDistance = 5;
                break;
            case "motorbike":
                marginalUtilityOfDistance = 12.38;
                break;
            case TransportMode.walk:
                marginalUtilityOfDistance = 0.002;
                break;
            case "lcv":
                marginalUtilityOfDistance = 0;
                break;
            case "bus":
                marginalUtilityOfDistance = 18.37;
                break;
            case "cart":
                marginalUtilityOfDistance = 0;
                break;
            case "truck":
                marginalUtilityOfDistance = 0;
                break;
            case "auto":
                marginalUtilityOfDistance = 16.3;
                break;
            case TransportMode.pt:
                marginalUtilityOfDistance = 5.12;
                break;
            default:
                throw new RuntimeException("No marginal utility of traveling is set for travel mode " + mode + ".");
        }
        return marginalUtilityOfDistance;
    }

    public enum MumbaiModeShareSplit2014 {
        motorbike, car, auto,  bus, pt, bicycle, truck, lcv, cart;
    }
    public enum DadarUserGroup {
        urban, commuter, through
    }
    public static double setConstant(final String travelMode) {
        double constant;
        switch (travelMode) {
            case TransportMode.car:
                constant = 0;
                break;
            case "bicycle":
            case "cycle":
            case TransportMode.bike:
                constant = 0;
                break;
            case "motorbike":
                constant = 0;
                break;
            case TransportMode.walk:
                constant = 0;
                break;
            case "lcv":
                constant = 0;
                break;
            case "bus":
                constant = 0;
                break;
            case "cart":
                constant = 0;
                break;
            case "truck":
                constant = 0;
                break;
            case "auto":
                constant = 0;
                break;
            case TransportMode.pt:
                constant = 0;
                break;
            default:
                throw new RuntimeException("No constant is set for travel mode " + travelMode + ".");
        }
        return constant;
    }
}
