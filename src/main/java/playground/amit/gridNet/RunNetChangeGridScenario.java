package playground.amit.gridNet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.shivam.trafficChar.TrafficCharQSimModule;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

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
        config.controler().setOutputDirectory("output/GridScenarioTrafficChar");
//        config.network().setTimeVariantNetwork(true);

        TrafficCharConfigGroup trafficCharConfigGroup = new TrafficCharConfigGroup();
        QSimConfigGroup qSimConfigGroupFIFO = new QSimConfigGroup();
        qSimConfigGroupFIFO.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
        trafficCharConfigGroup.addQSimConfigGroup("FIFO", qSimConfigGroupFIFO);
        trafficCharConfigGroup.addQSimConfigGroup("default", config.qsim());
        config.getModules().put(TrafficCharConfigGroup.GROUP_NAME, trafficCharConfigGroup);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        rgs.addVehicleTypes(scenario);

        for (Link link: scenario.getNetwork().getLinks().values()) {
//			if (link.getAttributes().getAttribute("type").equals("primary"))
            if (link.getCapacity()==1500.)
                link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, "FIFO");
            else
                link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, TrafficCharConfigGroup.ROAD_TYPE_DEFAULT);
        }

//        { //let's reduce the capacities of all links to half of the assigned value at 8am
//            NetworkChangeEvent e1 = new NetworkChangeEvent(8*3600.);
//            e1.addLinks(scenario.getNetwork().getLinks().values());
//            NetworkChangeEvent.ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,0.5);
//            e1.setFlowCapacityChange(cv);
//            NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),e1);
//        }
//        { //let's increase the capacities of all links to double of the assigned value at 9am
//            NetworkChangeEvent e1 = new NetworkChangeEvent(10*3600.);
//            e1.addLinks(scenario.getNetwork().getLinks().values());
//            NetworkChangeEvent.ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,2);
//            e1.setFlowCapacityChange(cv);
//            NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(),e1);
//        }

        Controler controler = new Controler(scenario);
        controler.addOverridingQSimModule(new TrafficCharQSimModule());
        controler.run();
    }

}
