package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * A linear penalty for work from home assuming that the working at home is not as good as in office. I think, ideally, it should be factored in with maringal utility of performing.
 */
public class WFHPricing implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler {

    private final String TYPE = "PENALTY_WORKING_HOME";
    private final String PARTNER = "SELF";
    private final Map<Id<Person>,Double> personId2ActStartTime = new HashMap<>();
    private final double factor;
    private final String WFHMode;
    private final List<String> actTypes;

    @Inject
    private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
    @Inject
    private EventsManager events;

    public WFHPricing(double factor, String WFHMode, List<String> actTypes){
        this.factor = factor;
        this.WFHMode = WFHMode;
        this.actTypes = actTypes;
    }

    @Override
    public void reset(int iteration) {
        this.personId2ActStartTime.clear();
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (personId2ActStartTime.get(event.getPersonId())!=null && (this.actTypes.contains(event.getActType())) ){
            double actDuration = event.getTime() - personId2ActStartTime.remove(event.getPersonId());
            double amount2Pay = - (actDuration/3600.) * planCalcScoreConfigGroup.getPerforming_utils_hr() * factor;

            Event moneyEvent = new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount2Pay, TYPE, PARTNER);
            events.processEvent(moneyEvent);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (personId2ActStartTime.get(event.getPersonId())!=null && (this.actTypes.contains(event.getActType())) ){
            personId2ActStartTime.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        //register here if the travel mode is WFHWalk
        if (event.getLegMode().equalsIgnoreCase(this.WFHMode)){
            personId2ActStartTime.put(event.getPersonId(), 0.);
        }
    }
}
