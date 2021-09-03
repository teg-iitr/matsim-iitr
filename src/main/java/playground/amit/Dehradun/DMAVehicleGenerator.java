package playground.amit.Dehradun;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class DMAVehicleGenerator {

    public static void generateVehicles(Scenario scenario) {

        Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory factory = vehicles.getFactory();

        {
            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModes.car.name(), VehicleType.class));
            vehicleType.setPcuEquivalents(1.0);
            vehicleType.setMaximumVelocity(80/3.6);
            vehicles.addVehicleType(vehicleType);
        }
        {
            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModes.bicycle.name(), VehicleType.class));
            vehicleType.setPcuEquivalents(0.15);
            vehicleType.setMaximumVelocity(20/3.6);
            vehicles.addVehicleType(vehicleType);
        }
        {
            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModes.auto.name(), VehicleType.class));
            vehicleType.setPcuEquivalents(0.8);
            vehicleType.setMaximumVelocity(40/3.6);
            vehicles.addVehicleType(vehicleType);
        }
        {
            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModes.motorbike.name(), VehicleType.class));
            vehicleType.setPcuEquivalents(0.2);
            vehicleType.setMaximumVelocity(80/3.6);
            vehicles.addVehicleType(vehicleType);
        }
    }
}
