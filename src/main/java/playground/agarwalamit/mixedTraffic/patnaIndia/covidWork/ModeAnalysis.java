package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Scenario;
import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.activity.departureArrival.FilteredDepartureTimeAnalyzer;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
/**
 * @author amit
 */

public class ModeAnalysis {

    public static void main(String[] args) {
        String outputDir = "C:/Users/Amit Agarwal/Documents/patna/calib/";
        String runCases [] = new String [] {"calb2020_holes_WFH_39"};

        for (String rc : runCases) {
            String outputExperiencedPlans = outputDir+"/"+rc+"/"+rc+".output_experienced_plans.xml.gz";
            String eventsFile = outputDir+"/"+rc+"/"+rc+".output_events.xml.gz";

//            Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(outputExperiencedPlans);

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

            ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile,  PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
            mtta.run();
        }




    }
}
