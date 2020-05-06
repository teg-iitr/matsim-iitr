package playground.agarwalamit.Chandigarh;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import playground.agarwalamit.utils.LoadMyScenarios;

import java.util.List;

public class PlansPrep {
    private final String out_network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone_fixed.xml.gz";
    private final String out_plans = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_plans_test.xml.gz";

    public static void main(String[] args) {
        new PlansPrep().run();
    }

    private void run(){
        Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(out_network);
        Population population = scenario.getPopulation();

        // 100 persons: starting from 1A, 1C, 1E, 2C, 2E, 3C, 3E, 4F
       List<String> startLinks = List.of(ChandigarhConstants.link_1A, ChandigarhConstants.link_1C, ChandigarhConstants.link_1E,
               ChandigarhConstants.link_2C, ChandigarhConstants.link_2E, ChandigarhConstants.link_3C, ChandigarhConstants.link_3E, ChandigarhConstants.link_4F);
        for (String link : startLinks) {
            Activity firstAct = population.getFactory().createActivityFromLinkId(ChandigarhConstants.start_act_type,Id.createLinkId(link));
            firstAct.setEndTime(10.*3600+ 900 *  MatsimRandom.getRandom().nextDouble());

            for (int i =0; i<100; i ++){
                Person person = population.getFactory().createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = population.getFactory().createPlan();
                plan.addActivity(firstAct);
                plan.addLeg(population.getFactory().createLeg(TransportMode.car));
                Activity secondAct = population.getFactory().createActivityFromLinkId(ChandigarhConstants.end_act_type,Id.createLinkId(getDestinationLinkIdFrom1A(link, i)));
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
//            starting from 1A, 1C, 1E, 2C, 2E, 3C, 3E, 4F
            case ChandigarhConstants.link_1A : //2B, 2D, 3B, 3D, 4A
                if (number < 20 ) return ChandigarhConstants.link_2B; //2B
                else if (number < 40 )  return ChandigarhConstants.link_2D; //2D
                else if (number < 60 )  return ChandigarhConstants.link_3B; //3B
                else if (number < 80 )  return ChandigarhConstants.link_3D; //3D
                else if (number < 100 )  return ChandigarhConstants.link_4A; //4A
                else throw new RuntimeException("Unknown destination for origin '1A'.");

            case ChandigarhConstants.link_1C: //2B, 2D, 3B, 3D, 4A
                if (number < 20 ) return ChandigarhConstants.link_2B; //2B
                else if (number < 40 )  return ChandigarhConstants.link_2D; //2D
                else if (number < 60 )  return ChandigarhConstants.link_3B; //3B
                else if (number < 80 )  return ChandigarhConstants.link_3D; //3D
                else if (number < 100 )  return ChandigarhConstants.link_4A; //4A
                else throw new RuntimeException("Unknown destination for origin '1C'.");

            case ChandigarhConstants.link_1E: //2B, 2D, 3B, 3D, 4A
                if (number < 20 ) return ChandigarhConstants.link_2B; //2B
                else if (number < 40 )  return ChandigarhConstants.link_2D; //2D
                else if (number < 60 )  return ChandigarhConstants.link_3B; //3B
                else if (number < 80 )  return ChandigarhConstants.link_3D; //3D
                else if (number < 100 )  return ChandigarhConstants.link_4A; //4A
                else throw new RuntimeException("Unknown destination for origin '1E'.");

            case ChandigarhConstants.link_2C: //1B 1D 1F 3B 3D 4A
                if (number < 16 )  return ChandigarhConstants.link_1B; //1B
                else if (number < 32 )  return ChandigarhConstants.link_1D; //1D
                else if (number < 48 )  return ChandigarhConstants.link_1F; //1F
                else if (number < 64 )  return ChandigarhConstants.link_3B; //3B
                else if (number < 80 )  return ChandigarhConstants.link_3D; //3D
                else if (number < 100 )  return ChandigarhConstants.link_4A; // 4A
                else throw new RuntimeException("Unknown destination for origin '2C'.");

            case ChandigarhConstants.link_2E: //1B 1D 1F 3B 3D 4A
                if (number < 16 )  return ChandigarhConstants.link_1B; //1B
                else if (number < 32 )  return ChandigarhConstants.link_1D; //1D
                else if (number < 48 )  return ChandigarhConstants.link_1F; //1F
                else if (number < 64 )  return ChandigarhConstants.link_3B; //3B
                else if (number < 80 )  return ChandigarhConstants.link_3D; //3D
                else if (number < 100 )  return ChandigarhConstants.link_4A; // 4A
                else throw new RuntimeException("Unknown destination for origin '2E'.");

            case ChandigarhConstants.link_3C : //1B 1D 1F 2B 2D
                if (number < 20 )  return ChandigarhConstants.link_1B; //1B
                else if (number < 40 )  return ChandigarhConstants.link_1D; //1D
                else if (number < 60 )  return ChandigarhConstants.link_1F; //1F
                else if (number < 80 ) return ChandigarhConstants.link_2B; //2B
                else if (number < 100 )  return ChandigarhConstants.link_2D; //2D
                else throw new RuntimeException("Unknown destination for origin '3C'.");

            case ChandigarhConstants.link_3E:
                if (number < 20 )  return ChandigarhConstants.link_1B; //1B
                else if (number < 40 )  return ChandigarhConstants.link_1D; //1D
                else if (number < 60 )  return ChandigarhConstants.link_1F; //1F
                else if (number < 80 ) return ChandigarhConstants.link_2B; //2B
                else if (number < 100 )  return ChandigarhConstants.link_2D; //2D
                else throw new RuntimeException("Unknown destination for origin '3E'.");

            case ChandigarhConstants.link_4F:
                if (number < 20 )  return ChandigarhConstants.link_1B; //1B
                else if (number < 40 )  return ChandigarhConstants.link_1D; //1D
                else if (number < 60 )  return ChandigarhConstants.link_1F; //1F
                else if (number < 80 ) return ChandigarhConstants.link_2B; //2B
                else if (number < 100 )  return ChandigarhConstants.link_2D; //2D
                else throw new RuntimeException("Unknown destination for origin '4F'.");
        }
        throw new RuntimeException("Unknown destination.");
    }
}
