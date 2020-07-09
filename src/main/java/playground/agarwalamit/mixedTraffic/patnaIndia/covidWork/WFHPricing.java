package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.misc.Time;

import java.util.HashMap;
import java.util.Map;

/**
 * A linear penalty for work from home assuming that the working at home is not as good as in office. I think, ideally, it should be factored in with maringal utility of performing.
 *
 * @author amit
 */
public class WFHPricing implements ActivityStartEventHandler, ActivityEndEventHandler, AfterMobsimListener {

    private final String TYPE = "PENALTY_WORKING_HOME";
    private final String PARTNER = "SELF";
    private final Map<Id<Person>,PersonActInfo> personsActInfo = new HashMap<>();
    private final double factor;

    @Inject
    private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
    @Inject
    private EventsManager events;
    @Inject
    private WFHActivity wfhActivity;

    public WFHPricing(double factor){
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
                penatlySum += -(act.getDuration()/3600.) * planCalcScoreConfigGroup.getPerforming_utils_hr() * factor;
            }
            Event moneyEvent = new PersonMoneyEvent(24.0*3600., personActInfo.personId, penatlySum, TYPE, PARTNER);
            events.processEvent(moneyEvent);
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        handleActsAndThrowMoneyEvents();
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
                return endTime - startTime;
            } else if (Double.isInfinite(startTime) && Double.isFinite(endTime)) { // e.g. home
                return endTime - 0.;
            } else if (Double.isInfinite(endTime) && Double.isFinite(startTime)) { // last act
                return Time.MIDNIGHT - startTime;
            } else throw new RuntimeException("Start and end times are infinite.");
        }
    }
}
