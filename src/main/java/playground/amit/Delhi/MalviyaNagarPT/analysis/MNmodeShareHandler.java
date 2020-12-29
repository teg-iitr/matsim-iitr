package playground.amit.Delhi.MalviyaNagarPT.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

public class MNmodeShareHandler implements PersonDepartureEventHandler {

//    private Map<String, Integer> mode2NumOfLegs = new HashMap<>();

    private Map<Id<Person>,String> person2PtUsers = new HashMap<>();
    private Map<Id<Person>,String> person2WalkUsers= new HashMap<>();

    private Map<String, Integer> stuckCnt = new HashMap<>();

    private int cntOfPtTrips;
    private int cntOfPtUsers;
    private int cntOfPtStuck;

    private int cntOfWalkUsers;
    private int cntOfWalkTrips;
    private int getCntOfWalkStuck;


    @Override
    public void handleEvent(PersonDepartureEvent event) {
        String pt= TransportMode.pt;
        String walk=TransportMode.walk;

        if (event.getLegMode().equals(pt)) {
            this.cntOfPtTrips = this.cntOfPtTrips + 1;

            if(!this.person2PtUsers.containsKey(event.getPersonId())) {
                this.person2PtUsers.put(event.getPersonId(), event.getLegMode());
                this.cntOfPtUsers = this.cntOfPtUsers + 1;
            }
        }
        else if (event.getLegMode().equals(walk)){
            this.cntOfWalkTrips = this.cntOfWalkTrips + 1;
            if(!this.person2WalkUsers.containsKey(event.getPersonId())) {
                this.person2WalkUsers.put(event.getPersonId(), event.getLegMode());
                this.cntOfWalkUsers = this.cntOfWalkUsers + 1;
            }
        }
    }

//    @Override
//    public void handleEvent(PersonStuckEvent event) {
//        String mode = event.getLegMode();
//
//        if ( this.stuckCnt.containsKey(mode)) {
//            int countSoFar = this.stuckCnt.get(mode);
//            this.stuckCnt.put(mode, countSoFar+1);
//        } else {
//            this.stuckCnt.put(mode, 1);
//        }
//        if (this.stuckCnt.containsKey("pt")){
//            this.cntOfPtStuck = this.stuckCnt.get("pt");
//        } else
//            this.cntOfPtStuck = 0;
//    }

    List<Double> modeShare(){
        int totalTrips = this.cntOfPtTrips+this.cntOfWalkTrips;
        double ptShare = 100. * (double) this.cntOfPtTrips / totalTrips;
        double walkShare = 100. * (double) this.cntOfWalkTrips / totalTrips;
        List<Double> modeShare_mn = new ArrayList<>();
        modeShare_mn.add(ptShare);
        modeShare_mn.add(walkShare);


        return modeShare_mn;
    }

//    int getCntOfPtTrips() {
//        return cntOfPtTrips;
//    }
//
//    int getCntOfPtUsers() {
//        return cntOfPtUsers;
//    }
//
//    int getCntOfWalkTrips() {
//        return cntOfWalkTrips;
//    }
//
//    int getCntOfWalkUsers() {
//        return cntOfWalkUsers;
//    }


}
