package playground.amit.gridNet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

/**
 * Created by Amit on 20/09/2020
 */
public class GridPlans {

    public final static String GRID_PLANS = "input/grid/gridPlans.xml.gz";
    private final Random random = MatsimRandom.getLocalInstance();
    private final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

    public static void main(String[] args) {

        GridPlans gridPlans = new GridPlans();
        gridPlans.createPlans();
        new PopulationWriter(gridPlans.scenario.getPopulation()).write(GRID_PLANS);
    }

    public void createPlans(){
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        //let us create 100 persons --> 40 HWH, 25 HEH, 10 HSH, 10 HLH, 15 HShH
        /**
         * Home location can be anywhere in the grid
         * Education is also in the city center
         * Work location will be central part of grid net, leave 1/4 grid on each side
         * Social anywhere
         * Leisure outside city center
         * Shopping close to home
         */
        for(int i = 0; i < 1000; i ++) {
            Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
            Plan plan = factory.createPlan();
            Activity home = factory.createActivityFromCoord("home", getRandomCoordInGrid());
            home.setEndTime(9+random.nextDouble() * 3600.);
            plan.addActivity(home);
            Leg leg = factory.createLeg(getTravelMode(random.nextInt(100)));
            plan.addLeg(leg);
            String actType = getTripPurpose(random.nextInt(100));
            Activity secondAct = factory.createActivityFromCoord(actType, getCoordFromPurpose(actType, home.getCoord()));
            secondAct.setEndTime(home.getEndTime().seconds()+getActivityDuration(actType));
            plan.addActivity(secondAct);
            plan.addLeg(leg);
            plan.addActivity(factory.createActivityFromCoord("home", home.getCoord()));
            person.addPlan(plan);
            population.addPerson(person);
        }
    }

    private Coord getCoordFromPurpose(String purpose, Coord homeCoord){
        switch (purpose){
            case "social":
            case "home": return getRandomCoordInGrid();

            case "work":
            case "education": return getCoordCityCenter();

            case "leisure": return getLeisureCoord();
            case "shopping": return getShoppingCoord(homeCoord);
        }
        throw new RuntimeException("Trip purpose "+purpose + "is undefined.");
    }

    private double getActivityDuration(String purpose){
        switch (purpose){
            case "social": return 3+random.nextDouble()*3600.;
            case "home": return 10+random.nextDouble()* 3600.;// should not be called for home.

            case "work": return 8+random.nextDouble()*3600.;
            case "education": return 6+random.nextDouble()*3600.;

            case "leisure": return 3+random.nextDouble()*3600.;
            case "shopping": return 1+random.nextDouble()*3600.;
        }
        throw new RuntimeException("Trip purpose "+purpose + "is undefined.");
    }

    private String getTravelMode(int number){
        if (number <40) return "car";
        else if(number < 65) return "bicycle";
        else if (number < 100) return "motorcycle";
//        else if (number < 80) return "walk";
//        else if (number < 100) return "pt";
        else {
            throw new RuntimeException("Chose a number less than 100.");
        }
    }

    private String getTripPurpose(int number){
        if (number <40) return "work";
        else if(number < 65) return "education";
        else if (number < 75) return "social";
        else if (number < 85) return "leisure";
        else if (number < 100) return "shopping";
        else {
            throw new RuntimeException("Chose a number less than 100.");
        }
    }

    private Coord getRandomCoordInGrid(){
        double x = getRandomNumber();
        double y = getRandomNumber();
        return new Coord(x, y);
    }

    private Coord getCoordCityCenter(){
        //like city center
        double x = GridNetwork.LengthOfGrid / 4 + random.nextDouble() * GridNetwork.LengthOfGrid / 2;
        double y = GridNetwork.LengthOfGrid / 4 + random.nextDouble() * GridNetwork.LengthOfGrid / 2;
        return new Coord(x, y);
    }

    private boolean isCenter(double number){
        return number > GridNetwork.LengthOfGrid / 4 && number < 3 * GridNetwork.LengthOfGrid;
    }

    private double getRandomNumber(){
        return random.nextDouble() * GridNetwork.LengthOfGrid;
    }

    private Coord getLeisureCoord(){
        //outside city center
        double x = getRandomNumber();
        while (!isCenter(x)) {
            x = getRandomNumber();
        }
        double y = getRandomNumber();
        while(!isCenter(y)){
            y= getRandomNumber();
        }
        return new Coord(x, y);
    }

    private Coord getShoppingCoord(Coord home){
        return new Coord( home.getX() + random.nextDouble() * 1000., home.getY() + random.nextDouble() * 1000.);
    }
}
