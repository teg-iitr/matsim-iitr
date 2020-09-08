package playground.agarwalamit.idling;

/**
 * Created by Amit on 08/09/2020.
 */
public class IdlingThresholdCalculator {

    public static double getThresholdDuration(IdlingUtils.VehicleClass vehicleClass, double lengthQueue, double speedOfQueueDissipation){
        return Math.max(0, IdlingUtils.getFuelConsumption_Reignition(vehicleClass)/ IdlingUtils.getFuelConsumption_Idling(vehicleClass) - lengthQueue/speedOfQueueDissipation);
    }

    public static double getWastedFule(double thresholdValue, double timeEngineOff, IdlingUtils.VehicleClass vehicleClass) {
        double idlingDuration = timeEngineOff - thresholdValue;

        if(idlingDuration > 0 ) return idlingDuration * IdlingUtils.getFuelConsumption_Idling(vehicleClass);
        else {
            return idlingDuration * IdlingUtils.getFuelConsumption_Idling(vehicleClass) + IdlingUtils.getFuelConsumption_Reignition(vehicleClass);
        }
    }

}
