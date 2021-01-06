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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Delhi.MalviyaNagarPT.MN_TransitDemandGenerator;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class PlansIITR {
    private final String mode = TransportMode.car;
    private final double startTime = 9*3600.;
    private final double endTime = 19*3600.;
    private final double timebin = 1*3600.;
    private final int addBuffertoGate = 50;
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
            String num = "1"; //for vikasnagar gate
            String num2 = "2"; //for sarawati kunj

            Coord coordFromZone= homeCoord(num,addBuffertoGate);    //trips will be originated randomly within buffer distance
            Coord coordToZone=workCoordinate();
            Coord coordToZone2= homeCoord(num2,addBuffertoGate);

            List<Coord> twoGates = new ArrayList<>();
            twoGates.add(coordFromZone);
            twoGates.add(coordToZone2);
            Coord randomCoord =getRandomElement(twoGates);
            Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
            Plan plan = factory.createPlan();


            Activity home = factory.createActivityFromCoord("origin", randomCoord);
            home.setEndTime(getRandomEndTime(startTime, timebin));
            plan.addActivity(home);

            Leg leg = factory.createLeg(mode);
            plan.addLeg(leg);

            Activity work = factory.createActivityFromCoord("destination", coordToZone);
            work.setEndTime(getRandomEndTime(endTime, timebin));
            plan.addActivity(work);

            Leg leg2 = factory.createLeg(mode);
            plan.addLeg(leg2);

            Activity home2 = factory.createActivityFromCoord("origin", randomCoord);
            plan.addActivity(home2);

            person.addPlan(plan);
            population.addPerson(person);
        }
        new PopulationWriter(population).write(out_plansIITR);
    }


    private Coord homeCoord( String gateNo, int addBufferDistance){
        /**
         * generating location near Gate 1 or 2
         */
        Coord outCord=  gateEntry().get(gateNo);
        if(outCord==null) throw new RuntimeException("No coordinate is not found for the zone"+gateNo);
        outCord= new Coord(outCord.getX()+random.nextInt(addBufferDistance) , outCord.getY()+random.nextInt(addBufferDistance));
        return outCord;
    }

    private Coord workCoordinate() {
        /**
         * adding random destination points
         */
        ArrayList<Node> nodes = new ArrayList<>(network.getNodes().values());
        int b = rand.nextInt(nodes.size());
        return nodes.get(b).getCoord();
    }

    private Map<String, Coord> gateEntry(){
        Map<String, Coord> gate2Coord =new HashMap<>();
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, NetworkIITR.IITRCoordinate);
        Coord gate1 = new Coord ((double) 77.8922221, (double) 29.8719055);
        Coord gate2 = new Coord ((double) 77.8997401, (double) 29.8693905);
        Coord coord1 = ct.transform(gate1);
        Coord coord2 = ct.transform(gate2);
        gate2Coord.put("1",coord1);
        gate2Coord.put("2", coord2);
        return gate2Coord;
    }

    private double getRandomEndTime(double start, double timebin){
        return start + random.nextInt((int) timebin);
    }

    private Coord getRandomElement(List<Coord> list){
        return list.get(rand.nextInt(list.size()));
    }
}
