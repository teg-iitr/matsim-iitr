package playground.agarwalamit.Chandigarh;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import playground.agarwalamit.utils.LoadMyScenarios;

public class PlansPrep {
    private final String out_network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone.xml.gz";
    private final String out_plans = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_plans.xml.gz";

    public static void main(String[] args) {
        new PlansPrep().run();
    }

    private void run(){
        Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(out_network);
        Population population = scenario.getPopulation();

        // 100 persons: starting from 1A, 1C, 1E, 2C, 2E, 3C, 3E, 4A
        String [] startLinks = new String [] {"3346","422","2280","3337","3300","2395","3502","631"} ;
        for (String link : startLinks) {
            Activity firstAct = population.getFactory().createActivityFromLinkId("start",Id.createLinkId(link));
            firstAct.setEndTime(6.*3600+ 3600 *  MatsimRandom.getRandom().nextDouble());

            for (int i =0; i<100; i ++){
                Person person = population.getFactory().createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = population.getFactory().createPlan();
                plan.addActivity(firstAct);
                plan.addLeg(population.getFactory().createLeg(TransportMode.car));
                Activity secondAct = population.getFactory().createActivityFromLinkId("end",Id.createLinkId(getDestinationLinkIdFrom1A(link, i)));
                plan.addActivity(secondAct);
                person.addPlan(plan);
                population.addPerson(person);
            }
        }


        //repeat above exercise for others.

        new PopulationWriter(population).write(out_plans);
    }

    private String getDestinationLinkIdFrom1A(String link, int number){
        // there could be any other distribution rather than uniform distribution
        switch (link) {
            case "3346" : //2B, 2D, 3B, 3D, 4A
                if (number < 20 ) return "3359"; //2B
                else if (number < 40 )  return "1303-1676"; //2D
                else if (number < 60 )  return "2374"; //3B
                else if (number < 80 )  return "3494"; //3D
                else if (number < 100 )  return "3379"; //4A
                else throw new RuntimeException("Unknown destination for origin '1A'.");

            case "422": //2B, 2D, 3B, 3D, 4A
                if (number < 20 ) return "3359";
                else if (number < 40 )  return "1303-1676";
                else if (number < 60 )  return "2374";
                else if (number < 80 )  return "3494";
                else if (number < 100 )  return "3379";
                else throw new RuntimeException("Unknown destination for origin '1C'.");

            case "2280": //2B, 2D, 3B, 3D, 4A
                if (number < 20 ) return "3359";
                else if (number < 40 )  return "1303-1676";
                else if (number < 60 )  return "2374";
                else if (number < 80 )  return "3494";
                else if (number < 100 )  return "3379";
                else throw new RuntimeException("Unknown destination for origin '1E'.");

            case "3337": //1B 1D 1F 3B 3D 4A
                if (number < 16 )  return "538-531"; //1B
                else if (number < 32 )  return "2282"; //1D
                else if (number < 48 )  return "3354"; //1F
                else if (number < 64 )  return "2374"; //3B
                else if (number < 80 )  return "3494"; //3D
                else if (number < 100 )  return "3379"; // 4A
                else throw new RuntimeException("Unknown destination for origin '2C'.");

            case "3300": //1B 1D 1F 3B 3D 4A
                if (number < 16 )  return "538-531"; //1B
                else if (number < 32 )  return "2282"; //1D
                else if (number < 48 )  return "3354"; //1F
                else if (number < 64 )  return "2374"; //3B
                else if (number < 80 )  return "3494"; //3D
                else if (number < 100 )  return "3379"; // 4A
                else throw new RuntimeException("Unknown destination for origin '2E'.");

            case "2395" : //1B 1D 1F 2B 2D
                if (number < 20 )  return "538-531"; //1B
                else if (number < 40 )  return "2282"; //1D
                else if (number < 60 )  return "3354"; //1F
                else if (number < 80 ) return "3359"; //2B
                else if (number < 100 )  return "1303-1676"; //2D
                else throw new RuntimeException("Unknown destination for origin '3C'.");

            case "3502":
                if (number < 20 )  return "538-531"; //1B
                else if (number < 40 )  return "2282"; //1D
                else if (number < 60 )  return "3354"; //1F
                else if (number < 80 ) return "3359"; //2B
                else if (number < 100 )  return "1303-1676"; //2D
                else throw new RuntimeException("Unknown destination for origin '3E'.");

            case "631":
                if (number < 20 )  return "538-531"; //1B
                else if (number < 40 )  return "2282"; //1D
                else if (number < 60 )  return "3354"; //1F
                else if (number < 80 ) return "3359"; //2B
                else if (number < 100 )  return "1303-1676"; //2D
                else throw new RuntimeException("Unknown destination for origin '4F'.");
        }
        throw new RuntimeException("Unknown destination.");
    }
}
