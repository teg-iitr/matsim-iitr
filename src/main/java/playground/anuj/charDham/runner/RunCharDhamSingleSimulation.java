package playground.anuj.charDham.runner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoiceIITR.frozenepsilons.BestReplyLocationChoicePlanStrategy;
import org.matsim.contrib.locationchoiceIITR.frozenepsilons.DestinationChoiceContext;
import org.matsim.contrib.locationchoiceIITR.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapperModule;
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

import static playground.anuj.charDham.population.CharDhamInitialPlans.SAMPLE_SIZE;

/**
 * Main class to run the Char Dham Yatra MATSim simulation.
 * This version uses NetworkChangeEvents to programmatically close all roads
 * at night, preventing travel. This is a direct way to manage network state over time.
 */
public class RunCharDhamSingleSimulation {

    // --- FILE PATHS (using the original network file) ---
    private static final String NETWORK_FILE = "output/network_charDham_modified.xml.gz";
    private static final String PLANS_FILE = "output/plan_charDham_updated_v3.xml";
    private static final String FACILITIES_FILE = "output/facilities_charDham.xml";
    private static final String OUTPUT_DIRECTORY = "output/charDham/";
    private static final String CONFIG_OUTPUT_FILE = OUTPUT_DIRECTORY + "/config_charDham.xml";
    private static final String TIME_VARIANT_LINKS_FILE = "input/timeVariant_links.csv";

    // --- SIMULATION PARAMETERS ---
    private static final int LAST_ITERATION = 100;
    private static final double FLOW_CAPACITY_FACTOR = SAMPLE_SIZE;
    private static final double STORAGE_CAPACITY_FACTOR = SAMPLE_SIZE * 10;
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
        // Schedule the nightly road closures using NetworkChangeEvents
        scheduleNightlyLinkClosures(scenario, TIME_VARIANT_LINKS_FILE);

        // Set up and run the controller
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
    private static void configureLocationChoice(Config config) {
        // Enable the base destination choice module
        FrozenTastesConfigGroup ftConfig = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class);
        ftConfig.setAlgorithm(FrozenTastesConfigGroup.Algotype.bestResponse); // Use bestResponse with FrozenTastes
        ftConfig.setFlexibleTypes("rest");
        ftConfig.setPlanSelector("BestScore");
        ftConfig.setEpsilonScaleFactors("100.0");
        ftConfig.setScaleFactor(10);

        // Set maxDistanceDCScore to control the search radius for facilities
        ftConfig.setMaxDistanceDCScore(10000);
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
        double reopenTimeOfDay_s = 28 * 3600;  // 12:00 AM

        // Create closure events for the first 5 days of the simulation
        int numberOfDaysToClose = 100;

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

    private static void configureQSim(Config config) {
        config.qsim().setStartTime(SIMULATION_START_TIME_H * 3600.0);
//        config.qsim().setEndTime(5 * 24 * 3600);
        config.qsim().setUsePersonIdForMissingVehicleId(true);
        config.qsim().setFlowCapFactor(FLOW_CAPACITY_FACTOR);
        config.qsim().setStorageCapFactor(STORAGE_CAPACITY_FACTOR);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
        config.qsim().setStuckTime(7200);
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

        addActivityParams(config, "rest", REST_STOP_TYPICAL_DURATION_S, 0, 0, MINIMUM_REST_DURATION_S);


        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams(CAR_MODE);
        carParams.setConstant(0);
        carParams.setMarginalUtilityOfTraveling(-6);
//        carParams.setMonetaryDistanceRate(-0.005);
        config.scoring().addModeParams(carParams);

        ScoringConfigGroup.ModeParams taxiParams = new ScoringConfigGroup.ModeParams(TAXI_MODE);
        taxiParams.setConstant(0);
        taxiParams.setMarginalUtilityOfTraveling(-6);
//        carParams.setMonetaryDistanceRate(-0.005);
        config.scoring().addModeParams(taxiParams);

        ScoringConfigGroup.ModeParams motorbikeParams = new ScoringConfigGroup.ModeParams(MOTORBIKE_MODE);
        motorbikeParams.setConstant(0);
        motorbikeParams.setMarginalUtilityOfTraveling(-6);
//        motorbikeParams.setMonetaryDistanceRate(-0.005);
        config.scoring().addModeParams(motorbikeParams);

        ScoringConfigGroup.ModeParams minibusParams = new ScoringConfigGroup.ModeParams(MINI_BUS_MODE);
        minibusParams.setConstant(0);
        minibusParams.setMarginalUtilityOfTraveling(-3);
        config.scoring().addModeParams(minibusParams);

        ScoringConfigGroup.ModeParams busParams = new ScoringConfigGroup.ModeParams(BUS_MODE);
        busParams.setConstant(0);
        busParams.setMarginalUtilityOfTraveling(-3);
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
     * Rewrites the replanning strategy to use the required Location Choice strategy.
     */
    private static void configureReplanning(Config config) {
        config.replanning().clearStrategySettings(); // Clear existing strategies first
        ChangeModeConfigGroup changeTripMode = config.changeMode();
        changeTripMode.setModes(new String [] {modes.toString()});

        // The main strategy for performing location choice
        ReplanningConfigGroup.StrategySettings lcStrategy = new ReplanningConfigGroup.StrategySettings();
        lcStrategy.setStrategyName(FrozenTastesConfigGroup.GROUP_NAME);
        lcStrategy.setWeight(0.3); // High weight to ensure location choice is explored
        config.replanning().addStrategySettings(lcStrategy);

        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator, 0.6);
        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ReRoute, 0.1);
//        addStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode, 0.1);

        config.timeAllocationMutator().setMutationRange(2.0 * 3600.0);
        config.timeAllocationMutator().setMutateAroundInitialEndTimeOnly(true);
        config.timeAllocationMutator().setAffectingDuration(false);
        config.timeAllocationMutator().setMutationRangeStep(10.0 * 60.0);

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