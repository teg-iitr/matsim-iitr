package playground.amit.Dehradun;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class DMATestController {

    public static void main(String[] args) {

        String filePath = "/Users/amit/Downloads/WP50_Haridwar-Rishikesh/output_motorized/run240/";

        Config config = ConfigUtils.loadConfig(filePath+"run240.output_config.xml");
        config.network().setInputFile(filePath+"run240.output_network.xml.gz");
        config.plans().setInputFile(filePath+"run240.output_plans.xml.gz");
        config.vehicles().setVehiclesFile(filePath+"run240.output_allVehicles.xml.gz");
        config.controller().setOutputDirectory("/Users/amit/Downloads/WP50_Haridwar-Rishikesh/output_motorized/run240_output/");

        Scenario scenario = ScenarioUtils.loadScenario(config);



        Controler controller = new Controler(scenario);
//        controller.addOverridingModule(new SimWrapperModule());
        controller.run();


    }

}
