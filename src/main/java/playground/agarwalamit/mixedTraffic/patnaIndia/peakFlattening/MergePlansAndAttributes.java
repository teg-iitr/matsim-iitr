package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MergePlansAndAttributes {

    private final static String inputPlansFile = "../../patna/input/baseCaseOutput_plans.xml.gz";
    private final static String inputAttributesFile = "../../patna/input/output_personAttributes.xml.gz";

    private final static String outputPlanFile = "../../patna/input/baseCaseOutput_plans_June2020.xml.gz";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(inputPlansFile);
        config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
        config.plans().setInputPersonAttributeFile(inputAttributesFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        new PopulationWriter(scenario.getPopulation()).write(outputPlanFile);

    }


}
