package playground.amit.Dehradun;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import java.io.File;

public class DMAController {
    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String config_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_config.xml.gz";

    public static void main(String[] args) {

        String config_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_config.xml.gz" ;
        String runId = "test";

        if (args.length > 0) {
            config_file = args[0];
            runId = args[1];
        }

        System.out.println(new File(config_file).exists());

        Config config = ConfigUtils.loadConfig(config_file);
        config.controler().setRunId(runId);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        DMAVehicleGenerator.generateVehicles(scenario);

        Controler controler = new Controler(scenario);
        controler.run();

    }

}

