package playground.nidhi.examJan.ques4;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class VehiclesIITR {
    public static void main(String[] args) {
        Config config= ConfigUtils.createConfig();
        Scenario scenario= ScenarioUtils.loadScenario(config);
        Vehicles vehs=scenario.getVehicles();

        VehicleType car=vehs.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
        car.setMaximumVelocity(40/3.6);
        car.setPcuEquivalents(1.0);
        vehs.addVehicleType(car);

        new MatsimVehicleWriter(vehs).writeFile("C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_vehicle.xml.gz");

    }
}
