package playground.shivam.signals;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public class LanesEventReader implements MatsimReader {
    private MatsimEventsReader delegate;
    public LanesEventReader(EventsManager events) {
        this.delegate = new MatsimEventsReader(events);
        this.delegate.addCustomEventMapper(LaneEnterEvent.EVENT_TYPE, event -> {
            Map<String, String> attributes = event.getAttributes();
            double time = Double.NaN;
            Id<Link> linkId = null;
            Id<Lane> laneId = null;
            Id<Vehicle> vehicleId = null;

            for (Map.Entry<String, String> stringStringEntry : attributes.entrySet()) {
                if ("time".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                    time = Double.parseDouble((String) ((Map.Entry<?, ?>) stringStringEntry).getValue());
                } else if (!"type".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                    if ("link".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                        linkId = Id.createLinkId((String) ((Map.Entry<?, ?>) stringStringEntry).getValue());
                    } else if ("vehicle".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                        vehicleId = Id.createVehicleId((String) ((Map.Entry<?, ?>) stringStringEntry).getValue());
                    } else {
                        laneId = Id.create((String) ((Map.Entry<?, ?>) stringStringEntry).getValue(), Lane.class);
                    }
                }
            }
            return new LaneEnterEvent(time, vehicleId, linkId, laneId);
        });
        this.delegate.addCustomEventMapper(LaneLeaveEvent.EVENT_TYPE, event -> {
            Map<String, String> attributes = event.getAttributes();
            double time = Double.NaN;
            Id<Link> linkId = null;
            Id<Lane> laneId = null;
            Id<Vehicle> vehicleId = null;

            for (Map.Entry<String, String> stringStringEntry : attributes.entrySet()) {
                if ("time".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                    time = Double.parseDouble((String) ((Map.Entry<?, ?>) stringStringEntry).getValue());
                } else if (!"type".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                    if ("link".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                        linkId = Id.createLinkId((String) ((Map.Entry<?, ?>) stringStringEntry).getValue());
                    } else if ("vehicle".equals(((Map.Entry<?, ?>) stringStringEntry).getKey())) {
                        vehicleId = Id.createVehicleId((String) ((Map.Entry<?, ?>) stringStringEntry).getValue());
                    } else {
                        laneId = Id.create((String) ((Map.Entry<?, ?>) stringStringEntry).getValue(), Lane.class);
                    }
                }
            }
            return new LaneLeaveEvent(time, vehicleId, linkId, laneId);
        });
    }
    @Override
    public void readFile(String filename) {
        this.delegate.readFile(filename);
    }

    @Override
    public void readURL(URL url) {
        this.delegate.readURL(url);
    }
}


