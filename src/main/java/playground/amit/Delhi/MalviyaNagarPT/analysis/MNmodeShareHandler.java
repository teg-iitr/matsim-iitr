package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypeIdentifier;

import java.util.*;


public class MNmodeShareHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler
                                            ,ActivityEndEventHandler, TransitDriverStartsEventHandler {



    private final Map<Id<Person>, List<String>> person_Modes = new HashMap<>();
    private final List<Id<Person>> driverId= new ArrayList<>();
    private final String pt = TransportMode.pt;
    private final String walk = TransportMode.walk;




    private final Map<Id<Person>, String> person_MainMode = new HashMap<>();


    int  cntOfPtTrips;
    int cntOfWalkTrips;

    @Override
    public void reset(int iteration) {
        this.person_MainMode.clear();
        this.person_Modes.clear();
        this.driverId.clear();
    }


    @Override
    public void handleEvent(PersonDepartureEvent event) {
    Id<Person> personId = event.getPersonId();

//        if( driverId.remove(personId) ) {
//            return;
//        }

        if(person_Modes.get(personId) == null){
            List<String> leglist = new ArrayList<>();
            leglist.add(event.getLegMode());
            person_Modes.put(event.getPersonId(), leglist );
        } else{
            List<String> allModes= person_Modes.get(personId);
            allModes.add(event.getLegMode());
            person_Modes.put(personId,allModes);
        }

        for(Map.Entry<Id<Person>, List<String>> persMod:person_Modes.entrySet()){
            if (persMod.getValue().contains(pt)){
                person_MainMode.put(event.getPersonId(), pt);
            } else {
                person_MainMode.put(event.getPersonId(), event.getLegMode());
            }
        }
        cntOfPtTrips = Collections.frequency(person_MainMode.values(), pt);
        cntOfWalkTrips= Collections.frequency(person_MainMode.values(), walk);
    }


    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        driverId.add(event.getDriverId());
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





    public int getCntOfPtTrips() {
        return cntOfPtTrips;
    }

    public int getCntOfWalkTrips() {
        return cntOfWalkTrips;
    }


    public Map<Id<Person>, List<String>> getPerson_Modes() {
        return person_Modes;
    }

    public Map<Id<Person>, String> getPerson_MainMode() {
        return person_MainMode;
    }




}
