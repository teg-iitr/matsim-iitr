package org.matsim.contrib.signals.analysis;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * Class to bind the signal analyze and writing tool to the simulation.
 *
 * @author tthunig
 */
public class TtSignalAnalysisListener implements IterationEndsListener {

    private static final Logger log = Logger.getLogger(TtSignalAnalysisListener.class);

    @Inject
    private Scenario scenario;

    @Inject
    private SignalAnalysisWriter writer;

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        // write analyzed data
        writer.writeIterationResults(event.getIteration());

        // handle last iteration
        if (event.getIteration() == scenario.getConfig().controler().getLastIteration()) {
            // close overall writing stream
            writer.closeAllStreams();
            // plot overall iteration results
        }
    }
}