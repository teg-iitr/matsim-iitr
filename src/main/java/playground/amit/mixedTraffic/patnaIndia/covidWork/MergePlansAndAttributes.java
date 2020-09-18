package playground.amit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
/**
 * @author amit
 */

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

        for (Person person : scenario.getPopulation().getPersons().values()) {
            String ug =  (String) person.getAttributes().removeAttribute(PatnaUtils.SUBPOP_ATTRIBUTE);
            PopulationUtils.putSubpopulation(person, ug);
        }

        new PopulationWriter(scenario.getPopulation()).write(outputPlanFile);

    }


}
