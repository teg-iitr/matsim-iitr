package playground.amit.Dehradun.metro2021scenario;

import playground.amit.Dehradun.DehradunUtils.TravelModesBaseCase2017;

/**
 * @author Amit
 */
public class UtilityComputation {

    public static double getUtilExceptMetro(TravelModesBaseCase2017 mode, double distance_km, double travelTime_h){
        double util = 0.;

        //TODO: the ASCs must be changed.
        switch (mode){
            case car:
                util = 0.0 - 95.18 * travelTime_h - 9.09 * distance_km;
                break;
            case motorbike:
                util = 0.0 - 53.11 * travelTime_h - 12.38 * distance_km;
                break;
            case bicycle:
                util = 0.0 - 95.18 * travelTime_h - 9.09 * distance_km;
                break;
            case walk:
                util = 0.0 - 95.18 * travelTime_h - 9.09 * distance_km;
                break;
            case bus:
                util = 0.0 - 44.14 * travelTime_h - 18.37 * distance_km;
                break;
            case IPT:
                util = 0.0 - 38.50 * travelTime_h - 16.30 * distance_km;
                break;
            default:
                throw new IllegalStateException("Unknown mode: " + mode);
        }
        return util;
    }

    public static double getUtilMetroWithoutASC(double distance_km, double travelTime_h){
        return - 45.27 * travelTime_h - 5.12 * distance_km;
    }
}
