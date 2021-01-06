package playground.nidhi.examJan.ques4;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class CongestionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {

    private Map<Id<Vehicle>, Double> earliestLinkExitTime = new HashMap<>() ;
    private Network network;

    public CongestionHandler(){
        this.network = network ;
    }

    @Override
    public void reset(int iteration) {
        this.earliestLinkExitTime.clear();
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Vehicle> vehId = Id.createVehicleId( event.getPersonId()) ;
        this.earliestLinkExitTime.put(vehId, event.getTime() ) ;
    }
    @Override
    public void handleEvent(LinkEnterEvent event) {
        Link link = network.getLinks().get(event.getLinkId()) ;
        double linkLength = link.getLength();
        double linkSpeed= link.getFreespeed(event.getTime());
        double linkTravelTime = linkLength/linkSpeed;
        this.earliestLinkExitTime.put(event.getVehicleId(), event.getTime()+linkTravelTime ) ;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Link link = network.getLinks().get(event.getLinkId()) ;
        double linkLength = link.getLength();
        double linkSpeed= link.getFreespeed(event.getTime());
        double linkTravelTime = linkLength/linkSpeed;
        this.earliestLinkExitTime.put(event.getVehicleId(), event.getTime()+linkTravelTime ) ;
    }

}
