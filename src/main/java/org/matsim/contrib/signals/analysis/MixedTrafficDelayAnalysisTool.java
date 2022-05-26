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
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class MixedTrafficDelayAnalysisTool implements SignalGroupStateChangedEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonStuckEventHandler, AfterMobsimListener {
    private static final Logger LOG = Logger.getLogger(DelayAnalysisTool.class);
    private final Network network;
    private boolean considerStuckedAgents;
    private double totalDelay;
    private Map<Double, Map<Id<Link>, Double>> summedBygoneDelayPerCycle;
    private Map<Id<Link>, Double> totalDelayPerLink;
    private Map<Id<Link>, Integer> numberOfAgentsPerLink;
    private Map<Id<Person>, Double> earliestLinkExitTimePerAgent;
    private Map<Id<Vehicle>, Set<Id<Person>>> vehicleIdToPassengerIds;
    private Map<Id<Link>, Map<Id<VehicleType>, Double>> flowPerLinkPerVehicleType;
    private Map<Double, Map<Id<Link>, Map<Id<VehicleType>, Double>>> summedBygoneFlowPerLinkPerVehicleTypePerCycle;
    private Map<Id<Vehicle>, Vehicle> vehicleMap;
    private double flow;
    private double currentCycleTime;
    private Map<Id<SignalSystem>, Id<SignalGroup>> firstSignalGroupOfSignalSystem;
    private Map<Id<SignalGroup>, Double> lastSwitchesToGreen;
    private Map<Id<SignalGroup>, Double> lastSwitchesToRed;
    private final List<Id<VehicleType>> vehicleTypes;

    public MixedTrafficDelayAnalysisTool(Network network, Vehicles vehicles) {
        this.considerStuckedAgents = false;
        this.totalDelay = 0.0D;
        this.totalDelayPerLink = new HashMap();
        this.numberOfAgentsPerLink = new HashMap();
        this.earliestLinkExitTimePerAgent = new HashMap();
        this.vehicleIdToPassengerIds = new HashMap();
        this.flowPerLinkPerVehicleType = new TreeMap<>();
        this.network = network;
        this.vehicleMap = vehicles.getVehicles();
        this.vehicleTypes = new ArrayList<>(vehicles.getVehicleTypes().keySet());
        this.summedBygoneDelayPerCycle = new TreeMap<>();
        this.firstSignalGroupOfSignalSystem = new HashMap<>();
        this.summedBygoneFlowPerLinkPerVehicleTypePerCycle = new TreeMap<>();
    }

    @Inject
    public MixedTrafficDelayAnalysisTool(Network network, EventsManager em, Vehicles vehicles) {
        this(network, vehicles);
        em.addHandler(this);
    }

    public void reset(int iteration) {
        this.totalDelay = 0.0D;
        this.earliestLinkExitTimePerAgent.clear();
        this.vehicleIdToPassengerIds.clear();
        this.totalDelayPerLink.clear();
        this.numberOfAgentsPerLink.clear();
        this.flowPerLinkPerVehicleType.clear();
        this.flow = 0.0D;
        this.lastSwitchesToGreen = new HashMap();
        this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.clear();
        this.lastSwitchesToRed = new HashMap();
    }

    public void handleEvent(PersonDepartureEvent event) {
        this.earliestLinkExitTimePerAgent.put(event.getPersonId(), event.getTime() + 1.0D);
    }


    public void handleEvent(PersonEntersVehicleEvent event) {
        if (!this.vehicleIdToPassengerIds.containsKey(event.getVehicleId())) {
            this.vehicleIdToPassengerIds.put(event.getVehicleId(), new HashSet());
        }
        ((Set) this.vehicleIdToPassengerIds.get(event.getVehicleId())).add(event.getPersonId());
    }

    public void handleEvent(LinkEnterEvent event) {
        Link currentLink = (Link) this.network.getLinks().get(event.getLinkId());
        double freespeedTt = currentLink.getLength() / currentLink.getFreespeed();
        double matsimFreespeedTT = Math.floor(freespeedTt + 1.0D);
        if (event.getTime() == 4706)
            System.out.println();
        this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.putIfAbsent(this.currentCycleTime, new TreeMap<>());
        if (!this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.get(this.currentCycleTime).containsKey(event.getLinkId())) {
            this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.get(this.currentCycleTime).putIfAbsent(event.getLinkId(), new HashMap<>());
            for (var vehicleType : vehicleTypes)
                this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.get(this.currentCycleTime).get(event.getLinkId()).putIfAbsent(vehicleType, 0.0D);
        }
        for (Object o : this.vehicleIdToPassengerIds.get(event.getVehicleId())) {
            Id<Person> passengerId = (Id) o;
            this.earliestLinkExitTimePerAgent.put(passengerId, event.getTime() + matsimFreespeedTT);
        }
        this.flow += this.vehicleMap.get(event.getVehicleId()).getType().getPcuEquivalents();
        for (var vehicleType : vehicleTypes) {
            if (this.vehicleMap.get(event.getVehicleId()).getType().getId().equals(vehicleType)) {
//                this.flowPerLinkPerVehicleType.get(event.getLinkId()).put(vehicleType, this.flowPerLinkPerVehicleType.get(event.getLinkId()).get(vehicleType) + this.vehicleMap.get(event.getVehicleId()).getType().getPcuEquivalents());
                this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.get(this.currentCycleTime).get(event.getLinkId()).put(vehicleType, this.summedBygoneFlowPerLinkPerVehicleTypePerCycle.get(this.currentCycleTime).get(event.getLinkId()).get(vehicleType) + this.vehicleMap.get(event.getVehicleId()).getType().getPcuEquivalents());
            }
        }

    }

    public void handleEvent(LinkLeaveEvent event) {
        if (!this.totalDelayPerLink.containsKey(event.getLinkId())) {
            this.totalDelayPerLink.put(event.getLinkId(), 0.0D);
            this.numberOfAgentsPerLink.put(event.getLinkId(), 0);
        }
        this.summedBygoneDelayPerCycle.putIfAbsent(this.currentCycleTime, new HashMap<>());
        if (!this.summedBygoneDelayPerCycle.get(this.currentCycleTime).containsKey(event.getLinkId())) {
            this.summedBygoneDelayPerCycle.get(this.currentCycleTime).put(event.getLinkId(), 0.0D);
        }
//        if (!this.flowPerLinkPerVehicleType.containsKey(event.getLinkId())) {
//            this.flowPerLinkPerVehicleType.put(event.getLinkId(), new HashMap<>());
//            for (var vehicleType: vehicleTypes)
//                this.flowPerLinkPerVehicleType.get(event.getLinkId()).put(vehicleType, 0.0D);
//        }

        Iterator var2 = ((Set) this.vehicleIdToPassengerIds.get(event.getVehicleId())).iterator();

        while (var2.hasNext()) {
            Id<Person> passengerId = (Id) var2.next();
            double currentDelay = event.getTime() - (Double) this.earliestLinkExitTimePerAgent.remove(passengerId);

            this.totalDelayPerLink.put(event.getLinkId(), (Double) this.totalDelayPerLink.get(event.getLinkId()) + currentDelay);
            this.summedBygoneDelayPerCycle.get(this.currentCycleTime).put(event.getLinkId(), currentDelay);
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
            if (!this.summedBygoneDelayPerCycle.get(this.currentCycleTime).containsKey(event.getLinkId())) {
                this.summedBygoneDelayPerCycle.get(this.currentCycleTime).put(event.getLinkId(), 0.0D);
            }

            double stuckDelay = event.getTime() - (Double) this.earliestLinkExitTimePerAgent.remove(event.getPersonId());
            LOG.warn("Add delay " + stuckDelay + " of agent " + event.getPersonId() + " that stucked on link " + event.getLinkId());
            this.totalDelayPerLink.put(event.getLinkId(), (Double) this.totalDelayPerLink.get(event.getLinkId()) + stuckDelay);
            this.summedBygoneDelayPerCycle.get(this.currentCycleTime).put(event.getLinkId(), stuckDelay);
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
//    public Map<Id<Link>, Double> getFlowPerLinkPerVehicleType() {
//        return flowPerLinkPerVehicleType;
//    }

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

    public void handleEvent(SignalGroupStateChangedEvent event) {

        switch (event.getNewState()) {
            case RED:
                this.lastSwitchesToRed.put(event.getSignalGroupId(), event.getTime());
                Double lastSwitchToGreen = (Double) this.lastSwitchesToGreen.remove(event.getSignalGroupId());
                this.doBygoneGreenTimeAnalysis(event, lastSwitchToGreen);
                break;
            case GREEN:
                this.lastSwitchesToGreen.put(event.getSignalGroupId(), event.getTime());
                this.doCycleAnalysis(event);
                Double lastSwitchToRed = (Double) this.lastSwitchesToRed.remove(event.getSignalGroupId());
                this.doBygoneGreenTimeAnalysis(event, lastSwitchToRed);
        }

    }

    private void doCycleAnalysis(SignalGroupStateChangedEvent event) {
        if (!this.firstSignalGroupOfSignalSystem.containsKey(event.getSignalSystemId())) {
            this.firstSignalGroupOfSignalSystem.put(event.getSignalSystemId(), event.getSignalGroupId());
        }
        if (event.getSignalGroupId().equals(this.firstSignalGroupOfSignalSystem.get(event.getSignalSystemId()))) {
            this.currentCycleTime = event.getTime();
        }

    }

    private void doBygoneGreenTimeAnalysis(SignalGroupStateChangedEvent event, Double lastSwitch) {


    }

    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        this.lastSwitchesToGreen.clear();
        this.lastSwitchesToRed.clear();
    }

    public Map<Double, Map<Id<Link>, Double>> getSummedBygoneDelayPerCycle() {
        return summedBygoneDelayPerCycle;
    }

    public Map<Double, Map<Id<Link>, Map<Id<VehicleType>, Double>>> getSummedBygoneFlowPerLinkPerVehicleTypePerCycle() {
        return summedBygoneFlowPerLinkPerVehicleTypePerCycle;
    }


}
