package playground.shivam.trafficChar;


import org.matsim.core.controler.AbstractModule;

public class TrafficCharModule extends AbstractModule {

	@Override
	public void install() {
		installQSimModule(new TrafficCharQSimModule());
	}
}
