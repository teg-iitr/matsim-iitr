package playground.sapan.dma;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Set;


public class RunBaseline {
    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.addCoreModules();

        config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
        config.controller().setOutputDirectory("./output/haridwarPRT/output/");
        config.controller().setLastIteration(10);
        config.network().setInputFile("./input/haridwarPRT/Haridwar-PRT-Network.xml.gz");
        config.transit().setUseTransit(true);
        config.transit().setTransitScheduleFile("./input/haridwarPRT/Haridwar-PRT-TransitSchedule.xml.gz");
        config.transit().setVehiclesFile("./input/haridwarPRT/Haridwar-PRT-TransitVehicles.xml.gz");
        config.qsim().setMainModes(Set.of(PRTScheduleGenerator.mode));
        config.qsim().setEndTime(24*3600.);
        config.routing().setNetworkModes(Set.of(PRTScheduleGenerator.mode));

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        Person p = populationFactory.createPerson(Id.createPersonId(1));
        Plan plan = populationFactory.createPlan();

        Activity act1 = populationFactory.createActivityFromLinkId("home", Id.createLinkId("prtstop1_prtstop2"));
        act1.setEndTime(6*3600. + 49.);
        Leg leg = populationFactory.createLeg(PRTScheduleGenerator.mode);
        plan.addActivity(act1);
        plan.addLeg(leg);

        Activity act2 = populationFactory.createActivityFromLinkId("work", Id.createLinkId("prtstop9_prtstop10"));
        plan.addActivity(act2);
        p.addPlan(plan);
        population.addPerson(p);

        Controler controler = new Controler( scenario ) ;

        controler.addOverridingModule(new SwissRailRaptorModule());

        controler.run();
    }
}
