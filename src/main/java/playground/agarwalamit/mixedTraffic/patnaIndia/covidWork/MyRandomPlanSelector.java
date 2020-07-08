package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;
/**
 * @author amit
 */

public class MyRandomPlanSelector<T extends BasicPlan, I> implements PlanSelector<T, I> {
    public MyRandomPlanSelector() {

    }

    public T selectPlan(HasPlansAndId<T, I> person) {
        if (person.getPlans().size() == 0) {
            return null;
        } else {
            int index = (int)(MatsimRandom.getRandom().nextDouble() * (double)person.getPlans().size());
            boolean wfhPlan = isWFHPlan((Plan) person.getPlans().get(index));
            if (wfhPlan) return selectPlan(person);
            else return person.getPlans().get(index);
        }
    }

    private boolean isWFHPlan(Plan plan){
        if (plan.getPlanElements().get(1) instanceof Leg) return false;
        else return true;
    }
}