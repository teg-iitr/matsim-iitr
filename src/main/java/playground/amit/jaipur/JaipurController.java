package playground.amit.jaipur;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import playground.amit.jaipur.plans.ODMatrixGenerator;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Amit, created on 09-03-2022
 */

public class JaipurController {

    public static void main(String[] args) {

//        String configFile = "";
//        Config config = ConfigUtils.loadConfig(configFile);

        String networkFile = "C:\\Users\\Amit\\Documents\\git-repos\\matsim-Indian-scenarios\\Jaipur\\matsimFiles\\jaipur_net_insideDistrictBoundary_09032022.xml.gz";
        String plansFile = "C:\\Users\\Amit\\Downloads\\matsim-example-scenario\\input\\plans.xml.gz";
        Collection<String> mainModes = Arrays.asList("car","motorbike","truck");
        String outputDir = "C:/Users/Amit/Downloads/matsim-example-scenario/output/";

        Config config = ConfigUtils.createConfig();

        config.network().setInputFile(networkFile);
        config.plans().setInputFile(plansFile);

        //
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ); // overtaking

        config.qsim().setFlowCapFactor(1.0);
        config.qsim().setStorageCapFactor(1.0);

        config.qsim().setMainModes(mainModes);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setEndTime(30*3600.); //

        config.routing().setNetworkModes(mainModes);

        ScoringConfigGroup.ActivityParams homeAct = new ScoringConfigGroup.ActivityParams(ODMatrixGenerator.ORIGIN_ACTIVITY);
        homeAct.setTypicalDuration(11*3600.);
        homeAct.setScoringThisActivityAtAll(false);
        config.scoring().addActivityParams(homeAct);

        ScoringConfigGroup.ActivityParams destinationAct = new ScoringConfigGroup.ActivityParams(ODMatrixGenerator.DESTINATION_ACTIVITY);;
        destinationAct.setTypicalDuration(10*3600.);
        destinationAct.setScoringThisActivityAtAll(false);
        config.scoring().addActivityParams(destinationAct);

        config.scoring().setPerforming_utils_hr(0.);

        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams("car");
        carParams.setMarginalUtilityOfTraveling(-6.0);
        carParams.setConstant(-0.5);
        config.scoring().addModeParams(carParams);

        ScoringConfigGroup.ModeParams motorbikeParams = new ScoringConfigGroup.ModeParams("motorbike");
        motorbikeParams.setConstant(0.0);
        motorbikeParams.setMarginalUtilityOfTraveling(-5.0);
        config.scoring().addModeParams(motorbikeParams);

        ScoringConfigGroup.ModeParams truckParams = new ScoringConfigGroup.ModeParams("truck");
        truckParams.setConstant(-1.0);
        truckParams.setMarginalUtilityOfTraveling(-7.0);
        config.scoring().addModeParams(truckParams);

        config.controller().setOutputDirectory(outputDir);
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(100);
        config.controller().setDumpDataAtEnd(true);
        config.controller().setCreateGraphs(true);
        config.controller().setWriteEventsInterval(50);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
//        config.controller().setRunId();

        ReplanningConfigGroup.StrategySettings reRoute = new ReplanningConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.30);
        config.replanning().addStrategySettings(reRoute);

        ReplanningConfigGroup.StrategySettings planSelector = new ReplanningConfigGroup.StrategySettings();
        planSelector.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta);
        planSelector.setWeight(0.7);
        config.replanning().addStrategySettings(planSelector);

        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);

        new ConfigWriter(config).write("config.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        VehicleType car = scenario.getVehicles().getFactory().createVehicleType(Id.create("car",VehicleType.class));
        car.setPcuEquivalents(1.0);
        car.setMaximumVelocity(100.0/3.6);
        scenario.getVehicles().addVehicleType(car);

        VehicleType motorbike = scenario.getVehicles().getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
        motorbike.setMaximumVelocity(80.0/3.6);
        motorbike.setPcuEquivalents(0.25);
        scenario.getVehicles().addVehicleType(motorbike);

        VehicleType truck = scenario.getVehicles().getFactory().createVehicleType(Id.create("truck", VehicleType.class));
        truck.setMaximumVelocity(80.0/3.6);
        truck.setPcuEquivalents(3.0);
        scenario.getVehicles().addVehicleType(truck);

        Controler controler = new Controler(scenario);
        controler.run();

    }

}
