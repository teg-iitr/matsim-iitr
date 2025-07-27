package playground.shivam.trafficChar;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.TrafficCharQNetworkFactory;

public class TrafficCharQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		this.bind(QNetworkFactory.class).to(TrafficCharQNetworkFactory.class);
	}

}
