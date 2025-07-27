package playground.anuj;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.timegeography.LocationChoicePlanStrategy;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    private static final String PLANS_FILE = "output/plan_charDham_updated_v2.xml";
    private static final String FACILITIES_FILE = "output/facilities_charDham.xml";
    private static final String BASE_OUTPUT_DIRECTORY = "output/charDham_runs/"; // Base directory for all runs
    private static final String TIME_VARIANT_LINKS_FILE = "input/timeVariant_links.csv";
    private static final String PARAMETER_RUNS_CSV = "input/parameter_runs.csv"; // New: CSV for run parameters

    // --- SIMULATION PARAMETERS (Defaults, can be overridden by CSV) ---
    private static final double SIMULATION_START_TIME_H = 4.0;
    private static final double TEMPLE_OPENING_TIME_H = 5.0;  // 5 AM
    private static final double TEMPLE_CLOSING_TIME_H = 16.0; // 4 PM
    private static final double REST_STOP_TYPICAL_DURATION_S = 2.0 * 3600.0; // 2 hours

    // --- MODE & SCORING PARAMETERS ---
    static final String CAR_MODE = "car";
    static final String MOTORBIKE_MODE = "motorbike";
    static final String TRAVELLER_MODE = "traveller";
    static final Collection<String> modes = Arrays.asList(CAR_MODE, MOTORBIKE_MODE, TRAVELLER_MODE);

    /**
     * Helper class to hold parameters for a single simulation run.
     */
    private static class RunParameters {
        String runId;
        int lastIteration;
        double flowCapacityFactor;
        double storageCapacityFactor;
        double lateArrivalUtilsHr;
        double performingUtilsHr;
        double carMarginalUtilityOfTraveling;
        double bikeMarginalUtilityOfTraveling;
        double travellerMarginalUtilityOfTraveling;
        double locationChoiceWeight;
        double timeAllocationMutatorWeight;
        double reRouteWeight;

        // Constructor to parse a CSV line
        RunParameters(String[] parts, String[] headers) {
            Map<String, String> dataMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                if (i < parts.length) {
                    dataMap.put(headers[i].trim(), parts[i].trim());
                }
            }

            this.runId = dataMap.getOrDefault("run_id", "default_run");
            this.lastIteration = Integer.parseInt(dataMap.getOrDefault("last_iteration", "20"));
            this.flowCapacityFactor = Double.parseDouble(dataMap.getOrDefault("flow_capacity_factor", "1.0"));
            this.storageCapacityFactor = Double.parseDouble(dataMap.getOrDefault("storage_capacity_factor", "1.0"));
            this.lateArrivalUtilsHr = Double.parseDouble(dataMap.getOrDefault("lateArrival_utils_hr", "-1.0"));
            this.performingUtilsHr = Double.parseDouble(dataMap.getOrDefault("performing_utils_hr", "6.0"));
            this.carMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("car_marginalUtilityOfTraveling", "-6.0"));
            this.bikeMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("bike_marginalUtilityOfTraveling", "-6.0"));
            this.travellerMarginalUtilityOfTraveling = Double.parseDouble(dataMap.getOrDefault("traveller_marginalUtilityOfTraveling", "-6.0"));
            this.locationChoiceWeight = Double.parseDouble(dataMap.getOrDefault("locationChoice_weight", "0.3"));
            this.timeAllocationMutatorWeight = Double.parseDouble(dataMap.getOrDefault("timeAllocationMutator_weight", "0.6"));
            this.reRouteWeight = Double.parseDouble(dataMap.getOrDefault("reRoute_weight", "0.1"));
        }
    }

    public static void main(String[] args) {
        List<RunParameters> runs = readRunParameters();

        if (runs.isEmpty()) {
            System.err.println("No simulation parameters found in " + PARAMETER_RUNS_CSV + ". Exiting.");
            return;
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
        configureLocationChoice(config);
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

        VehicleType motorbike = VehicleUtils.createVehicleType(Id.create(MOTORBIKE_MODE, VehicleType.class), MOTORBIKE_MODE);
        motorbike.setPcuEquivalents(0.25);
        motorbike.setMaximumVelocity(80 / 3.6);
        motorbike.getCapacity().setSeats(2);
        vehicles.addVehicleType(motorbike);

        VehicleType traveller = VehicleUtils.createVehicleType(Id.create(TRAVELLER_MODE, VehicleType.class), TRAVELLER_MODE);
        traveller.setPcuEquivalents(2);
        traveller.setMaximumVelocity(50 / 3.6);
        traveller.getCapacity().setSeats(22);
        traveller.getCapacity().setStandingRoom(5);
        vehicles.addVehicleType(traveller);

        config.routing().setNetworkModes(modes);

        // Schedule the nightly road closures using NetworkChangeEvents (fixed logic)
        scheduleNightlyLinkClosures(scenario);

        // Set up and run the controller
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
                addPlanStrategyBinding(DestinationChoiceConfigGroup.GROUP_NAME).toProvider(new jakarta.inject.Provider<PlanStrategy>() {
                    @Inject
                    TimeInterpretation timeInterpretation;

                    @Override
                    public PlanStrategy get() {
                        return new LocationChoicePlanStrategy(scenario, tripRouterProvider, timeInterpretation);
                    }
                });
            }
        });
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TRAVELLER_MODE).to(carTravelTime());
                addTravelDisutilityFactoryBinding(TRAVELLER_MODE).to(carTravelDisutilityFactoryKey());
                addTravelTimeBinding(MOTORBIKE_MODE).to(carTravelTime());
                addTravelDisutilityFactoryBinding(MOTORBIKE_MODE).to(carTravelDisutilityFactoryKey());
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
    private static void configureLocationChoice(Config config) {
        DestinationChoiceConfigGroup dcConfig = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class);
        dcConfig.setAlgorithm(DestinationChoiceConfigGroup.Algotype.random);
        dcConfig.setFlexibleTypes("rest");
        dcConfig.setPlanSelector("ChangeExpBeta");
        dcConfig.setEpsilonScaleFactors("5.0");
        dcConfig.setRadius(10.0);
        dcConfig.setScaleFactor(1);
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
    private static void scheduleNightlyLinkClosures(Scenario scenario) {
        Network network = scenario.getNetwork();
        Set<Id<Link>> linksToClose = readLinkIdsFromCsv();

        double closeTimeOfDay_s = 22 * 3600; // 10:00 PM
        double reopenTimeOfDay_s = 4 * 3600;  // 4:00 AM

        int numberOfDaysToClose = 10; // Fixed for now

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
                        NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 10/3.6)); // Effectively closes the link
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
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
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
        config.scoring().setLateArrival_utils_hr(params.lateArrivalUtilsHr); // From CSV
        config.scoring().setPerforming_utils_hr(params.performingUtilsHr); // From CSV

        addActivityParams(config, "rest", REST_STOP_TYPICAL_DURATION_S, 0, 0, 3600.0);

        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams(CAR_MODE);
        carParams.setConstant(0);
        carParams.setMarginalUtilityOfTraveling(params.carMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(carParams);

        ScoringConfigGroup.ModeParams motorbikeParams = new ScoringConfigGroup.ModeParams(MOTORBIKE_MODE);
        motorbikeParams.setConstant(0);
        motorbikeParams.setMarginalUtilityOfTraveling(params.bikeMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(motorbikeParams);

        ScoringConfigGroup.ModeParams travellerParams = new ScoringConfigGroup.ModeParams(TRAVELLER_MODE);
        travellerParams.setConstant(0);
        travellerParams.setMarginalUtilityOfTraveling(params.travellerMarginalUtilityOfTraveling); // From CSV
        config.scoring().addModeParams(travellerParams);

        addActivityParams(config, "Haridwar", 24 * 3600.0, 0, 0, 0); // Open 24h
        addActivityParams(config, "visit-Srinagar", 24 * 3600.0, 0, 0, 0);
        addActivityParams(config, "visit-Sonprayag", 24 * 3600.0, 0, 0, 0);
        addActivityParams(config, "visit-Gaurikund", 24 * 3600.0, 0, 0, 0);
        addActivityParams(config, "visit-Uttarkashi", 24 * 3600.0, 0, 0, 0);
        addActivityParams(config, "visit-Barkot", 24 * 3600.0, 0, 0, 0);
        addActivityParams(config, "visit-Joshimath", 24 * 3600.0, 0, 0, 0);

        // Main pilgrimage sites (Temples) with consistent opening/closing times
        double templeOpeningTime_s = TEMPLE_OPENING_TIME_H * 3600.0;
        double templeClosingTime_s = TEMPLE_CLOSING_TIME_H * 3600.0;
        double templeVisitDuration_s = 6 * 3600.0;
        double minimalTempleDuration_s = 2 * 3600.0;

        addActivityParams(config, "visit-Kedarnath", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "visit-Gangotri", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "visit-Yamunotri", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
        addActivityParams(config, "visit-Badrinath", templeVisitDuration_s, templeOpeningTime_s, templeClosingTime_s, minimalTempleDuration_s);
    }

    /**
     * Configures the replanning strategy with weights from CSV.
     */
    private static void configureReplanning(Config config, RunParameters params) {
        config.replanning().clearStrategySettings();

        ReplanningConfigGroup.StrategySettings lcStrategy = new ReplanningConfigGroup.StrategySettings();
        lcStrategy.setStrategyName(DestinationChoiceConfigGroup.GROUP_NAME);
        lcStrategy.setWeight(params.locationChoiceWeight); // From CSV
        config.replanning().addStrategySettings(lcStrategy);

        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator, params.timeAllocationMutatorWeight); // From CSV
        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ReRoute, params.reRouteWeight); // From CSV

        config.timeAllocationMutator().setMutationRange(7200);
        config.timeAllocationMutator().setMutateAroundInitialEndTimeOnly(true);
        config.timeAllocationMutator().setAffectingDuration(false);
        config.timeAllocationMutator().setMutationRangeStep(60 * 10);

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