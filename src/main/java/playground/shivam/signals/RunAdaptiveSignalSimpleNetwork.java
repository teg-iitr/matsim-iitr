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
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.*;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;

import java.util.Arrays;
import java.util.List;

public class RunAdaptiveSignalSimpleNetwork {
    private final Controler controler;

    public RunAdaptiveSignalSimpleNetwork() {
        final Config config = defineConfig();
        final Scenario scenario = defineScenario(config);
        controler = new Controler(scenario);

        /* the signals extensions works for planbased, sylvia and laemmer signal controller
         * by default and is pluggable for your own signal controller like this: */
        new Signals.Configurator(controler).addSignalControllerFactory(LaemmerSignalController.IDENTIFIER,
                LaemmerSignalController.LaemmerFactory.class);
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
        if (number < 80) return "car";
        else return "truck";
    }

    public void run() {
//		controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        controler.run();
    }

    private static Scenario defineScenario(Config config) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        createNetwork(scenario);
        createPopulation(scenario);
        createSignals(scenario);
//        new PopulationWriter(scenario.getPopulation()).write("pop.xml");

        QSimConfigGroup qsim = scenario.getConfig().qsim();

        List<String> mainModes = Arrays.asList("car", "truck");
        qsim.setMainModes(mainModes);
        Vehicles vehicles = scenario.getVehicles();
        for (String mode : mainModes) {
            VehicleType veh = VehicleUtils.createVehicleType(Id.create(mode, VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getStuckTime(mode));
            veh.setNetworkMode(mode);
            vehicles.addVehicleType(veh);
        }
//        new MatsimVehicleWriter(scenario.getVehicles()).writeFile("vehi.xml");
        ChangeModeConfigGroup changeTripMode = scenario.getConfig().changeMode();
        changeTripMode.setModes(new String[]{"car", "truck"});
        config.plansCalcRoute().setNetworkModes(mainModes);
        return scenario;
    }

    private static Config defineConfig() {
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory("output/RunAdaptiveSignalSimpleNetwork/");

        config.controler().setLastIteration(10);
        config.qsim().setUsingFastCapacityUpdate(false);

        PlanCalcScoreConfigGroup.ActivityParams dummyAct = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
        dummyAct.setTypicalDuration(12 * 3600);
        config.planCalcScore().addActivityParams(dummyAct);
        {
            StrategyConfigGroup.StrategySettings strat = new StrategyConfigGroup.StrategySettings();
            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected.toString());
            strat.setWeight(0.0);
            strat.setDisableAfter(config.controler().getLastIteration());
            config.strategy().addStrategySettings(strat);
        }

        OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
        otfvisConfig.setDrawTime(true);

        SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalConfigGroup.setUseSignalSystems(true);

        LaemmerConfigGroup laemmerConfigGroup = ConfigUtils.addOrGetModule(config,
                LaemmerConfigGroup.GROUP_NAME, LaemmerConfigGroup.class);
        laemmerConfigGroup.setActiveRegime(LaemmerConfigGroup.Regime.COMBINED);
        laemmerConfigGroup.setDesiredCycleTime(90);
        laemmerConfigGroup.setMinGreenTime(5);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setCreateGraphs(true);


        return config;
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
        Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
        SignalGroupData signalGroup1 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId1);
        signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
        signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup1);

        Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
        SignalGroupData signalGroup2 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId2);
        signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
        signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup2);

        // create the signal control
        SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);

        // create a plan for the signal system (with defined cycle time and offset 0)
        SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
        signalSystemControl.addSignalPlanData(signalPlan);

        // specify signal group settings for both signal groups
        signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 5));
        signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 10, 55));
        signalPlan.setOffset(0);
    }
}
