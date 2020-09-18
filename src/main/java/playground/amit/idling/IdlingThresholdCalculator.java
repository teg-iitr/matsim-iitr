package playground.amit.idling;

/**
 * Created by Amit on 08/09/2020.
 */
public class IdlingThresholdCalculator {

    private final FuelConsumptionValues fuelConsumptionValues;

    public IdlingThresholdCalculator(FuelConsumptionValues fuelConsumptionValues) {
        this.fuelConsumptionValues = fuelConsumptionValues;
    }

    public IdlingThresholdCalculator(){
        fuelConsumptionValues = new FuelConsumptionValues();
        for (IdlingUtils.VehicleClass vehicleClass :  IdlingUtils.VehicleClass.values()) {
            fuelConsumptionValues.addFuelConsumptionValues(vehicleClass,
                    IdlingUtils.getFuelConsumption_Idling(vehicleClass),
                    IdlingUtils.getFuelConsumption_Reignition(vehicleClass));
        }
    }

    public double getThresholdDuration(IdlingUtils.VehicleClass vehicleClass, double lengthQueue, double speedOfQueueDissipation){
        return Math.max(0, this.fuelConsumptionValues.getFuelConsumptionReignition(vehicleClass)/
                this.fuelConsumptionValues.getFuelConsumptionIdling(vehicleClass) - lengthQueue/speedOfQueueDissipation);
    }

    public double getWastedFuel(double thresholdValue, double timeEngineOff, IdlingUtils.VehicleClass vehicleClass) {
        double idlingDuration = timeEngineOff - thresholdValue;

        if(idlingDuration > 0 ) return idlingDuration * this.fuelConsumptionValues.getFuelConsumptionIdling(vehicleClass);
        else { // engine-off which should not be the case; thus reignition will consume additional fuel than saved in idling
            return idlingDuration * this.fuelConsumptionValues.getFuelConsumptionIdling(vehicleClass) + this.fuelConsumptionValues.getFuelConsumptionReignition(vehicleClass);
        }
    }

}
