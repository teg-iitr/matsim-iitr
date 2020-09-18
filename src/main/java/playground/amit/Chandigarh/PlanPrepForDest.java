package playground.amit.Chandigarh;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;

import org.matsim.core.population.PopulationUtils;
import playground.amit.utils.LoadMyScenarios;

public class PlanPrepForDest {
	private final String out_network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone_fixed.xml.gz";
	private final String out_plans="C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_plans_4Cadyts.xml.gz";

	private Population population;
	
//	public class DestData	{
//		String destID;
//		int no;
//		 public DestData(String destID,  int no) {
//		        this.destID = destID;
//		        this.no = no;
//		    }
//	}
//	public class OriginData{
//		public String LinkId;
//		public ArrayList<DestData> knownData = new ArrayList<DestData>();
//		public ArrayList<DestData> unknownData = new ArrayList<DestData>();
//		public OriginData(String LinkId,ArrayList<DestData> knownData,ArrayList<DestData> unknownData) {
//			this.LinkId=LinkId;
//			this.knownData=knownData;
//			this.unknownData=unknownData;		
//		}
//	}
//	OriginData o1=new OriginData("3346",new ArrayList<PlanPrepForDest.DestData>(Arrays.asList(new DestData("", 22),new DestData("", 54))));

	
	//plans for unknown 2a
	public static void main(String args[]) {	
		new PlanPrepForDest().generatePlans();
//		System.out.println(ChandigarhConstants.Unknown_Destinations_1A_1C_1E);
	}
	
	private void generatePlans() {
		Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(out_network);
        population = scenario.getPopulation();

      //plans for unknown destinations
        generatePlans(ChandigarhConstants.link_1A,88,ChandigarhConstants.Unknown_Destinations_1A_1C_1E);
        generatePlans(ChandigarhConstants.link_1C,14,ChandigarhConstants.Unknown_Destinations_1A_1C_1E);
        generatePlans(ChandigarhConstants.link_1E,57,ChandigarhConstants.Unknown_Destinations_1A_1C_1E);

        generatePlans(ChandigarhConstants.link_2C,19,ChandigarhConstants.Unknown_Destinations_2C_2E_towards_3A);
        generatePlans(ChandigarhConstants.link_2C,34,ChandigarhConstants.Unknown_Destinations_2C_2E_towards_2F);

        generatePlans(ChandigarhConstants.link_2E,74, ChandigarhConstants.Unknown_Destinations_2C_2E_towards_3A);
        generatePlans(ChandigarhConstants.link_2E,22,ChandigarhConstants.Unknown_Destinations_2C_2E_towards_2F);

        generatePlans(ChandigarhConstants.link_3C,83,ChandigarhConstants.Unknown_Destinations_3C_3E_4F);
        generatePlans(ChandigarhConstants.link_3E,66,ChandigarhConstants.Unknown_Destinations_3C_3E_4F);
        generatePlans(ChandigarhConstants.link_4F,142,ChandigarhConstants.Unknown_Destinations_3C_3E_4F);

        new PopulationWriter(population).write(out_plans);
	}
	private void generatePlans(String startLinkId, int numberOfVehicles, List<String> destinations) {
		for (int i =0; i<numberOfVehicles; i++){
            Person person = population.getFactory().createPerson(Id.createPersonId(population.getPersons().size()));
            Plan plan = population.getFactory().createPlan();
            Activity firstAct = population.getFactory().createActivityFromLinkId(ChandigarhConstants.start_act_type,Id.createLinkId(startLinkId));
            firstAct.setEndTime(10.*3600+ 900 *  MatsimRandom.getRandom().nextDouble());
            plan.addActivity(firstAct);
            plan.addLeg(population.getFactory().createLeg(TransportMode.car));

              for(String destLink:destinations)	{
                  Plan newPlan = population.getFactory().createPlan();
                  PopulationUtils.copyFromTo(plan, newPlan);
                  Activity secondAct = population.getFactory().createActivityFromLinkId(ChandigarhConstants.end_act_type, Id.createLinkId(destLink));
                  newPlan.addActivity(secondAct);
                  person.addPlan(newPlan); // this must be inside destinations links loop, otherwise ONLY one plan will be generated.
              }

            population.addPerson(person);
        }
	}
	
}
