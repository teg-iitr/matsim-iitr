package playground.shivam.signals.runner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.analysis.*;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
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
                this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });
        controler.run();


    }
}
