package playground.agarwalamit.idling;

/**
 * Created by Amit on 08/09/2020.
 */
public class IdlingUtils {

    public static enum VehicleClass {
        Car, MTW;
    }
    public static final double queueDissipation = 4.167; //TODO magic number

    /**
     *
     * @param vehicle
     * @return fuel consumption in cc
     */
    public static double getFuelConsumption_Reignition(VehicleClass vehicle){
        switch (vehicle) {
            case Car:
                return 1.2;
            case MTW:
                return 0.6; //TODO check the value
        }
        throw new RuntimeException("Vehicle class "+ vehicle+ " is not recognized.");
    }

    /**
     *
     * @param vehicle
     * @return fuel consumption in cc/sec
     */
    public static double getFuelConsumption_Idling(VehicleClass vehicle){
        switch (vehicle) {
            case Car:
                return 0.17;
            case MTW:
                return 0.1; //TODO check the value
        }
        throw new RuntimeException("Vehicle class "+ vehicle+ " is not recognized.");
    }

//    public static double getIdling

}
