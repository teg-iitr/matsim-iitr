package playground.nidhi.practice.eventHandlingPract.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PtHandler implements TransitDriverStartsEventHandler {
    private final static List<Id> ptDriverIDs = new ArrayList<Id>();
    private final static Logger log = Logger.getLogger(String.valueOf(PtHandler.class));
    @Override
    public void reset(int iteration) {
      ptDriverIDs.clear();
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        Id ptDriverId = event.getDriverId();

        if(! ptDriverIDs.contains(ptDriverId)){
            ptDriverIDs.add(ptDriverId);
            System.out.println(ptDriverIDs);
        }


    }

    public List<Id> getPtDriverIDs(){
        if(ptDriverIDs.isEmpty()){
            log.warning("No pt driver(s) identified. List is empty");
        }
        return ptDriverIDs;
    }

}
