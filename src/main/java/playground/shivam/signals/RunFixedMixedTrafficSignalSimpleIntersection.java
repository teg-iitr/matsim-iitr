package playground.shivam.signals;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.MixedTrafficDelayAnalysisTool;
import org.matsim.contrib.signals.analysis.MixedTrafficSignalAnalysisTool;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.MixedTrafficLaemmerSignalController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.*;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.*;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RunFixedMixedTrafficSignalSimpleIntersection {
    private Controler controler;
    static String outputDirectory = "output/RunFixedMixedTrafficSignalSimpleIntersection/";

    private static final double LANE_LENGTH = 500;
    private static final int LANE_CAPACITY = 7200;
    private static final int NO_LANES = 1;
    private static final double LINK_LENGTH = 1000;

    private static final int LINK_CAPACITY = 1000;
    private int CYCLE = 120;
    private final int ONSET = 0;
    private final int DROPPING = 60;

    private final int AGENTS_PER_LEFT_APPROACH = 200;
    // seconds
    private final int OFFSET_LEFT_APPROACH = 60;
    private final int DROPPING_LEFT_APPROACH = 80;
    private final int AGENTS_PER_TOP_APPROACH = 800;
    private final int OFFSET_TOP_APPROACH = 20;
    private final int DROPPING_TOP_APPROACH = 100;
    private final int AGENTS_PER_RIGHT_APPROACH = 400;
    private final int OFFSET_RIGHT_APPROACH = 60;
    private final int DROPPING_RIGHT_APPROACH = 100;
    private final int AGENTS_PER_BOTTOM_APPROACH = 600;
    private final int OFFSET_BOTTOM_APPROACH = 10;
    private final int DROPPING_BOTTOM_APPROACH = 70;

    public RunFixedMixedTrafficSignalSimpleIntersection() throws IOException {
        final Config config = defineConfig();
        final Scenario scenario = defineScenario(config);


        controler = new Controler(scenario);

        Signals.configure(controler);

        // create the path to the output directory if it does not exist yet
        Files.createDirectories(Paths.get(outputDirectory));

        config.controler().setOutputDirectory(outputDirectory);
        config.controler().setLastIteration(10);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());

        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);
        //write config to file
        String configFile = outputDirectory + "config.xml";
        ConfigWriter configWriter = new ConfigWriter(config);
        configWriter.write(configFile);

//        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
//        signalSystemsConfigGroup.setUseSignalSystems(true);
//        ----
        SignalsData signalsData = (SignalsData) controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME);
        List<Id<SignalSystem>> signalSystemIds = new ArrayList<>(signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().keySet());
        List<Id<Signal>> signalIds = new ArrayList<>();
        List<SignalData> signalData = new ArrayList<>();

        List<Id<SignalGroup>> signalGroupIds = new ArrayList<>();
        Map<Id<Link>, Id<Signal>> linkId2signalId = new HashMap<>();
        Map<Id<Signal>, Id<SignalGroup>> signalId2signalGroupId = new HashMap<>();
        Map<Id<Link>, Id<SignalGroup>> linkId2signalGroupId = new HashMap<>();

        (new MixedTrafficSignals.Configurator(this.controler)).addSignalControllerFactory(MixedTrafficLaemmerSignalController.IDENTIFIER,
                MixedTrafficLaemmerSignalController.LaemmerFactory.class);

        MixedTrafficSignalAnalysisTool signalAnalyzer = new MixedTrafficSignalAnalysisTool();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(signalAnalyzer);
                this.addControlerListenerBinding().toInstance(signalAnalyzer);
            }
        });
        // add general analysis tools
        MixedTrafficDelayAnalysisTool delayAnalysis = new MixedTrafficDelayAnalysisTool(controler.getScenario().getNetwork(), controler.getScenario().getVehicles());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(delayAnalysis);
            }
        });

        for (var signalSystemId : signalSystemIds) {
            Map<Id<SignalGroup>, SignalGroupData> signalGroupDataBySystemId = signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId);
            signalGroupDataBySystemId.values().forEach(signalGroupData -> signalIds.addAll(new ArrayList<>(signalGroupData.getSignalIds())));
            signalGroupIds.addAll(signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().get(signalSystemId).keySet());
            signalsData.getSignalSystemsData().getSignalSystemData().values().forEach(signalSystemData -> signalData.addAll(signalSystemData.getSignalData().values()));
            signalGroupDataBySystemId.values().forEach(signalGroupData -> signalGroupData.getSignalIds().forEach(signalId -> signalId2signalGroupId.put(signalId, signalGroupData.getId())));
        }
        signalData.forEach(signalD -> linkId2signalId.put(signalD.getLinkId(), signalD.getId()));
        linkId2signalId.forEach((linkId, signalId) -> linkId2signalGroupId.put(linkId, signalId2signalGroupId.get(signalId)));


        Map<Double, Map<Id<SignalGroup>, Double>> greenTimePerCycle = signalAnalyzer.getSummedBygoneSignalGreenTimesPerCycle();
        Map<Double, Map<Id<Link>, Double>> delayPerCycle = delayAnalysis.getSummedBygoneDelayPerCycle();
        Map<Double, Map<Id<Link>, Map<Id<VehicleType>, Double>>> flowPerCycle = delayAnalysis.getSummedBygoneFlowPerLinkPerVehicleTypePerCycle();

        writeResult(outputDirectory + "greenTimesPerCycle.csv", List.of(new String[]{"cycle_time", "signal_group", "link_ids", "green_time"}), false);
        controler.run();
        for (var outerEntry : greenTimePerCycle.entrySet()) {
            for (var innerEntry : outerEntry.getValue().entrySet()) {
                List<Id<Link>> linkIds = new ArrayList<>();
                StringBuilder stringBuilder = new StringBuilder();
                for (var linkSignalGroupId : linkId2signalGroupId.entrySet()) {
                    if (linkSignalGroupId.getValue().equals(innerEntry.getKey()))
                        linkIds.add(linkSignalGroupId.getKey());
                }
                for (var linkId: linkIds)
                    stringBuilder.append(linkId).append("|");
                writeResult(outputDirectory + "greenTimesPerCycle.csv", List.of(new String[]{outerEntry.getKey().toString(), innerEntry.getKey().toString(), stringBuilder.substring(0, stringBuilder.length() - 1), String.valueOf(innerEntry.getValue())}), true);
            }
        }

        writeResult(outputDirectory + "delayPerCycle.csv", List.of(new String[]{"cycle_time", "link_id", "delay"}), false);

        for (var outerEntry : delayPerCycle.entrySet()) {
            for (var innerEntry : outerEntry.getValue().entrySet()) {
                writeResult(outputDirectory + "delayPerCycle.csv", List.of(new String[]{outerEntry.getKey().toString(), innerEntry.getKey().toString(), String.valueOf(innerEntry.getValue())}), true);
            }
        }

        List<String> flowColumns = new ArrayList();
        flowColumns.add("cycle_time");
        List<Id<Link>> linkIdList = new ArrayList<>(controler.getScenario().getNetwork().getLinks().keySet());
        Collections.sort(linkIdList);
        for (var linkId: linkIdList) {
            for (var vehicleType: controler.getScenario().getVehicles().getVehicleTypes().keySet()) {
                String linkIdWithVehicleType = linkId.toString() + "_" + vehicleType.toString();
                flowColumns.add(linkIdWithVehicleType);
            }
        }
        writeResult(outputDirectory + "flowPerCycle.csv", flowColumns, false);
        Map<Id<VehicleType>, Double> emptyFlow = new HashMap<>();
        // filling non-present linkIds
        for (var vehicleType: controler.getScenario().getVehicles().getVehicleTypes().keySet()) {
            emptyFlow.putIfAbsent(vehicleType, 0.0);
        }

        for (var linkId: controler.getScenario().getNetwork().getLinks().keySet()) {
            for (var outerEntry : flowPerCycle.entrySet()) {
                flowPerCycle.get(outerEntry.getKey()).putIfAbsent(linkId, emptyFlow);
            }
        }
        for (var outerEntry : flowPerCycle.entrySet()) {
            List<String> linkIdWithFlowValues = new ArrayList<>();
            linkIdWithFlowValues.add(outerEntry.getKey().toString());
            for (var innerEntry : outerEntry.getValue().entrySet()) {
                for (var innerInnerEntry: innerEntry.getValue().entrySet()) {
                    linkIdWithFlowValues.add(innerInnerEntry.getValue().toString());
                }
            }
            writeResult(outputDirectory + "flowPerCycle.csv", linkIdWithFlowValues, true);
        }
    }

    public static void main(String[] args) throws IOException {
        RunFixedMixedTrafficSignalSimpleIntersection fixedMixedTrafficSignalSimpleIntersection = new RunFixedMixedTrafficSignalSimpleIntersection();
        fixedMixedTrafficSignalSimpleIntersection.run(false);
    }

    private void run(boolean startOtfvis) {

        EventsManager manager = EventsUtils.createEventsManager();

        PrepareForSimUtils.createDefaultPrepareForSim(controler.getScenario()).run();
        QSim qSim = new QSimBuilder(controler.getScenario().getConfig()).useDefaults().build(controler.getScenario(), manager);

        if (startOtfvis) {

            // otfvis configuration.  There is more you can do here than via file!
//                final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
//                otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
//                otfVisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.byId);
//
//                OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(controler.getScenario().getConfig(), controler.getScenario(), manager, qSim);
//                OTFClientLive.run(controler.getScenario().getConfig(), server);

            // add the module that start the otfvis visualization with signals
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }
        controler.run();
    }
    public static void writeResult(String filename, List<String> values, boolean append) {
        FileWriter csvwriter;
        BufferedWriter bufferedWriter = null;
        try {
            csvwriter = new FileWriter(filename, append);
            bufferedWriter = new BufferedWriter(csvwriter);
            StringJoiner stringJoiner = new StringJoiner(",");
            for (var value: values) {
                stringJoiner.add(value);
            }
            bufferedWriter.write(stringJoiner.toString());
            bufferedWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert bufferedWriter != null;
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        scenario.getConfig().network().setLaneDefinitionsFile("lane_definitions_v2.0.xml");
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
                Id.create("1", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("2", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("3", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("4", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("5", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("6", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("7", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("8", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("9", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("10", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("11", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("12", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
    }


    private void createPopulation(Scenario scenario) {
        Population population = scenario.getPopulation();

        String[] odRelations = {"1_2-7_6-L", "1_2-4_5-L", "1_2-8_9-L", "6_7-4_5-T", "6_7-8_9-T", "6_7-2_1-T",
                "5_4-8_9-R", "5_4-2_1-R", "5_4-7_6-R", "9_8-2_1-B", "9_8-7_6-B", "9_8-4_5-B",};


        for (String od : odRelations) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];

            String approach = od.split("-")[2];
            int agentsPerApproach;
            int offset;

            if (approach.equalsIgnoreCase("L")) {
                agentsPerApproach = AGENTS_PER_LEFT_APPROACH;
                offset = OFFSET_LEFT_APPROACH;
            }
            else if (approach.equalsIgnoreCase("T")) {
                agentsPerApproach = AGENTS_PER_TOP_APPROACH;
                offset = OFFSET_TOP_APPROACH;
            }
            else if (approach.equalsIgnoreCase("R")) {
                agentsPerApproach = AGENTS_PER_RIGHT_APPROACH;
                offset = OFFSET_RIGHT_APPROACH;
            }
            else {
                agentsPerApproach = AGENTS_PER_BOTTOM_APPROACH;
                offset = OFFSET_BOTTOM_APPROACH;
            }
            for (int i = 0; i < agentsPerApproach; i++) {
                // create a person
                Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));

                // create a plan for the person that contains all this
                // information
                Plan plan = population.getFactory().createPlan();

                // create a start activity at the from link
                Activity homeAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
                // distribute agents uniformly during one hour.
                homeAct.setEndTime(i + offset);
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
        if (number <= 60) return "car";
        else return "truck";
    }

    private Config defineConfig() throws IOException {
        Config config = ConfigUtils.createConfig();

        config.travelTimeCalculator().setMaxTime(5 * 60 * 60);

        config.qsim().setStartTime(0);
        config.qsim().setEndTime(5 * 60 * 60);
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


        return config;
    }

    private void createGroupsAndSystem(SignalSystemsData signalSystemsData, SignalGroupsData signalGroupsData) {
        SignalSystemData sys = signalSystemsData.getFactory().createSignalSystemData(Id.create("3", SignalSystem.class));
        signalSystemsData.addSignalSystemData(sys);
        SignalSystemsDataFactory factory = signalSystemsData.getFactory();

        SignalUtils.createAndAddSignal(sys, factory, Id.create("1", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("2", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("3", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("4", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("5", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("6", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("7", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("8", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("9", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("10", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.l", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("11", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.s", Lane.class)));
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
                null, Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.s", Lane.class), Id.create("2_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);

        // straight lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);

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
                null, Arrays.asList(Id.create("7_3.l", Lane.class), Id.create("7_3.s", Lane.class), Id.create("7_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);

        // straight lane (alignment 0)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);

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
                null, Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.s", Lane.class), Id.create("4_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);

        // straight lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);

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
                null, Arrays.asList(Id.create("8_3.l", Lane.class), Id.create("8_3.s", Lane.class), Id.create("8_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);

        // straight lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);

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
            link.setFreespeed(10);
            net.addLink(link);
        }
        new NetworkWriter(net).write(outputDirectory + "network.xml.gz");
    }
}
