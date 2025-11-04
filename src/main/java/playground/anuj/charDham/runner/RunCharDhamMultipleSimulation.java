package playground.anuj.charDham.runner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoiceIITR.frozenepsilons.BestReplyLocationChoicePlanStrategy;
import org.matsim.contrib.locationchoiceIITR.frozenepsilons.DestinationChoiceContext;
import org.matsim.contrib.locationchoiceIITR.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Main class to run multiple Char Dham Yatra MATSim simulations based on parameters
 * read from a CSV file. Each row in the CSV triggers a new simulation run with
 * its own output directory.
 */
public class RunCharDhamMultipleSimulation {

    // --- FILE PATHS (using the original network file) ---
    private static final String NETWORK_FILE = "output/network_charDham_modified.xml.gz";
    private static final String PLANS_FILE = "output/plan_charDham_updated_v3.xml";
    private static final String FACILITIES_FILE = "output/facilities_charDham.xml";
    private static final String BASE_OUTPUT_DIRECTORY = "output/charDham_runs/"; // Base directory for all runs
    private static final String TIME_VARIANT_LINKS_FILE = "input/timeVariant_links.csv";
    private static final String PARAMETER_RUNS_CSV = "input/parameter_runs.csv"; // New: CSV for run parameters

    // --- SIMULATION PARAMETERS (Defaults, can be overridden by CSV) ---
    private static final double SIMULATION_START_TIME_H = 4.0;
    private static final double TEMPLE_OPENING_TIME_H = 5.0;  // 5 AM
    private static final double TEMPLE_CLOSING_TIME_H = 16.0; // 4 PM
    private static final double REST_STOP_TYPICAL_DURATION_S = 2.0 * 3600.0; // 2 hours
    private static final double MINIMUM_REST_DURATION_S = 3600.0;

    // --- MODE & SCORING PARAMETERS ---
    public static final String CAR_MODE = "car";
    public static final String TAXI_MODE = "taxi";
    public static final String MOTORBIKE_MODE = "motorbike";
    public static final String BUS_MODE = "bus";
    public static final String MINI_BUS_MODE = "miniBus";
    public static final String WALK_MODE = TransportMode.walk;
    static final Collection<String> modes = Arrays.asList(CAR_MODE, TAXI_MODE, MOTORBIKE_MODE, BUS_MODE, MINI_BUS_MODE);
    /**
     * Helper class to hold parameters for a single simulation run.
     */
    private static class RunParameters {
        String runId;
        int lastIteration;
        double flowCapacityFactor;
        double storageCapacityFactor;
//        double lateArrivalUtilsHr;
        double performingUtilsHr;
        double carMarginalUtilityOfTraveling;
        double taxiMarginalUtilityOfTraveling;
        double bikeMarginalUtilityOfTraveling;
        double miniBusMarginalUtilityOfTraveling;
        double busMarginalUtilityOfTraveling;
        double locationChoiceWeight;
        double locationChoiceSearchRadius;
        double timeAllocationMutatorWeight;
        double mutationRange;
        double mutationRangeStep;
        double reRouteWeight;
//        double changeTripModeWeight;
        double nightTimeSpeed;

        // Constructor to parse a CSV line
        RunParameters(String[] parts, String[] headers) {
            Map<String, String> dataMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                if (i < parts.length) {
                    dataMap.put(headers[i].trim(), parts[i].trim());
                }
            }

            this.runId = dataMap.getOrDefault("run_id", "default_run");
            this.lastIteration = Integer.parseInt(dataMap.getOrDefault("last_iteration", "2"));
            this.flowCapacityFactor = Double.parseDouble(dataMap.getOrDefault("flow_capacity_factor", "0.5"));
            this.storageCapacityFactor = Double.parseDouble(dataMap.getOrDefault("storage_capacity_factor", "0.5")) * 10.0;
//            this.lateArrivalUtilsHr = Double.parseDouble(dataMap.getOrDefault("lateArrival_utils_hr", "-1.0"));
            this.performingUtilsHr = Double.parseDouble(dataMap.getOrDefault("performing_utils_hr", "6.0"));
            this.carMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("car_marginalUtilityOfTraveling", "-6.0"));
            this.taxiMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("taxi_marginalUtilityOfTraveling", "-6.0"));
            this.bikeMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("bike_marginalUtilityOfTraveling", "-6.0"));
            this.busMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("bus_marginalUtilityOfTraveling", "-6.0"));
            this.miniBusMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("miniBus_marginalUtilityOfTraveling", "-6.0"));
            this.locationChoiceWeight = Double.parseDouble(dataMap.getOrDefault("locationChoice_weight", "0.3"));
            this.locationChoiceSearchRadius = Double.parseDouble(dataMap.getOrDefault("locationChoice_searchRadius", "20000.0"));
            this.timeAllocationMutatorWeight = Double.parseDouble(dataMap.getOrDefault("timeAllocationMutator_weight", "0.6"));
            this.mutationRange = Double.parseDouble(dataMap.getOrDefault("mutationRange", "7200.0"));
            this.mutationRangeStep = Double.parseDouble(dataMap.getOrDefault("mutationRangeStep", "600.0"));
            this.reRouteWeight = Double.parseDouble(dataMap.getOrDefault("reRoute_weight", "0.1"));
//            this.changeTripModeWeight = Double.parseDouble(dataMap.getOrDefault("changeTripMode_weight", "0.1"));
            this.nightTimeSpeed = Double.parseDouble(dataMap.getOrDefault("nightTimeSpeed", "1.78"));
        }
        RunParameters() {
            this(new String[]{}, new String[]{});
        }
    }

    public static void main(String[] args) {
        // Check if the parameter CSV file exists. If not, create it with default values.
        if (!Files.exists(Paths.get(PARAMETER_RUNS_CSV))) {
            writeDefaultParametersCsv();
        }

        List<RunParameters> runs = readRunParameters();

        if (runs.isEmpty()) {
            System.out.println("No valid parameters found in " + PARAMETER_RUNS_CSV + ". Running with a single default parameter set.");
            runs.add(new RunParameters()); // Add a default run if the file was empty or only had headers
        }

        System.out.println("Starting multiple MATSim simulations based on " + PARAMETER_RUNS_CSV);
        for (int i = 0; i < runs.size(); i++) {
            RunParameters params = runs.get(i);
            String currentOutputDir = BASE_OUTPUT_DIRECTORY + params.runId + "/";
            System.out.println("\n--- Running Simulation: " + params.runId + " (Run " + (i + 1) + " of " + runs.size() + ") ---");
            System.out.println("Output will be saved to: " + currentOutputDir);

            try {
                // Ensure the output directory exists
                Files.createDirectories(Paths.get(currentOutputDir));
                runSingleSimulation(params, currentOutputDir);
                System.out.println("--- Simulation " + params.runId + " COMPLETED ---");
            } catch (Exception e) {
                System.err.println("!!! Error running simulation " + params.runId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("\nAll simulations finished.");
    }

    /**
     * Reads simulation parameters from a CSV file.
     *
     * @return A list of RunParameters objects, one for each row.
     */
    private static List<RunParameters> readRunParameters() {
        List<RunParameters> runs = new ArrayList<>();
        try (BufferedReader br = IOUtils.getBufferedReader(RunCharDhamMultipleSimulation.PARAMETER_RUNS_CSV)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.err.println("CSV file is empty: " + RunCharDhamMultipleSimulation.PARAMETER_RUNS_CSV);
                // Do NOT call writeDefaultParametersCsv() here, as it's handled in main()
                return runs;
            }
            String[] headers = headerLine.split(",");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines
                String[] parts = line.split(",");
                if (parts.length != headers.length) {
                    System.err.println("Warning: Skipping malformed line (column count mismatch) in " + RunCharDhamMultipleSimulation.PARAMETER_RUNS_CSV + ": " + line);
                    continue;
                }
                try {
                    runs.add(new RunParameters(parts, headers));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Skipping line due to number format error in " + RunCharDhamMultipleSimulation.PARAMETER_RUNS_CSV + ": " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading parameter runs CSV file: " + RunCharDhamMultipleSimulation.PARAMETER_RUNS_CSV);
            e.printStackTrace();
        }
        return runs;
    }
    /**
     * Writes a single row of default parameters to the PARAMETER_RUNS_CSV file.
     * This method is called only if the file is initially empty or unreadable.
     */
    private static void writeDefaultParametersCsv() {
        List<String> headers = Arrays.asList(
                "run_id",
                "last_iteration",
                "flow_capacity_factor",
                "storage_capacity_factor",
//                "lateArrival_utils_hr",
                "performing_utils_hr",
                "car_marginalUtilityOfTraveling",
                "taxi_marginalUtilityOfTraveling",
                "bike_marginalUtilityOfTraveling",
                "bus_marginalUtilityOfTraveling",
                "miniBus_marginalUtilityOfTraveling",
                "locationChoice_weight",
                "locationChoice_searchRadius",
                "timeAllocationMutator_weight",
                "mutationRange",
                "mutationRangeStep",
                "reRoute_weight",
//                "changeTripMode_weight",
                "nightTimeSpeed"
        );

        // Create a default RunParameters instance to get its default values
        RunParameters defaultParams = new RunParameters();

        List<String> defaultValues = Arrays.asList(
                defaultParams.runId,
                String.valueOf(defaultParams.lastIteration),
                String.valueOf(defaultParams.flowCapacityFactor),
                String.valueOf(defaultParams.storageCapacityFactor),
//                String.valueOf(defaultParams.lateArrivalUtilsHr),
                String.valueOf(defaultParams.performingUtilsHr),
                String.valueOf(defaultParams.carMarginalUtilityOfTraveling),
                String.valueOf(defaultParams.taxiMarginalUtilityOfTraveling),
                String.valueOf(defaultParams.bikeMarginalUtilityOfTraveling),
                String.valueOf(defaultParams.miniBusMarginalUtilityOfTraveling),
                String.valueOf(defaultParams.busMarginalUtilityOfTraveling),
                String.valueOf(defaultParams.locationChoiceWeight),
                String.valueOf(defaultParams.locationChoiceSearchRadius),
                String.valueOf(defaultParams.timeAllocationMutatorWeight),
                String.valueOf(defaultParams.mutationRange),
                String.valueOf(defaultParams.mutationRangeStep),
                String.valueOf(defaultParams.reRouteWeight),
//                String.valueOf(defaultParams.changeTripModeWeight),
                String.valueOf(defaultParams.nightTimeSpeed)
        );

        try (PrintWriter writer = new PrintWriter(new FileWriter(PARAMETER_RUNS_CSV))) {
            writer.println(String.join(",", headers));
            writer.println(String.join(",", defaultValues));
            System.out.println("Created default parameters CSV at: " + PARAMETER_RUNS_CSV);
        } catch (IOException e) {
            System.err.println("FATAL ERROR: Could not write default parameters CSV to " + PARAMETER_RUNS_CSV + ": " + e.getMessage());
            // This is a critical error, as we can't even write the default config.
            // The program might not be able to proceed meaningfully.
        }
    }
    /**
     * Configures and runs a single MATSim simulation.
     *
     * @param params The parameters for this specific run.
     * @param outputDirectory The unique output directory for this run.
     */
    private static void runSingleSimulation(RunParameters params, String outputDirectory) {
        Config config = ConfigUtils.createConfig();

        // Configure controller with run-specific output directory and iterations
        configureController(config, params, outputDirectory);
        configureNetworkAndPlans(config);
        configureLocationChoice(config, params);
        configureTrafficCharScenario(config);
        // Configure QSim with run-specific flow/storage factors
        configureQSim(config, params);
        // Configure scoring with run-specific utility values
        configureScoring(config, params);
        // Configure replanning with run-specific strategy weights
        configureReplanning(config, params);

        // Write the config file for this specific run
        new ConfigWriter(config).write(outputDirectory + "config_charDham.xml");

        // Load the scenario first, as we need its network to schedule the closures
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Vehicles vehicles = scenario.getVehicles();

        // Add vehicle types (these are fixed, not from CSV)
        VehicleType car = VehicleUtils.createVehicleType(Id.create(CAR_MODE, VehicleType.class), CAR_MODE);
        car.setPcuEquivalents(1.0);
        car.setMaximumVelocity(70 / 3.6);
        car.getCapacity().setSeats(5);
        vehicles.addVehicleType(car);

        VehicleType taxi = VehicleUtils.createVehicleType(Id.create(TAXI_MODE, VehicleType.class), TAXI_MODE);
        taxi.setPcuEquivalents(1.5);
        taxi.setMaximumVelocity(60 / 3.6);
        taxi.getCapacity().setSeats(10);
        vehicles.addVehicleType(taxi);

        VehicleType motorbike = VehicleUtils.createVehicleType(Id.create(MOTORBIKE_MODE, VehicleType.class), MOTORBIKE_MODE);
        motorbike.setPcuEquivalents(0.5);
        motorbike.setMaximumVelocity(80 / 3.6);
        motorbike.getCapacity().setSeats(2);
        vehicles.addVehicleType(motorbike);

        VehicleType minibus = VehicleUtils.createVehicleType(Id.create(MINI_BUS_MODE, VehicleType.class), MINI_BUS_MODE);
        minibus.setPcuEquivalents(2.5);
        minibus.setMaximumVelocity(50 / 3.6);
        minibus.getCapacity().setSeats(15);
        minibus.getCapacity().setStandingRoom(5);
        vehicles.addVehicleType(minibus);

        VehicleType bus = VehicleUtils.createVehicleType(Id.create(BUS_MODE, VehicleType.class), BUS_MODE);
        bus.setPcuEquivalents(3);
        bus.setMaximumVelocity(50 / 3.6);
        bus.getCapacity().setSeats(30);
        bus.getCapacity().setStandingRoom(5);
        vehicles.addVehicleType(bus);

        config.routing().setNetworkModes(modes);

        // Schedule the nightly road closures using NetworkChangeEvents (fixed logic)
        scheduleNightlyLinkClosures(scenario, params);

        final DestinationChoiceContext lcContext = new DestinationChoiceContext(scenario) ;
        scenario.addScenarioElement(DestinationChoiceContext.ELEMENT_NAME, lcContext);
        // Set up and run the controller
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding(FrozenTastesConfigGroup.GROUP_NAME).to(BestReplyLocationChoicePlanStrategy.class);
            }
        });
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TAXI_MODE).to(carTravelTime());
                addTravelDisutilityFactoryBinding(TAXI_MODE).to(carTravelDisutilityFactoryKey());

                addTravelTimeBinding(MOTORBIKE_MODE).to(carTravelTime());
                addTravelDisutilityFactoryBinding(MOTORBIKE_MODE).to(carTravelDisutilityFactoryKey());

                addTravelTimeBinding(MINI_BUS_MODE).to(carTravelTime());
                addTravelDisutilityFactoryBinding(MINI_BUS_MODE).to(carTravelDisutilityFactoryKey());

                addTravelTimeBinding(BUS_MODE).to(carTravelTime());
                addTravelDisutilityFactoryBinding(BUS_MODE).to(carTravelDisutilityFactoryKey());

                new SimWrapperModule();
            }
        });
        controler.run();
    }

    /**
     * Configures the TrafficCharScenario custom module.
     */
    private static void configureTrafficCharScenario(Config config) {
        TrafficCharConfigGroup trafficCharConfigGroup = new TrafficCharConfigGroup();

        QSimConfigGroup qSimConfigGroupFIFO = new QSimConfigGroup();
        qSimConfigGroupFIFO.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
        trafficCharConfigGroup.addQSimConfigGroup(TrafficCharConfigGroup.ROAD_TYPE, qSimConfigGroupFIFO);
        trafficCharConfigGroup.addQSimConfigGroup(TrafficCharConfigGroup.ROAD_TYPE_DEFAULT, config.qsim());
        config.getModules().put(TrafficCharConfigGroup.GROUP_NAME, trafficCharConfigGroup);
    }

    /**
     * Configures the LocationChoice module.
     */
    private static void configureLocationChoice(Config config, RunParameters params) {
        FrozenTastesConfigGroup ftConfig = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class);
        ftConfig.setAlgorithm(FrozenTastesConfigGroup.Algotype.bestResponse); // Use bestResponse with FrozenTastes
        ftConfig.setFlexibleTypes("rest");
        ftConfig.setPlanSelector("BestScore");
        ftConfig.setEpsilonScaleFactors("100.0");
        ftConfig.setScaleFactor(1);

        // Set maxDistanceDCScore to control the search radius for facilities
        ftConfig.setMaxDistanceDCScore(params.locationChoiceSearchRadius);
    }

    private static Set<Id<Link>> readLinkIdsFromCsv() {
        Set<Id<Link>> linkIds = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(RunCharDhamMultipleSimulation.TIME_VARIANT_LINKS_FILE))) {
            String line = br.readLine();
            if (line == null) {
                throw new RuntimeException("CSV file is empty: " + RunCharDhamMultipleSimulation.TIME_VARIANT_LINKS_FILE);
            }
            if (!line.trim().equalsIgnoreCase("id")) {
                throw new RuntimeException("CSV file's first line must be the header 'id'. Found: '" + line + "' in file: " + RunCharDhamMultipleSimulation.TIME_VARIANT_LINKS_FILE);
            }
            while ((line = br.readLine()) != null) {
                String linkIdString = line.trim();
                if (!linkIdString.isEmpty()) {
                    linkIds.add(Id.createLinkId(linkIdString));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading link ID file: " + RunCharDhamMultipleSimulation.TIME_VARIANT_LINKS_FILE, e);
        }
        System.out.println("Read " + linkIds.size() + " link IDs from " + RunCharDhamMultipleSimulation.TIME_VARIANT_LINKS_FILE);
        if (linkIds.isEmpty()) {
            System.err.println("Warning: No link IDs were found in " + RunCharDhamMultipleSimulation.TIME_VARIANT_LINKS_FILE + ". Please check the file content.");
        }
        return linkIds;
    }

    /**
     * Schedules nightly network change events to close all links from 10 PM to 4 AM.
     * This is achieved by drastically reducing the free speed on the links, making them impassable.
     *
     * @param scenario The MATSim scenario containing the network.
     */
    private static void scheduleNightlyLinkClosures(Scenario scenario, RunParameters params) {
        Network network = scenario.getNetwork();
        Set<Id<Link>> linksToClose = readLinkIdsFromCsv();

        double closeTimeOfDay_s = 22 * 3600; // 10:00 PM
        double reopenTimeOfDay_s = 28 * 2600;  // 4:00 AM

        int numberOfDaysToClose = 100; // Fixed for now

        System.out.println("Scheduling nightly link closures for " + linksToClose.size() + " links for " + numberOfDaysToClose + " days...");

        for (Id<Link> linkId : linksToClose) {
            Link link = network.getLinks().get(linkId);
            if (link == null) {
                System.err.println("❌ Link ID '" + linkId + "' from CSV not found in the network. Skipping.");
                continue;
            }
            for (int day = 0; day < numberOfDaysToClose; day++) {
                double dayOffset_s = day * 24 * 3600;

                // --- Event to close the link at 10 PM ---
                double closeEventTime = dayOffset_s + closeTimeOfDay_s;
                NetworkChangeEvent closeEvent = new NetworkChangeEvent(closeEventTime);
                closeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(
                        NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, params.nightTimeSpeed)); // Effectively closes the link
                closeEvent.addLink(link);
                NetworkUtils.addNetworkChangeEvent(network, closeEvent);

                // --- Event to reopen the link at 4 AM the NEXT day ---
                double reopenEventTime = dayOffset_s + (24 * 3600) + reopenTimeOfDay_s;
                NetworkChangeEvent reopenEvent = new NetworkChangeEvent(reopenEventTime);
                reopenEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(
                        NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, link.getFreespeed())); // Restores original freespeed
                reopenEvent.addLink(link);
                NetworkUtils.addNetworkChangeEvent(network, reopenEvent);
            }
        }
        System.out.println("✔ Nightly link closure events have been successfully added to the network.");
    }

    private static void configureController(Config config, RunParameters params, String outputDirectory) {
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(params.lastIteration); // From CSV
        config.controller().setOutputDirectory(outputDirectory); // Unique for each run
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setWritePlansInterval(20);
        config.controller().setWriteEventsInterval(20);
        config.vspExperimental().setWritingOutputEvents(true);
    }

    private static void configureNetworkAndPlans(Config config) {
        config.network().setInputFile(NETWORK_FILE);
        config.network().setTimeVariantNetwork(true);
        config.plans().setInputFile(PLANS_FILE);
        config.facilities().setInputFile(FACILITIES_FILE);
    }

    private static void configureQSim(Config config, RunParameters params) {
        config.qsim().setStartTime(SIMULATION_START_TIME_H * 3600.0);
        config.qsim().setUsePersonIdForMissingVehicleId(true);
        config.qsim().setFlowCapFactor(params.flowCapacityFactor); // From CSV
        config.qsim().setStorageCapFactor(params.storageCapacityFactor); // From CSV
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
        config.qsim().setStuckTime(3600.);
        config.qsim().setRemoveStuckVehicles(false);
        config.qsim().setNotifyAboutStuckVehicles(true);
        config.qsim().setMainModes(modes);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
    }

    private static void configureScoring(Config config, RunParameters params) {
        config.scoring().setWriteExperiencedPlans(true);
        config.scoring().setLearningRate(1.0);
        config.scoring().setBrainExpBeta(2.0);
//        config.scoring().setLateArrival_utils_hr(params.lateArrivalUtilsHr); // From CSV
        config.scoring().setPerforming_utils_hr(params.performingUtilsHr); // From CSV

        addActivityParams(config, "rest", REST_STOP_TYPICAL_DURATION_S, 0, 0, MINIMUM_REST_DURATION_S);

        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams(CAR_MODE);
        carParams.setConstant(0);
        carParams.setMarginalUtilityOfTraveling(params.carMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(carParams);

        ScoringConfigGroup.ModeParams taxiParams = new ScoringConfigGroup.ModeParams(TAXI_MODE);
        taxiParams.setConstant(0);
        taxiParams.setMarginalUtilityOfTraveling(params.taxiMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(taxiParams);

        ScoringConfigGroup.ModeParams motorbikeParams = new ScoringConfigGroup.ModeParams(MOTORBIKE_MODE);
        motorbikeParams.setConstant(0);
        motorbikeParams.setMarginalUtilityOfTraveling(params.bikeMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(motorbikeParams);

        ScoringConfigGroup.ModeParams minibusParams = new ScoringConfigGroup.ModeParams(MINI_BUS_MODE);
        minibusParams.setConstant(0);
        minibusParams.setMarginalUtilityOfTraveling(params.miniBusMarginalUtilityOfTraveling);
        config.scoring().addModeParams(minibusParams);

        ScoringConfigGroup.ModeParams busParams = new ScoringConfigGroup.ModeParams(BUS_MODE);
        busParams.setConstant(0);
        busParams.setMarginalUtilityOfTraveling(params.busMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(busParams);

        addActivityParams(config, "Haridwar", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S); // Open 24h
        addActivityParams(config, "Srinagar", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "Sonprayag", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "Gaurikund", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "Uttarkashi", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "Barkot", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "Joshimath", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "GovindGhat", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);
        addActivityParams(config, "Ghangaria", 24 * 3600.0, 0, 0, MINIMUM_REST_DURATION_S);

        // Main pilgrimage sites (Temples) with consistent opening/closing times
        double templeOpeningTime_s = TEMPLE_OPENING_TIME_H * 3600.0;
        double templeClosingTime_s = TEMPLE_CLOSING_TIME_H * 3600.0;
        double templeVisitDuration_s = 6 * 3600.0;
        double minimalTempleDuration_s = 3 * 3600.0;

        addActivityParams(config, "Kedarnath", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "Gangotri", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "Yamunotri", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "Badrinath", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "Hemkund_Sahib", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
    }

    /**
     * Configures the replanning strategy with weights from CSV.
     */
    private static void configureReplanning(Config config, RunParameters params) {
        config.replanning().clearStrategySettings();
//        ChangeModeConfigGroup changeTripMode = config.changeMode();
//        changeTripMode.setModes(new String [] {modes.toString()});

        ReplanningConfigGroup.StrategySettings lcStrategy = new ReplanningConfigGroup.StrategySettings();
        lcStrategy.setStrategyName(FrozenTastesConfigGroup.GROUP_NAME);
        lcStrategy.setWeight(params.locationChoiceWeight); // From CSV
        config.replanning().addStrategySettings(lcStrategy);

        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator, params.timeAllocationMutatorWeight); // From CSV
        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ReRoute, params.reRouteWeight); // From CSV
//        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode, params.changeTripModeWeight);

        config.timeAllocationMutator().setMutationRange(params.mutationRange);
        config.timeAllocationMutator().setMutateAroundInitialEndTimeOnly(true);
        config.timeAllocationMutator().setAffectingDuration(false);
        config.timeAllocationMutator().setMutationRangeStep(params.mutationRangeStep);

        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
    }

    private static void addActivityParams(Config config, String activityType, double typicalDuration, double openingTime, double closingTime, double minimalDuration) {
        ScoringConfigGroup.ActivityParams activityParams = new ScoringConfigGroup.ActivityParams(activityType);
        activityParams.setTypicalDuration(typicalDuration);
        if (openingTime > 0 && closingTime > openingTime) {
            activityParams.setOpeningTime(openingTime);
            activityParams.setClosingTime(closingTime);
        }
        if (minimalDuration > 0)
            activityParams.setMinimalDuration(minimalDuration);
        config.scoring().addActivityParams(activityParams);
    }

    private static void addStrategy(Config config, String strategyName, double weight) {
        ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
        strategySettings.setStrategyName(strategyName);
        strategySettings.setWeight(weight);
        config.replanning().addStrategySettings(strategySettings);
    }
}