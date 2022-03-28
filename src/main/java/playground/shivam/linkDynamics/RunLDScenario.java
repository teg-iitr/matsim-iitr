package playground.shivam.linkDynamics;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import playground.amit.jaipur.plans.ODMatrixGenerator;
import playground.shivam.linkDynamics.core.LDConfigGroup;

import java.util.Arrays;
import java.util.Collection;

public class RunLDScenario {

	public static void main(String[] args) {
		String networkFile = "/Users/shivam4896/Downloads/network.xml.gz";
		String plansFile = "/Users/shivam4896/Downloads/plans.xml.gz";
		Collection<String> mainModes = Arrays.asList("car", "motorbike", "truck");
		String outputDir = "/Users/shivam4896/Documents/GitHub/matsim-iitr/output/linkDynamics/";

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
		config.qsim().setEndTime(30 * 3600.); //

		config.plansCalcRoute().setNetworkModes(mainModes);

		PlanCalcScoreConfigGroup.ActivityParams homeAct = new PlanCalcScoreConfigGroup.ActivityParams(ODMatrixGenerator.ORIGIN_ACTIVITY);
		homeAct.setTypicalDuration(11 * 3600.);
		homeAct.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(homeAct);

		PlanCalcScoreConfigGroup.ActivityParams destinationAct = new PlanCalcScoreConfigGroup.ActivityParams(ODMatrixGenerator.DESTINATION_ACTIVITY);
		;
		destinationAct.setTypicalDuration(10 * 3600.);
		destinationAct.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(destinationAct);

		config.planCalcScore().setPerforming_utils_hr(0.);

		PlanCalcScoreConfigGroup.ModeParams carParams = new PlanCalcScoreConfigGroup.ModeParams("car");
		carParams.setMarginalUtilityOfTraveling(-6.0);
		carParams.setConstant(-0.5);
		config.planCalcScore().addModeParams(carParams);

		PlanCalcScoreConfigGroup.ModeParams motorbikeParams = new PlanCalcScoreConfigGroup.ModeParams("motorbike");
		motorbikeParams.setConstant(0.0);
		motorbikeParams.setMarginalUtilityOfTraveling(-5.0);
		config.planCalcScore().addModeParams(motorbikeParams);

		PlanCalcScoreConfigGroup.ModeParams truckParams = new PlanCalcScoreConfigGroup.ModeParams("truck");
		truckParams.setConstant(-1.0);
		truckParams.setMarginalUtilityOfTraveling(-7.0);
		config.planCalcScore().addModeParams(truckParams);

		config.controler().setOutputDirectory(outputDir);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setDumpDataAtEnd(true);
		config.controler().setCreateGraphs(true);
		config.controler().setWriteEventsInterval(50);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
//        config.controler().setRunId();

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
		reRoute.setWeight(0.30);
		config.strategy().addStrategySettings(reRoute);

		StrategyConfigGroup.StrategySettings planSelector = new StrategyConfigGroup.StrategySettings();
		planSelector.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta);
		planSelector.setWeight(0.7);
		config.strategy().addStrategySettings(planSelector);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		LDConfigGroup ldConfigGroup = new LDConfigGroup();

		ldConfigGroup.setLinkDynamics1(LDConfigGroup.LinkDynamics.PassingQ);
		ldConfigGroup.setLinkDynamics2(LDConfigGroup.LinkDynamics.FIFO);
		ldConfigGroup.setTrafficDynamics1(LDConfigGroup.TrafficDynamics.withHoles);
		ldConfigGroup.setTrafficDynamics2(LDConfigGroup.TrafficDynamics.queue);
		config.getModules().put(LDConfigGroup.GROUP_NAME, ldConfigGroup);

		new ConfigWriter(config).write("config.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleType car = scenario.getVehicles().getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setPcuEquivalents(1.0);
		car.setMaximumVelocity(100.0 / 3.6);
		scenario.getVehicles().addVehicleType(car);

		VehicleType motorbike = scenario.getVehicles().getFactory().createVehicleType(Id.create("motorbike", VehicleType.class));
		motorbike.setMaximumVelocity(80.0 / 3.6);
		motorbike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(motorbike);

		VehicleType truck = scenario.getVehicles().getFactory().createVehicleType(Id.create("truck", VehicleType.class));
		truck.setMaximumVelocity(80.0 / 3.6);
		truck.setPcuEquivalents(3.0);
		scenario.getVehicles().addVehicleType(truck);

		Controler controler = new Controler(scenario);
		controler.run();
	}
}
