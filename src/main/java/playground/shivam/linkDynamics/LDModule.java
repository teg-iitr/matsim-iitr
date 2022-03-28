package playground.shivam.linkDynamics;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.DynamicHeadwayQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;

public class LDModule extends AbstractQSimModule {

	@Override
	public void configureQSim() {
	}

	@Provides
	@Singleton
	public QNetworkFactory provideQNetworkFactory(EventsManager events, Scenario scenario) {
		DynamicHeadwayQNetworkFactory networkFactory = new DynamicHeadwayQNetworkFactory(events, scenario);
		networkFactory.setLinkSpeedCalculator(new DefaultLinkSpeedCalculator());
		return networkFactory;
	}

}
