package org.matsim.contrib.signals.builder;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.signals.sensor.LinkSensorManager;
import org.matsim.contrib.signals.sensor.MixedTrafficLinkSensorManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;


final class MixedTrafficSensorBasedSignalControlerListener implements SignalControlerListener, IterationStartsListener,
        ShutdownListener {

    @Inject(optional = true)
    SignalSystemsManager signalManager = null;
    @Inject(optional = true)
    MixedTrafficLinkSensorManager sensorManager = null;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (this.sensorManager != null)
            this.sensorManager.reset(event.getIteration());
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        this.writeData(event.getServices().getScenario(), event.getServices().getControlerIO());
    }

    private void writeData(Scenario sc, OutputDirectoryHierarchy controlerIO){
        new SignalsScenarioWriter(controlerIO).writeSignalsData(sc);
    }

}
