package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Keeping WFH plan only if it is selected
 *
 * @author amit
 */
public class WFHPlansKeeper {

    public static void main(String[] args) {

        String plansFile = "C:/Users/Amit Agarwal/Documents/patna/calib/calb2020_holes_WFH_35/calb2020_holes_WFH_35.output_experienced_plans.xml.gz";
        Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(plansFile);

//        WFHPlansKeeper.printStatsWFH_withLeg(scenario.getPopulation(), "WFHwalk");
        WFHPlansKeeper.printStatsWFH_withoutLeg(scenario.getPopulation());

    }

    public void process(Population population, String WFHMode){
        for (Person person : population.getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            if ( ((Leg)selectedPlan.getPlanElements().get(1)).getMode().equals(WFHMode)) {
                int plans = person.getPlans().size();
                for (int i = 0; i <plans ; i++) {
                    Plan pl = person.getPlans().get(i);
                    if (! pl.equals(selectedPlan)) person.removePlan(pl);
                }
            }
        }
    }

    private static int wfhCounter = 0;

    public static void printStatsWFH_withoutLeg(Population population){
        PatnaPersonFilter filter = new PatnaPersonFilter();
        for(Person person : population.getPersons().values()){
            if (filter.getUserGroupAsStringFromPersonId(person.getId()).equals(PatnaPersonFilter.PatnaUserGroup.urban.toString())) {
                Plan plan = person.getSelectedPlan();
                if (plan.getPlanElements().size()==2) {
                    if ( ((Activity)plan.getPlanElements().get(0)).getType().startsWith("WorkFromHome") &&
                            ((Activity)plan.getPlanElements().get(1)).getType().startsWith("WorkFromHome") ){
                        wfhCounter++;
                    } else {
                        System.out.println("Person "+person.getId()+" has the selected plan with two plan elements which are not work from home activity types.");
                    }
                }
            }
        }
        System.out.println("Total number of work from home plans are "+ wfhCounter);
    }

    public static void printStatsWFH_withLeg(Population population, String WFHMode) {
        PatnaPersonFilter filter = new PatnaPersonFilter();
        for(Person person : population.getPersons().values()){
            for (Plan plan : person.getPlans()) {
                if (filter.getUserGroupAsStringFromPersonId(person.getId()).equals(PatnaPersonFilter.PatnaUserGroup.urban.toString())) {
                    Id<Link> firstActCoord = ((Activity) plan.getPlanElements().get(0)).getLinkId();
                    Id<Link> secondActCoord = ((Activity) plan.getPlanElements().get(2)).getLinkId();
                    if (firstActCoord.toString().equals(secondActCoord.toString())) {
                        String mode = ((Leg)plan.getPlanElements().get(1)).getMode();
                        if (! mode.equals(WFHMode)) {
                            System.out.println("For person Id "+person.getId()+", in a plan, the first and second activities have same coordinates. But the travel mode is "+mode);
                        }
                    }
                }

                // travel time should be zero only for WFHMode
                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Leg) {
                        Leg leg = ((Leg)pe);
                        if (leg.getTravelTime().seconds()==0) {
                            if (! leg.getMode().equals(WFHMode)) {
                                System.out.println("For person Id "+person.getId()+", in a plan, leg mode is "+leg.getMode() +" and travel time is zero.");
                            }
                        }
                    }
                }
            }
        }
    }
}
