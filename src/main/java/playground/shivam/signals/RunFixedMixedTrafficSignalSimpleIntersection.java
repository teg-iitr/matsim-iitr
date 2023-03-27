package playground.shivam.signals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.analysis.MixedTrafficDelayAnalysisTool;
import org.matsim.contrib.signals.analysis.MixedTrafficSignalAnalysisTool;
import org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;

import java.io.IOException;

import static playground.shivam.signals.config.CreateAdaptiveConfig.defineFixedConfig;
import static playground.shivam.signals.scenarios.CreateScenarioFromConfig.defineScenario;

public class RunFixedMixedTrafficSignalSimpleIntersection {
    private Controler controler;
    private static String outputDirectory = "output/RunFixedMixedTrafficSignalSimpleIntersection/";
    private static String signalController = DefaultPlanbasedSignalSystemController.IDENTIFIER;
    private static Class<? extends SignalControllerFactory> signalControllerFactoryClassName = DefaultPlanbasedSignalSystemController.FixedTimeFactory.class;

    public RunFixedMixedTrafficSignalSimpleIntersection() throws IOException {
        final Config config = defineFixedConfig(outputDirectory);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController);

        controler = new Controler(scenario);

        Signals.configure(controler);

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
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }

        (new MixedTrafficSignals.Configurator(this.controler)).addSignalControllerFactory(signalController,
                signalControllerFactoryClassName);

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

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TtQueueLengthAnalysisTool.class).asEagerSingleton();
                addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
                addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);
            }
        });

        controler.run();

        /*
        SignalsData signalsData = (SignalsData) controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME);
        List<Id<SignalSystem>> signalSystemIds = new ArrayList<>(signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().keySet());
        List<Id<Signal>> signalIds = new ArrayList<>();
        List<SignalData> signalData = new ArrayList<>();

        List<Id<SignalGroup>> signalGroupIds = new ArrayList<>();
        Map<Id<Link>, Id<Signal>> linkId2signalId = new HashMap<>();
        Map<Id<Signal>, Id<SignalGroup>> signalId2signalGroupId = new HashMap<>();
        Map<Id<Link>, Id<SignalGroup>> linkId2signalGroupId = new HashMap<>();
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
                if (stringBuilder.length() > 1)
                    writeResult(outputDirectory + "greenTimesPerCycle.csv", List.of(new String[]{outerEntry.getKey().toString(), innerEntry.getKey().toString(), stringBuilder.substring(0, stringBuilder.length() - 1), String.valueOf(innerEntry.getValue())}), true);
                writeResult(outputDirectory + "greenTimesPerCycle.csv", List.of(new String[]{outerEntry.getKey().toString(), innerEntry.getKey().toString(), stringBuilder.toString(), String.valueOf(innerEntry.getValue())}), true);
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
        }*/
    }

}
