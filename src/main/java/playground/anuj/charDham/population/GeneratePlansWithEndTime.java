package playground.anuj.charDham.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static playground.anuj.charDham.runner.RunCharDhamSingleSimulation.*;

public class GeneratePlansWithEndTime {


    public static final String importantLocationsFile = FileUtils.Chhar_DHAM_HOME_DIR+"input/anuj/UKCharDhamPrimeLocations.txt";
    public static final String outputPlansFile = "output/plan_charDham_updated_v6.xml"; // Updated output file name
    private final Map<String, Coord> location2Coord = new HashMap<>();
    public static final String PASSENGER_ATTRIBUTE = "numberOfPassengers";
//    private static final double REST_STOP_DURATION = 3.0 * 3600.0;     // 3 hours for a chosen rest stop
//    private static final double DHAM_VISIT_DURATION = 6.0 * 3600.0;
//    private static final double LEISURE_TIME = 1 * 3600.0;                   //chai sutta break leisure activity
//    private static final double OVERNIGHT_STAY_DURATION = 10.0 * 3600.0; // 10 hours for an overnight halt
    private static final int DEPARTURE_WINDOW_DAYS = 8;
    public static final double SAMPLE_SIZE = 0.1;
//    public static final String DUMMY_MODE = "dummy"; //actually this is used for transition purpose for tansitinon bw two difffernt activities


    public void normalize(double[] weights) {
        double sum = 0;
        for (double w : weights) sum += w;

        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sum;
        }
    }

    private final Random random = new Random(); // For random choices
    // New: CSV file for dham activity chains and weights
    private static final String DHAM_CHAIN_FREQUENCIES_FILE = FileUtils.Chhar_DHAM_HOME_DIR+"input/anuj/dham_activity_chain_frequencies.csv";
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

    public GeneratePlansWithEndTime() {
    }

    public static void main(String[] args) {
        new GeneratePlansWithEndTime().run();
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

        //added files for distributions across hours and dates
        double[] dayWeights = readDayWeights(FileUtils.Chhar_DHAM_HOME_DIR+"input/day_weight.csv", DEPARTURE_WINDOW_DAYS);
        normalize(dayWeights);
        double[] hourWeights = readHourWeights(FileUtils.Chhar_DHAM_HOME_DIR+"input/time_weight.csv", 24);
        normalize(hourWeights);
        double currentTime;

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
            String personIdString = "Pilgrim_" + primaryMode + "_" + currentPersonIdCounter++;
            Person person = populationFactory.createPerson(Id.createPersonId(personIdString));
            Plan plan = populationFactory.createPlan();

            plan.getAttributes().putAttribute(PASSENGER_ATTRIBUTE, passengersForThisVehicle);
            totalPassengersGenerated += passengersForThisVehicle;
            numberOfPlansGenerated++; // Increment count of generated plans/vehicles

//            int departureDay = random.nextInt(DEPARTURE_WINDOW_DAYS) + 1;
            // Start activity in Haridwar with a random end time between 12 AM and 12 AM on day 1

            int departureDay = sampleIndex(dayWeights, random) + 1;
            int hour = sampleIndex(hourWeights, random);
            double minute = random.nextDouble();
            currentTime = (departureDay - 1) * 24 * 3600
                    + (hour + minute) * 3600;


            //plan generation
            String currentLocation = "Haridwar";
            addActivityWithEndTime(populationFactory, plan, currentLocation, location2Coord.get(currentLocation), currentTime);
            // Get a random dham sequence based on weights from CSV
            List<String> dhamSequence = getRandomDhamSequence();
            // Add dham visits based on sequence
            for (String dham : dhamSequence) {
//                addLeg(populationFactory, plan, primaryMode);

                switch (dham) {
                    case "Yamunotri" -> {
                        // Haridwar → Barkot
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Barkot", location2Coord.get("Barkot"), currentTime, primaryMode);
                        currentLocation = "Barkot";
                        // Barkot → JankiChatti
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "JankiChatti", location2Coord.get("JankiChatti"), currentTime, primaryMode);
                        currentLocation = "JankiChatti";


                        // Trek to Yamunotri
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Yamunotri", location2Coord.get("Yamunotri"), currentTime, WALK_MODE);
                        currentLocation = "Yamunotri";


                        // Return Yamunotri → JankiChatti
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "JankiChatti", location2Coord.get("JankiChatti"), currentTime, WALK_MODE);
                        currentLocation = "JankiChatti";

                        // JankiChatti → Barkot
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Barkot", location2Coord.get("Barkot"), currentTime, primaryMode);
                        currentLocation = "Barkot";
                    }


                    case "Gangotri" -> {
                        // Travel to Uttarkashi (handles night constraint)
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Uttarkashi", location2Coord.get("Uttarkashi"), currentTime, primaryMode);
                        currentLocation = "Uttarkashi";


                        // Travel to Gangotri
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Gangotri", location2Coord.get("Gangotri"), currentTime, primaryMode);
                        currentLocation = "Gangotri";


                    }

                    case "Kedarnath" -> {

                        // Travel to Sonprayag
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Sonprayag", location2Coord.get("Sonprayag"), currentTime, primaryMode);
                        currentLocation = "Sonprayag";


                        // Sonprayag → Gaurikund
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Gaurikund", location2Coord.get("Gaurikund"), currentTime, primaryMode);
                        currentLocation = "Gaurikund";


                        // Trek to Kedarnath
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Kedarnath", location2Coord.get("Kedarnath"), currentTime, WALK_MODE);
                        currentLocation = "Kedarnath";


                        // Return trek
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Gaurikund", location2Coord.get("Gaurikund"), currentTime, WALK_MODE);
                        currentLocation = "Gaurikund";
                    }


                    case "Badrinath" -> {

                        // Travel to Joshimath (with night constraint)
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Joshimath", location2Coord.get("Joshimath"), currentTime, primaryMode);
                        currentLocation = "Joshimath";


                        // Joshimath → Badrinath
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Badrinath", location2Coord.get("Badrinath"), currentTime, primaryMode);
                        currentLocation = "Badrinath";


                        // Return to Joshimath (important for Hemkund linkage)
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Joshimath", location2Coord.get("Joshimath"), currentTime, primaryMode);
                        currentLocation = "Joshimath";
                    }

                    case "Hemkund_Sahib" -> {

                        // Travel to GovindGhat
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "GovindGhat", location2Coord.get("GovindGhat"), currentTime, primaryMode);
                        currentLocation = "GovindGhat";


                        // Trek: GovindGhat → Ghangaria
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Ghangaria", location2Coord.get("Ghangaria"), currentTime, WALK_MODE);
                        currentLocation = "Ghangaria";


                        // Trek: Ghangaria → Hemkund Sahib
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Hemkund_Sahib", location2Coord.get("Hemkund_Sahib"), currentTime, WALK_MODE);
                        currentLocation = "Hemkund_Sahib";




                        // Return: Hemkund → Ghangaria
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "Ghangaria", location2Coord.get("Ghangaria"), currentTime, WALK_MODE);
                        currentLocation = "Ghangaria";

                        // Return: Ghangaria → GovindGhat
                        currentTime = handleTravelWithNightConstraint(populationFactory, plan, currentLocation,
                                "GovindGhat", location2Coord.get("GovindGhat"), currentTime, WALK_MODE);
                        currentLocation = "GovindGhat";
                    }
                }
            }
            addLeg(populationFactory, plan, primaryMode);

            addFinalActivity(populationFactory, plan, "Haridwar", location2Coord.get("Haridwar"));


//            currentTime = handleTravelWithNightConstraint(populationFactory, plan,
//                    currentLocation, "Haridwar", location2Coord.get("Haridwar"), currentTime, primaryMode);

//            currentTime = addActivityWithDuration(populationFactory, plan, "Haridwar",
//                    location2Coord.get("Haridwar"), currentTime, getTravelTime(currentLocation, "Haridwar")

            person.addPlan(plan);
            population.addPerson(person);
        }
        // Write population to output file
        PopulationUtils.sampleDown(population, SAMPLE_SIZE);
        new PopulationWriter(population).write(outputPlansFile);
    }

    /**
     * Reads Dham activity chain frequencies and weights from a CSV file.
     * Populates `dhamChainDataList` with the top `numRowsToRead` entries by count
     * and calculates `NUM_AGENTS` (total passengers) based on these selected entries.
     *
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
     *
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

    private double addActivityWithDuration(PopulationFactory factory, Plan plan, String type, Coord coord, double startTime, double duration) {
        double endTime = startTime + duration;
        Activity activity = factory.createActivityFromCoord(type, coord);
        activity.setEndTime(endTime);
        plan.addActivity(activity);
        return endTime; //
    }

    private void addFinalActivity(PopulationFactory factory, Plan plan, String type, Coord coord) {
        Activity activity = factory.createActivityFromCoord(type, coord);
        plan.addActivity(activity);
    }

    private void addLeg(PopulationFactory factory, Plan plan, String mode) {
        Leg leg = factory.createLeg(mode);
        plan.addLeg(leg);
    }


    //added this to catach the departure window less than 8 error
    public double[] readDayWeights(String filePath, int size) {
        double[] weights = new double[size];
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                int day;
                double weight;
                try {
                    day = Integer.parseInt(parts[0].trim());
                    weight = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid numbers: " + line);
                    continue;
                }
                if (day < 1 || day > size) {
                    System.err.println("Day " + day + " out of range (1-" + size + "). Skipping.");
                    continue;
                }
                weights[day - 1] = weight;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return weights;
    }












//    public double[] readDayWeights(String filePath, int size) {
//        double[] weights = new double[size];
//
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            String line = br.readLine(); // skip header
//
//
//            while ((line = br.readLine()) != null) {
////                String[] parts = line.split(",", -1); // keeps empty fields
//
//                String[] parts = line.split(",");
//
//                int day = Integer.parseInt(parts[0].trim());
//                double weight = Double.parseDouble(parts[1].trim());
//
//                weights[day - 1] = weight;
//
//                if (weights[day - 1] == 0) {
//                    throw new RuntimeException("Missing weight for day " + day);
//                }
//                //
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        return weights;
//    }


    public double[] readHourWeights(String filePath, int size) {
        double[] weights = new double[size];

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int hour = Integer.parseInt(parts[0].trim());
                double weight = Double.parseDouble(parts[1].trim());

                if (hour < 0 || hour >= size) {
                    throw new RuntimeException("Hour " + hour + " out of range (0-" + (size - 1) + ")");
                }
                weights[hour] = weight;
            }

            // Optional: verify that no weight is zero (except maybe allowed)
            for (int i = 0; i < size; i++) {
                if (weights[i] == 0) {
                    System.err.println("Warning: Zero weight for hour " + i);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return weights;
    }

    //traveltime OD added

    private static double getTravelTime(String from, String to) {
        String key = from + "-" + to;

        if (!travelTimeMap.containsKey(key)) {
            throw new RuntimeException("Missing OD pair: " + key);
        }

        return travelTimeMap.get(key);
    }

    private static final Map<String, Double> travelTimeMap = new HashMap<>();

    private static void loadTravelTimesFromCSV(String filePath) {
        try (BufferedReader br = IOUtils.getBufferedReader(filePath)) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String from = parts[0].trim();
                String to = parts[1].trim();
                double time = Double.parseDouble(parts[2].trim());
                travelTimeMap.put(from + "-" + to, time);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read travel times from " + filePath, e);
        }
    }

    static {
        loadTravelTimesFromCSV(FileUtils.Chhar_DHAM_HOME_DIR+"input/travelTimes.csv");
    }


    private double handleTravelWithNightConstraint(
            PopulationFactory factory, Plan plan,
            String from, String to, Coord coord, double currentTime, String mode
    ) {
        double travelTime = getTravelTime(from, to);
        // If travel would violate night ban, shift currentTime to next 04:00
        while (isNightTravel(currentTime, travelTime)) {
            double secondsInDay = 24 * 3600;
            double timeOfDay = currentTime % secondsInDay;
            double nextMorning = 4 * 3600; // 04:00
            if (timeOfDay < nextMorning) {
                currentTime += (nextMorning - timeOfDay);
            } else {
                currentTime += (secondsInDay - timeOfDay) + nextMorning;
            }
        }
        // Now travel is safe – add leg and arrival activity
        addLeg(factory, plan, mode);
        currentTime = addActivityWithDuration(factory, plan, to, coord, currentTime, travelTime);
        return currentTime;
    }



//    private double handleTravelWithNightConstraint(
//            PopulationFactory factory, Plan plan,
//            String from, String to, Coord coord, double currentTime, String mode
//    ) {
//        double travelTime = getTravelTime(from, to);
//        while (isNightTravel(currentTime, travelTime)) {
//            // Add dummy leg to separate previous activity from overnight
////            addLeg(factory, plan, WALK_MODE);
//            // Add overnight activity
//            currentTime = addActivityWithDuration(
//                    factory, plan, "overnight",
//                    location2Coord.get(from),
//                    currentTime,
//                    getOvernightDuration(currentTime)
//            );
//
//        }
//        // Real travel leg
//        addLeg(factory, plan, mode);
//        // Arrival activity at destination
//        currentTime = addActivityWithDuration(factory, plan, to, coord, currentTime, travelTime);
//        return currentTime;
//    }

    //overnight journey restriction
    private static boolean isNightTravel(double startTime, double travelTime) {

        double endTime = startTime + travelTime;

        for (double t = startTime; t <= endTime; t += 1800) { // check every 30 min
            double hour = (t % (24 * 3600)) / 3600;

            if (hour >= 22 || hour < 4) {
                return true;
            }
        }

        return false;
    }


    private double getOvernightDuration(double currentTime) {
        double secondsInDay = 24 * 3600;
        double timeOfDay = currentTime % secondsInDay;

        double nextMorning = 4 * 3600;

        if (timeOfDay < nextMorning) {
            return nextMorning - timeOfDay;
        } else {
            return (secondsInDay - timeOfDay) + nextMorning;
        }
    }


    public int sampleIndex(double[] weights, Random random) {
        double r = random.nextDouble();
        double cum = 0;

        for (int i = 0; i < weights.length; i++) {
            cum += weights[i];
            if (r <= cum) return i;
        }
        return weights.length - 1;
    }
//
//    private void addDummyLeg(PopulationFactory factory, Plan plan) {
//        Leg leg = factory.createLeg(DUMMY_MODE);
//        plan.addLeg(leg);
//    }


}
