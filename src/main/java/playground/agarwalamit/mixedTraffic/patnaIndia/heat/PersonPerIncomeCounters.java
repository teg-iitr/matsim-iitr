package playground.agarwalamit.mixedTraffic.patnaIndia.heat;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Amit on 15/09/2020.
 */
public class PersonPerIncomeCounters {

    private final String plansFile ;
    private final String attributesFile;
    public final double [] incomeCategories = {500,750,2000,4000,6250,20000};
    private SortedMap<Double, Integer> incomeToPersonsCounter = new TreeMap<>();
    private final PatnaPersonFilter personFilter = new PatnaPersonFilter();
    private final String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();

    PersonPerIncomeCounters(String plansFile, String attributesFile) {
        this.plansFile = plansFile;
        this.attributesFile = attributesFile;

        for (double inc : incomeCategories) {
            this.incomeToPersonsCounter.put(inc, 0);
        }
    }

    public static void main(String[] args) {
//        String runCase = "run2020_17_BAU";
        String runCase = "BT-b";
        String outFile = "C:/Users/Amit/Documents/git-repos/workingPapers/iitr/2020/papers/economicBenefitsCycling/graphics/"+runCase+"_income_persons.txt";
        String plansFile  = "C:\\Users\\Amit\\Documents\\repos\\runs-svn\\patnaIndia\\run108\\jointDemand\\policies\\0.15pcu\\"+runCase+"\\output_plans.xml.gz"; // for income per person
        String attribtuesFile  = "C:\\Users\\Amit\\Documents\\repos\\runs-svn\\patnaIndia\\run108\\jointDemand\\policies\\0.15pcu\\"+runCase+"\\output_personAttributes.xml.gz"; // for income per person

        PersonPerIncomeCounters personPerIncomeCounters = new PersonPerIncomeCounters(plansFile, attribtuesFile);
        personPerIncomeCounters.process();
        personPerIncomeCounters.writeData(outFile);
    }

    private void writeData (String file) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(file)) {
            writer.write("incomeCategory\tnumberOfPerson\n");
            for(double inc : incomeToPersonsCounter.keySet()){
                writer.write(inc+"\t"+incomeToPersonsCounter.get(inc)+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e);
        }
    }

    private void process(){
        Scenario scenario = LoadMyScenarios.loadScenarioFromPlansAndAttributes(this.plansFile, this.attributesFile);
        scenario.getPopulation().getPersons().entrySet()
                .stream()
                .filter(e->this.personFilter.getUserGroupAsStringFromPersonId(e.getKey()).equals(userGroup))
                .forEach(e -> {
            double inc = (Double) e.getValue().getAttributes().getAttribute(PatnaUtils.INCOME_ATTRIBUTE);
            this.incomeToPersonsCounter.put(inc, this.incomeToPersonsCounter.get(inc)+1);
        });

    }


}
