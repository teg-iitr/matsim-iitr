package playground.nidhi.examJan.ques4;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;

public class PlansIITR {
    private final String mode = TransportMode.car;
    private final double startTime = 9*3600.;
    private final double timebin = 1*3600.;
    private Map<String, Coord> zoneCoords = new HashMap<>();
    private final Random random = MatsimRandom.getLocalInstance();
    private Network network ;
    public Scenario scenario;
    private  Random rand = new Random();


    public PlansIITR() {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_matsim_network.xml.gz");
        this.scenario = ScenarioUtils.loadScenario(config);
        network = this.scenario.getNetwork();
    }

    public static void main(String[] args) {
        PlansIITR  plansIITR = new PlansIITR();
        plansIITR.run();
    }

    public void run() {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();

        PopulationFactory factory = scenario.getPopulation().getFactory();
        String out_plansIITR= "C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_population.xml.gz";

        for (int j = 0 ; j < 180 ; j++){
            Coord coordFromZone=homeCoord();
            Coord coordToZone=workCoordinate();

            Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
            Plan plan = factory.createPlan();

            Activity home = factory.createActivityFromCoord("origin", coordFromZone);
            home.setEndTime(getRandomEndTime(startTime, timebin));
            plan.addActivity(home);

            Leg leg = factory.createLeg(mode);
            plan.addLeg(leg);

            Activity work = factory.createActivityFromCoord("destination", coordToZone);
            plan.addActivity(work);

            person.addPlan(plan);
            population.addPerson(person);
        }
        new PopulationWriter(population).write(out_plansIITR);
    }

    private Coord homeCoord(){
        Collection<? extends Node> values = network.getNodes().values();
        ArrayList<Node> nodes = new ArrayList<>(values);
        int x = rand.nextInt(nodes.size());
        return nodes.get(x).getCoord();
    }

    private Coord workCoordinate() {
        ArrayList<Node> nodes = new ArrayList<>(network.getNodes().values());
        int y = rand.nextInt(nodes.size());
        return nodes.get(y).getCoord();
    }

    private double getRandomEndTime(double start, double timebin){
        return start + random.nextInt((int) timebin);
    }


}
