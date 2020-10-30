package playground.amit.Delhi.MalviyaNagarPT;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
public class ConfigOD {

	public static void main(String[] args) {
	
      Config config=ConfigUtils.createConfig();
		
		ControlerConfigGroup ccg= config.controler();
		ccg.setOutputDirectory("C:\\Users\\Nidhi\\Desktop\\Matsim Paper\\outputOD");
		ccg.setFirstIteration(0);
		ccg.setLastIteration(10);

	    config.network().setInputFile("C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\Planet_south_delhi_matsim.xml.gz");
	    config.plans().setInputFile("C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\DemandOD.xml.gz");
	  
	    config.network().setTimeVariantNetwork(true);
	    
	    
		Scenario scenario=ScenarioUtils.loadScenario(config);
		
		
		
		Controler controler = new Controler(scenario);
		controler.run();
	}

}

//No route was found from node 5096873922 to node 301037285. Some possible reasons:
