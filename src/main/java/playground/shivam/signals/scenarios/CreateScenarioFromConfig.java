package playground.shivam.signals.scenarios;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesDataFactory;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesWriter10;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;

import java.util.Arrays;
import java.util.List;

import static playground.shivam.signals.lanes.CreateLanes.createLanes;
import static playground.shivam.signals.network.CreateNetwork.createNetwork;
import static playground.shivam.signals.population.CreatePopulation.createPopulation;
import static playground.shivam.signals.signalSystems.CreateGroupsAndSystem.createGroupsAndSystem;
import static playground.shivam.signals.signalSystems.CreateSignalControl.createSystemControl;

public class CreateScenarioFromConfig {
    public static Scenario defineScenario(Config config, String outputDirectory, String signalController) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        signalSystemsConfigGroup.setUseAmbertimes(true);
        //signalSystemsConfigGroup.setUseIntergreenTimes(true);
        signalSystemsConfigGroup.setActionOnIntergreenViolation(SignalSystemsConfigGroup.ActionOnSignalSpecsViolation.WARN);

        SignalsData signalsData = SignalUtils.createSignalsData(signalSystemsConfigGroup);

        AmberTimesData amberTimesData = signalsData.getAmberTimesData();
        amberTimesData.setDefaultAmber(5);
        amberTimesData.setDefaultAmberTimeGreen(2.0);
        AmberTimesDataFactory amberTimesDataFactory = amberTimesData.getFactory();
        AmberTimesWriter10 amberTimesWriter10 = new AmberTimesWriter10(amberTimesData);
        amberTimesWriter10.write(outputDirectory + "amber_time.xml.gz");
        signalSystemsConfigGroup.setAmberTimesFile(outputDirectory + "amber_time.xml.gz");

        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);
        //scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        createNetwork(scenario, outputDirectory);
        createPopulation(scenario, outputDirectory);
        Vehicles vehicles = scenario.getVehicles();

        List<String> mainModes = Arrays.asList("car", "truck");
        for (String mode : mainModes) {
            VehicleType veh = VehicleUtils.createVehicleType(Id.create(mode, VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getStuckTime(mode));
            veh.setNetworkMode(mode);
            vehicles.addVehicleType(veh);
        }
        // create lanes for the scenario
        createLanes(scenario);

        /* fill the SignalsData object with information:
         * signal systems - specify signalized intersections
         * signal groups - specify signals that always have the same signal control
         * signal control - specify cycle time, onset and dropping time, offset... for all signal groups */
        createGroupsAndSystem(signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
        createSystemControl(signalsData.getSignalControlData(), Id.create("3", SignalSystem.class), signalController);

        // set output files
        scenario.getConfig().network().setLaneDefinitionsFile(outputDirectory + "lane_definitions_v2.0.xml");
        signalSystemsConfigGroup.setSignalSystemFile(outputDirectory + "signal_systems.xml");
        signalSystemsConfigGroup.setSignalGroupsFile(outputDirectory + "signal_groups.xml");
        signalSystemsConfigGroup.setSignalControlFile(outputDirectory + "signal_control.xml");

        // write lanes to file
        LanesWriter writerDelegate = new LanesWriter(scenario.getLanes());
        writerDelegate.write(config.network().getLaneDefinitionsFile());

        // write signal information to file
        SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
        signalsWriter.setSignalSystemsOutputFilename(signalSystemsConfigGroup.getSignalSystemFile());
        signalsWriter.setSignalGroupsOutputFilename(signalSystemsConfigGroup.getSignalGroupsFile());
        signalsWriter.setSignalControlOutputFilename(signalSystemsConfigGroup.getSignalControlFile());
       signalsWriter.writeSignalsData(scenario);

        return scenario;
    }

}
