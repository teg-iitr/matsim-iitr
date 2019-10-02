package playground;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class MyControler {
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		controler.run();
	}
	

}
