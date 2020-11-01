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

import com.opencsv.ICSVWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Amit on 25/10/2020
 */
public class DemandFromOD {

    public static final String toCoordinateSystem  = "EPSG:32643";
    private static final double startTime = 8*3600.;
    private static final double timebin = 2*3600.;
    private static final Random random = MatsimRandom.getLocalInstance();
    private static final String mode = "bus";

    private static final Map<String, Coord> busStopToCoordinate = new HashMap<>();

    public static void main(String[] args) {

        //String odMatrix = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhiMalviyaNagar_PT/2016-10_MalviyaNagarODSurveyData.txt";
        String odMatrix ="C:\\Users\\Nidhi\\Desktop\\MalviyaNagar_PT\\Files\\2016-10_MalviyaNagarODSurveyData.txt";
        String coordinatesFile = "C:\\Users\\Nidhi\\Desktop\\MalviyaNagar_PT\\Files\\Book2.csv";
        String out_plansOD="C:\\Users\\Nidhi\\Desktop\\MalviyaNagar_PT\\Files\\DemandOD.xml.gz";
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        PopulationFactory factory = scenario.getPopulation().getFactory();
        

        BufferedReader reader = IOUtils.getBufferedReader(odMatrix);
        parseCoordinateFile(coordinatesFile); 
       
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
                            Coord coordFromZone = getCoordFromZone(origin);
							Activity originAct = factory.createActivityFromCoord("origin", coordFromZone);
                            originAct.setEndTime(getRandomEndTime(startTime, timebin));
                            Leg leg = factory.createLeg(TransportMode.car);
                            plan.addActivity(originAct);
                            plan.addLeg(leg);
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
        
        
        new PopulationWriter(population).write(out_plansOD);
    }
    
    
    
 public static void parseCoordinateFile(String coordinateFile) {
	 //reading the file
	BufferedReader reader = IOUtils.getBufferedReader(coordinateFile);
	 CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, toCoordinateSystem);
	try {
		String line = reader.readLine();
			boolean isHeader = true;
		while(line!=null) {
			
			if (isHeader) {
				isHeader = false;

			} else {
				String parts [] = line.split(",");
				String index = parts[0];
				Coord cord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
				// transform the cord in to the correct EPSG
				 Coord transformCoord = ct.transform(cord);
				 busStopToCoordinate.put(index, transformCoord);
			}
			
			
			line = reader.readLine();
		}
		
		
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	 
 }
    
    
    private static Coord getCoordFromZone(String zone) {
    	Coord outCord = busStopToCoordinate.get(zone);
        return outCord;
     
    }

    private static double getRandomEndTime(double start, double timebin){
        return start + random.nextInt((int) timebin);
    }

	
}
