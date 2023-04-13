package playground.shivam.signals.runner;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.analysis.*;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.vehicles.VehicleType;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

import java.util.*;

import static playground.shivam.signals.writer.CSVWriter.writeResult;

public class RunMatsim {
     public static final Logger log = LogManager.getLogger(RunMatsim.class);
     public static  Map<Id<SignalSystem>, Double> avgCycleTimePerSystem;
     public static Map<Id<Link>, Double> avgDelayPerLink;
     public static double totalDelay;
    public static void run(boolean startOtfvis, Controler controler, String signalController, Class<? extends SignalControllerFactory> signalControllerFactoryClassName, String outputDirectory) {

        EventsManager manager = EventsUtils.createEventsManager();

        PrepareForSimUtils.createDefaultPrepareForSim(controler.getScenario()).run();
        QSim qSim = new QSimBuilder(controler.getScenario().getConfig()).useDefaults().build(controler.getScenario(), manager);

        if (startOtfvis) {
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }

        (new MixedTrafficSignals.Configurator(controler)).addSignalControllerFactory(signalController,
                signalControllerFactoryClassName);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(TtQueueLengthAnalysisTool.class).asEagerSingleton();
                this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
                this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);

                this.bind(SignalAnalysisTool.class).asEagerSingleton();
                this.bind(SignalAnalysisWriter.class).asEagerSingleton();
                this.bind(DelayAnalysisTool.class).asEagerSingleton();
                this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        DelayAnalysisTool delayAnalysis = new DelayAnalysisTool(controler.getScenario().getNetwork());
        SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(delayAnalysis);

                this.addEventHandlerBinding().toInstance(signalAnalyzer);
                this.addControlerListenerBinding().toInstance(signalAnalyzer);
            }
        });

        controler.run();

        avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();
        avgDelayPerLink = delayAnalysis.getAvgDelayPerLink();

        /*SignalsData signalsData = (SignalsData) controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME);
        for (Id<SignalSystem> signalSystemId : signalsData.getSignalSystemsData().getSignalSystemData().keySet()) {
            log.info("avg cycle time per system " + signalSystemId + ": " + avgCycleTimePerSystem.get(signalSystemId));
        }

        for (Link link: controler.getScenario().getNetwork().getLinks().values())
            log.info("avg delay per link " + link.getId() + ": " + avgDelayPerLink.get(link.getId()));
*/
        totalDelay = delayAnalysis.getTotalDelay();

        log.info("total delay: " + totalDelay);
    }
}
