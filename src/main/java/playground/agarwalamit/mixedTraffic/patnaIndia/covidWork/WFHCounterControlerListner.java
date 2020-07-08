package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import org.apache.log4j.Logger;;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.PersonFilter;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author amit
 */
public class WFHCounterControlerListner implements IterationEndsListener, ShutdownListener {

    public static final Logger LOGGER = Logger.getLogger(WFHCounterControlerListner.class);

    public static final String FILENAME = "workFromHomeStats";

    private final Population population;
    final private BufferedWriter out;
    final private String fileName;
    private final ControlerConfigGroup controlerConfigGroup;
    @com.google.inject.Inject(optional=true) private final PersonFilter personFilter;
    private final WFHActivity wfhActivity;

    @Inject
    WFHCounterControlerListner(Population population, OutputDirectoryHierarchy controlerIO,
                               ControlerConfigGroup controlerConfigGroup, PersonFilter personFilter,
                               WFHActivity wfhActivity){
        this.controlerConfigGroup = controlerConfigGroup;
        this.population = population;
        this.fileName = controlerIO.getOutputFilename(FILENAME);
        this.out = IOUtils.getBufferedWriter(this.fileName + ".txt");
        this.personFilter = personFilter;
        this.wfhActivity = wfhActivity;
        try {
            if(this.personFilter==null) {
                this.out.write("ITERATION\tcounter\tavg. score\t best. score\t worst. score\n");
            } else {
                this.out.write("ITERATION\tuserGroup\tcounter\tavg. score\t best. score\t worst. score\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("The data is not written. "+e);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (this.personFilter==null){

            List<Double> scores = new ArrayList<>();
            for(Person person: population.getPersons().values()) {
                Plan plan = person.getSelectedPlan();
                if (isWFHPlan(plan)) {
                    scores.add(plan.getScore());
                }
            }

            write(event.getIteration(), null, scores);

        } else {
            SortedMap<String, List<Double>> userGroup2Score = new TreeMap<>();
            this.personFilter.getUserGroupsAsStrings().forEach(ug-> userGroup2Score.put(ug, new ArrayList<>()));

            for (Person person : this.population.getPersons().values()) {
                if (isWFHPlan(person.getSelectedPlan())) {
                    userGroup2Score.get(this.personFilter.getUserGroupAsStringFromPersonId(person.getId())).add(person.getSelectedPlan().getScore());
                }
            }

            userGroup2Score.entrySet().stream().forEach(e -> write(event.getIteration(), e.getKey(), e.getValue()));
        }
    }

    private void write(int iteration, String ug, List<Double> scores){
        if (scores.size()==0) return;

        double counter = scores.size();
        double avgScore = scores.stream().mapToDouble(i -> i).average().orElse(0.) / counter;
        double maxScore = Collections.max(scores);
        double minScore = Collections.min(scores);

        try {
            this.out.write(iteration+"\t");
            if (ug!=null){
                this.out.write(ug+"\t");
            }
            this.out.write(counter+"\t"+ avgScore +"\t" + maxScore +"\t"+ minScore+"\n");
            this.out.flush();
        } catch (IOException e) {
            throw new RuntimeException("The data is not written. "+e);
        }
    }

    private boolean isWFHPlan(Plan plan) {
        // no plea element should have leg
        boolean wfhAct = false;
        for (PlanElement planElement : plan.getPlanElements()){
            if (planElement instanceof Leg) {
                if (wfhAct) {
                    LOGGER.warn("In a plan, a "+this.wfhActivity.getWfhActivityNamePrefix()+" is found which has a leg mode in it.");
                }
            } else if (planElement instanceof Activity){

                if (((Activity)planElement).getType().startsWith(this.wfhActivity.getWfhActivityNamePrefix())) {
                    wfhAct= true;
                }
            } else {
                throw new RuntimeException("Unrecognized plan element... "+planElement);
            }
        }
        return wfhAct;
    }

    @Override
    public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
        try {
            this.out.close();
        } catch (IOException e) {
            throw new RuntimeException("The data is not written. "+e);
        }
    }

}
