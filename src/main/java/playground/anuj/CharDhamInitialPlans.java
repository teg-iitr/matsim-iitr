package playground.anuj;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.amit.Dehradun.DehradunUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class CharDhamInitialPlans {

    public static final String importantLocationsFile = "input/anuj/UKCharDhamPrimeLocations.txt";
    public static final String outputPlansFile = "output/plan_charDham.xml";
    private final Map<String, Coord> location2Coord = new HashMap<>();
    private static final double REST_STOP_DURATION = 2.0 * 3600.0;     // 2 hours for a chosen rest stop
    private static final double DHAM_VISIT_DURATION = 6.0 * 3600.0;      // 3 hours for darshan/visit
    private static final double OVERNIGHT_STAY_DURATION = 12.0 * 3600.0; // 12 hours for an overnight halt
    public static final String PASSENGER_ATTRIBUTE = "numberOfPassengers";

    public static void main(String[] args) {
        new CharDhamInitialPlans().run();
    }

    public void run() {
        // Load coordinates for important locations
        storeLocations();

        // Initialize MATSim scenario
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        final String CAR_MODE = "car";
        final String MOTORBIKE_MODE = "motorbike";


        for (int i = 1; i <= 3; i++) {
            List<String> dhamSequence;
            String primaryMode;
            String personIdString;
            double startActivityEndTime;
            int numberOfPassengers;

            if (i == 1) { // Persons 1 to 5
                personIdString = "CarCharDham_" + i;
                startActivityEndTime = 8.0 * 3600.0; // 8:00 AM
                dhamSequence = List.of("Yamunotri", "Gangotri", "Kedarnath", "Badrinath");
                primaryMode = CAR_MODE;
                numberOfPassengers = 5;
            } else if (i == 2) { // Person 6 to 9
                personIdString = "CarDoDham_" + i;
                startActivityEndTime = 16.0 * 3600.0; // 10:00 AM
                dhamSequence = List.of("Kedarnath", "Badrinath");
                primaryMode = CAR_MODE;
                numberOfPassengers = 4;
            } else { // Person 10
                personIdString = "MotorbikeTeenDham_" + i;
                startActivityEndTime = 22.0 * 3600.0; // 10:00 PM
                dhamSequence = List.of("Gangotri", "Kedarnath", "Badrinath");
                primaryMode = MOTORBIKE_MODE;
                numberOfPassengers = 1;
            }
            // Start activity in Haridwar
            Person person = populationFactory.createPerson(Id.createPersonId(personIdString));
            Plan plan = populationFactory.createPlan();
            plan.getAttributes().putAttribute(PASSENGER_ATTRIBUTE, numberOfPassengers);

            // Start activity in Haridwar with the assigned custom end time
            addActivityWithEndTime(populationFactory, plan, "Haridwar", location2Coord.get("Haridwar"), startActivityEndTime);

            // Add dham visits based on sequence
            for (String dham : dhamSequence) {
                addLeg(populationFactory, plan, primaryMode);
                addLocationChoiceActivity(populationFactory, plan, "rest", location2Coord.get("Haridwar"), REST_STOP_DURATION);
                if (dham.equals("Yamunotri")) {
                    addLeg(populationFactory, plan, primaryMode);
                    // Stop at Barkot for a meal before proceeding.
                    addActivityWithDuration(populationFactory, plan, "visit-Barkot", location2Coord.get("Barkot"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);
                    // Visit Yamunotri itself.
                    addActivityWithDuration(populationFactory, plan, "visit-Yamunotri", location2Coord.get("Yamunotri"), DHAM_VISIT_DURATION);
                } else if (dham.equals("Gangotri")) {
                    addLeg(populationFactory, plan, primaryMode);
                    // Stop at Uttarkashi for a rest.
                    addActivityWithDuration(populationFactory, plan, "visit-Uttarkashi", location2Coord.get("Uttarkashi"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);
                    // Visit Gangotri.
                    addActivityWithDuration(populationFactory, plan, "visit-Gangotri", location2Coord.get("Gangotri"), DHAM_VISIT_DURATION);
                } else if (dham.equals("Kedarnath")) {
                    addLeg(populationFactory, plan, primaryMode);
                    addActivityWithDuration(populationFactory, plan, "visit-Srinagar", location2Coord.get("Srinagar"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);
                    // Overnight halt at Sonprayag before the trek.
                    addActivityWithDuration(populationFactory, plan, "visit-Sonprayag", location2Coord.get("Sonprayag"), OVERNIGHT_STAY_DURATION);
                    addLeg(populationFactory, plan, primaryMode); // Sonprayag to Gaurikund by local taxi
                    addActivityWithDuration(populationFactory, plan, "visit-Gaurikund", location2Coord.get("Gaurikund"), REST_STOP_DURATION); // Short stop to start trek
                    addLeg(populationFactory, plan, TransportMode.walk);
                    addActivityWithDuration(populationFactory, plan, "visit-Kedarnath", location2Coord.get("Kedarnath"), DHAM_VISIT_DURATION);
                } else if (dham.equals("Badrinath")) {
                    addLeg(populationFactory, plan, primaryMode);
                    addActivityWithDuration(populationFactory, plan, "visit-Joshimath", location2Coord.get("Joshimath"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);
                    // Visit Badrinath.
                    addActivityWithDuration(populationFactory, plan, "visit-Badrinath", location2Coord.get("Badrinath"), DHAM_VISIT_DURATION);
                }
            }
            addLeg(populationFactory, plan, primaryMode);
            // The final activity has no end time, lasting until the simulation ends.
            addFinalActivity(populationFactory, plan, "Haridwar", location2Coord.get("Haridwar"));

            // Add the completed plan to the person and the person to the population
            person.addPlan(plan);
            population.addPerson(person);
        }

        // Write population to output file
        new PopulationWriter(population).write(outputPlansFile);
    }

    private void addLocationChoiceActivity(PopulationFactory factory, Plan plan, String type, Coord coord, double durationInSeconds) {
        // null coordinate signals to MATSim that a location needs to be chosen
        Activity activity = factory.createActivityFromCoord(type, coord);
        activity.setMaximumDuration(durationInSeconds);
        plan.addActivity(activity);
    }

    private void storeLocations() {
        // Load coordinates from file with transformation
        try (BufferedReader reader = IOUtils.getBufferedReader(importantLocationsFile)) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                Coord originalCoord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[1]));
                Coord transformedCoord = DehradunUtils.transformation.transform(originalCoord);
                location2Coord.put(parts[0].trim(), transformedCoord);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading location file: " + e.getMessage());
        }
    }

    private void addActivityWithEndTime(PopulationFactory factory, Plan plan, String type, Coord coord, double endTime) {
        Activity activity = factory.createActivityFromCoord(type, coord);
        activity.setEndTime(endTime);
        plan.addActivity(activity);
    }

    private void addActivityWithDuration(PopulationFactory factory, Plan plan, String type, Coord coord, double durationInSeconds) {
        Activity activity = factory.createActivityFromCoord(type, coord);
        activity.setMaximumDuration(durationInSeconds);
        plan.addActivity(activity);
    }

    private void addFinalActivity(PopulationFactory factory, Plan plan, String type, Coord coord) {
        Activity activity = factory.createActivityFromCoord(type, coord);
        plan.addActivity(activity);
    }

    private void addLeg(PopulationFactory factory, Plan plan, String mode) {
        Leg leg = factory.createLeg(mode);
        plan.addLeg(leg);
    }


}
