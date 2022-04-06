package playground.shivam.trafficChar;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import playground.amit.jaipur.plans.ODMatrixGenerator;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.util.Arrays;
import java.util.Collection;

public class RunTrafficCharScenario {

	public static void main(String[] args) {
		String networkFile = "input/network.xml.gz";
		String plansFile = "input/plans.xml.gz";
		Collection<String> mainModes = Arrays.asList("car", "motorbike", "truck");
		String outputDir = "output/trafficDynamics/";

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
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
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

		TrafficCharConfigGroup trafficCharConfigGroup = new TrafficCharConfigGroup();

		QSimConfigGroup qSimConfigGroupFIFO = new QSimConfigGroup();
		qSimConfigGroupFIFO.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		trafficCharConfigGroup.addQSimConfigGroup("FIFO", qSimConfigGroupFIFO);
		trafficCharConfigGroup.addQSimConfigGroup(TrafficCharConfigGroup.ROAD_TYPE_DEFAULT, config.qsim());
		config.getModules().put(TrafficCharConfigGroup.GROUP_NAME, trafficCharConfigGroup);

		new ConfigWriter(config).write("config.xml");

		ConfigGroup configGroup = config.getModules().get(TrafficCharConfigGroup.GROUP_NAME);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// set roadtypes attributes
		Network network = scenario.getNetwork();

		for (Link link: network.getLinks().values()) {
			if (link.getAttributes().getAttribute("type").equals("primary"))
				link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, "FIFO");
			else
				link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, TrafficCharConfigGroup.ROAD_TYPE_DEFAULT);
		}
		new NetworkWriter(network).write("/Users/shivam4896/Downloads/network.xml");

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
		controler.addOverridingQSimModule(new TrafficCharQSimModule());
		controler.run();
	}
}
