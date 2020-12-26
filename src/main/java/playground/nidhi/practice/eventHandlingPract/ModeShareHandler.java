package playground.nidhi.practice.eventHandlingPract;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypeIdentifier;
import playground.amit.analysis.modalShare.ModalShareEventHandler;
import playground.amit.utils.PersonFilter;

import java.util.*;

public class ModeShareHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler, PersonStuckEventHandler {
//
//    private final ModalShareEventHandler delegate = new ModalShareEventHandler();
//    private final String userGroup;
//    private final PersonFilter pf;
//
//	public ModeShareHandler () {
//        this(null,null);
//    }
//
//	public ModeShareHandler (final String userGroup, final PersonFilter pf) {
//        this.userGroup = userGroup;
//        this.pf = pf;
//        if ( (this.userGroup == null && this.pf != null) || (this.userGroup != null && this.pf == null) ) {
//            throw new RuntimeException("Either of user group or person filter is null. Aborting...");
//        }
//    }
//
//    @Override
//    public void reset(int iteration) {
//        this.delegate.reset(iteration);
//    }
//
//    @Override
//    public void handleEvent(PersonStuckEvent event) {
//        if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
//        else {
//            if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
//                this.delegate.handleEvent(event);
//            }
//        }
//    }
//
//    @Override
//    public void handleEvent(ActivityStartEvent event) {
//        if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
//        else {
//            if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
//                this.delegate.handleEvent(event);
//            }
//        }
//    }
//
//    @Override
//    public void handleEvent(TransitDriverStartsEvent event) {
//        if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
//        else {
//            if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getDriverId()))) {
//                this.delegate.handleEvent(event);
//            }
//        }
//    }
//
//    @Override
//    public void handleEvent(PersonDepartureEvent event) {
//        if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
//        else {
//            if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
//                this.delegate.handleEvent(event);
//            }
//        }
//    }
//
//    public SortedMap<String, Integer> getMode2numberOflegs() {
//        return this.delegate.getMode2numberOflegs();
//    }
       private final SortedMap<String, Integer> mode2numberOflegs = new TreeMap<>();
        private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
        // agents who first departs with transitWalk and their subsequent modes are stored here until it starts a regular act (home/work/leis/shop)
        private final Map<Id<Person>, List<String>> person2Modes = new HashMap<>();

        @Override
        public void reset(int iteration) {
            this.mode2numberOflegs.clear();
            this.transitDriverPersons.clear();
            this.person2Modes.clear();
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            String legMode = event.getLegMode();
            Id<Person> personId = event.getPersonId();

            if( transitDriverPersons.remove(personId) ) {
                // transit driver drives "car" which should not be counted in the modal share.
                return;
            }

            //at this point, it could be main leg (e.g. car/bike) or start of a stage activity (e.g. car/pt interaction)
            List<String> usedModes = person2Modes.getOrDefault(event.getPersonId(), new ArrayList<>());
            usedModes.add(event.getLegMode());
            person2Modes.put(event.getPersonId(), usedModes);

            System.out.println(event.getLegMode()+ " " +event.getPersonId());
        }

        @Override
        public void handleEvent(TransitDriverStartsEvent event) {
//            transitDriverPersons.add(event.getDriverId());
            System.out.println(event.getDepartureId()+" "+event.getTransitLineId()+ " "+ event.getTransitRouteId()+ " "+ event.getVehicleId());
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if( person2Modes.containsKey(event.getPersonId()) ) {
                if( ! StageActivityTypeIdentifier.isStageActivity(event.getActType()) ) {
                    String legMode = getMainMode(person2Modes.remove(event.getPersonId()));
                    storeMode(legMode);
                    System.out.println(event.getCoord());
                } else {
                    // else continue storing leg modes
                }
            } else {
                // no need to throw exception for the persons who are not departed. This might happen for the cases where only activities are present in a plan (work-home-plans).
//            throw new RuntimeException("Person "+event.getPersonId()+" is not registered.");
            }
        }

        @Override
        public void handleEvent(PersonStuckEvent event) {
            if( person2Modes.containsKey( event.getPersonId()) ) {
                // since mode for transit users is determined at activity start, so storing mode for stuck agents so that, these can be handeled later.
                List<String> modes = person2Modes.get(event.getPersonId());
                modes.add(event.getLegMode());
            }
        }

        private void storeMode(String legMode) {
            mode2numberOflegs.merge(legMode, 1, (a, b) -> a + b);
        }

        private void handleRemainingTransitUsers(){
            Logger.getLogger(ModalShareEventHandler.class).warn("A few transit users are not handle due to stuckAndAbort. Handling them now.");
            for(Id<Person> pId : person2Modes.keySet()){
                String legMode = getMainMode(person2Modes.get(pId));
                storeMode(legMode);
            }
            person2Modes.clear();
        }

        public SortedMap<String, Integer> getMode2numberOflegs() {
            if (!person2Modes.isEmpty()) {
                handleRemainingTransitUsers();
            }
            return mode2numberOflegs;
        }

        private String getMainMode(List<String> modes){
            if (modes.size()==1) return modes.get(0).equals(TransportMode.transit_walk) ? TransportMode.walk: modes.get(0);

            if (modes.contains(TransportMode.pt)) return TransportMode.pt;
            if (modes.contains(TransportMode.car)) return TransportMode.car;
            if (modes.contains(TransportMode.bike)) return TransportMode.bike;
            if (modes.contains(TransportMode.walk)) return TransportMode.walk;
            if (modes.contains(TransportMode.ride)) return TransportMode.ride;

            if (modes.contains(TransportMode.transit_walk) || modes.contains(TransportMode.access_walk) || modes.contains(TransportMode.egress_walk)) {
                return TransportMode.walk;
            }

            throw new RuntimeException("Unknown mode(s) "+ modes.toString());
        }
}