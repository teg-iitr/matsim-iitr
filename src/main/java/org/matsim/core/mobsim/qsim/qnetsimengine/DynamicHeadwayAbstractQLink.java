package org.matsim.core.mobsim.qsim.qnetsimengine;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

abstract class DynamicHeadwayAbstractQLink implements QLinkI {
    private static final Logger log = LogManager.getLogger(DynamicHeadwayAbstractQLink.class);
    private final Link link;
    private NetElementActivationRegistry netElementActivationRegistry;
    private final Map<String, Object> customAttributes = new HashMap();
    private final Map<Id<Vehicle>, QVehicle> parkedVehicles = new ConcurrentHashMap(10);
    private final Map<Id<Person>, MobsimAgent> additionalAgentsOnLink = new ConcurrentHashMap();
    private final Map<Id<Vehicle>, Queue<MobsimDriverAgent>> driversWaitingForCars = new LinkedHashMap();
    private final Map<Id<Person>, MobsimDriverAgent> driversWaitingForPassengers = new LinkedHashMap();
    private final Map<Id<Vehicle>, Set<MobsimAgent>> passengersWaitingForCars = new LinkedHashMap();
    private final Queue<QVehicle> waitingList = new LinkedList();
    private boolean active = false;
    private TransitQLink transitQLink;
    private final QNodeI toQNode;
    private final NetsimEngineContext context;
    private final NetsimInternalInterface netsimEngine;
    private final LinkSpeedCalculator linkSpeedCalculator;
    private final VehicleHandler vehicleHandler;
    private static int wrnCnt = 0;
    private final DynamicHeadwayAbstractQLink.QLinkInternalInterface qLinkInternalInterface = new DynamicHeadwayAbstractQLink.QLinkInternalInterface();
    private final Double pcu;

    DynamicHeadwayAbstractQLink(Link link, QNodeI toNode, NetsimEngineContext context, NetsimInternalInterface netsimEngine2, LinkSpeedCalculator linkSpeedCalculator, VehicleHandler vehicleHandler, Double pcu) {
        this.link = link;
        this.toQNode = toNode;
        this.context = context;
        this.netsimEngine = netsimEngine2;
        this.linkSpeedCalculator = linkSpeedCalculator;
        this.vehicleHandler = vehicleHandler;
        this.pcu = pcu;
    }

    public QNodeI getToNode() {
        return this.toQNode;
    }

    private void activateLink() {
        if (!this.active) {
            this.netElementActivationRegistry.registerLinkAsActive(this);
            this.active = true;
        }

    }

    public void addParkedVehicle(MobsimVehicle vehicle, boolean isInitial) {
        QVehicle qveh = (QVehicle)vehicle;
        if (this.parkedVehicles.put(qveh.getId(), qveh) != null && wrnCnt < 1) {
            ++wrnCnt;
            log.warn("existing vehicle on link was just overwritten by other vehicle with same ID.  Not clear what this means.  Continuing anyways ...");
            log.warn(" This message given only once.");
        }

        qveh.setCurrentLink(this.link);
        if (isInitial) {
            this.vehicleHandler.handleInitialVehicleArrival(qveh, this.link);
        }

    }

    public final void addParkedVehicle(MobsimVehicle vehicle) {
        this.addParkedVehicle(vehicle, true);
    }

    final boolean letVehicleArrive(QVehicle qveh) {
        if (this.vehicleHandler.handleVehicleArrival(qveh, this.getLink())) {
            this.addParkedVehicle(qveh, false);
            double now = this.context.getSimTimer().getTimeOfDay();
            this.context.getEventsManager().processEvent(new VehicleLeavesTrafficEvent(now, qveh.getDriver().getId(), this.link.getId(), qveh.getId(), qveh.getDriver().getMode(), 1.0D));
            this.netsimEngine.letVehicleArrive(qveh);
            this.makeVehicleAvailableToNextDriver(qveh);
            return true;
        } else {
            return false;
        }
    }

    public final QVehicle removeParkedVehicle(Id<Vehicle> vehicleId) {
        return (QVehicle)this.parkedVehicles.remove(vehicleId);
    }

    public QVehicle getParkedVehicle(Id<Vehicle> vehicleId) {
        return (QVehicle)this.parkedVehicles.get(vehicleId);
    }

    private final void addDepartingVehicle(MobsimVehicle mvehicle) {
        QVehicle vehicle = (QVehicle)mvehicle;
        this.waitingList.add(vehicle);
        vehicle.setCurrentLink(this.getLink());
        this.activateLink();
        this.vehicleHandler.handleVehicleDeparture(vehicle, this.link);
    }

    public void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
        this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
    }

    public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> mobsimAgentId) {
        return (MobsimAgent)this.additionalAgentsOnLink.remove(mobsimAgentId);
    }

    public Collection<MobsimAgent> getAdditionalAgentsOnLink() {
        return Collections.unmodifiableCollection(this.additionalAgentsOnLink.values());
    }

    public void clearVehicles() {
        double now = this.context.getSimTimer().getTimeOfDay();
        Set<Id<Person>> stuckAgents = new HashSet();
        Iterator var4 = this.parkedVehicles.values().iterator();

        while(true) {
            QVehicle veh;
            Iterator var6;
            while(true) {
                if (!var4.hasNext()) {
                    this.parkedVehicles.clear();
                    var4 = this.driversWaitingForPassengers.values().iterator();

                    while(var4.hasNext()) {
                        MobsimAgent driver = (MobsimAgent)var4.next();
                        if (!stuckAgents.contains(driver.getId())) {
                            stuckAgents.add(driver.getId());
                            this.context.getEventsManager().processEvent(new PersonStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
                            this.context.getAgentCounter().incLost();
                            this.context.getAgentCounter().decLiving();
                        }
                    }

                    this.driversWaitingForPassengers.clear();
                    var4 = this.driversWaitingForCars.values().iterator();

                    MobsimAgent passenger;
                    while(var4.hasNext()) {
                        Queue<MobsimDriverAgent> queue = (Queue)var4.next();
                        var6 = queue.iterator();

                        while(var6.hasNext()) {
                            passenger = (MobsimAgent)var6.next();
                            if (!stuckAgents.contains(passenger.getId())) {
                                stuckAgents.add(passenger.getId());
                                this.context.getEventsManager().processEvent(new PersonStuckEvent(now, passenger.getId(), passenger.getCurrentLinkId(), passenger.getMode()));
                                this.context.getAgentCounter().incLost();
                                this.context.getAgentCounter().decLiving();
                            }
                        }
                    }

                    this.driversWaitingForCars.clear();
                    var4 = this.passengersWaitingForCars.values().iterator();

                    while(var4.hasNext()) {
                        Set<MobsimAgent> passengers = (Set)var4.next();
                        var6 = passengers.iterator();

                        while(var6.hasNext()) {
                            passenger = (MobsimAgent)var6.next();
                            if (!stuckAgents.contains(passenger.getId())) {
                                stuckAgents.add(passenger.getId());
                                this.context.getEventsManager().processEvent(new PersonStuckEvent(now, passenger.getId(), passenger.getCurrentLinkId(), passenger.getMode()));
                                this.context.getAgentCounter().incLost();
                                this.context.getAgentCounter().decLiving();
                            }
                        }
                    }

                    this.passengersWaitingForCars.clear();
                    var4 = this.waitingList.iterator();

                    while(var4.hasNext()) {
                        veh = (QVehicle)var4.next();
                        if (!stuckAgents.contains(veh.getDriver().getId())) {
                            stuckAgents.add(veh.getDriver().getId());
                            this.context.getEventsManager().processEvent(new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
                            this.context.getEventsManager().processEvent(new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
                            this.context.getAgentCounter().incLost();
                            this.context.getAgentCounter().decLiving();
                        }
                    }

                    this.waitingList.clear();
                    return;
                }

                veh = (QVehicle)var4.next();
                if (veh.getDriver() == null) {
                    break;
                }

                if (veh.getDriver().getState() == State.LEG && !stuckAgents.contains(veh.getDriver().getId())) {
                    stuckAgents.add(veh.getDriver().getId());
                    this.context.getEventsManager().processEvent(new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
                    this.context.getEventsManager().processEvent(new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
                    this.context.getAgentCounter().incLost();
                    this.context.getAgentCounter().decLiving();
                    break;
                }
            }

            var6 = veh.getPassengers().iterator();

            while(var6.hasNext()) {
                PassengerAgent passenger = (PassengerAgent)var6.next();
                if (!stuckAgents.contains(passenger.getId())) {
                    stuckAgents.add(passenger.getId());
                    MobsimAgent mobsimAgent = (MobsimAgent)passenger;
                    this.context.getEventsManager().processEvent(new PersonStuckEvent(now, mobsimAgent.getId(), veh.getCurrentLink().getId(), mobsimAgent.getMode()));
                    this.context.getAgentCounter().incLost();
                    this.context.getAgentCounter().decLiving();
                }
            }
        }
    }

    void makeVehicleAvailableToNextDriver(QVehicle veh) {
        Id<Vehicle> vehicleId = veh.getId();
        Set<MobsimAgent> passengers = (Set)this.passengersWaitingForCars.get(vehicleId);
        if (passengers != null) {
            List<MobsimAgent> passengersToHandle = new ArrayList(passengers);
            Iterator var5 = passengersToHandle.iterator();

            while(var5.hasNext()) {
                MobsimAgent passenger = (MobsimAgent)var5.next();
                this.unregisterPassengerAgentWaitingForCar(passenger, vehicleId);
                this.insertPassengerIntoVehicle(passenger, vehicleId);
            }
        }

        Queue<MobsimDriverAgent> driversWaitingForCar = (Queue)this.driversWaitingForCars.get(veh.getId());
        boolean thereIsDriverWaiting = driversWaitingForCar != null && !driversWaitingForCar.isEmpty();
        if (thereIsDriverWaiting) {
            MobsimDriverAgent driverWaitingForPassengers = (MobsimDriverAgent)this.driversWaitingForPassengers.get(((MobsimDriverAgent)driversWaitingForCar.element()).getId());
            if (driverWaitingForPassengers != null) {
                return;
            }
        }

        if (thereIsDriverWaiting && veh.getDriver() == null) {
            veh.setDriver((DriverAgent)driversWaitingForCar.remove());
            if (driversWaitingForCar.isEmpty()) {
                Queue<MobsimDriverAgent> r = (Queue)this.driversWaitingForCars.remove(veh.getId());

                assert r == driversWaitingForCar;
            }

            this.removeParkedVehicle(veh.getId());
            this.letVehicleDepart(veh);
        }

    }

    public final void letVehicleDepart(QVehicle vehicle) {
        double now = this.context.getSimTimer().getTimeOfDay();
        MobsimDriverAgent driver = vehicle.getDriver();
        if (driver == null) {
            throw new RuntimeException("Vehicle cannot depart without a driver!");
        } else {
            EventsManager eventsManager = this.context.getEventsManager();
            eventsManager.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));
            this.addDepartingVehicle(vehicle);
        }
    }

    public final boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id<Vehicle> vehicleId) {
        double now = this.context.getSimTimer().getTimeOfDay();
        QVehicle vehicle = this.getParkedVehicle(vehicleId);
        if (vehicle == null) {
            this.registerPassengerAgentWaitingForCar(passenger, vehicleId);
            return false;
        } else {
            boolean added = vehicle.addPassenger((PassengerAgent)passenger);
            if (!added) {
                Logger var10000 = log;
                String var10001 = passenger.getId().toString();
                var10000.warn("Passenger " + var10001 + " could not be inserted into vehicle " + vehicleId.toString() + " since there is no free seat available!");
                return false;
            } else {
                ((PassengerAgent)passenger).setVehicle(vehicle);
                EventsManager eventsManager = this.context.getEventsManager();
                eventsManager.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), vehicle.getId()));
                return true;
            }
        }
    }

    public QVehicle getVehicle(Id<Vehicle> vehicleId) {
        QVehicle ret = (QVehicle)this.parkedVehicles.get(vehicleId);
        return ret;
    }

    public final Collection<MobsimVehicle> getAllVehicles() {
        Collection<MobsimVehicle> vehicles = this.getAllNonParkedVehicles();
        vehicles.addAll(this.parkedVehicles.values());
        return vehicles;
    }

    public final Map<String, Object> getCustomAttributes() {
        return this.customAttributes;
    }

    void setNetElementActivationRegistry(NetElementActivationRegistry qSimEngineRunner) {
        this.netElementActivationRegistry = qSimEngineRunner;
    }

    public void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) {
        Id<Vehicle> vehicleId = agent.getPlannedVehicleId();
        Queue<MobsimDriverAgent> queue = (Queue)this.driversWaitingForCars.get(vehicleId);
        if (queue == null) {
            queue = new LinkedList();
            this.driversWaitingForCars.put(vehicleId, queue);
        }

        ((Queue)queue).add(agent);
    }

    public void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
        this.driversWaitingForPassengers.put(agent.getId(), agent);
    }

    public MobsimAgent unregisterDriverAgentWaitingForPassengers(Id<Person> agentId) {
        return (MobsimAgent)this.driversWaitingForPassengers.remove(agentId);
    }

    public void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
        Set<MobsimAgent> passengers = (Set)this.passengersWaitingForCars.get(vehicleId);
        if (passengers == null) {
            passengers = new LinkedHashSet();
            this.passengersWaitingForCars.put(vehicleId, passengers);
        }

        ((Set)passengers).add(agent);
    }

    public MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
        Set<MobsimAgent> passengers = (Set)this.passengersWaitingForCars.get(vehicleId);
        return passengers != null && passengers.remove(agent) ? agent : null;
    }

    public Set<MobsimAgent> getAgentsWaitingForCar(Id<Vehicle> vehicleId) {
        Set<MobsimAgent> set = (Set)this.passengersWaitingForCars.get(vehicleId);
        return set != null ? Collections.unmodifiableSet(set) : null;
    }

    public Link getLink() {
        return this.link;
    }

    boolean isActive() {
        return this.active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    Queue<QVehicle> getWaitingList() {
        return this.waitingList;
    }

    TransitQLink getTransitQLink() {
        return this.transitQLink;
    }

    void setTransitQLink(TransitQLink transitQLink) {
        this.transitQLink = transitQLink;
    }

    public final DynamicHeadwayAbstractQLink.QLinkInternalInterface getInternalInterface() {
        return this.qLinkInternalInterface;
    }

    public Double getPCU() {
        return this.pcu;
    }

    public final class QLinkInternalInterface {
        public QLinkInternalInterface() {
        }

        public QNodeI getToNodeQ() {
            return DynamicHeadwayAbstractQLink.this.toQNode;
        }

        public Node getToNode() {
            return DynamicHeadwayAbstractQLink.this.link.getToNode();
        }

        public double getFreespeed() {
            return DynamicHeadwayAbstractQLink.this.link.getFreespeed();
        }

        public Id<Link> getId() {
            return DynamicHeadwayAbstractQLink.this.link.getId();
        }

        public AbstractQLink.HandleTransitStopResult handleTransitStop(double now, QVehicle veh, TransitDriverAgent driver, Id<Link> linkId) {
            return DynamicHeadwayAbstractQLink.this.transitQLink.handleTransitStop(now, veh, driver, linkId);
        }

        public void addParkedVehicle(QVehicle veh) {
            DynamicHeadwayAbstractQLink.this.addParkedVehicle(veh);
        }

        public boolean letVehicleArrive(QVehicle veh) {
            return DynamicHeadwayAbstractQLink.this.letVehicleArrive(veh);
        }

        public void makeVehicleAvailableToNextDriver(QVehicle veh) {
            DynamicHeadwayAbstractQLink.this.makeVehicleAvailableToNextDriver(veh);
        }

        public void activateLink() {
            DynamicHeadwayAbstractQLink.this.activateLink();
        }

        public double getMaximumVelocityFromLinkSpeedCalculator(QVehicle veh, double now) {
            LinkSpeedCalculator linkSpeedCalculator = DynamicHeadwayAbstractQLink.this.linkSpeedCalculator;
            Gbl.assertNotNull(linkSpeedCalculator);
            return linkSpeedCalculator.getMaximumVelocity(veh, DynamicHeadwayAbstractQLink.this.link, now);
        }

        public void setCurrentLinkToVehicle(QVehicle veh) {
            veh.setCurrentLink(DynamicHeadwayAbstractQLink.this.link);
        }

        public QLaneI getAcceptingQLane() {
            return DynamicHeadwayAbstractQLink.this.getAcceptingQLane();
        }

        public List<QLaneI> getOfferingQLanes() {
            return DynamicHeadwayAbstractQLink.this.getOfferingQLanes();
        }

        public Node getFromNode() {
            return DynamicHeadwayAbstractQLink.this.link.getFromNode();
        }

        public double getFreespeed(double now) {
            return DynamicHeadwayAbstractQLink.this.link.getFreespeed(now);
        }

        public int getNumberOfLanesAsInt(double now) {
            return NetworkUtils.getNumberOfLanesAsInt(now, DynamicHeadwayAbstractQLink.this.link);
        }

        public Link getLink() {
            return DynamicHeadwayAbstractQLink.this.link;
        }
    }

    public static enum HandleTransitStopResult {
        continue_driving,
        rehandle,
        accepted;

        private HandleTransitStopResult() {
        }
    }
}
