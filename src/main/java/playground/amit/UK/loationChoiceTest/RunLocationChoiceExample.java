package playground.amit.UK.loationChoiceTest;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
//import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastes;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.Arrays;
import java.util.List;

public class RunLocationChoiceExample {
    public static void main(String[] args) {
        // Create a new MATSim config object
        Config config = ConfigUtils.createConfig();
        config.controller().setLastIteration(10);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        // Configure Network
        config.network().setInputFile("/Users/amit/Downloads/network.xml");
        // Configure Population Plans
        config.plans().setInputFile("/Users/amit/Downloads/plans.xml");
        // Configure Facilities
        config.facilities().setInputFile("/Users/amit/Downloads/facilities.xml");
        // Configure Controler settings
        config.controller().setOutputDirectory("/Users/amit/Downloads/output");


        // Enable Destination Choice Module
        DestinationChoiceConfigGroup dcConfig = new DestinationChoiceConfigGroup();
        dcConfig.setAlgorithm(DestinationChoiceConfigGroup.Algotype.random);
        dcConfig.setPlanSelector("ChangeExpBeta");
        dcConfig.setFlexibleTypes("shopping");
        dcConfig.setEpsilonScaleFactors("shopping");
        config.addModule(dcConfig);

        FrozenTastesConfigGroup ftcg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class);
        ftcg.setEpsilonScaleFactors("10.0");
        ftcg.setFlexibleTypes("shopping");
        ftcg.setDestinationSamplePercent(5);

        new ConfigWriter(config).write("/Users/amit/Downloads/config.xml");

        // Configure QSim (Simulation Engine)
        config.qsim().setEndTime(30 * 3600);  // Simulation runs for 30 hours
        config.network().setTimeVariantNetwork(true);
        // Configure Scoring
        ScoringConfigGroup scoring = config.scoring();
        scoring.setLearningRate(1.0);
        scoring.setBrainExpBeta(2.0);

        // Activity scoring settings
        ScoringConfigGroup.ActivityParams homeActivity = new ScoringConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 3600);
        scoring.addActivityParams(homeActivity);

        ScoringConfigGroup.ActivityParams shoppingActivity = new ScoringConfigGroup.ActivityParams("shopping");
        shoppingActivity.setTypicalDuration(2 * 3600);
        scoring.addActivityParams(shoppingActivity);

        // Configure Strategy
        config.replanning().setMaxAgentPlanMemorySize(5);





        ReplanningConfigGroup.StrategySettings stratDest = new ReplanningConfigGroup.StrategySettings();
//        stratDest.setStrategyName(FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY);
        stratDest.setStrategyName(FrozenTastesConfigGroup.GROUP_NAME);
        stratDest.setWeight(0.5);
        config.replanning().addStrategySettings(stratDest);

        ReplanningConfigGroup.StrategySettings strat = new ReplanningConfigGroup.StrategySettings();
        strat.setStrategyName("BestScore"); // Updated to match config.xml
        strat.setWeight(0.3);
        config.replanning().addStrategySettings(strat);

        // Load the scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getPopulation().getPersons().values().forEach(p->p.getAttributes().putAttribute("shopping",1000.));

        ActivityFacility af1 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s1", ActivityFacility.class), new Coord(1000.,0));
        af1.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));

        ActivityFacility af2 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s2", ActivityFacility.class), new Coord(2000.,0));
        af2.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));

        scenario.getActivityFacilities().addActivityFacility(af1);
        scenario.getActivityFacilities().addActivityFacility(af2);



        List<String> linkIds = Arrays.asList("1_2", "2_3", "3_4"); // Add your link IDs here
        // Add network change events (e.g., closing a link after 6 PM)
        addNetworkChangeEvents(scenario,linkIds);


        // Create MATSim controler
        Controler controler = new Controler(scenario);

//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                addPlanStrategyBinding("MyLocationChoice").to(BestReplyLocationChoicePlanStrategy.class);
//            }
//        });

//        FrozenTastes.configure(controler);
//        controler.run();
        throw new RuntimeException("FrozenTastes.configure(controler) is not available in matsim 2025.0");
    }



    private static void addNetworkChangeEvents(Scenario scenario, List<String> linkIds) {
        // Define the times for closing and restoring links
        double stopTime = 12 * 3600; // 12 PM
        double restoreTime = (24 + 12) * 3600; // 12 AM next day

        // Loop through each link ID and apply the network change events
        for (String linkId : linkIds) {
            Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(linkId));
            if (link != null) {
                // Create a network change event to stop flow (close the link)
                NetworkChangeEvent stopFlowEvent = new NetworkChangeEvent(stopTime);
                stopFlowEvent.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR, 0.0)); // Set capacity to 0
                stopFlowEvent.addLink(link);
                NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), stopFlowEvent);

                // Create a network change event to restore flow (reopen the link)
                NetworkChangeEvent restoreFlowEvent = new NetworkChangeEvent(restoreTime);
                restoreFlowEvent.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.FACTOR, link.getCapacity())); // Restore original capacity
                restoreFlowEvent.addLink(link);
                NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), restoreFlowEvent);
            } else {
                System.err.println("Link not found: " + linkId);
            }
        }
    }
}
