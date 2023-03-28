package org.matsim.contrib.signals.builder;

import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.core.controler.AllowsConfiguration;

public final class MixedTrafficSignals {
    public MixedTrafficSignals() {
    }

    public static void configure(AllowsConfiguration ao) {
        ao.addOverridingModule(new MixedTrafficSignalsModule());
        //ao.addOverridingModule(new SignalsModule());
        ao.addOverridingQSimModule(new SignalsQSimModule());
    }

    public static class Configurator {
        private final MixedTrafficSignalsModule mixedTrafficSignalsModule = new MixedTrafficSignalsModule();

        public Configurator(AllowsConfiguration ao) {
            ao.addOverridingModule(this.mixedTrafficSignalsModule);
            ao.addOverridingQSimModule(new SignalsQSimModule());
        }

        public final void addSignalControllerFactory(String key, Class<? extends SignalControllerFactory> signalControllerFactoryClassName) {
            this.mixedTrafficSignalsModule.addSignalControllerFactory(key, signalControllerFactoryClassName);
        }
    }
}