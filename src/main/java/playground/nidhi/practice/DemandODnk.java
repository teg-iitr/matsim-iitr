package playground.nidhi.practice;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class DemandODnk {
	public static final String toCoordinateSystem  = "EPSG:32643";
    private static final double startTime = 8*3600.;
    private static final double timebin = 2*3600.;
    private static final Random random = MatsimRandom.getLocalInstance();
    private static final String mode = "bus";

    public static void main(String[] args) {

        String odMatrix = "C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\2016-10_MalviyaNagarODSurveyData.txt";
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        PopulationFactory factory = scenario.getPopulation().getFactory();

        BufferedReader reader = IOUtils.getBufferedReader(odMatrix);
        try {
            String line = reader.readLine();
            String [] origins = null;
            while(line!=null) {
                String [] parts = line.split("\t");
                if (origins==null) { // origins --> store as labels
                    origins = parts;
                } else {
                    String destination = parts[0];
                    for(int index = 1; index < origins.length ; index++){
                        String origin = origins[index];
                        int trips = Integer.parseInt(parts[index]);
                        for (int j = 0 ; j < trips ; j++){
                            Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
                            Plan plan = factory.createPlan();
                            Activity originAct = factory.createActivityFromCoord("origin", getCoordFromZone(origin));
                            originAct.setEndTime(getRandomEndTime(startTime, timebin));
                            plan.addActivity(originAct);
                            Leg leg = factory.createLeg(mode);
                            Activity destAct = factory.createActivityFromCoord("destination", getCoordFromZone(destination));
                            plan.addActivity(destAct);
                            person.addPlan(plan);
                            population.addPerson(person);
                        }
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }

    private static Coord getCoordFromZone(String zone){
        //TODO fix this
        return null;
    }

    private static double getRandomEndTime(double start, double timebin){
        return start + random.nextInt((int) timebin);
    }
}
