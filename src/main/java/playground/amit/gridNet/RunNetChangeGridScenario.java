package playground.amit.gridNet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

/*
 * An example of time-dependent network
 */
public class RunNetChangeGridScenario {

    public static void main(String[] args) {
        new RunNetChangeGridScenario().run();
    }

    private void run(){
        RunGridScenario rgs = new RunGridScenario();
        Config config = rgs.prepareConfig();
        config.controler().setOutputDirectory("C:/Users/Amit Agarwal/Downloads/gridNetwork/output_timeDepNet/");
        config.network().setTimeVariantNetwork(true);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        rgs.addVehicleTypes(scenario);

        { //let's reduce the capacities of all links to half of the assigned value at 8am
            NetworkChangeEvent e1 = new NetworkChangeEvent(8*3600.);
            e1.addLinks(scenario.getNetwork().getLinks().values());
            NetworkChangeEvent.ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,0.5);
            e1.setFlowCapacityChange(cv);
            NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),e1);
        }
        { //let's increase the capacities of all links to double of the assigned value at 9am
            NetworkChangeEvent e1 = new NetworkChangeEvent(10*3600.);
            e1.addLinks(scenario.getNetwork().getLinks().values());
            NetworkChangeEvent.ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,2);
            e1.setFlowCapacityChange(cv);
            NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),e1);
        }

        Controler controler = new Controler(scenario);
        controler.run();
    }

}
