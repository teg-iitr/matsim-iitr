package playground.anuj;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.contrib.locationchoice.timegeography.LocationChoicePlanStrategy;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Main class to run the Char Dham Yatra MATSim simulation.
 * This version uses NetworkChangeEvents to programmatically close all roads
 * at night, preventing travel. This is a direct way to manage network state over time.
 */
public class RunCharDhamSingleSimulation {

    // --- FILE PATHS (using the original network file) ---
    private static final String NETWORK_FILE = "output/network_charDham_modified.xml.gz";
    private static final String PLANS_FILE = "output/plan_charDham_updated_v2.xml";
    private static final String FACILITIES_FILE = "output/facilities_charDham.xml";
    private static final String OUTPUT_DIRECTORY = "output/charDham/";
    private static final String CONFIG_OUTPUT_FILE = OUTPUT_DIRECTORY + "/config_charDham.xml";
    private static final String TIME_VARIANT_LINKS_FILE = "input/timeVariant_links.csv";

    // --- SIMULATION PARAMETERS ---
    private static final int LAST_ITERATION = 20;
    private static final double FLOW_CAPACITY_FACTOR = 1.0;
    private static final double STORAGE_CAPACITY_FACTOR = 1.0;
    private static final double SIMULATION_START_TIME_H = 4.0;
    private static final double TEMPLE_OPENING_TIME_H = 5.0;  // 5 AM
    private static final double TEMPLE_CLOSING_TIME_H = 16.0; // 4 PM
    private static final double REST_STOP_TYPICAL_DURATION_S = 2.0 * 3600.0; // 2 hours

    // --- MODE & SCORING PARAMETERS ---
    static final String CAR_MODE = "car";
    static final String MOTORBIKE_MODE = "motorbike";
    static final String TRAVELLER_MODE = "traveller";
    static final Collection<String> modes = Arrays.asList(CAR_MODE, MOTORBIKE_MODE, TRAVELLER_MODE);
    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();

        configureController(config);
        configureNetworkAndPlans(config);
        configureLocationChoice(config);
        configureTrafficCharScenario(config);
        configureQSim(config);
        configureScoring(config);
        configureReplanning(config);

        new ConfigWriter(config).write(CONFIG_OUTPUT_FILE);

        // Load the scenario first, as we need its network to schedule the closures
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Vehicles vehicles = scenario.getVehicles();

//        scenario.getPopulation().getPersons().values().forEach(p -> p.getAttributes().putAttribute("rest", 100.0));

        VehicleType car = VehicleUtils.createVehicleType(Id.create(CAR_MODE, VehicleType.class));
        car.setPcuEquivalents(1.0);
        car.setMaximumVelocity(70 / 3.6);
        car.getCapacity().setSeats(5);
        vehicles.addVehicleType(car);

        VehicleType motorbike = VehicleUtils.createVehicleType(Id.create(MOTORBIKE_MODE, VehicleType.class));
        motorbike.setPcuEquivalents(0.25);
        motorbike.setMaximumVelocity(80 / 3.6);
        motorbike.getCapacity().setSeats(2);
        vehicles.addVehicleType(motorbike);

        VehicleType traveller = VehicleUtils.createVehicleType(Id.create(TRAVELLER_MODE, VehicleType.class));
        traveller.setPcuEquivalents(2);
        traveller.setMaximumVelocity(50 / 3.6);
        traveller.getCapacity().setSeats(22);
        traveller.getCapacity().setStandingRoom(5);
        vehicles.addVehicleType(traveller);

        config.routing().setNetworkModes(modes);
        // Schedule the nightly road closures using NetworkChangeEvents
        scheduleNightlyLinkClosures(scenario, TIME_VARIANT_LINKS_FILE);

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
        // Enable the base destination choice module
        DestinationChoiceConfigGroup dcConfig = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class);
        dcConfig.setAlgorithm(DestinationChoiceConfigGroup.Algotype.random); // Use random choice from the choice set
        // Set the activity type for which location choice should be performed
        dcConfig.setFlexibleTypes("rest");
        dcConfig.setPlanSelector("ChangeExpBeta");
        dcConfig.setEpsilonScaleFactors("5.0");
        dcConfig.setRadius(10.0);
        dcConfig.setScaleFactor(1);
    }

    private static Set<Id<Link>> readLinkIdsFromCsv(String filePath) {
        Set<Id<Link>> linkIds = new HashSet<>();
        // Use a try-with-resources block for automatic file closing
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            // 1. Check for an empty file
            if (line == null) {
                throw new RuntimeException("CSV file is empty: " + filePath);
            }

            // 2. Validate the header
            if (!line.trim().equalsIgnoreCase("id")) {
                throw new RuntimeException("CSV file's first line must be the header 'id'. Found: '" + line + "' in file: " + filePath);
            }

            // 3. Read the rest of the lines containing the link IDs
            while ((line = br.readLine()) != null) {
                String linkIdString = line.trim();
                // 4. Skip any blank lines in the file
                if (!linkIdString.isEmpty()) {
                    linkIds.add(Id.createLinkId(linkIdString));
                }
            }
        } catch (IOException e) {
            // Catches file-not-found or other read errors
            throw new RuntimeException("Error reading link ID file: " + filePath, e);
        }

        System.out.println("Read " + linkIds.size() + " link IDs from " + filePath);
        // Add a warning if the file was read but no IDs were found
        if (linkIds.isEmpty()) {
            System.err.println("Warning: No link IDs were found in " + filePath + ". Please check the file content.");
        }
        return linkIds;
    }
    /**
     * Schedules nightly network change events to close all links from 10 PM to 4 AM.
     * This is achieved by drastically reducing the free speed on the links, making them impassable.
     *
     * @param scenario The MATSim scenario containing the network.
     */
    private static void scheduleNightlyLinkClosures(Scenario scenario, String linksFilePath) {
        Network network = scenario.getNetwork();
        Set<Id<Link>> linksToClose = readLinkIdsFromCsv(linksFilePath);

        // Define the closure and reopening times in seconds from midnight
        double closeTimeOfDay_s = 22 * 3600; // 10:00 PM
        double reopenTimeOfDay_s = 4 * 3600;  // 4:00 AM

        // Create closure events for the first 5 days of the simulation
        int numberOfDaysToClose = 10;

        System.out.println("Scheduling nightly link closures for all links for " + numberOfDaysToClose + " days...");

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
                        NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, link.getFreespeed())); // Restores original cap
                reopenEvent.addLink(link);
                NetworkUtils.addNetworkChangeEvent(network, reopenEvent);
            }
        }
        System.out.println("✔ Nightly link closure events have been successfully added to the network.");
    }


    private static void configureController(Config config) {
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(LAST_ITERATION);
        config.controller().setOutputDirectory(OUTPUT_DIRECTORY);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.vspExperimental().setWritingOutputEvents(true);

    }

    private static void configureNetworkAndPlans(Config config) {
        config.network().setInputFile(NETWORK_FILE);
        config.network().setTimeVariantNetwork(true);
        config.plans().setInputFile(PLANS_FILE);
        config.facilities().setInputFile(FACILITIES_FILE);
    }

    private static void configureQSim(Config config) {
        config.qsim().setStartTime(SIMULATION_START_TIME_H * 3600.0);
//        config.qsim().setEndTime(5 * 24 * 3600);
        config.qsim().setUsePersonIdForMissingVehicleId(true);
        config.qsim().setFlowCapFactor(FLOW_CAPACITY_FACTOR);
        config.qsim().setStorageCapFactor(STORAGE_CAPACITY_FACTOR);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setStuckTime(3600.);
        config.qsim().setRemoveStuckVehicles(false);
        config.qsim().setNotifyAboutStuckVehicles(true);
        config.qsim().setMainModes(modes);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
    }

    private static void configureScoring(Config config) {
        config.scoring().setWriteExperiencedPlans(true);
        config.scoring().setLearningRate(1.0);
        config.scoring().setBrainExpBeta(2.0);
        config.scoring().setLateArrival_utils_hr(-1);
        config.scoring().setPerforming_utils_hr(6);
//        config.scoring().setMemorizingExperiencedPlans(true);

        addActivityParams(config, "rest", REST_STOP_TYPICAL_DURATION_S, 0, 0, 3600.0);


        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams(CAR_MODE);
        carParams.setConstant(0);
        carParams.setMarginalUtilityOfTraveling(-6);
//        carParams.setMonetaryDistanceRate(-0.005);
        config.scoring().addModeParams(carParams);

        ScoringConfigGroup.ModeParams motorbikeParams = new ScoringConfigGroup.ModeParams(MOTORBIKE_MODE);
        motorbikeParams.setConstant(0);
        motorbikeParams.setMarginalUtilityOfTraveling(-6);
//        motorbikeParams.setMonetaryDistanceRate(-0.005);
        config.scoring().addModeParams(motorbikeParams);

        ScoringConfigGroup.ModeParams travellerParams = new ScoringConfigGroup.ModeParams(TRAVELLER_MODE);
        travellerParams.setConstant(0);
        travellerParams.setMarginalUtilityOfTraveling(-6);
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
     * Rewrites the replanning strategy to use the required Location Choice strategy.
     */
    private static void configureReplanning(Config config) {
        config.replanning().clearStrategySettings(); // Clear existing strategies first

        // The main strategy for performing location choice
        ReplanningConfigGroup.StrategySettings lcStrategy = new ReplanningConfigGroup.StrategySettings();
        lcStrategy.setStrategyName(DestinationChoiceConfigGroup.GROUP_NAME);
        lcStrategy.setWeight(0.3); // High weight to ensure location choice is explored
        config.replanning().addStrategySettings(lcStrategy);

        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator, 0.6);
        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ReRoute, 0.1);
//        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute, 0.5);
        config.timeAllocationMutator().setMutationRange(7200);
        config.timeAllocationMutator().setMutateAroundInitialEndTimeOnly(true);
        config.timeAllocationMutator().setAffectingDuration(false);
        config.timeAllocationMutator().setMutationRangeStep(60 * 10);

        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
//        config.replanningAnnealer().setActivateAnnealingModule(true);
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