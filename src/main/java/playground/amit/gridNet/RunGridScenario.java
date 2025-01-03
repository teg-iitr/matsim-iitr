package playground.amit.gridNet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class RunGridScenario {

    public static void main(String[] args) {

        RunGridScenario rgs = new RunGridScenario();
        Config config = rgs.prepareConfig();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        rgs.addVehicleTypes(scenario);

        new ConfigWriter(config).write("output/GridScenarioTrafficChar/grid_config.xml");
        new MatsimVehicleWriter(scenario.getVehicles()).writeFile("output/GridScenarioTrafficChar/grid_vehicles.xml.gz");
//        org.matsim.core.controler.Controler controler = new org.matsim.core.controler.Controler(scenario);
//        controler.run();
    }

    void addVehicleTypes(Scenario scenario){
        Vehicles vehs = scenario.getVehicles();
        {
            VehicleType vt = vehs.getFactory().createVehicleType(Id.create("car", VehicleType.class));
            vt.setMaximumVelocity(60/3.6);
            vt.setPcuEquivalents(1.0);
            vehs.addVehicleType(vt);
        }
        {
            VehicleType vt = vehs.getFactory().createVehicleType(Id.create("motorcycle", VehicleType.class));
            vt.setMaximumVelocity(60/3.6);
            vt.setPcuEquivalents(0.25);
            vehs.addVehicleType(vt);
        }
        {
            VehicleType vt = vehs.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
            vt.setMaximumVelocity(20/3.6);
            vt.setPcuEquivalents(0.25);
            vehs.addVehicleType(vt);
        }
    }

    Config prepareConfig() {

//        String net = "C:\\\\Users\\\\Amit\\\\Google Drive\\\\iitr_gmail_drive\\\\project_data\\\\matsim_grid\\\\gridNet.xml.gz";
//        String plans = "C:\\Users\\Amit Agarwal\\Downloads\\gridNetwork\\input\\plans.xml";

        Collection<String> mainModes = Arrays.asList(TransportMode.car,"bicycle","motorcycle");

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(GridPlans.GRID_PLANS);
        config.network().setInputFile(GridNetwork.NETWORK_FILE);

        config.controller().setOutputDirectory("output/GridScenarioTrafficChar");
        config.controller().setLastIteration(40);
        config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

        config.qsim().setFlowCapFactor(0.05);
        config.qsim().setStorageCapFactor(0.1);
        config.qsim().setMainModes(mainModes);
        config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
        config.qsim().setLinkDynamics(LinkDynamics.PassingQ);

        config.travelTimeCalculator().setSeparateModes(true);

        config.travelTimeCalculator().setAnalyzedModes(new HashSet<>(mainModes));

        config.routing().setNetworkModes(mainModes);
//		config.routing().getOrCreateModeRoutingParams(TransportMode.pt).setBeelineDistanceFactor(1.5);
        config.routing().getOrCreateModeRoutingParams(TransportMode.pt).setTeleportedModeFreespeedFactor(1.8);

        config.routing().removeModeRoutingParams("walk");
//		config.routing().getOrCreateModeRoutingParams(TransportMode.walk).setBeelineDistanceFactor(1.);
        config.routing().getOrCreateModeRoutingParams(TransportMode.walk).setTeleportedModeFreespeedFactor(2.4);

        config.scoring().getOrCreateModeParams(TransportMode.car).setConstant(-1.3);
        config.scoring().getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfTraveling(-6);

        config.scoring().getOrCreateModeParams("bicycle").setConstant(1.6);
        config.scoring().getOrCreateModeParams("bicycle").setMarginalUtilityOfTraveling(-6);

        config.scoring().getOrCreateModeParams("motorcycle").setConstant(-1.3);
        config.scoring().getOrCreateModeParams("motorcycle").setMarginalUtilityOfTraveling(-6);

        config.scoring().getOrCreateModeParams("pt").setConstant(-0.65);

        StrategySettings reRoute = new StrategySettings();
        reRoute.setStrategyName(DefaultStrategy.ReRoute);
        reRoute.setWeight(0.2);
        config.replanning().addStrategySettings(reRoute);

        StrategySettings modeChoice = new StrategySettings();
        modeChoice.setStrategyName(DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.15);
        config.replanning().addStrategySettings(modeChoice);

        ChangeModeConfigGroup changeTripMode = config.changeMode();
        changeTripMode.setModes(new String [] {TransportMode.car,"motorcycle","bicycle"});

        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
        {
            ActivityParams ap = new ActivityParams("home");
            ap.setTypicalDuration(12*3600.);
            config.scoring().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("work");
            ap.setTypicalDuration(8*3600.);
            config.scoring().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("education");
            ap.setTypicalDuration(6*3600.);
            config.scoring().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("leisure");
            ap.setTypicalDuration(3*3600.);
            config.scoring().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("social");
            ap.setTypicalDuration(3*3600.);
            config.scoring().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("shopping");
            ap.setTypicalDuration(1*3600.);
            config.scoring().addActivityParams(ap);
        }
        return config;
    }
}
