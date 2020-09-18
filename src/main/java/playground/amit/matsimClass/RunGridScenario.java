package playground.amit.matsimClass;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class RunGridScenario {

    public static void main(String[] args) {

        RunGridScenario rgs = new RunGridScenario();
        Config config = rgs.prepareConfig();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        rgs.addVehicleTypes(scenario);

        new ConfigWriter(config).write("C:\\Users\\Amit\\Google Drive\\iitr_gmail_drive\\project_data\\matsim_grid\\grid_config.xml");
        new MatsimVehicleWriter(scenario.getVehicles()).writeFile("C:\\Users\\Amit\\Google Drive\\iitr_gmail_drive\\project_data\\matsim_grid\\grid_vehicles.xml.gz");
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

        String net = "C:\\\\Users\\\\Amit\\\\Google Drive\\\\iitr_gmail_drive\\\\project_data\\\\matsim_grid\\\\gridNet.xml.gz";
//        String plans = "C:\\Users\\Amit Agarwal\\Downloads\\gridNetwork\\input\\plans.xml";

        Collection<String> mainModes = Arrays.asList(TransportMode.car,"bicycle","motorcycle");

        Config config = ConfigUtils.createConfig();
//        config.plans().setInputFile(plans);
        config.network().setInputFile(net);

        config.controler().setOutputDirectory("C:/Users/Amit Agarwal/Downloads/gridNetwork/output/");
        config.controler().setLastIteration(40);
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

        config.qsim().setFlowCapFactor(0.05);
        config.qsim().setStorageCapFactor(0.1);
        config.qsim().setMainModes(mainModes);
        config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
        config.qsim().setLinkDynamics(LinkDynamics.PassingQ);

        config.travelTimeCalculator().setSeparateModes(true);

        config.travelTimeCalculator().setAnalyzedModes(new HashSet<>(mainModes));

        config.plansCalcRoute().setNetworkModes(mainModes);
//		config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.pt).setBeelineDistanceFactor(1.5);
        config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.pt).setTeleportedModeFreespeedFactor(1.8);

        config.plansCalcRoute().removeModeRoutingParams("walk");
//		config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.walk).setBeelineDistanceFactor(1.);
        config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.walk).setTeleportedModeFreespeedFactor(2.4);

        config.planCalcScore().getOrCreateModeParams(TransportMode.car).setConstant(-1.3);
        config.planCalcScore().getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfTraveling(-6);

        config.planCalcScore().getOrCreateModeParams("bicycle").setConstant(1.6);
        config.planCalcScore().getOrCreateModeParams("bicycle").setMarginalUtilityOfTraveling(-6);

        config.planCalcScore().getOrCreateModeParams("motorcycle").setConstant(-1.3);
        config.planCalcScore().getOrCreateModeParams("motorcycle").setMarginalUtilityOfTraveling(-6);

        config.planCalcScore().getOrCreateModeParams("pt").setConstant(-0.65);

        StrategySettings reRoute = new StrategySettings();
        reRoute.setStrategyName(DefaultStrategy.ReRoute);
        reRoute.setWeight(0.2);
        config.strategy().addStrategySettings(reRoute);

        StrategySettings modeChoice = new StrategySettings();
        modeChoice.setStrategyName(DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.15);
        config.strategy().addStrategySettings(modeChoice);

        ChangeModeConfigGroup changeTripMode = config.changeMode();
        changeTripMode.setModes(new String [] {TransportMode.car,TransportMode.walk,TransportMode.pt,"motorcycle","bicycle"});

        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        {
            ActivityParams ap = new ActivityParams("home");
            ap.setTypicalDuration(12*3600.);
            config.planCalcScore().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("work");
            ap.setTypicalDuration(9*3600.);
            config.planCalcScore().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("education");
            ap.setTypicalDuration(6*3600.);
            config.planCalcScore().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("leisure");
            ap.setTypicalDuration(4*3600.);
            config.planCalcScore().addActivityParams(ap);
        }
        {
            ActivityParams ap = new ActivityParams("other");
            ap.setTypicalDuration(6*3600.);
            config.planCalcScore().addActivityParams(ap);
        }
        return config;
    }
}
