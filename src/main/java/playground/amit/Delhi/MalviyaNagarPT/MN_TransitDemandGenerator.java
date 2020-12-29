package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Amit on 25/10/2020
 */
public class MN_TransitDemandGenerator {

	public static final String toCoordinateSystem  = "EPSG:32643";

	private final String mode = TransportMode.pt;
    private final double startTime = 8*3600.;
    private final double timebin = 2*3600.;
    private final Random random = MatsimRandom.getLocalInstance();
    private final int addBufferCloaseToPTDistance = 10;

	private final Map<String, Coord> busStopToCoordinate = new HashMap<>();

	public static void main(String[] args) {
		new MN_TransitDemandGenerator().run();
	}

	public void run() {
		String coordinatesFile = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/PT_stops_coordinates_links.csv";
		parseCoordinateFile(coordinatesFile);

		String odMatrix = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/2016-10_MalviyaNagarODSurveyData.txt";
		String out_plansOD = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/MN_transitDemand_2020-11-01.xml.gz";

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

							Coord coordFromZone = getCoordFromZone(origin, addBufferCloaseToPTDistance);
							Activity originAct = factory.createActivityFromCoord("origin", coordFromZone);
							originAct.setEndTime(getRandomEndTime(startTime, timebin));
							plan.addActivity(originAct);

                            Leg leg = factory.createLeg(mode);
							plan.addLeg(leg);

							Activity destAct = factory.createActivityFromCoord("destination", getCoordFromZone(destination, addBufferCloaseToPTDistance));
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
		new PopulationWriter(population).write(out_plansOD);
	}

	public void parseCoordinateFile(String coordinateFile) {
		BufferedReader reader = IOUtils.getBufferedReader(coordinateFile);
		 CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, toCoordinateSystem);
		try {
			String line = reader.readLine();
				boolean isHeader = true;
			while(line!=null) {
				if (isHeader) {
					isHeader = false;
				} else {
					String [] parts = line.split(",");
					String index = parts[0];
					Coord cord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
					Coord transformCoord = ct.transform(cord);
					busStopToCoordinate.put(index, transformCoord);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not read. Reason "+e);
		}
	}

	private Coord getCoordFromZone(String zone, int addBufferDistance_m) {
		Coord outCord = busStopToCoordinate.get(zone);
        if(outCord==null) throw new RuntimeException("No coordinate is not found for the zone/ stop ID "+zone);

		outCord = new Coord(outCord.getX()+random.nextInt(addBufferDistance_m) , outCord.getY()+random.nextInt(addBufferDistance_m));
		return outCord;
	}

	private double getRandomEndTime(double start, double timebin){
		return start + random.nextInt((int) timebin);
	}
}
