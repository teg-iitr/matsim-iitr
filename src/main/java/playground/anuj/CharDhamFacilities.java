package playground.anuj;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.*;

import java.io.BufferedReader;
import java.io.IOException;

public class CharDhamFacilities {
    private static final String FACILITIES_INPUT_CSV = "input/facilities_uk.csv";
    private static final String FACILITIES_OUTPUT_XML = "output/facilities_charDham.xml";

    public static void main(String[] args) {
        new CharDhamFacilities().create();
    }

    public void create() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        ActivityFacilities facilities = scenario.getActivityFacilities();
        ActivityFacilitiesFactory factory = facilities.getFactory();

        try (BufferedReader reader = IOUtils.getBufferedReader(FACILITIES_INPUT_CSV)) {
            String line = reader.readLine(); // Skip header: "id,x,y,capacity"

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String id = parts[0].trim();
                Coord coord = new Coord(Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim()));
                int capacity = Integer.parseInt(parts[3].trim());

                createRestFacility(factory, facilities, id, coord, capacity);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading facilities CSV file.", e);
        }

        new FacilitiesWriter(facilities).write(FACILITIES_OUTPUT_XML);
        System.out.println("✔ Facilities file created successfully at: " + FACILITIES_OUTPUT_XML);
    }

    private void createRestFacility(ActivityFacilitiesFactory factory, ActivityFacilities facilities, String id, Coord location, int capacity) {
        // Create a facility at the given location
        ActivityFacility facility = factory.createActivityFacility(Id.create(id, ActivityFacility.class), location);

        // Define the "rest" activity for this facility
        ActivityOption restActivity = factory.createActivityOption("rest");
        restActivity.setCapacity(capacity);

        // This facility is open 24 hours, so no opening times are needed.

        // Add the "rest" activity option to the facility
        facility.addActivityOption(restActivity);

        // Add the completed facility to the scenario's facilities container
        facilities.addActivityFacility(facility);
    }
}
