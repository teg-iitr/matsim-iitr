package playground.anuj.charDham.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DehradunUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static playground.anuj.charDham.runner.RunCharDhamSingleSimulation.*;

public class CharDhamInitialPlans {

    public static final String importantLocationsFile = "input/anuj/UKCharDhamPrimeLocations.txt";
    public static final String outputPlansFile = "output/plan_charDham_updated_v3.xml"; // Updated output file name
    private final Map<String, Coord> location2Coord = new HashMap<>();
    public static final String PASSENGER_ATTRIBUTE = "numberOfPassengers";

    // Reintroduced duration constants as requested
    private static final double REST_STOP_DURATION = 3.0 * 3600.0;     // 2 hours for a chosen rest stop
    private static final double DHAM_VISIT_DURATION = 6.0 * 3600.0;      // 6 hours for darshan/visit
    private static final double OVERNIGHT_STAY_DURATION = 12.0 * 3600.0; // 12 hours for an overnight halt

    public static final double SAMPLE_SIZE = 0.001;

    private final Random random = new Random(); // For random choices

    // New: CSV file for dham activity chains and weights
    private static final String DHAM_CHAIN_FREQUENCIES_FILE = "input/anuj/dham_activity_chain_frequencies.csv";
    private List<DhamChainData> dhamChainDataList; // To store parsed CSV data
    private int NUM_AGENTS; // Will be calculated from CSV counts (total passengers)
    private static final int DEFAULT_TOP_ROWS_TO_READ = 25; // Default number of top rows to consider

    /**
     * Inner class to hold data from each row of the dham_activity_chain_frequencies.csv.
     * Implements Comparable to allow sorting by count in descending order.
     */
    private static class DhamChainData implements Comparable<DhamChainData> {
        List<String> activityChain;
        int count;
        double weight;

        DhamChainData(List<String> activityChain, int count, double weight) {
            this.activityChain = activityChain;
            this.count = count;
            this.weight = weight;
        }

        @Override
        public int compareTo(DhamChainData other) {
            // Sort in descending order of count
            return Integer.compare(other.count, this.count);
        }
    }

    public CharDhamInitialPlans() {
    }

    public static void main(String[] args) {
        new CharDhamInitialPlans().run();
    }

    public void run() {
        // Load coordinates for important locations
        storeLocations();

        // Read Dham activity chains and calculate NUM_AGENTS (total passengers)
        readDhamActivityChains(DEFAULT_TOP_ROWS_TO_READ);

        if (NUM_AGENTS == 0) {
            System.err.println("No agents (passengers) to generate. Check " + DHAM_CHAIN_FREQUENCIES_FILE + " for valid data or adjust DEFAULT_TOP_ROWS_TO_READ.");
            return;
        }

        // Initialize MATSim scenario
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        int currentPersonIdCounter = 0;
        int totalPassengersGenerated = 0;
        int numberOfPlansGenerated = 0;

        // Loop until all target passengers (NUM_AGENTS) have been assigned to a plan
        while (totalPassengersGenerated < NUM_AGENTS) {
            String primaryMode;
            int minPassengers;
            int maxPassengers;

            // Randomly choose primary mode based on fixed percentages
            double modeChoice = random.nextDouble();
            if (modeChoice < 0.30) {
                primaryMode = CAR_MODE;
                minPassengers = 1;
                maxPassengers = 5;
            } else if (modeChoice < 0.55) {
                primaryMode = TAXI_MODE;
                minPassengers = 3;
                maxPassengers = 8;
            } else if (modeChoice < 0.65) {
                primaryMode = MOTORBIKE_MODE;
                minPassengers = 1;
                maxPassengers = 2;
            } else if (modeChoice < 0.80) {
                primaryMode = MINI_BUS_MODE;
                minPassengers = 15;
                maxPassengers = 25;
            } else { // 20% chance for BUS_MODE
                primaryMode = BUS_MODE;
                minPassengers = 20;
                maxPassengers = 35;
            }

            // Determine actual number of passengers for this vehicle/plan
            int passengersForThisVehicle = random.nextInt(maxPassengers - minPassengers + 1) + minPassengers;
            // Ensure we don't exceed the total NUM_AGENTS target
            passengersForThisVehicle = Math.min(passengersForThisVehicle, NUM_AGENTS - totalPassengersGenerated);

            if (passengersForThisVehicle == 0) {
                // This can happen if remaining passengers is less than minPassengers for any mode.
                // Break the loop if no more passengers can be assigned.
                break;
            }
            String personIdString = "Pilgrim_" + primaryMode  + "_" + currentPersonIdCounter++ ;
            Person person = populationFactory.createPerson(Id.createPersonId(personIdString));
            Plan plan = populationFactory.createPlan();

            plan.getAttributes().putAttribute(PASSENGER_ATTRIBUTE, passengersForThisVehicle);
            totalPassengersGenerated += passengersForThisVehicle;
            numberOfPlansGenerated++; // Increment count of generated plans/vehicles

            // Start activity in Haridwar with a random end time between 12 AM and 12 AM on day 1
            addActivityWithEndTime(populationFactory, plan, "Haridwar", location2Coord.get("Haridwar"), randomEndTime(0, 24, random.nextInt(1) + 1));

            // Get a random dham sequence based on weights from CSV
            List<String> dhamSequence = getRandomDhamSequence();

            // Add dham visits based on sequence
            for (String dham : dhamSequence) {
                // Add a leg before each new dham visit (using the primary mode)
                addLeg(populationFactory, plan, primaryMode);
                addLocationChoiceActivity(populationFactory, plan, "rest", location2Coord.get("Haridwar"), REST_STOP_DURATION);
                addLeg(populationFactory, plan, primaryMode);

                switch (dham) {
                    case "Kedarnath" -> {
                        // Stop at Srinagar for rest
                        addActivityWithDuration(populationFactory, plan, "Srinagar", location2Coord.get("Srinagar"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        // Overnight halt at Sonprayag before the trek, departing next morning
                        addActivityWithDuration(populationFactory, plan, "Sonprayag", location2Coord.get("Sonprayag"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode); // Sonprayag to Gaurikund by local taxi

                        addActivityWithDuration(populationFactory, plan, "Gaurikund", location2Coord.get("Gaurikund"), REST_STOP_DURATION); // Short stop to start trek
                        addLeg(populationFactory, plan, WALK_MODE); // Gaurikund to Kedarnath trek

                        addActivityWithDuration(populationFactory, plan, "Kedarnath", location2Coord.get("Kedarnath"), DHAM_VISIT_DURATION); // Visit duration
                        addLeg(populationFactory, plan, WALK_MODE); // Kedarnath to Gaurikund trek

                        addActivityWithDuration(populationFactory, plan, "Gaurikund", location2Coord.get("Gaurikund"), REST_STOP_DURATION); // Short stop after trek
                        addLeg(populationFactory, plan, primaryMode); // Gaurikund to Sonprayag by local taxi/vehicle

                        // Another overnight halt at Sonprayag on return, departing next morning
                        addActivityWithDuration(populationFactory, plan, "Sonprayag", location2Coord.get("Sonprayag"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Srinagar", location2Coord.get("Srinagar"), REST_STOP_DURATION);
                    }
                    case "Gangotri" -> {
                        // Intermediate stop in Uttarkashi
                        addActivityWithDuration(populationFactory, plan, "Uttarkashi", location2Coord.get("Uttarkashi"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Gangotri", location2Coord.get("Gangotri"), DHAM_VISIT_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Uttarkashi", location2Coord.get("Uttarkashi"), REST_STOP_DURATION);
                    }
                    case "Yamunotri" -> {
                        // Intermediate stop in Barkot
                        addActivityWithDuration(populationFactory, plan, "Barkot", location2Coord.get("Barkot"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Yamunotri", location2Coord.get("Yamunotri"), DHAM_VISIT_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Barkot", location2Coord.get("Barkot"), REST_STOP_DURATION);
                    }
                    case "Badrinath" -> {
                        // Intermediate stop in Joshimath
                        addActivityWithDuration(populationFactory, plan, "Joshimath", location2Coord.get("Joshimath"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Badrinath", location2Coord.get("Badrinath"), DHAM_VISIT_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Joshimath", location2Coord.get("Joshimath"), REST_STOP_DURATION);
                    }
                    case "Hemkund_Sahib" -> {
                        // Intermediate stop in Joshimath
                        addActivityWithDuration(populationFactory, plan, "GovindGhat", location2Coord.get("GovindGhat"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "Ghangaria", location2Coord.get("Ghangaria"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, WALK_MODE);

                        addActivityWithDuration(populationFactory, plan, "Hemkund_Sahib", location2Coord.get("Hemkund_Sahib"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, WALK_MODE);

                        addActivityWithDuration(populationFactory, plan, "Ghangaria", location2Coord.get("Ghangaria"), REST_STOP_DURATION);
                        addLeg(populationFactory, plan, primaryMode);

                        addActivityWithDuration(populationFactory, plan, "GovindGhat", location2Coord.get("GovindGhat"), REST_STOP_DURATION);
                    }
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
        PopulationUtils.sampleDown(population, SAMPLE_SIZE);
        new PopulationWriter(population).write(outputPlansFile);
        System.out.println("Generated " + numberOfPlansGenerated + " plans (vehicles) for a total of " + totalPassengersGenerated + " passengers, saved to " + outputPlansFile);
    }

    /**
     * Reads Dham activity chain frequencies and weights from a CSV file.
     * Populates `dhamChainDataList` with the top `numRowsToRead` entries by count
     * and calculates `NUM_AGENTS` (total passengers) based on these selected entries.
     * @param numRowsToRead The number of top rows (by count) to read from the CSV.
     */
    private void readDhamActivityChains(int numRowsToRead) {
        List<DhamChainData> allDhamChainData = new ArrayList<>();
        try (BufferedReader br = IOUtils.getBufferedReader(DHAM_CHAIN_FREQUENCIES_FILE)) {
            String headerLine = br.readLine(); // Read and skip header
            if (headerLine == null) {
                throw new IOException("CSV file is empty: " + DHAM_CHAIN_FREQUENCIES_FILE);
            }
            String[] headers = headerLine.split(",");
            int activityChainCol = -1;
            int countCol = -1;
            int weightCol = -1;

            // Find column indices
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                if ("activity_chain".equalsIgnoreCase(header)) {
                    activityChainCol = i;
                } else if ("count".equalsIgnoreCase(header)) {
                    countCol = i;
                } else if ("weight".equalsIgnoreCase(header)) {
                    weightCol = i;
                }
            }

            if (activityChainCol == -1 || countCol == -1 || weightCol == -1) {
                throw new IOException("Missing one or more required columns (activity_chain, count, weight) in CSV: " + DHAM_CHAIN_FREQUENCIES_FILE);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines
                String[] parts = line.split(",");

                // Basic check for sufficient columns
                if (parts.length <= Math.max(activityChainCol, Math.max(countCol, weightCol))) {
                    System.err.println("Skipping malformed line (not enough columns): " + line);
                    continue;
                }

                try {
                    // Parse activity chain string into a List<String>
                    List<String> activityChain = Arrays.stream(parts[activityChainCol].trim().split(" - "))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    int count = Integer.parseInt(parts[countCol].trim());
                    double weight = Double.parseDouble(parts[weightCol].trim());

                    if (count < 0 || weight < 0) {
                        System.err.println("Skipping line with negative count or weight: " + line);
                        continue;
                    }

                    allDhamChainData.add(new DhamChainData(activityChain, count, weight));

                } catch (NumberFormatException e) {
                    System.err.println("Skipping line due to number format error in " + DHAM_CHAIN_FREQUENCIES_FILE + ": " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading Dham activity chain frequencies file: " + DHAM_CHAIN_FREQUENCIES_FILE, e);
        }

        // Sort all data by count in descending order
        Collections.sort(allDhamChainData);

        // Take only the top N rows
        this.dhamChainDataList = allDhamChainData.stream()
                .limit(numRowsToRead)
                .collect(Collectors.toList());

        // Calculate NUM_AGENTS (total passengers) from the counts of the selected top N rows
        this.NUM_AGENTS = this.dhamChainDataList.stream().mapToInt(d -> d.count).sum();

        System.out.println("Read " + allDhamChainData.size() + " activity chains from " + DHAM_CHAIN_FREQUENCIES_FILE + ".");
        System.out.println("Considering top " + this.dhamChainDataList.size() + " chains (requested: " + numRowsToRead + "). Target total passengers: " + NUM_AGENTS);
    }

    /**
     * Selects a random Dham sequence from the loaded `dhamChainDataList` based on their weights.
     * @return A List of Dham names representing the chosen sequence.
     */
    private List<String> getRandomDhamSequence() {
        if (dhamChainDataList.isEmpty()) {
            return Collections.emptyList(); // Should be caught earlier by NUM_AGENTS check
        }

        double totalWeight = dhamChainDataList.stream().mapToDouble(d -> d.weight).sum();
        if (totalWeight <= 0) {
            // Fallback if all weights are zero or negative, pick randomly without weighting
            System.err.println("Warning: Total weight for Dham sequences is zero or negative. Picking a random sequence without weighting.");
            return dhamChainDataList.get(random.nextInt(dhamChainDataList.size())).activityChain;
        }

        double rand = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;

        for (DhamChainData data : dhamChainDataList) {
            cumulativeWeight += data.weight;
            if (rand <= cumulativeWeight) {
                return data.activityChain;
            }
        }
        // Fallback in case of floating point inaccuracies, return a random one
        return dhamChainDataList.get(random.nextInt(dhamChainDataList.size())).activityChain;
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