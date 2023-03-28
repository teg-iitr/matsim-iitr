package org.matsim.contrib.signals.sensor;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;
import java.util.Map;

@Singleton
public final class MixedTrafficLinkSensorManager implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler, PersonDepartureEventHandler, LaneEnterEventHandler, LaneLeaveEventHandler {
    private static final Logger log = LogManager.getLogger(LinkSensorManager.class);
    private Map<Id<Link>, MixedTrafficLinkSensor> linkIdSensorMap = new HashMap();
    private Map<Id<Link>, Map<Id<Lane>, MixedTrafficLaneSensor>> linkIdLaneIdSensorMap = new HashMap();
    private final Network network;
    private final Vehicles vehicles;
    private Lanes laneDefinitions = null;
    private Map<Id<Person>, Id<Link>> personDepartureLinks = new HashMap();

    @Inject
    public MixedTrafficLinkSensorManager(Scenario scenario, EventsManager events) {
        this.network = scenario.getNetwork();
        this.vehicles = scenario.getVehicles();
        if (scenario.getConfig().network().getLaneDefinitionsFile() != null || scenario.getConfig().qsim().isUseLanes()) {
            this.laneDefinitions = scenario.getLanes();
        }

        events.addHandler(this);
    }

    public void registerNumberOfCarsInDistanceMonitoring(Id<Link> linkId, Double distanceMeter) {
        if (!this.linkIdSensorMap.containsKey(linkId)) {
            Link link = this.network.getLinks().get(linkId);
            if (link == null) {
                throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
            }

            this.linkIdSensorMap.put(link.getId(), new MixedTrafficLinkSensor(link, vehicles.getVehicles()));
        }

        ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(linkId)).registerDistanceToMonitor(distanceMeter);
    }

    public void registerNumberOfCarsOnLaneInDistanceMonitoring(Id<Link> linkId, Id<Lane> laneId, Double distanceMeter) {
        Link link = (Link)this.network.getLinks().get(linkId);
        if (link == null) {
            throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
        } else if (this.laneDefinitions != null && this.laneDefinitions.getLanesToLinkAssignments().get(linkId) != null && ((LanesToLinkAssignment)this.laneDefinitions.getLanesToLinkAssignments().get(linkId)).getLanes().get(laneId) != null) {
            if (!this.linkIdLaneIdSensorMap.containsKey(linkId)) {
                this.linkIdLaneIdSensorMap.put(linkId, new HashMap());
            }

            if (!((Map)this.linkIdLaneIdSensorMap.get(linkId)).containsKey(laneId)) {
                Lane lane = (Lane)((LanesToLinkAssignment)this.laneDefinitions.getLanesToLinkAssignments().get(linkId)).getLanes().get(laneId);
                ((Map)this.linkIdLaneIdSensorMap.get(linkId)).put(laneId, new MixedTrafficLaneSensor(link, lane, vehicles.getVehicles()));
            }

            ((MixedTrafficLaneSensor)((Map)this.linkIdLaneIdSensorMap.get(linkId)).get(laneId)).registerDistanceToMonitor(distanceMeter);
        } else {
            throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
        }
    }

    public void registerAverageNumberOfCarsPerSecondMonitoringOnLane(Id<Link> linkId, Id<Lane> laneId, double lookBackTime, double timeBucketCollectionDuration) {
        Link link = (Link)this.network.getLinks().get(linkId);
        if (link == null) {
            throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
        } else if (this.laneDefinitions != null && this.laneDefinitions.getLanesToLinkAssignments().get(linkId) != null && ((LanesToLinkAssignment)this.laneDefinitions.getLanesToLinkAssignments().get(linkId)).getLanes().get(laneId) != null) {
            if (!this.linkIdLaneIdSensorMap.containsKey(linkId)) {
                this.linkIdLaneIdSensorMap.put(linkId, new HashMap());
            }

            if (!((Map)this.linkIdLaneIdSensorMap.get(linkId)).containsKey(laneId)) {
                Lane lane = (Lane)((LanesToLinkAssignment)this.laneDefinitions.getLanesToLinkAssignments().get(linkId)).getLanes().get(laneId);
                ((Map)this.linkIdLaneIdSensorMap.get(linkId)).put(laneId, new MixedTrafficLaneSensor(link, lane, vehicles.getVehicles()));
            }

            ((MixedTrafficLaneSensor)((Map)this.linkIdLaneIdSensorMap.get(linkId)).get(laneId)).registerAverageVehiclesPerSecondToMonitor(lookBackTime, timeBucketCollectionDuration);
        } else {
            throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
        }
    }
    public void registerNumberOfCarsMonitoring(Id<Link> linkId) {
        if (!this.linkIdSensorMap.containsKey(linkId)) {
            Link link = (Link)this.network.getLinks().get(linkId);
            if (link == null) {
                throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
            }

            this.linkIdSensorMap.put(linkId, new MixedTrafficLinkSensor(link, vehicles.getVehicles()));
        }

    }

    public void registerNumberOfCarsMonitoringOnLane(Id<Link> linkId, Id<Lane> laneId) {
        Link link = (Link)this.network.getLinks().get(linkId);
        if (link == null) {
            throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
        } else if (this.laneDefinitions != null && this.laneDefinitions.getLanesToLinkAssignments().get(linkId) != null && ((LanesToLinkAssignment)this.laneDefinitions.getLanesToLinkAssignments().get(linkId)).getLanes().get(laneId) != null) {
            if (!this.linkIdLaneIdSensorMap.containsKey(linkId)) {
                this.linkIdLaneIdSensorMap.put(linkId, new HashMap());
            }

            if (!((Map)this.linkIdLaneIdSensorMap.get(linkId)).containsKey(laneId)) {
                Lane lane = (Lane)((LanesToLinkAssignment)this.laneDefinitions.getLanesToLinkAssignments().get(linkId)).getLanes().get(laneId);
                ((Map)this.linkIdLaneIdSensorMap.get(linkId)).put(laneId, new MixedTrafficLaneSensor(link, lane, vehicles.getVehicles()));
            }

        } else {
            throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
        }
    }

    public void registerAverageNumberOfCarsPerSecondMonitoringOnLane(Id<Link> linkId, Id<Lane> laneId) {
        this.registerAverageNumberOfCarsPerSecondMonitoringOnLane(linkId, laneId, 1.0D / 0.0, 1.0D / 0.0);
    }

    public void registerAverageNumberOfCarsPerSecondMonitoring(Id<Link> linkId) {
        this.registerAverageNumberOfCarsPerSecondMonitoring(linkId, 1.0D / 0.0, 1.0D / 0.0);
    }

    public int getNumberOfCarsOnLink(Id<Link> linkId) {
        if (!this.linkIdSensorMap.containsKey(linkId)) {
            throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
        } else {
            return ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(linkId)).getNumberOfCarsOnLink();
        }
    }

    public int getNumberOfCarsOnLane(Id<Link> linkId, Id<Lane> laneId) {
        if (!this.linkIdLaneIdSensorMap.containsKey(linkId)) {
            throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
        } else {
            Map<Id<Lane>, MixedTrafficLaneSensor> map = (Map)this.linkIdLaneIdSensorMap.get(linkId);
            if (map != null && map.containsKey(laneId)) {
                return ((MixedTrafficLaneSensor)map.get(laneId)).getNumberOfCarsOnLane();
            } else {
                throw new IllegalStateException("No sensor on lane " + laneId + " of link " + linkId + "! Register measurement for this link lane pair!");
            }
        }
    }

    public void registerAverageNumberOfCarsPerSecondMonitoring(Id<Link> linkId, double lookBackTime, double timeBucketCollectionDuration) {
        if (!this.linkIdSensorMap.containsKey(linkId)) {
            Link link = (Link)this.network.getLinks().get(linkId);
            if (link == null) {
                throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
            }

            this.linkIdSensorMap.put(link.getId(), new MixedTrafficLinkSensor(link, vehicles.getVehicles()));
        }

        ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(linkId)).registerAverageVehiclesPerSecondToMonitor(lookBackTime, timeBucketCollectionDuration);
    }

    public double getNumberOfCarsInDistance(Id<Link> linkId, Double distanceMeter, double timeSeconds) {
        if (!this.linkIdSensorMap.containsKey(linkId)) {
            throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
        } else {
            return ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(linkId)).getNumberOfCarsInDistance(distanceMeter, timeSeconds);
        }
    }

    public double getNumberOfCarsInDistanceOnLane(Id<Link> linkId, Id<Lane> laneId, Double distanceMeter, double timeSeconds) {
        if (!this.linkIdLaneIdSensorMap.containsKey(linkId)) {
            throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
        } else {
            Map<Id<Lane>, MixedTrafficLaneSensor> map = (Map)this.linkIdLaneIdSensorMap.get(linkId);
            if (map != null && map.containsKey(laneId)) {
                return ((MixedTrafficLaneSensor)map.get(laneId)).getNumberOfCarsInDistance(distanceMeter, timeSeconds);
            } else {
                throw new IllegalStateException("No sensor on lane " + laneId + " of link " + linkId + "! Register measurement for this link lane pair!");
            }
        }
    }

    public double getAverageArrivalRateOnLink(Id<Link> linkId, double now) {
        return ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(linkId)).getAvgVehiclesPerSecond(now);
    }

    public double getAverageArrivalRateOnLane(Id<Link> linkId, Id<Lane> laneId, double now) {
        return ((MixedTrafficLaneSensor)((Map)this.linkIdLaneIdSensorMap.get(linkId)).get(laneId)).getAvgVehiclesPerSecond(now);
    }

    public void handleEvent(LinkEnterEvent event) {
        if (this.linkIdSensorMap.containsKey(event.getLinkId())) {
            ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(event.getLinkId())).handleEvent(event);
        }

    }

    public void handleEvent(LinkLeaveEvent event) {
        if (this.linkIdSensorMap.containsKey(event.getLinkId())) {
            ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(event.getLinkId())).handleEvent(event);
        }

    }

    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (this.linkIdSensorMap.containsKey(event.getLinkId())) {
            ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(event.getLinkId())).handleEvent(event);
        }

    }

    public void handleEvent(PersonDepartureEvent event) {
        this.personDepartureLinks.put(event.getPersonId(), event.getLinkId());
    }

    public void handleEvent(PersonEntersVehicleEvent event) {
        if (this.linkIdSensorMap.containsKey(this.personDepartureLinks.get(event.getPersonId()))) {
            ((MixedTrafficLinkSensor)this.linkIdSensorMap.get(this.personDepartureLinks.get(event.getPersonId()))).handleEvent(event);
        }

    }

    public void reset(int iteration) {
        this.linkIdSensorMap.clear();
        this.linkIdLaneIdSensorMap.clear();
    }

    public void handleEvent(LaneLeaveEvent event) {
        if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())) {
            Map<Id<Lane>, MixedTrafficLaneSensor> map = (Map)this.linkIdLaneIdSensorMap.get(event.getLinkId());
            if (map.containsKey(event.getLaneId())) {
                ((MixedTrafficLaneSensor)map.get(event.getLaneId())).handleEvent(event);
            }
        }

    }

    public void handleEvent(LaneEnterEvent event) {
        if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())) {
            Map<Id<Lane>, MixedTrafficLaneSensor> map = (Map)this.linkIdLaneIdSensorMap.get(event.getLinkId());
            if (map.containsKey(event.getLaneId())) {
                ((MixedTrafficLaneSensor)map.get(event.getLaneId())).handleEvent(event);
            }
        }

    }
}