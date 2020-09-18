package playground.amit.idling;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amit on 08/09/2020.
 */
public class FuelConsumptionValues {

    private final List<IdlingUtils.VehicleClass> vehicleClasses = new ArrayList<>();
    private final List<Double> fuelConsumption_idling = new ArrayList<>();
    private final List<Double> getFuelConsumption_reignition = new ArrayList<>();

    public FuelConsumptionValues(){}

    public void addFuelConsumptionValues(IdlingUtils.VehicleClass vehicleClass, double fuelIdling_reignition, double fuelIdling_idling){
        this.vehicleClasses.add(vehicleClass);
        this.fuelConsumption_idling.add(fuelIdling_idling);
        this.getFuelConsumption_reignition.add(fuelIdling_reignition);
    }

    public double getFuelConsumptionIdling(IdlingUtils.VehicleClass vehicleClass){
        int index = this.vehicleClasses.indexOf(vehicleClass);
        return this.fuelConsumption_idling.get(index);
    }

    public double getFuelConsumptionReignition(IdlingUtils.VehicleClass vehicleClass){
        int index = this.vehicleClasses.indexOf(vehicleClass);
        return this.getFuelConsumption_reignition.get(index);
    }
}
