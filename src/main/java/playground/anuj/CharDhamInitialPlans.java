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
    public static final String outputPlansFile = "output/plan_charDham_updated_v2.xml"; // Updated output file name
    private final Map<String, Coord> location2Coord = new HashMap<>();
    public static final String PASSENGER_ATTRIBUTE = "numberOfPassengers";

    // Reintroduced duration constants as requested
    private static final double REST_STOP_DURATION = 2.0 * 3600.0;     // 2 hours for a chosen rest stop
    private static final double DHAM_VISIT_DURATION = 6.0 * 3600.0;      // 6 hours for darshan/visit
    private static final double OVERNIGHT_STAY_DURATION = 12.0 * 3600.0; // 12 hours for an overnight halt

    private final Random random = new Random(); // For random choices

    // Constants for modes
    public static final String CAR_MODE = "car";
    public static final String MOTORBIKE_MODE = "motorbike";
    public static final String TRAVELLER_MODE = "traveller";
    public static final String WALK_MODE = TransportMode.walk; // For treks

    // Dham weights for generating sequences
    private final Map<List<String>, Double> dhamWeights = new HashMap<>();

    private static final int NUM_AGENTS = 1000; // Number of agents to generate

    public CharDhamInitialPlans() {
        // Initialize dham weights based on typical pilgrimage patterns
        // Size 1 combinations
        dhamWeights.put(List.of("Yamunotri"), 0.006);
        dhamWeights.put(List.of("Gangotri"), 0.01);
        dhamWeights.put(List.of("Kedarnath"), 0.19);
        dhamWeights.put(List.of("Badrinath"), 0.12);

        // Size 2 combinations
        dhamWeights.put(List.of("Yamunotri", "Gangotri"), 0.5);
        dhamWeights.put(List.of("Yamunotri", "Kedarnath"), 0.2);
        dhamWeights.put(List.of("Yamunotri", "Badrinath"), 0.2);
        dhamWeights.put(List.of("Gangotri", "Kedarnath"), 0.1);
        dhamWeights.put(List.of("Gangotri", "Badrinath"), 0.2);
        dhamWeights.put(List.of("Kedarnath", "Badrinath"), 0.6);

        // Size 3 combinations
        dhamWeights.put(List.of("Yamunotri", "Gangotri", "Kedarnath"), 0.5);
        dhamWeights.put(List.of("Yamunotri", "Gangotri", "Badrinath"), 0.2);
        dhamWeights.put(List.of("Yamunotri", "Kedarnath", "Badrinath"), 0.1);
        dhamWeights.put(List.of("Gangotri", "Kedarnath", "Badrinath"), 0.5);

        // Size 4 combinations
        dhamWeights.put(List.of("Yamunotri", "Gangotri", "Kedarnath", "Badrinath"), 0.8);
    }

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

        // Generate all possible dham combinations (including Hemkund Sahib)
        List<List<String>> allDhamCombinations = generateDhamCombinations(
                Arrays.asList("Yamunotri", "Gangotri", "Kedarnath", "Badrinath"));

        for (int i = 0; i < NUM_AGENTS; i++) {
            String personIdString = "CharDhamPilgrim_" + i;
            Person person = populationFactory.createPerson(Id.createPersonId(personIdString));
            Plan plan = populationFactory.createPlan();

            String primaryMode;
            int numberOfPassengers;

            // Randomly choose primary mode (e.g., 60% car, 20% motorbike, 20% traveller)
            double modeChoice = random.nextDouble();
            if (modeChoice < 0.60) { // 60% chance for CAR_MODE
                primaryMode = CAR_MODE;
                numberOfPassengers = random.nextInt(2) + 4; // 4 or 5 passengers
            } else if (modeChoice < 0.60 + 0.25) { // 25% chance for MOTORBIKE_MODE (0.60 to 0.85)
                primaryMode = MOTORBIKE_MODE;
                numberOfPassengers = random.nextInt(2) + 1; // 1 or 2 passengers
            } else { // 15% chance for TRAVELLER_MODE (0.85 to 1.0)
                primaryMode = TRAVELLER_MODE;
                numberOfPassengers = random.nextInt(17) + 10; // 10 to 26 passengers (26 - 10 + 1 = 17)
            }
            plan.getAttributes().putAttribute(PASSENGER_ATTRIBUTE, numberOfPassengers);
            // Start activity in Haridwar with a random end time between 4 AM and 8 AM on day 1
            addActivityWithEndTime(populationFactory, plan, "Haridwar", location2Coord.get("Haridwar"), randomEndTime(4, 24, random.nextInt(4) + 1));

            // Get a random dham sequence based on weights
            List<String> dhamSequence = getRandomDhamSequence(allDhamCombinations, dhamWeights);

            // Add dham visits based on sequence
            for (String dham : dhamSequence) {
                // Add a leg before each new dham visit (using the primary mode)
                addLeg(populationFactory, plan, primaryMode);
                addLocationChoiceActivity(populationFactory, plan, "rest", location2Coord.get("Haridwar"), REST_STOP_DURATION);
                addLeg(populationFactory, plan, primaryMode);
                if (dham.equals("Kedarnath")) {
                    // Stop at Srinagar for rest
                    addActivityWithDuration(populationFactory, plan, "visit-Srinagar", location2Coord.get("Srinagar"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);

                    // Overnight halt at Sonprayag before the trek, departing next morning
                    addActivityWithDuration(populationFactory, plan, "visit-Sonprayag", location2Coord.get("Sonprayag"), REST_STOP_DURATION);

                    addLeg(populationFactory, plan, primaryMode); // Sonprayag to Gaurikund by local taxi

                    addActivityWithDuration(populationFactory, plan, "visit-Gaurikund", location2Coord.get("Gaurikund"), REST_STOP_DURATION); // Short stop to start trek
                    addLeg(populationFactory, plan, WALK_MODE); // Gaurikund to Kedarnath trek

                    addActivityWithDuration(populationFactory, plan, "visit-Kedarnath", location2Coord.get("Kedarnath"), DHAM_VISIT_DURATION); // Visit duration
                    addLeg(populationFactory, plan, WALK_MODE); // Kedarnath to Gaurikund trek

                    addActivityWithDuration(populationFactory, plan, "visit-Gaurikund", location2Coord.get("Gaurikund"), REST_STOP_DURATION); // Short stop after trek
                    addLeg(populationFactory, plan, primaryMode); // Gaurikund to Sonprayag by local taxi/vehicle

                    // Another overnight halt at Sonprayag on return, departing next morning
                    addActivityWithDuration(populationFactory, plan, "visit-Sonprayag", location2Coord.get("Sonprayag"), REST_STOP_DURATION);

                } else if (dham.equals("Gangotri")) {
                    // Intermediate stop in Uttarkashi
                    addActivityWithDuration(populationFactory, plan, "visit-Uttarkashi", location2Coord.get("Uttarkashi"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);

                    addActivityWithDuration(populationFactory, plan, "visit-Gangotri", location2Coord.get("Gangotri"), DHAM_VISIT_DURATION);
                } else if (dham.equals("Yamunotri")) {
                    // Intermediate stop in Barkot
                    addActivityWithDuration(populationFactory, plan, "visit-Barkot", location2Coord.get("Barkot"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);

                    addActivityWithDuration(populationFactory, plan, "visit-Yamunotri", location2Coord.get("Yamunotri"), DHAM_VISIT_DURATION);
                } else if (dham.equals("Badrinath")) {
                    // Intermediate stop in Joshimath
                    addActivityWithDuration(populationFactory, plan, "visit-Joshimath", location2Coord.get("Joshimath"), REST_STOP_DURATION);
                    addLeg(populationFactory, plan, primaryMode);

                    addActivityWithDuration(populationFactory, plan, "visit-Badrinath", location2Coord.get("Badrinath"), DHAM_VISIT_DURATION);
                }
            }
            // Return to Haridwar
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

    private double randomEndTime(int minHour, int maxHour, int dayOffset) {
        // Calculate start and end seconds for the random range on the specific day
        int startSeconds = (dayOffset - 1) * 24 * 3600 + minHour * 3600;
        int endSeconds = (dayOffset - 1) * 24 * 3600 + maxHour * 3600;

        // Ensure endSeconds is strictly greater than startSeconds for random.nextDouble()
        if (endSeconds <= startSeconds) {
            // If the range is invalid or zero, return the start of the range
            return startSeconds;
        }

        return startSeconds + random.nextDouble() * (endSeconds - startSeconds);
    }


    private List<String> getRandomDhamSequence(List<List<String>> combinations, Map<List<String>, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double rand = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;

        for (List<String> combination : combinations) {
            // Use getOrDefault to assign a small default weight to combinations not explicitly listed
            double weight = weights.getOrDefault(combination, 0.0001);
            cumulativeWeight += weight;
            if (rand <= cumulativeWeight) {
                return combination;
            }
        }
        // Fallback in case of floating point inaccuracies or if no combination is selected
        return combinations.get(random.nextInt(combinations.size()));
    }

    private List<List<String>> generateDhamCombinations(List<String> dhams) {
        List<List<String>> combinations = new ArrayList<>();
        for (int i = 1; i <= dhams.size(); i++) {
            combinations.addAll(getCombinations(dhams, i));
        }
        return combinations;
    }

//    private void addActivity(PopulationFactory factory, Plan plan, String type, Coord coord, double endTime) {
//        Activity activity = factory.createActivityFromCoord(type, coord);
//        if (endTime > 0) {
//            activity.setEndTime(endTime);
//        }
//        plan.addActivity(activity);
//    }

    private List<List<String>> getCombinations(List<String> dhams, int size) {
        if (size == 0) return List.of(Collections.emptyList());
        if (dhams.isEmpty()) return List.of();

        List<List<String>> combinations = new ArrayList<>();
        String first = dhams.get(0);
        List<String> rest = dhams.subList(1, dhams.size());

        // Include first element
        for (List<String> smaller : getCombinations(rest, size - 1)) {
            List<String> combination = new ArrayList<>();
            combination.add(first);
            combination.addAll(smaller);
            combinations.add(combination);
        }

        // Exclude first element
        combinations.addAll(getCombinations(rest, size));

        return combinations;
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
