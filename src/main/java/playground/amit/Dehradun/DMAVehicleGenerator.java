package playground.amit.Dehradun;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 *
 * @author Amit
 *
 */
public class DMAVehicleGenerator {

    public static void generateVehicles(Scenario scenario) {

        Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory factory = vehicles.getFactory();

        {
            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModesBaseCase2017.car.name(), VehicleType.class));
            vehicleType.setPcuEquivalents(1.0);
            vehicleType.setMaximumVelocity(80/3.6);
            vehicles.addVehicleType(vehicleType);
        }
//        {
//            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModesBaseCase2017.auto.name(), VehicleType.class));
//            vehicleType.setPcuEquivalents(0.5);
//            vehicleType.setMaximumVelocity(50/3.6);
//            vehicles.addVehicleType(vehicleType);
//        }
        {
            VehicleType vehicleType = factory.createVehicleType(Id.create(DehradunUtils.TravelModesBaseCase2017.motorbike.name(), VehicleType.class));
            vehicleType.setPcuEquivalents(0.15);
            vehicleType.setMaximumVelocity(80/3.6);
            vehicles.addVehicleType(vehicleType);
        }
    }
}
