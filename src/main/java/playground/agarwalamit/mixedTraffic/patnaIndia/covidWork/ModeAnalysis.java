package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Scenario;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
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
            Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(outputExperiencedPlans);

            ModalShareFromPlans modalShareFromPlans = new ModalShareFromPlans(scenario.getPopulation(), PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
            modalShareFromPlans.run();

            modalShareFromPlans.writeResults(outputDir+"/"+rc+"/analysis/urbanModalShare_outputExpPlans.txt");
        }




    }
}
