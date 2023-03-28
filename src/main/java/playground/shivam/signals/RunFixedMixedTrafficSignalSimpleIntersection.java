package playground.shivam.signals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.analysis.SignalAnalysisWriter;
import org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool;
import org.matsim.contrib.signals.analysis.TtSignalAnalysisListener;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
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
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

import java.io.IOException;

import static playground.shivam.signals.config.CreateFixedConfig.defineFixedConfig;
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

        MixedTrafficSignals.configure(controler);

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

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(TtQueueLengthAnalysisTool.class).asEagerSingleton();
                this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
                this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);

                this.bind(SignalAnalysisTool.class).asEagerSingleton();
                this.bind(SignalAnalysisWriter.class).asEagerSingleton();
                this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        controler.run();

    }

}
