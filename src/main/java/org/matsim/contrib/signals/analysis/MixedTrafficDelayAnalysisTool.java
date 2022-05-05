package org.matsim.contrib.signals.analysis;

import com.google.inject.Inject;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

public class MixedTrafficDelayAnalysisTool implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonStuckEventHandler {
    private static final Logger LOG = Logger.getLogger(DelayAnalysisTool.class);
    private final Network network;
    private boolean considerStuckedAgents;
    private double totalDelay;
    private Map<Id<Link>, Double> totalDelayPerLink;
    private Map<Id<Link>, Integer> numberOfAgentsPerLink;
    private Map<Id<Person>, Double> earliestLinkExitTimePerAgent;
    private Map<Id<Vehicle>, Set<Id<Person>>> vehicleIdToPassengerIds;
    private Map<Id<Link>, Double> flowPerLink;
    private Map<Id<Vehicle>, Vehicle> vehicleMap;
    private double flow;

    public MixedTrafficDelayAnalysisTool(Network network, Map<Id<Vehicle>, Vehicle> vehicleMap) {
        this.considerStuckedAgents = false;
        this.totalDelay = 0.0D;
        this.totalDelayPerLink = new HashMap();
        this.numberOfAgentsPerLink = new HashMap();
        this.earliestLinkExitTimePerAgent = new HashMap();
        this.vehicleIdToPassengerIds = new HashMap();
        this.flowPerLink = new HashMap<>();
        this.network = network;
        this.vehicleMap = vehicleMap;
        this.flow = 0.0D;
    }

    @Inject
    public MixedTrafficDelayAnalysisTool(Network network, EventsManager em, Map<Id<Vehicle>, Vehicle> vehicleMap) {
        this(network, vehicleMap);
        em.addHandler(this);
    }

    public void reset(int iteration) {
        this.totalDelay = 0.0D;
        this.earliestLinkExitTimePerAgent.clear();
        this.vehicleIdToPassengerIds.clear();
        this.totalDelayPerLink.clear();
        this.numberOfAgentsPerLink.clear();
        this.flowPerLink.clear();
        this.flow = 0.0D;
    }

    public void handleEvent(PersonDepartureEvent event) {
        this.earliestLinkExitTimePerAgent.put(event.getPersonId(), event.getTime() + 1.0D);
    }


    public void handleEvent(PersonEntersVehicleEvent event) {
        if (!this.vehicleIdToPassengerIds.containsKey(event.getVehicleId())) {
            this.vehicleIdToPassengerIds.put(event.getVehicleId(), new HashSet());
        }
        this.flow += this.vehicleMap.get(event.getVehicleId()).getType().getPcuEquivalents();
        ((Set) this.vehicleIdToPassengerIds.get(event.getVehicleId())).add(event.getPersonId());
    }

    public void handleEvent(LinkEnterEvent event) {
        if (!this.flowPerLink.containsKey(event.getLinkId())) {
            this.flowPerLink.put(event.getLinkId(), 0.0D);
        }
        Link currentLink = (Link) this.network.getLinks().get(event.getLinkId());
        double freespeedTt = currentLink.getLength() / currentLink.getFreespeed();
        double matsimFreespeedTT = Math.floor(freespeedTt + 1.0D);

        for (Object o : this.vehicleIdToPassengerIds.get(event.getVehicleId())) {
            Id<Person> passengerId = (Id) o;
            this.flow += this.vehicleMap.get(event.getVehicleId()).getType().getPcuEquivalents();
            this.flowPerLink.put(event.getLinkId(), this.flow);
            this.earliestLinkExitTimePerAgent.put(passengerId, event.getTime() + matsimFreespeedTT);
        }

    }

    public void handleEvent(LinkLeaveEvent event) {
        if (!this.totalDelayPerLink.containsKey(event.getLinkId())) {
            this.totalDelayPerLink.put(event.getLinkId(), 0.0D);
            this.numberOfAgentsPerLink.put(event.getLinkId(), 0);
        }

        if (!this.flowPerLink.containsKey(event.getLinkId())) {
            this.flowPerLink.put(event.getLinkId(), 0.0D);
        }
        Iterator var2 = ((Set) this.vehicleIdToPassengerIds.get(event.getVehicleId())).iterator();

        while (var2.hasNext()) {
            Id<Person> passengerId = (Id) var2.next();
            double currentDelay = event.getTime() - (Double) this.earliestLinkExitTimePerAgent.remove(passengerId);
            this.flow -= this.vehicleMap.get(event.getVehicleId()).getType().getPcuEquivalents();
            this.flowPerLink.put(event.getLinkId(), this.flow);
            this.totalDelayPerLink.put(event.getLinkId(), (Double) this.totalDelayPerLink.get(event.getLinkId()) + currentDelay);
            this.totalDelay += currentDelay;
            this.numberOfAgentsPerLink.put(event.getLinkId(), (Integer) this.numberOfAgentsPerLink.get(event.getLinkId()) + 1);
        }

    }

    public void handleEvent(PersonStuckEvent event) {
        if (this.considerStuckedAgents) {
            if (!this.totalDelayPerLink.containsKey(event.getLinkId())) {
                this.totalDelayPerLink.put(event.getLinkId(), 0.0D);
                this.numberOfAgentsPerLink.put(event.getLinkId(), 0);
            }

            double stuckDelay = event.getTime() - (Double) this.earliestLinkExitTimePerAgent.remove(event.getPersonId());
            LOG.warn("Add delay " + stuckDelay + " of agent " + event.getPersonId() + " that stucked on link " + event.getLinkId());
            this.totalDelayPerLink.put(event.getLinkId(), (Double) this.totalDelayPerLink.get(event.getLinkId()) + stuckDelay);
            this.totalDelay += stuckDelay;
            this.numberOfAgentsPerLink.put(event.getLinkId(), (Integer) this.numberOfAgentsPerLink.get(event.getLinkId()) + 1);
        }

    }

    public double getTotalDelay() {
        return this.totalDelay;
    }

    public Map<Id<Link>, Double> getTotalDelayPerLink() {
        return this.totalDelayPerLink;
    }
    public Map<Id<Link>, Double> getFlowPerLink() {
        return flowPerLink;
    }

    public double getFlow() {
        return flow;
    }

    public Map<Id<Link>, Double> getAvgDelayPerLink() {
        Map<Id<Link>, Double> avgDelayMap = new HashMap();
        Iterator var2 = this.totalDelayPerLink.keySet().iterator();

        while (var2.hasNext()) {
            Id<Link> linkId = (Id) var2.next();
            avgDelayMap.put(linkId, (Double) this.totalDelayPerLink.get(linkId) / (double) (Integer) this.numberOfAgentsPerLink.get(linkId));
        }

        return avgDelayMap;
    }

    public void considerDelayOfStuckedAgents() {
        this.considerStuckedAgents = true;
    }

}
