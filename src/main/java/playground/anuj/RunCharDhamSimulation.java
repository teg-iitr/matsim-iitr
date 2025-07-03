package playground.anuj;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Main class to run the Char Dham Yatra MATSim simulation.
 * This version uses the official roadpricing contribution to programmatically
 * create a RoadPricingScheme that prevents agents from traveling at night.
 * This is the recommended, best-practice approach.
 */
public class RunCharDhamSimulation {

    // --- FILE PATHS (using the original network file) ---
    private static final String NETWORK_FILE = "output/network_charDham.xml.gz";
    private static final String PLANS_FILE = "output/plan_charDham.xml";
    private static final String OUTPUT_DIRECTORY = "output/charDham/";
    private static final String CONFIG_OUTPUT_FILE = OUTPUT_DIRECTORY + "/config_charDham.xml";

    // --- SIMULATION & TOLL PARAMETERS ---
    private static final int LAST_ITERATION = 50;
    private static final double FLOW_CAPACITY_FACTOR = 0.01;
    private static final double STORAGE_CAPACITY_FACTOR = 0.01;
    private static final double SIMULATION_START_TIME_H = 4.0;
    private static final double NIGHT_START_TIME_H = 20; // 10 PM
    private static final double NIGHT_END_TIME_H = 28.0;   // 4 AM (next day, 24 + 4)
    private static final double PROHIBITIVE_TOLL_AMOUNT = 1.0E9; // A very large number

    // --- MODE & SCORING PARAMETERS ---
    private static final String CAR_MODE = "car";
    private static final String MOTORBIKE_MODE = "motorbike";
//    private static final double LATE_ARRIVAL_UTIL_PER_HOUR = -1;
    private static final double TOLL_DISUTILITY_PER_UNIT = -1.0;

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();

        configureController(config);
        configureNetworkAndPlans(config);
        // We no longer need a toll file in the config, it will be created in code.
        RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class);
        rpConfig.setTollLinksFile(null);

        configureQSim(config);
        configureScoring(config);
        configureReplanning(config);

        new ConfigWriter(config).write(CONFIG_OUTPUT_FILE);

        // Load the scenario first, as we need its network to create the scheme
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Create the road pricing scheme programmatically
        RoadPricingScheme scheme = createNightTimePricingScheme(scenario);

        // Set up the controller
        Controler controler = new Controler(scenario);

        // Install the official RoadPricingModule with our custom scheme
        controler.addOverridingModule(new RoadPricingModule(scheme));

        controler.run();
    }

    /**
     * Creates a RoadPricingScheme in memory that applies a prohibitive toll on ALL links
     * during the night hours (22:00 to 04:00).
     */
    private static RoadPricingScheme createNightTimePricingScheme(Scenario scenario) {
        // Create a new, empty scheme and add it to the scenario
        RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario);

        // Set its properties
        RoadPricingUtils.setName( scheme, "NightTravelRestriction");
        RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
        RoadPricingUtils.setDescription(scheme, "A prohibitive toll on all links from 10 PM to 4 AM.");
        for (int i = 1; i < 4; i++) {
            for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
                RoadPricingUtils.addLinkSpecificCost(scheme, linkId, Time.parseTime(String.valueOf((NIGHT_START_TIME_H + 24*i) * 3600)),
                        Time.parseTime(String.valueOf((NIGHT_END_TIME_H + 24*i) * 3600)),
                        PROHIBITIVE_TOLL_AMOUNT);
            }
        }
        // As seen in the decompiled code, create a general cost for a time window.
        // This will apply to all links that are part of the scheme.
        RoadPricingUtils.createAndAddGeneralCost(scheme,
                Time.parseTime(String.valueOf(NIGHT_START_TIME_H * 3600)),
                Time.parseTime(String.valueOf(NIGHT_END_TIME_H * 3600)),
                PROHIBITIVE_TOLL_AMOUNT);

        // Now, add ALL links from the network to this scheme.
        for (Link link : scenario.getNetwork().getLinks().values()) {
            RoadPricingUtils.addLink(scheme, link.getId());
        }

        return scheme;
    }


    private static void configureController(Config config) {
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(LAST_ITERATION);
        config.controller().setOutputDirectory(OUTPUT_DIRECTORY);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.vspExperimental().setWritingOutputEvents(true);
        config.qsim().setStuckTime(3600);
        config.qsim().setRemoveStuckVehicles(true);
        Set<String> modes = new HashSet<>(Arrays.asList("car", "motorbike"));
        config.qsim().setMainModes(modes);
        config.routing().setNetworkModes(modes);
    }

    private static void configureNetworkAndPlans(Config config) {
        config.network().setInputFile(NETWORK_FILE);
        config.plans().setInputFile(PLANS_FILE);
    }

    private static void configureQSim(Config config) {
        config.qsim().setStartTime(SIMULATION_START_TIME_H * 3600.0);
        config.qsim().setFlowCapFactor(FLOW_CAPACITY_FACTOR);
        config.qsim().setStorageCapFactor(STORAGE_CAPACITY_FACTOR);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
    }

    private static void configureScoring(Config config) {
//        config.scoring().setLateArrival_utils_hr(LATE_ARRIVAL_UTIL_PER_HOUR);
        config.scoring().setWriteExperiencedPlans(true);

        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams(CAR_MODE);
        carParams.setConstant(0.);
        carParams.setMarginalUtilityOfTraveling(-0.64);
        carParams.setMonetaryDistanceRate(-3.7*Math.pow(10, -5));
        config.scoring().addModeParams(carParams);

        ScoringConfigGroup.ModeParams motorbikeParams = new ScoringConfigGroup.ModeParams(MOTORBIKE_MODE);
        motorbikeParams.setMarginalUtilityOfTraveling(-0.18);
        motorbikeParams.setMonetaryDistanceRate(-1.6*Math.pow(10, -5));
        config.scoring().addModeParams(motorbikeParams);

        addActivityParams(config, "Haridwar", 12 * 3600.0, 0, 0); // Open 24h
        addActivityParams(config, "visit-Srinagar", 12 * 3600.0, 0, 0);
        addActivityParams(config, "visit-Sonprayag", 12 * 3600.0,0, 0);
        addActivityParams(config, "visit-Gaurikund", 12 * 3600.0, 0, 0);
        addActivityParams(config, "visit-Uttarkashi", 12 * 3600.0, 0, 0);
        addActivityParams(config, "visit-Barkot", 12 * 3600.0, 0, 0);
        addActivityParams(config, "visit-Joshimath", 12 * 3600.0, 0, 0);

        addActivityParams(config, "visit-Kedarnath", 6 * 3600.0, 5.0 * 3600.0, 16.0 * 3600.0);
        addActivityParams(config, "visit-Gangotri", 6 * 3600.0,  5.0 * 3600.0, 16.0 * 3600.0);
        addActivityParams(config, "visit-Yamunotri", 6 * 3600.0, 5.0 * 3600.0, 16.0 * 3600.0);
        addActivityParams(config, "visit-Badrinath", 6 * 3600.0, 5.0 * 3600.0, 16.0 * 3600.0);
    }

    private static void configureReplanning(Config config) {
        addStrategy(config, "ReRoute", 0.15);
        addStrategy(config, "ChangeExpBeta", 0.15);
        addStrategy(config, "TimeAllocationMutator", 0.7);
        config.timeAllocationMutator().setMutationRange(7200.0);
        config.timeAllocationMutator().setAffectingDuration(true);
        config.replanning().setFractionOfIterationsToDisableInnovation(0.9);
    }

    private static void addActivityParams(Config config, String activityType, double typicalDuration, double openingTime, double closingTime) {
        ScoringConfigGroup.ActivityParams activityParams = new ScoringConfigGroup.ActivityParams(activityType);
        activityParams.setTypicalDuration(typicalDuration);
        if (openingTime > 0) activityParams.setOpeningTime(openingTime);
        if (closingTime > 0) activityParams.setClosingTime(closingTime);
        config.scoring().addActivityParams(activityParams);
    }

    private static void addStrategy(Config config, String strategyName, double weight) {
        ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
        strategySettings.setStrategyName(strategyName);
        strategySettings.setWeight(weight);
        config.replanning().addStrategySettings(strategySettings);
    }
}