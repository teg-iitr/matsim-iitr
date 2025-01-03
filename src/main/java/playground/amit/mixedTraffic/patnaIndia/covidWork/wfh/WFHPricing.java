package playground.amit.mixedTraffic.patnaIndia.covidWork.wfh;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A linear penalty for work from home assuming that the working at home is not as good as in office.
 * I think, ideally, it should be factored in with maringal utility of performing.
 *
 * @author amit
 */
public class WFHPricing implements ActivityStartEventHandler, ActivityEndEventHandler, AfterMobsimListener {

    private final static Logger LOGGER = LogManager.getLogger(WFHPricing.class);

    private final String TYPE = "PENALTY_WORKING_HOME";
    private final String PARTNER = "SELF";
    private final Map<Id<Person>,PersonActInfo> personsActInfo = new HashMap<>();
    private final double factor;
    private BufferedWriter out;

    @Inject
    private ScoringConfigGroup planCalcScoreConfigGroup;
    @Inject
    private EventsManager events;
    @Inject
    private WFHActivity wfhActivity;
    @Inject
    private OutputDirectoryHierarchy controlerIO;

    public WFHPricing(double factor){
        LOGGER.info("The WFH factor is "+ factor);
        this.factor = factor;
    }

    @Override
    public void reset(int iteration) {
        this.personsActInfo.clear();
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if(this.wfhActivity.isWFHActivity(event.getActType())) {
            this.personsActInfo.putIfAbsent(event.getPersonId(), new PersonActInfo(event.getPersonId()));

            PersonActInfo personActInfo = this.personsActInfo.get(event.getPersonId());
            personActInfo.acts.putIfAbsent(event.getActType(), new ActInfo(event.getActType())) ;
            ActInfo actInfo = personActInfo.acts.get(event.getActType());
            actInfo.endTime = event.getTime();
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if(this.wfhActivity.isWFHActivity(event.getActType())) {
            this.personsActInfo.putIfAbsent(event.getPersonId(), new PersonActInfo(event.getPersonId()));

            PersonActInfo personActInfo = this.personsActInfo.get(event.getPersonId());
            personActInfo.acts.putIfAbsent(event.getActType(), new ActInfo(event.getActType())) ;
            ActInfo actInfo = personActInfo.acts.get(event.getActType());
            actInfo.startTime = event.getTime();
        }
    }

    private void handleActsAndThrowMoneyEvents(){
        for (PersonActInfo personActInfo: this.personsActInfo.values()) {
            Map<String, ActInfo> actInfo = personActInfo.acts;
            double penatlySum = 0;
            for (ActInfo act : actInfo.values()) {
                double dur = act.getDuration();
                double util_perf = planCalcScoreConfigGroup.getPerforming_utils_hr();
                double ps = -(dur/3600.) * util_perf * factor;
                try {
                    this.out.write(personActInfo.personId+"\t"+act.actType+"\t"+dur+"\t"+util_perf+"\t"+ps+"\n");
                } catch (IOException e) {
                    throw new RuntimeException("Data is not written. "+e);
                }
                penatlySum += ps;
            }
            Event moneyEvent = new PersonMoneyEvent(24.0*3600., personActInfo.personId, penatlySum, TYPE, PARTNER);
            events.processEvent(moneyEvent);
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        this.out = IOUtils.getAppendingBufferedWriter(controlerIO.getIterationFilename(event.getIteration(), "_wfh_penalty"));
       try {
           this.out.write("personId\tact\tduration\tutil_performing\tpenalty\n");
           handleActsAndThrowMoneyEvents();
           this.out.close();
       } catch (IOException e) {
           throw new RuntimeException("Data is not written. "+e);
       }

    }

    private class PersonActInfo {
        Id<Person> personId;
        Map<String, ActInfo> acts;

        PersonActInfo(Id<Person> personId) {
            this.personId = personId;
            acts = new HashMap<>();
        }
    }

    private class ActInfo {
        String actType;

        ActInfo(String type) {
            this.actType = type;
        }

        double startTime = Double.NEGATIVE_INFINITY;
        double endTime = Double.NEGATIVE_INFINITY;

        double getDuration(){
            // wrapping of first and last acts are not important in linear penalty
            if (Double.isFinite(startTime) && Double.isFinite(endTime)) {
                if (endTime > startTime) return endTime - startTime;
                else {//wrapping up
                    return endTime - 0. + Time.MIDNIGHT - startTime;
                }
            } else if (Double.isInfinite(startTime) && Double.isFinite(endTime)) { // e.g. home
                return endTime - 0.;
            } else if (Double.isInfinite(endTime) && Double.isFinite(startTime)) { // last act
                return Time.MIDNIGHT - startTime;
            } else throw new RuntimeException("Start and end times are infinite.");
        }
    }
}
