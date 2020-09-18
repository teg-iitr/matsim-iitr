package playground.amit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;

/**
 * @author amit
 */

public class ModeAnalysis {

    public static void main(String[] args) {
        String outputDir = "C:/Users/Amit Agarwal/Documents/patna/policy/";
        String runCases [] = new String [] {"run2020_9"};

        for (String rc : runCases) {
            String outputExperiencedPlans = outputDir+"/"+rc+"/"+rc+".output_experienced_plans.xml.gz";
            String networkFile = outputDir+"/"+rc+"/"+rc+".output_network.xml.gz";
            String configFile = outputDir+"/"+rc+"/"+rc+".output_config.xml";
            String vehiclesFile = outputDir+"/"+rc+"/"+rc+".output_vehicles.xml.gz";
            String eventsFile = outputDir+"/"+rc+"/"+rc+".output_events.xml.gz";

            Config config = ConfigUtils.loadConfig(configFile);
            config.plans().setInputFile(outputExperiencedPlans);
            config.network().setInputFile(networkFile);
            config.vehicles().setVehiclesFile(vehiclesFile);

            Scenario scenario = ScenarioUtils.loadScenario(config);

//            ModalShareFromEvents msc_firstItEvents = new ModalShareFromEvents(eventsFile, PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
//            msc_firstItEvents.run();
//            msc_firstItEvents.writeResults(outputDir+"/"+rc+"/analysis/urbanModalShare_outputEvents.txt");

//            ModalShareFromPlans modalShareFromPlans = new ModalShareFromPlans(scenario.getPopulation(), PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
//            modalShareFromPlans.run();
//
//            modalShareFromPlans.writeResults(outputDir+"/"+rc+"/analysis/urbanModalShare_outputExpPlans.txt");

//            ActivityDepartureAnalyzer analyzer = new ActivityDepartureAnalyzer(eventsFile);
//            analyzer.run();
//            analyzer.writeResults(outputDir+"/"+rc+"/analysis/activityDepartureCoutners.txt");

//            FilteredDepartureTimeAnalyzer lmtdd = new FilteredDepartureTimeAnalyzer(eventsFile, 3600.);
//            lmtdd.run();
//            lmtdd.writeResults(outputDir+"/"+rc+"/analysis/departureCounts"+".txt");

//            StatsWriter.run(outputDir+"/"+rc+"/", rc);

//            ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile,  PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
//            mtta.run();

            ExperiencedDelayAnalyzer experiencedDelayAnalyzer = new ExperiencedDelayAnalyzer(eventsFile, scenario,
                    30, PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
            experiencedDelayAnalyzer.run();
            experiencedDelayAnalyzer.writeResults(outputDir+"/"+rc+"/analysis/"+rc+"_timebin2delay.txt");
            experiencedDelayAnalyzer.writePersonTripInfo(outputDir+"/"+rc+"/analysis/"+rc+"_personTripTimeDelayInfo.txt");
        }

    }
}
