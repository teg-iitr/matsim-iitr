package playground.shivam.signals;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.MixedTrafficDelayAnalysisTool;
import org.matsim.contrib.signals.analysis.MixedTrafficSignalAnalysisTool;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.laemmerFix.MixedTrafficLaemmerSignalController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.*;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RunAdaptiveSignalSimpleNetwork {
    private final Controler controler;
    static String outputDirectory = "output/RunAdaptiveSignalSimpleNetwork/";
    public RunAdaptiveSignalSimpleNetwork() {
        final Config config = defineConfig();
        final Scenario scenario = defineScenario(config);
        controler = new Controler(scenario);

        /* the signals extensions works for planbased, sylvia and laemmer signal controller
         * by default and is pluggable for your own signal controller like this: */
        (new MixedTrafficSignals.Configurator(this.controler)).addSignalControllerFactory(MixedTrafficLaemmerSignalController.IDENTIFIER,
                MixedTrafficLaemmerSignalController.LaemmerFactory.class);
    }

    public static void main(String[] args) {
        new RunAdaptiveSignalSimpleNetwork().run();
    }

    private static void createNetwork(Scenario scenario) {
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
            link.setCapacity(7200);
            link.setLength(1000);
            link.setFreespeed(10);
            net.addLink(link);
        }
    }

    private static void createPopulation(Scenario scenario) {
        Population population = scenario.getPopulation();

        String[] odRelations = {"1_2-4_5", "4_5-2_1", "6_7-8_9", "9_8-7_6"};

        for (String od : odRelations) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];

            for (int i = 0; i < 1800; i++) {
                // create a person
                Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));
                population.addPerson(person);

                // create a plan for the person that contains all this
                // information
                Plan plan = population.getFactory().createPlan();
                person.addPlan(plan);

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
            }
        }
    }

    private static String getTravelMode(int number) {
        if (number < 60) return "car";
        else return "truck";
    }

    public void run() {
//		controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        SignalsData signalsData = (SignalsData) controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME);
        List<Id<SignalSystem>> signalSystemIds = new ArrayList<>(signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().keySet());
        List<Id<Signal>> signalIds = new ArrayList<>();
        List<SignalData> signalData = new ArrayList<>();

        List<Id<SignalGroup>> signalGroupIds = new ArrayList<>();
        Map<Id<Link>, Id<Signal>> linkId2signalId = new HashMap<>();
        Map<Id<Signal>, Id<SignalGroup>> signalId2signalGroupId = new HashMap<>();
        Map<Id<Link>, Id<SignalGroup>> linkId2signalGroupId = new HashMap<>();

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
        controler.run();
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

    private static Scenario defineScenario(Config config) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
        SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalsConfigGroup.setUseSignalSystems(true);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        createNetwork(scenario);
        createPopulation(scenario);
        createSignals(scenario);
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
        return scenario;
    }

    private static Config defineConfig() {
        Config config = ConfigUtils.createConfig();

        config.controler().setOutputDirectory(outputDirectory);

        config.controler().setLastIteration(10);
        config.travelTimeCalculator().setMaxTime(18000);
        config.qsim().setStartTime(0.0D);
        config.qsim().setEndTime(18000.0D);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

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

        LaemmerConfigGroup laemmerConfigGroup = ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.GROUP_NAME, LaemmerConfigGroup.class);
        laemmerConfigGroup.setDesiredCycleTime(90);
        laemmerConfigGroup.setMinGreenTime(5);
        laemmerConfigGroup.setActiveRegime(LaemmerConfigGroup.Regime.COMBINED);
        config.getModules().put(LaemmerConfigGroup.GROUP_NAME, laemmerConfigGroup);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);

        return config;
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

    private static void createSignals(Scenario scenario) {
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

        SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();

        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();

        SignalControlData signalControl = signalsData.getSignalControlData();

        SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();

        // create signal system
        Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);
        SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
        signalSystems.addSignalSystemData(signalSystem);

        // create a signal for every inLink
        for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().keySet()) {
            SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
            signalSystem.addSignalData(signal);
            signal.setLinkId(inLinkId);
        }

        // group signals with non conflicting streams
        Id<SignalGroup> signalGroupId2_3 = Id.create("SignalGroup2_3", SignalGroup.class);
        SignalGroupData signalGroup2_3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId2_3);
        signalGroup2_3.addSignalId(Id.create("Signal2_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup2_3);

        Id<SignalGroup> signalGroupId4_3 = Id.create("SignalGroup4_3", SignalGroup.class);
        SignalGroupData signalGroup4_3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId4_3);
        signalGroup4_3.addSignalId(Id.create("Signal4_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup4_3);

        Id<SignalGroup> signalGroupId7_3 = Id.create("SignalGroup7_3", SignalGroup.class);
        SignalGroupData signalGroup7_3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId7_3);
        signalGroup7_3.addSignalId(Id.create("Signal7_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup7_3);

        Id<SignalGroup> signalGroupId8_3 = Id.create("SignalGroup8_3", SignalGroup.class);
        SignalGroupData signalGroup8_3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId8_3);
        signalGroup8_3.addSignalId(Id.create("Signal8_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup8_3);

        // create the signal control
        SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(MixedTrafficLaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);

        // create a plan for the signal system (with defined cycle time and offset 0)
//        SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
//        signalSystemControl.addSignalPlanData(signalPlan);
//
//        // specify signal group settings for both signal groups
//        signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 5));
//        signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 10, 55));
//        signalPlan.setOffset(0);
    }
}
