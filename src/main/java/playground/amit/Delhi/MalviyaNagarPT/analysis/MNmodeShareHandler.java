package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;

import java.util.*;


public class MNmodeShareHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler
                                            ,ActivityEndEventHandler, TransitDriverStartsEventHandler {


    private final Map<Id<Person>, List<String>> person_Modes = new HashMap<>();
    private final List<Id<Person>> driverIds = new ArrayList<>();


    @Override
    public void reset(int iteration) {
        this.person_Modes.clear();
        this.driverIds.clear();
    }


    @Override
    public void handleEvent(PersonDepartureEvent event) {
    Id<Person> personId = event.getPersonId();

        if( driverIds.remove(personId) ) {
            return;
        }

        if(person_Modes.get(personId) == null){
            List<String> leglist = new ArrayList<>();
            leglist.add(event.getLegMode());
            person_Modes.put(event.getPersonId(), leglist );
        } else{
            List<String> allModes= person_Modes.get(personId);
            allModes.add(event.getLegMode());
            person_Modes.put(personId,allModes);
        }

    }


    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        driverIds.add(event.getDriverId());
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {

    }

    @Override
    public void  handleEvent(ActivityStartEvent event){

    }


    @Override
    public void handleEvent(PersonArrivalEvent event) {

    }


    public Map<Id<Person>, List<String>> getPerson_Modes() {
//        for (List<String> modes : person_Modes.values()) {
//            if (modes.contains("pt")) {
//                cntOfPtTrips += cntOfPtTrips;
//            } else if (modes.contains("car")) {
//            } else {
//                cntOfWalkTrips += cntOfWalkTrips;
//            }
//        }
        return person_Modes;
    }

//    public Map<Id<Person>, String> getPerson_MainMode() {
//        for(Id<Person> pId : person_Modes.keySet()) {
//            List<String> modes = person_Modes.get(pId);
//        }
//        for(List<String> modes : person_Modes.values()){
//            if (modes.contains("pt")) {
//
//            }
//        }
//
//        for(Map.Entry<Id<Person>, List<String>> persMod :person_Modes.entrySet()){
//            if (persMod.getValue().contains(pt)){
//                person_MainMode.put(persMod.getKey(), pt);
//            } else if (persMod.getValue().contains(TransportMode.car)){
//                person_MainMode.put(persMod.getKey(), TransportMode.car);
//            }else{
//                person_MainMode.put(persMod.getKey(), walk);
//            }
//        }
//        return person_MainMode;
//    }

    public List<Id<Person>> getDriverIds() {
        return driverIds;
    }



}
