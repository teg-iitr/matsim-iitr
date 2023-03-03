package org.matsim.api.core.v01.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

public final class LaneEnterEvent extends Event {
    public static final String EVENT_TYPE = "entered lane";
    public static final String ATTRIBUTE_VEHICLE = "vehicle";
    public static final String ATTRIBUTE_LINK = "link";
    public static final String ATTRIBUTE_LANE = "lane";
    private final Id<Vehicle> vehicleId;
    private final Id<Link> linkId;
    private final Id<Lane> laneId;

    public LaneEnterEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, Id<Lane> laneId) {
        super(time);
        this.laneId = laneId;
        this.vehicleId = vehicleId;
        this.linkId = linkId;
    }

    public String getEventType() {
        return "entered lane";
    }

    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("vehicle", this.vehicleId.toString());
        attr.put("link", this.linkId.toString());
        attr.put("lane", this.laneId.toString());
        return attr;
    }

    public Id<Vehicle> getVehicleId() {
        return this.vehicleId;
    }

    public Id<Link> getLinkId() {
        return this.linkId;
    }

    public Id<Lane> getLaneId() {
        return this.laneId;
    }
}
