package playground.agarwalamit.Chandigarh;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.MatsimRandom;

import playground.agarwalamit.utils.LoadMyScenarios;

public class PlanPrepForDest {
	private final String out_network = "C:\\Users\\DELL\\Desktop\\Matsim input data\\inputs/chandigarh_matsim_net_insideZone.xml.gz";
	private final String out_plans="C:\\Users\\DELL\\Desktop\\Matsim input data\\plans.xml";

	
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
        Population population = scenario.getPopulation();
      //plans for unknown 2a
        initialisePlanGeneration(population,ChandigarhConstants.link_1A,88,ChandigarhConstants.Unknown_Destinations_1A_1C_1E);
        initialisePlanGeneration(population,ChandigarhConstants.link_1C,14,ChandigarhConstants.Unknown_Destinations_1A_1C_1E);
        initialisePlanGeneration(population,ChandigarhConstants.link_1E,57,ChandigarhConstants.Unknown_Destinations_1A_1C_1E);
        initialisePlanGeneration(population,ChandigarhConstants.link_2C,19,List.of(ChandigarhConstants.link_3B,ChandigarhConstants.link_3D));
        initialisePlanGeneration(population,ChandigarhConstants.link_2C,34,List.of(ChandigarhConstants.link_2F));
        initialisePlanGeneration(population,ChandigarhConstants.link_2E,74,List.of(ChandigarhConstants.link_2F));
        initialisePlanGeneration(population,ChandigarhConstants.link_2E,22,List.of(ChandigarhConstants.link_2F));
        initialisePlanGeneration(population,ChandigarhConstants.link_3C,83,ChandigarhConstants.Unknown_Destinations_3C_3E_4F);
        initialisePlanGeneration(population,ChandigarhConstants.link_3E,66,ChandigarhConstants.Unknown_Destinations_3C_3E_4F);
        initialisePlanGeneration(population,ChandigarhConstants.link_4F,142,ChandigarhConstants.Unknown_Destinations_3C_3E_4F);
        new PopulationWriter(population).write(out_plans);
	}
	private static void initialisePlanGeneration(Population population,String id,int no,List<String> destinations) {
		for (int i =0; i<no; i ++){
            Person person = population.getFactory().createPerson(Id.createPersonId(population.getPersons().size()));
            Plan plan = population.getFactory().createPlan();
              for(String destLink:destinations)	{
            	Activity firstAct = population.getFactory().createActivityFromLinkId("start",Id.createLinkId(id));
                firstAct.setEndTime(10.*3600+ 900 *  MatsimRandom.getRandom().nextDouble());
                plan.addActivity(firstAct);
                plan.addLeg(population.getFactory().createLeg(TransportMode.car));
                Activity secondAct = population.getFactory().createActivityFromLinkId("end",Id.createLinkId(destLink));
                plan.addActivity(secondAct);	
            }
            person.addPlan(plan);
            population.addPerson(person);
        }
	}
	
}
