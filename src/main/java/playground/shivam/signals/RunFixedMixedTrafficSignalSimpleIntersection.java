package playground.shivam.signals;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.*;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RunFixedMixedTrafficSignalSimpleIntersection {
    private Controler controler;
    static String outputDirectory = "output/RunFixedMixedTrafficSignalSimpleIntersection/";

    private static final double LANE_LENGTH = 250.0;
    private static final int LANE_CAPACITY = 1800;
    private static final int NO_LANES = 1;
    private static final double LINK_LENGTH = 500;

    private static final int LINK_CAPACITY = 1800;
    private int CYCLE = 120;
    private final int ONSET = 0;
    private final int DROPPING = 55;

    public RunFixedMixedTrafficSignalSimpleIntersection() throws IOException {
        final Config config = defineConfig();
        final Scenario scenario = defineScenario(config);

        controler = new Controler(scenario);

        Signals.configure( controler );

    }

    public static void main(String[] args) throws IOException {
        RunFixedMixedTrafficSignalSimpleIntersection fixedMixedTrafficSignalSimpleIntersection = new RunFixedMixedTrafficSignalSimpleIntersection();
        fixedMixedTrafficSignalSimpleIntersection.run(false);
    }

    private void run(boolean startOtfvis) {
        if (startOtfvis) {
            // add the module that start the otfvis visualization with signals
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }
        controler.run();
    }
    private Scenario defineScenario(Config config) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        SignalsData signalsData = SignalUtils.createSignalsData(signalSystemsConfigGroup);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);
//        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        this.createNetwork(scenario);
        this.createPopulation(scenario);
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
        this.createLanes(scenario);

        /* fill the SignalsData object with information:
         * signal systems - specify signalized intersections
         * signal groups - specify signals that always have the same signal control
         * signal control - specify cycle time, onset and dropping time, offset... for all signal groups */
        this.createGroupsAndSystem(signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
        this.createSystemControl(signalsData.getSignalControlData(), Id.create("3", SignalSystem.class),
                ONSET, DROPPING);

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
    private void createSystemControl(SignalControlData control, Id<SignalSystem> signalSystemId,
                                     int onset, int dropping) {
        SignalControlDataFactory fac = control.getFactory();

        // create and add signal control for the given system id
        SignalSystemControllerData controller = fac.createSignalSystemControllerData(signalSystemId);
        control.addSignalSystemControllerData(controller);
        controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);

        // create and add signal plan with defined cycle time and offset 0
        SignalPlanData plan = SignalUtils.createSignalPlan(fac, CYCLE, 0);
        controller.addSignalPlanData(plan);

        // create and add control settings for signal groups
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("1", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("2", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("3", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("4", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("5", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("6", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("7", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("8", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("9", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("10", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("11", SignalGroup.class), onset, dropping));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("12", SignalGroup.class), onset, dropping));
    }


    private void createPopulation(Scenario scenario) {
        Population population = scenario.getPopulation();

        String[] odRelations = {"2_3-3_7", "2_3-3_4", "2_3-3_8", "7_3-3_4", "7_3-3_8", "7_3-3_2",
                                "4_3-3_8", "4_3-3_2", "4_3-3_7", "8_3-3_2", "8_3-3_7", "8_3-3_4", };

        for (String od : odRelations) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];

            for (int i = 0; i < 1800; i++) {
                // create a person
                Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));

                // create a plan for the person that contains all this
                // information
                Plan plan = population.getFactory().createPlan();

                // create a start activity at the from link
                Activity homeAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
                // distribute agents uniformly during one hour.
                homeAct.setEndTime(i);
                plan.addActivity(homeAct);

                // create a dummy leg
                plan.addLeg(population.getFactory().createLeg(getTravelMode(MatsimRandom.getLocalInstance().nextInt(100))));

                // create a work activity at the to link
                Activity workAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
                plan.addActivity(workAct);

                person.addPlan(plan);
                population.addPerson(person);
            }
        }
        new PopulationWriter(population).write(outputDirectory + "population.xml.gz");
    }
    private static String getTravelMode(int number) {
        if (number < 60) return "car";
        else return "truck";
    }

    private Config defineConfig() throws IOException {
        Config config = ConfigUtils.createConfig();

        // create the path to the output directory if it does not exist yet
        Files.createDirectories(Paths.get(outputDirectory));

        config.controler().setOutputDirectory(outputDirectory);
        config.controler().setLastIteration(50);

        config.travelTimeCalculator().setMaxTime(18000);

        config.qsim().setStartTime(0.0D);
        config.qsim().setEndTime(18000.0D);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setNodeOffset(20.0);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        config.qsim().setUseLanes(true);

        PlanCalcScoreConfigGroup.ActivityParams dummyAct = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
        dummyAct.setTypicalDuration(12 * 3600);
        config.planCalcScore().addActivityParams(dummyAct);
        {
            StrategyConfigGroup.StrategySettings strat = new StrategyConfigGroup.StrategySettings();
            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
            strat.setWeight(0.0);
            strat.setDisableAfter(config.controler().getLastIteration());
            config.strategy().addStrategySettings(strat);
        }
        // from Jaipur controller
        PlanCalcScoreConfigGroup.ModeParams car = new PlanCalcScoreConfigGroup.ModeParams("car");
        car.setMarginalUtilityOfTraveling(-6.0);
        car.setConstant(-0.5);
        config.planCalcScore().addModeParams(car);

        PlanCalcScoreConfigGroup.ModeParams truck = new PlanCalcScoreConfigGroup.ModeParams("truck"); // using default for them.
        truck.setConstant(-1.0);
        truck.setMarginalUtilityOfTraveling(-7.0);
        config.planCalcScore().addModeParams(truck);

        QSimConfigGroup qsim = config.qsim();
        List<String> mainModes = Arrays.asList("car", "truck");
        qsim.setMainModes(mainModes);

        ChangeModeConfigGroup changeTripMode = config.changeMode();
        changeTripMode.setModes(new String[]{"car", "truck"});
        config.plansCalcRoute().setNetworkModes(mainModes);

        OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
        otfvisConfig.setDrawTime(true);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());

        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);

//        // set output filenames
//        config.network().setLaneDefinitionsFile(outputDirectory + "lane_definitions_v2.0.xml");

        //write config to file
        String configFile = outputDirectory + "config.xml";
        ConfigWriter configWriter = new ConfigWriter(config);
        configWriter.write(configFile);


        return config;
    }

    private void createGroupsAndSystem(SignalSystemsData signalSystemsData, SignalGroupsData signalGroupsData) {
        SignalSystemData sys = signalSystemsData.getFactory().createSignalSystemData(Id.create("3", SignalSystem.class));
        signalSystemsData.addSignalSystemData(sys);
        SignalSystemsDataFactory factory = signalSystemsData.getFactory();

        SignalUtils.createAndAddSignal(sys, factory, Id.create("1", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("2", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.ol", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("3", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("4", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("5", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.ol", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("6", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("7", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("8", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.ol", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("9", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("10", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("11", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.ol", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("12", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.r", Lane.class)));

        // create a signal group for every signal
        SignalUtils.createAndAddSignalGroups4Signals(signalGroupsData, sys);
    }
    private void createLanes(Scenario scenario) {
        Lanes lanes = scenario.getLanes();
        LanesFactory factory = lanes.getFactory();

        // create lanes for link 2_3
        LanesToLinkAssignment lanesForLink23 = factory
                .createLanesToLinkAssignment(Id.createLinkId("2_3"));
        lanes.addLanesToLinkAssignment(lanesForLink23);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);


        // create lanes for link 7_3
        LanesToLinkAssignment lanesForLink73 = factory
                .createLanesToLinkAssignment(Id.createLinkId("7_3"));
        lanes.addLanesToLinkAssignment(lanesForLink73);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("7_3.l", Lane.class), Id.create("7_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);


        // create lanes for link 4_3
        LanesToLinkAssignment lanesForLink43 = factory
                .createLanesToLinkAssignment(Id.createLinkId("4_3"));
        lanes.addLanesToLinkAssignment(lanesForLink43);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);


        // create lanes for link 8_3
        LanesToLinkAssignment lanesForLink83 = factory
                .createLanesToLinkAssignment(Id.createLinkId("8_3"));
        lanes.addLanesToLinkAssignment(lanesForLink83);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("8_3.l", Lane.class), Id.create("8_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);
    }
    private void createNetwork(Scenario scenario) {
        Network net = scenario.getNetwork();
        NetworkFactory fac = net.getFactory();

        net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
        net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(6), new Coord(0, 2000)));
        net.addNode(fac.createNode(Id.createNodeId(7), new Coord(0, 1000)));
        net.addNode(fac.createNode(Id.createNodeId(8), new Coord(0, -1000)));
        net.addNode(fac.createNode(Id.createNodeId(9), new Coord(0, -2000)));

        String[] links = {"1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4",
                "6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8"};

        for (String linkId : links) {
            String fromNodeId = linkId.split("_")[0];
            String toNodeId = linkId.split("_")[1];
            Link link = fac.createLink(Id.createLinkId(linkId),
                    net.getNodes().get(Id.createNodeId(fromNodeId)),
                    net.getNodes().get(Id.createNodeId(toNodeId)));
            link.setAllowedModes(Set.of("car", "truck"));

            link.setCapacity(LINK_CAPACITY);
            link.setLength(LINK_LENGTH);
            link.setFreespeed(15);
            net.addLink(link);
        }
        new NetworkWriter(net).write(outputDirectory + "network.xml.gz");
    }
}
