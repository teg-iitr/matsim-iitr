package playground.shivam.signals.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;

import static playground.shivam.signals.SignalUtils.*;

public class CreatePopulation {
    public static void createPopulation(Scenario scenario, String outputDirectory) {
        Population population = scenario.getPopulation();

        String[] odRelations = {"1_2-7_6-L", "1_2-4_5-L", "1_2-8_9-L", "6_7-4_5-T", "6_7-8_9-T", "6_7-2_1-T",
                "5_4-8_9-R", "5_4-2_1-R", "5_4-7_6-R", "9_8-2_1-B", "9_8-7_6-B", "9_8-4_5-B",};


        for (String od : odRelations) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];

            String approach = od.split("-")[2];
            int agentsPerApproach;
            int offset;

            if (approach.equalsIgnoreCase("L")) {
                agentsPerApproach = AGENTS_PER_LEFT_APPROACH;
                offset = OFFSET_LEFT_APPROACH;
            }
            else if (approach.equalsIgnoreCase("T")) {
                agentsPerApproach = AGENTS_PER_TOP_APPROACH;
                offset = OFFSET_TOP_APPROACH;
            }
            else if (approach.equalsIgnoreCase("R")) {
                agentsPerApproach = AGENTS_PER_RIGHT_APPROACH;
                offset = OFFSET_RIGHT_APPROACH;
            }
            else {
                agentsPerApproach = AGENTS_PER_BOTTOM_APPROACH;
                offset = OFFSET_BOTTOM_APPROACH;
            }
            for (int i = 0; i < agentsPerApproach; i++) {
                // create a person
                Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));

                // create a plan for the person that contains all this
                // information
                Plan plan = population.getFactory().createPlan();

                // create a start activity at the from link
                Activity homeAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
                // distribute agents uniformly during one hour.
                homeAct.setEndTime(i + offset);
                plan.addActivity(homeAct);

                // create a dummy leg
                plan.addLeg(population.getFactory().createLeg(getTravelMode(MatsimRandom.getLocalInstance().nextInt(100))));

                // create a work activity at the to link
                Activity workAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
                plan.addActivity(workAct);

                person.addPlan(plan);
                population.addPerson(person);
            }
        }
        new PopulationWriter(population).write(outputDirectory + "population.xml.gz");
    }

    private static String getTravelMode(int number) {
        if (number <= 60) return "car";
        else return "truck";
    }

}
