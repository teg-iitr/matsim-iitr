package playground.amit.fundamentalDiagrams;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;


/**
 * @author Amit, created on 11-03-2022
 */

public class FDQSimModule extends AbstractQSimModule {

    @Override
    protected void configureQSim() {
        this.bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
            @Inject
            private Scenario scenario;

            @Inject
            private EventsManager events;

            @Override
            public QNetworkFactory get() {
                final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
                return factory;
            }
        });
    }
}
