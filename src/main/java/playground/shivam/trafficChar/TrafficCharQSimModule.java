package playground.shivam.trafficChar;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.TrafficCharQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;

public class TrafficCharQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		this.bind(QNetworkFactory.class).toProvider(QNetworkFactoryProvider.class);
	}

	static private class QNetworkFactoryProvider implements Provider<QNetworkFactory> {
			@Inject
			private Scenario scenario;

			@Inject
			private EventsManager events;

			@Override
			public QNetworkFactory get() {
				TrafficCharQNetworkFactory networkFactory = new TrafficCharQNetworkFactory(events, scenario);
				networkFactory.setLinkSpeedCalculator(new DefaultLinkSpeedCalculator());
				return networkFactory;
			}
	}
}
