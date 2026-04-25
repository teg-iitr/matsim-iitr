package playground.anuj.locationChoice.toyScenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePlan {

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        int numberOfPersons = 1000;
        for (int i = 1; i <= numberOfPersons; i++) {
            Person person = populationFactory.createPerson(Id.createPersonId(i));
            Plan plan = populationFactory.createPlan();

            // Home activity (starting point)
            Activity homeActivity = populationFactory.createActivityFromCoord("home", new Coord(0, 0));
            homeActivity.setEndTime(8 * 3600);
            plan.addActivity(homeActivity);

            // Travel by car
            Leg legToWork = populationFactory.createLeg(TransportMode.car);
            plan.addLeg(legToWork);

            Activity work = populationFactory.createActivityFromCoord("work", new Coord(3000, 1000));
            work.setEndTime(14 * 3600);
            plan.addActivity(work);

            // Travel back home
            Leg legToShop = populationFactory.createLeg(TransportMode.car);
            plan.addLeg(legToShop);

            // Shopping activity at shop2
            Activity shoppingActivity = populationFactory.createActivityFromCoord("shopping", new Coord(1000, 10000));
            shoppingActivity.setEndTime(18 * 3600);
            plan.addActivity(shoppingActivity);
            plan.addLeg(legToWork);

            Activity work2 = populationFactory.createActivityFromCoord("work", new Coord(3000, 1000));
            work2.setEndTime(20 * 3600);
            plan.addActivity(work2);

            // Travel back home
            Leg legToHome = populationFactory.createLeg(TransportMode.car);
            plan.addLeg(legToHome);

            // Final home activity
            Activity returnHome = populationFactory.createActivityFromCoord("home", new Coord(0, 0));
            plan.addActivity(returnHome);

            // Add plan to person
            person.addPlan(plan);
            population.addPerson(person);
        }

        // Write the population to an XML file
        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write("DestinationChoiceTest/input/population_1000.xml");
    }
}
