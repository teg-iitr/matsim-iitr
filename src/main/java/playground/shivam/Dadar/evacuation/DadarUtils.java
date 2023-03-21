package playground.shivam.Dadar.evacuation;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DadarUtils {
    public static final String INPUT_FILES_PATH = "input/evacDadar/";
    public static final String OUTPUT_EVACUATION = "output/evacDadar/";
    public static final String OUTPUT_SINGLE_MODE = "output/dadarSingleMode";
    public static final String OUTPUT_All_MODES = "output/dadarAllModes";
    public static final String OUTPUT_WO_TELEPORTED_MODES = "output/dadarWOTeleportedModes";
    public static final Map<Id<Link>, Geometry> SAFE_POINTS = new HashMap<>();

    public static final String BOUNDARY_SHAPEFILE = INPUT_FILES_PATH + "boundaryDadar.shp";
    public static final String EVACUATION_ZONES_SHAPEFILE = INPUT_FILES_PATH + "evacuationZones.shp";
    public static final String ZONES_SHAPEFILE = INPUT_FILES_PATH + "zonesDadar.shp";
    //    public static final String boundaryShapeFile = "input/evacDadar/boundaryDadar.shp";
    public static final String ORIGIN_ACTIVITY = "origin";
    public static final String DESTINATION_ACTIVITY = "destination";
    public static final String MATSIM_NETWORK = INPUT_FILES_PATH + "dadar-network_smaller.xml.gz";
    public static final String EVACUATION_NETWORK = INPUT_FILES_PATH + "dadar_evac_network.xml.gz";

    public static final String SAFE_POINT_SHAPEFILE = INPUT_FILES_PATH + "dadarSafePoints.shp";
    public static final String OD_MATRIX = INPUT_FILES_PATH + "dadar_od_10_10_22.csv";

    public static final String modeShareFilePath = INPUT_FILES_PATH + "dadar_mode_share/";
    public static final String MATSIM_COUNTS = INPUT_FILES_PATH + "dadar-counts.xml.gz";
    public static final String PSEUDO_COUNTS_FROM_STATION = INPUT_FILES_PATH + "pseudoCounts.txt";

    public static final String MATSIM_PLANS = INPUT_FILES_PATH + "dadar-plans.xml.gz";
    public static final String MATSIM_SAMPLE_PLANS = INPUT_FILES_PATH + "plans_edited.xml";
    public static final String EVACUATION_PLANS = INPUT_FILES_PATH + "dadar_evac_plans.xml.gz";

    public static final String Dadar_EPSG = "EPSG:7767"; // same is used for Mumbai.
    public static  int numberOfSafePointsNeeded = 2;
    public static int ITERATIONS = 50;
    public static final Id<Link> safeLinkId = Id.createLinkId("safeLink_Dadar");

    public static final CoordinateTransformation REVERSE_TRANSFORMATION_WGS_84 = TransformationFactory
            .getCoordinateTransformation(Dadar_EPSG, TransformationFactory.WGS84);

    public static final CoordinateTransformation TRANSFORMATION_FROM_WSG_84 = TransformationFactory
            .getCoordinateTransformation(TransformationFactory.WGS84, Dadar_EPSG);

    public static final Collection<String> ALL_MODES =  Arrays.asList("motorbike", "car", "auto", "pt");
    public static final Collection<String> MAIN_MODES = Arrays.asList("motorbike", "car","auto");
    public static final Collection<String> TELEPORTED_MODES = Arrays.asList("pt",  "walk");
    public static final String SINGLE_MODE = TransportMode.car;

    public static double setMarginalUtilityOfTraveling(String mode) {
        double marginalUtilityOfTraveling;
        switch (mode) {
            case TransportMode.car:
                marginalUtilityOfTraveling = 95.18;
                marginalUtilityOfTraveling = 0;
                break;
            case "motorbike":
                marginalUtilityOfTraveling = 53.17;
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
                marginalUtilityOfDistance = 9.09;
                break;
            case "motorbike":
                marginalUtilityOfDistance = 12.38;
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
        motorbike, car, auto, pt;
    }
    public enum DadarUserGroup {
        urban, commuter, through
    }
    public static double setConstant(final String travelMode) {
        double constant;
        switch (travelMode) {
            case TransportMode.car:
                constant = -10250;
                break;
            case "motorbike":
                constant = 6335;
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
