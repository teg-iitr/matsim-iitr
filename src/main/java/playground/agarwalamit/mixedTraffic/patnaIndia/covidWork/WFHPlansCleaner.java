package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import playground.agarwalamit.mixedTraffic.patnaIndia.covidWork.wfh.WFHActivity;
import playground.agarwalamit.utils.LoadMyScenarios;

import java.util.ArrayList;
import java.util.List;

public class WFHPlansCleaner {

    public static void main(String[] args) {
        String plansFile = "C:/Users/Amit Agarwal/Documents/patna/calib/calb2020_holes_WFH_62/calb2020_holes_WFH_62.output_plans.xml.gz";
        String outputPlans = "C:/Users/Amit Agarwal/Documents/patna/input/run62_WFH_calibrated_output_Plans.xml.gz";
        Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(plansFile);
        new WFHPlansCleaner().process(scenario.getPopulation());

        new PopulationWriter(scenario.getPopulation()).write(outputPlans);
    }

    public void process(Population population) {
        List<Id<Person>> personsToBeRemoved = new ArrayList<>();
        for (Person person : population.getPersons().values()) {
            if (person.getSelectedPlan().getType()!=null){
                if (WFHActivity.isWFHPlan(person.getSelectedPlan().getType())) {
                    personsToBeRemoved.add(person.getId());
                } else {
                    List<Plan> plansToBeRemoved = new ArrayList<>();
                    for (Plan plan : person.getPlans()) {
                        if (WFHActivity.isWFHPlan(plan.getType())) plansToBeRemoved.add(plan);
                    }
                    plansToBeRemoved.forEach(person::removePlan);
                }
            } else {
                // nothing; keep person and all of plans
            }
        }
        personsToBeRemoved.forEach(population::removePerson);
    }
}
