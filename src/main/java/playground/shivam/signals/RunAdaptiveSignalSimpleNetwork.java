package playground.shivam.signals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.laemmerFix.MixedTrafficLaemmerSignalController;
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

import static playground.shivam.signals.config.CreateAdaptiveConfig.defineAdaptiveConfig;
import static playground.shivam.signals.scenarios.CreateScenarioFromConfig.defineScenario;

public class RunAdaptiveSignalSimpleNetwork {
    private Controler controler;
    private static String outputDirectory = "output/RunAdaptiveSignalSimpleNetwork/";
    private static String signalController = MixedTrafficLaemmerSignalController.IDENTIFIER;
    private static Class<? extends SignalControllerFactory> signalControllerFactoryClassName = MixedTrafficLaemmerSignalController.LaemmerFactory.class;

    public RunAdaptiveSignalSimpleNetwork() throws IOException {
        final Config config = defineAdaptiveConfig(outputDirectory);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController);

        controler = new Controler(scenario);

        MixedTrafficSignals.configure(controler);
    }

    public static void main(String[] args) throws IOException {
        RunAdaptiveSignalSimpleNetwork adaptiveSignalSimpleNetwork = new RunAdaptiveSignalSimpleNetwork();
        adaptiveSignalSimpleNetwork.run(false);
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
                bind(TtQueueLengthAnalysisTool.class).asEagerSingleton();
                addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
                addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });
        controler.run();

    }



}
