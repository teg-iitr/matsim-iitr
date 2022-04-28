package org.matsim.contrib.signals.builder;


import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController.FixedTimeFactory;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController.LaemmerFactory;
import org.matsim.contrib.signals.controller.laemmerFix.MixedTrafficLaemmerSignalController;
import org.matsim.contrib.signals.controller.sylvia.SylviaSignalController.SylviaFactory;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.sensor.DownstreamSensor;
import org.matsim.contrib.signals.sensor.LinkSensorManager;
import org.matsim.contrib.signals.sensor.MixedTrafficLinkSensorManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;

class MixedTrafficSignalsModule extends AbstractModule {
    private static final Logger log = Logger.getLogger(SignalsModule.class);
    private MapBinder<String, SignalControllerFactory> signalControllerFactoryMultibinder;
    private Map<String, Class<? extends SignalControllerFactory>> signalControllerFactoryClassNames = new HashMap();

    MixedTrafficSignalsModule() {
        this.signalControllerFactoryClassNames.put("DefaultPlanbasedSignalSystemController", FixedTimeFactory.class);
        this.signalControllerFactoryClassNames.put("SylviaSignalControl", SylviaFactory.class);
        this.signalControllerFactoryClassNames.put("LaemmerSignalController", LaemmerFactory.class);
        this.signalControllerFactoryClassNames.put("MixedTrafficLaemmerSignalController", MixedTrafficLaemmerSignalController.LaemmerFactory.class);
    }

    public void install() {
        this.getConfig().travelTimeCalculator().setSeparateModes(false);
        log.warn("setting travelTimeCalculatur.setSeparateModes to false since otherwise link2link routing does not work");
        this.signalControllerFactoryMultibinder = MapBinder.newMapBinder(this.binder(), new TypeLiteral<String>() {
        }, new TypeLiteral<SignalControllerFactory>() {
        });
        if (((SignalSystemsConfigGroup)ConfigUtils.addOrGetModule(this.getConfig(), "signalsystems", SignalSystemsConfigGroup.class)).isUseSignalSystems()) {
            this.bind(SignalModelFactory.class).to(SignalModelFactoryImpl.class);
            this.addControlerListenerBinding().to(SensorBasedSignalControlerListener.class);
            this.bind(MixedTrafficLinkSensorManager.class).in(Singleton.class);
            this.bind(LinkSensorManager.class).in(Singleton.class);
            this.bind(DownstreamSensor.class).in(Singleton.class);
            Iterator var1 = this.signalControllerFactoryClassNames.keySet().iterator();

            while(var1.hasNext()) {
                String identifier = (String)var1.next();
                this.signalControllerFactoryMultibinder.addBinding(identifier).to((Class)this.signalControllerFactoryClassNames.get(identifier));
            }

            this.bind(SignalSystemsManager.class).toProvider(FromDataBuilder.class).in(Singleton.class);
            this.addMobsimListenerBinding().to(QSimSignalEngine.class);
            this.bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
            this.addControlerListenerBinding().to(SignalEvents2ViaCSVWriter.class);
            this.addEventHandlerBinding().to(SignalEvents2ViaCSVWriter.class);
            if (this.getConfig().qsim().isUsingFastCapacityUpdate()) {
                throw new RuntimeException("Fast flow capacity update does not support signals");
            }
        }

        if (this.getConfig().controler().isLinkToLinkRoutingEnabled()) {
            this.bind(NetworkTurnInfoBuilderI.class).to(NetworkWithSignalsTurnInfoBuilder.class);
        }

    }

    final void addSignalControllerFactory(String key, Class<? extends SignalControllerFactory> signalControllerFactoryClassName) {
        this.signalControllerFactoryClassNames.put(key, signalControllerFactoryClassName);
    }
}

