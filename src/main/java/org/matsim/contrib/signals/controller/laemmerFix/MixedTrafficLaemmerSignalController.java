package org.matsim.contrib.signals.controller.laemmerFix;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.controller.AbstractSignalController;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.Regime;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.sensor.MixedTrafficDownstreamSensor;
import org.matsim.contrib.signals.sensor.MixedTrafficLinkSensorManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;

import java.util.*;

public final class MixedTrafficLaemmerSignalController extends AbstractSignalController implements SignalController, ControlerListener {
    public static final String IDENTIFIER = "MixedTrafficLaemmerSignalController";
    private MixedTrafficLaemmerSignalController.Request activeRequest = null;
    private Queue<MixedTrafficLaemmerSignalController.LaemmerSignal> regulationQueue = new LinkedList();
    private final List<MixedTrafficLaemmerSignalController.LaemmerSignal> laemmerSignals = new ArrayList();
    private MixedTrafficLinkSensorManager sensorManager;
    private MixedTrafficDownstreamSensor downstreamSensor;
    private Scenario scenario;
    private final Network network;
    private final Lanes lanes;
    private final Config config;
    private final LaemmerConfigGroup laemmerConfig;
    private SignalModelFactory factory;
    private double tIdle;
    private double systemOutflowCapacity;


    private MixedTrafficLaemmerSignalController(Scenario scenario, MixedTrafficLinkSensorManager sensorManager, MixedTrafficDownstreamSensor downstreamSensor) {
        this.sensorManager = sensorManager;
        this.network = scenario.getNetwork();
        this.lanes = scenario.getLanes();
        this.config = scenario.getConfig();
        this.downstreamSensor = downstreamSensor;
        this.laemmerConfig = (LaemmerConfigGroup)ConfigUtils.addOrGetModule(this.config, LaemmerConfigGroup.class);
    }
    public void simulationInitialized(double simStartTimeSeconds) {
        this.laemmerSignals.clear();
        this.activeRequest = null;
        this.regulationQueue.clear();
        this.systemOutflowCapacity = 0.0D;
        this.initializeSensoring();

        MixedTrafficLaemmerSignalController.LaemmerSignal laemmerSignal;
        for(Iterator var3 = this.system.getSignalGroups().values().iterator(); var3.hasNext(); this.systemOutflowCapacity += laemmerSignal.signalOutflowCapacityPerS) {
            SignalGroup group = (SignalGroup)var3.next();
            this.system.scheduleDropping(simStartTimeSeconds, group.getId());
            laemmerSignal = new MixedTrafficLaemmerSignalController.LaemmerSignal(group);
            this.laemmerSignals.add(laemmerSignal);
        }

    }

    public void updateState(double now) {
        this.updateRepresentativeDriveways(now);
        if (!this.laemmerConfig.getActiveRegime().equals(Regime.OPTIMIZING)) {
            this.updateActiveRegulation(now);
        }

        Iterator var3 = this.laemmerSignals.iterator();

        while(var3.hasNext()) {
            MixedTrafficLaemmerSignalController.LaemmerSignal signal = (MixedTrafficLaemmerSignalController.LaemmerSignal)var3.next();
            signal.update(now);
        }

        if (this.activeRequest != null && this.activeRequest.signal.group.getState().equals(SignalGroupState.GREEN)) {
            double remainingMinG = this.activeRequest.onsetTime + this.laemmerConfig.getMinGreenTime() - now;
            if (remainingMinG > 0.0D) {
                return;
            }
        }

        MixedTrafficLaemmerSignalController.LaemmerSignal selection = this.selectSignal();
        this.processSelection(now, selection);
    }


    private void updateActiveRegulation(double now) {
        if (this.activeRequest != null && !this.regulationQueue.isEmpty() && ((MixedTrafficLaemmerSignalController.LaemmerSignal)this.regulationQueue.peek()).equals(this.activeRequest.signal)) {
            MixedTrafficLaemmerSignalController.LaemmerSignal signal = (MixedTrafficLaemmerSignalController.LaemmerSignal)this.regulationQueue.peek();
            double n;
            if (signal.determiningLane != null) {
                n = this.getNumberOfExpectedVehiclesOnLane(now, signal.determiningLink, signal.determiningLane);
            } else {
                n = this.getNumberOfExpectedVehiclesOnLink(now, signal.determiningLink);
            }

            if (this.activeRequest.signal.regulationTime + this.activeRequest.onsetTime - now <= 0.0D || n == 0) {
                this.regulationQueue.poll();
            }
        }

    }

    private MixedTrafficLaemmerSignalController.LaemmerSignal selectSignal() {
        MixedTrafficLaemmerSignalController.LaemmerSignal max = null;
        if (!this.laemmerConfig.getActiveRegime().equals(Regime.OPTIMIZING)) {
            max = (MixedTrafficLaemmerSignalController.LaemmerSignal)this.regulationQueue.peek();
        }

        if (!this.laemmerConfig.getActiveRegime().equals(Regime.STABILIZING) && max == null) {
            double index = 0.0D;
            Iterator var4 = this.laemmerSignals.iterator();

            while(true) {
                MixedTrafficLaemmerSignalController.LaemmerSignal signal;
                do {
                    do {
                        if (!var4.hasNext()) {
                            return max;
                        }

                        signal = (MixedTrafficLaemmerSignalController.LaemmerSignal)var4.next();
                    } while(!(signal.index > index));
                } while(this.laemmerConfig.isCheckDownstream() && !this.downstreamSensor.allDownstreamLinksEmpty(this.system.getId(), signal.group.getId()));

                max = signal;
                index = signal.index;
            }
        } else {
            return max;
        }
    }

    private void processSelection(double now, MixedTrafficLaemmerSignalController.LaemmerSignal max) {
        if (this.activeRequest != null && (max == null || !max.equals(this.activeRequest.signal))) {
            if (this.activeRequest.onsetTime < now) {
                this.system.scheduleDropping(now, this.activeRequest.signal.group.getId());
            }

            this.activeRequest = null;
        }

        if (this.activeRequest == null && max != null) {
            this.activeRequest = new MixedTrafficLaemmerSignalController.Request(now + this.laemmerConfig.getIntergreenTime(), max);
        }

        if (this.activeRequest != null && this.activeRequest.isDue(now)) {
            this.system.scheduleOnset(now, this.activeRequest.signal.group.getId());
        }

    }

    private void updateRepresentativeDriveways(double now) {
        this.tIdle = this.laemmerConfig.getDesiredCycleTime();

        MixedTrafficLaemmerSignalController.LaemmerSignal signal;
        for(Iterator var3 = this.laemmerSignals.iterator(); var3.hasNext(); this.tIdle -= Math.max(signal.determiningLoad * this.laemmerConfig.getDesiredCycleTime() + this.laemmerConfig.getIntergreenTime(), this.laemmerConfig.getMinGreenTime())) {
            signal = (MixedTrafficLaemmerSignalController.LaemmerSignal)var3.next();
            signal.determineRepresentativeDriveway(now);
        }

        this.tIdle = Math.max(0.0D, this.tIdle);
    }

    private double getNumberOfExpectedVehiclesOnLink(double now, Id<Link> linkId) {
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0.0D, now);
    }

    private double getNumberOfExpectedVehiclesOnLane(double now, Id<Link> linkId, Id<Lane> laneId) {
        return ((LanesToLinkAssignment)this.lanes.getLanesToLinkAssignments().get(linkId)).getLanes().size() == 1 ? this.getNumberOfExpectedVehiclesOnLink(now, linkId) : this.sensorManager.getNumberOfCarsInDistanceOnLane(linkId, laneId, 0.0D, now);
    }

    private double getAverageArrivalRate(double now, Id<Link> linkId) {
        return this.laemmerConfig.getLinkArrivalRate(linkId) != null ? this.laemmerConfig.getLinkArrivalRate(linkId) : this.sensorManager.getAverageArrivalRateOnLink(linkId, now);
    }

    private double getAverageLaneArrivalRate(double now, Id<Link> linkId, Id<Lane> laneId) {
        if (((LanesToLinkAssignment)this.lanes.getLanesToLinkAssignments().get(linkId)).getLanes().size() > 1) {
            return this.laemmerConfig.getLaneArrivalRate(linkId, laneId) != null ? this.laemmerConfig.getLaneArrivalRate(linkId, laneId) : this.sensorManager.getAverageArrivalRateOnLane(linkId, laneId, now);
        } else {
            return this.getAverageArrivalRate(now, linkId);
        }
    }

    private void initializeSensoring() {
        Iterator var1 = this.system.getSignalGroups().values().iterator();

        while(var1.hasNext()) {
            SignalGroup group = (SignalGroup)var1.next();
            Iterator var3 = group.getSignals().values().iterator();

            while(var3.hasNext()) {
                Signal signal = (Signal)var3.next();
                if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                    Iterator var5 = signal.getLaneIds().iterator();

                    while(var5.hasNext()) {
                        Id<Lane> laneId = (Id)var5.next();
                        this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.0D);
                        this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoringOnLane(signal.getLinkId(), laneId, this.laemmerConfig.getLookBackTime(), this.laemmerConfig.getTimeBucketSize());
                    }
                }

                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.0D);
                this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId(), this.laemmerConfig.getLookBackTime(), this.laemmerConfig.getTimeBucketSize());
            }
        }

        if (this.laemmerConfig.isCheckDownstream()) {
            this.downstreamSensor.registerDownstreamSensors(this.system);
        }

    }

    class Request {
        private final double onsetTime;
        private final MixedTrafficLaemmerSignalController.LaemmerSignal signal;

        Request(double onsetTime, MixedTrafficLaemmerSignalController.LaemmerSignal laemmerSignal) {
            this.signal = laemmerSignal;
            this.onsetTime = onsetTime;
        }

        private boolean isDue(double now) {
            return now == this.onsetTime;
        }
    }

    class LaemmerSignal {
        SignalGroup group;
        double index = 0.0D;
        private double abortionPenalty = 0.0D;
        private boolean stabilize = false;
        private double a;
        private double regulationTime;
        private Id<Lane> determiningLane;
        private Id<Link> determiningLink;
        private double determiningArrivalRate;
        private double determiningLoad;
        private double signalOutflowCapacityPerS;
        private Map<Id<Lane>, Double> laneOutflowCapacitiesPerS;
        private Map<Id<Link>, Double> linkOutflowCapacitiesPerS;

        LaemmerSignal(SignalGroup signalGroup) {
            this.a = MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime();
            this.regulationTime = 0.0D;
            this.laneOutflowCapacitiesPerS = new HashMap();
            this.linkOutflowCapacitiesPerS = new HashMap();
            this.group = signalGroup;
            Iterator var3 = this.group.getSignals().values().iterator();

            while(true) {
                while(var3.hasNext()) {
                    Signal signal = (Signal)var3.next();
                    double laneOutflow;
                    if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().size() > 1) {
                        for(Iterator var9 = signal.getLaneIds().iterator(); var9.hasNext(); this.signalOutflowCapacityPerS += laneOutflow) {
                            Id<Lane> laneId = (Id)var9.next();
                            laneOutflow = ((Lane)((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().get(laneId)).getCapacityVehiclesPerHour() * MixedTrafficLaemmerSignalController.this.config.qsim().getFlowCapFactor() / 3600.0D;
                            this.laneOutflowCapacitiesPerS.put(laneId, laneOutflow);
                        }
                    } else {
                        double linkOutflowPerS = ((Link)MixedTrafficLaemmerSignalController.this.network.getLinks().get(signal.getLinkId())).getCapacity() * MixedTrafficLaemmerSignalController.this.config.qsim().getFlowCapFactor() / 3600.0D;
                        this.linkOutflowCapacitiesPerS.put(signal.getLinkId(), linkOutflowPerS);
                        this.signalOutflowCapacityPerS += linkOutflowPerS;
                    }
                }

                return;
            }
        }

        private void determineRepresentativeDriveway(double now) {
            this.determiningLoad = 0.0D;
            this.determiningLink = null;
            this.determiningLane = null;
            Iterator var3 = this.group.getSignals().values().iterator();

            while(true) {
                while(var3.hasNext()) {
                    Signal signal = (Signal)var3.next();
                    double arrivalRate;
                    if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().size() > 1) {
                        Iterator var11 = signal.getLaneIds().iterator();

                        while(var11.hasNext()) {
                            Id<Lane> laneId = (Id)var11.next();
                            arrivalRate = MixedTrafficLaemmerSignalController.this.getAverageLaneArrivalRate(now, signal.getLinkId(), laneId);
                            double tempLoad = Math.min(1.0D, arrivalRate / (Double)this.laneOutflowCapacitiesPerS.get(laneId));
                            if (tempLoad >= this.determiningLoad) {
                                this.determiningLoad = tempLoad;
                                this.determiningArrivalRate = arrivalRate;
                                this.determiningLane = laneId;
                                this.determiningLink = signal.getLinkId();
                            }
                        }
                    } else {
                        double arrivalRatex = MixedTrafficLaemmerSignalController.this.getAverageArrivalRate(now, signal.getLinkId());
                        arrivalRate = Math.min(1.0D, arrivalRatex / (Double)this.linkOutflowCapacitiesPerS.get(signal.getLinkId()));
                        if (arrivalRate >= this.determiningLoad) {
                            this.determiningLoad = arrivalRate;
                            this.determiningArrivalRate = arrivalRatex;
                            this.determiningLane = null;
                            this.determiningLink = signal.getLinkId();
                        }
                    }
                }

                return;
            }
        }

        private void update(double now) {
            this.updateAbortionPenalty(now);
            if (!MixedTrafficLaemmerSignalController.this.laemmerConfig.getActiveRegime().equals(Regime.OPTIMIZING)) {
                this.updateStabilization(now);
            }

            if (!this.stabilize) {
                this.calculatePriorityIndex(now);
            }

        }

        private void updateAbortionPenalty(double now) {
            this.abortionPenalty = 0.0D;
            if (MixedTrafficLaemmerSignalController.this.activeRequest != null && this.equals(MixedTrafficLaemmerSignalController.this.activeRequest.signal)) {
                double waitingTimeSum = 0.0D;
                double remainingInBetweenTime = Math.max(MixedTrafficLaemmerSignalController.this.activeRequest.onsetTime - now, 0.0D);

                double i;
                Iterator var9;
                Signal signal;
                Iterator var11;
                Id laneId;
                label74:
                for(i = remainingInBetweenTime; i < MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime(); ++i) {
                    var9 = this.group.getSignals().values().iterator();

                    while(true) {
                        while(true) {
                            if (!var9.hasNext()) {
                                continue label74;
                            }

                            signal = (Signal)var9.next();
                            if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().size() > 1) {
                                for(var11 = signal.getLaneIds().iterator(); var11.hasNext(); waitingTimeSum += (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLane(now + i, signal.getLinkId(), laneId)) {
                                    laneId = (Id)var11.next();
                                }
                            } else {
                                waitingTimeSum += (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLink(now + i, signal.getLinkId());
                            }
                        }
                    }
                }

                i = 0.0D;
                var9 = this.group.getSignals().values().iterator();

                while(true) {
                    while(var9.hasNext()) {
                        signal = (Signal)var9.next();
                        if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().size() > 1) {
                            for(var11 = signal.getLaneIds().iterator(); var11.hasNext(); i += (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLane(now + MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime(), signal.getLinkId(), laneId)) {
                                laneId = (Id)var11.next();
                            }
                        } else {
                            i += (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLink(now + MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime(), signal.getLinkId());
                        }
                    }

                    if (i > 0.0D) {
                        this.abortionPenalty += waitingTimeSum / i;
                    }
                    break;
                }
            }

        }

        private void calculatePriorityIndex(double now) {
            this.index = 0.0D;
            double nExpected;
            double remainingMinG;
            double nExpectedx;
            double reqGreenTime;
            double i;
            if (MixedTrafficLaemmerSignalController.this.activeRequest != null && MixedTrafficLaemmerSignalController.this.activeRequest.signal == this) {
                nExpected = Math.max(MixedTrafficLaemmerSignalController.this.activeRequest.onsetTime - now, 0.0D);
                remainingMinG = Math.max(MixedTrafficLaemmerSignalController.this.activeRequest.onsetTime - now + MixedTrafficLaemmerSignalController.this.laemmerConfig.getMinGreenTime() - nExpected, 0.0D);

                for(i = nExpected; i <= MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime(); ++i) {
                    nExpectedx = 0.0D;
                    reqGreenTime = remainingMinG;
                    Iterator var23 = this.group.getSignals().values().iterator();

                    while(var23.hasNext()) {
                        Signal signal = (Signal)var23.next();
                        double tempIndex;
                        double tempGreenTimex;
                        if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().size() > 1) {
                            Iterator var24 = signal.getLaneIds().iterator();

                            while(var24.hasNext()) {
                                Id<Lane> laneId = (Id)var24.next();
                                tempGreenTimex = (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLane(now + i + remainingMinG, signal.getLinkId(), laneId);
                                nExpectedx += tempGreenTimex;
                                double tempGreenTimexx = tempGreenTimex / (Double)this.laneOutflowCapacitiesPerS.get(laneId);
                                if (tempGreenTimexx > reqGreenTime) {
                                    reqGreenTime = tempGreenTimexx;
                                }
                            }
                        } else {
                            tempIndex = (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLink(now + i + remainingMinG, signal.getLinkId());
                            nExpectedx += tempIndex;
                            tempGreenTimex = tempIndex / (Double)this.linkOutflowCapacitiesPerS.get(signal.getLinkId());
                            if (tempGreenTimex > reqGreenTime) {
                                reqGreenTime = tempGreenTimex;
                            }
                        }

                        tempIndex = 0.0D;
                        if (nExpectedx > 0.0D) {
                            tempIndex = nExpectedx / (i + reqGreenTime);
                        }

                        if (tempIndex > this.index) {
                            this.index = tempIndex;
                        }
                    }
                }
            } else {
                nExpected = 0.0D;
                remainingMinG = MixedTrafficLaemmerSignalController.this.laemmerConfig.getMinGreenTime();
                Iterator var7 = this.group.getSignals().values().iterator();

                while(true) {
                    while(var7.hasNext()) {
                        Signal signalx = (Signal)var7.next();
                        if (signalx.getLaneIds() != null && !signalx.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signalx.getLinkId())).getLanes().size() > 1) {
                            Iterator var22 = signalx.getLaneIds().iterator();

                            while(var22.hasNext()) {
                                Id<Lane> laneIdx = (Id)var22.next();
                                reqGreenTime = (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLane(now + MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime() + MixedTrafficLaemmerSignalController.this.laemmerConfig.getMinGreenTime(), signalx.getLinkId(), laneIdx);
                                nExpected += reqGreenTime;
                                double tempGreenTime = reqGreenTime / (Double)this.laneOutflowCapacitiesPerS.get(laneIdx);
                                if (tempGreenTime > remainingMinG) {
                                    remainingMinG = tempGreenTime;
                                }
                            }
                        } else {
                            nExpectedx = (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLink(now + MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime() + MixedTrafficLaemmerSignalController.this.laemmerConfig.getMinGreenTime(), signalx.getLinkId());
                            nExpected += nExpectedx;
                            reqGreenTime = nExpectedx / (Double)this.linkOutflowCapacitiesPerS.get(signalx.getLinkId());
                            if (reqGreenTime > remainingMinG) {
                                remainingMinG = reqGreenTime;
                            }
                        }
                    }

                    i = 0.0D;
                    if (MixedTrafficLaemmerSignalController.this.activeRequest != null) {
                        i = MixedTrafficLaemmerSignalController.this.activeRequest.signal.abortionPenalty;
                    }

                    this.index = nExpected / (i + MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime() + remainingMinG);
                    break;
                }
            }

        }

        private void updateStabilization(double now) {
            if (this.determiningArrivalRate != 0.0D) {
                double n = 0.0D;
                if (this.determiningLane != null) {
                    n = (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLane(now, this.determiningLink, this.determiningLane);
                } else {
                    n = (double)MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLink(now, this.determiningLink);
                }

                if (n == 0.0D) {
                    this.a = MixedTrafficLaemmerSignalController.this.laemmerConfig.getIntergreenTime();
                } else {
                    this.a += MixedTrafficLaemmerSignalController.this.config.qsim().getTimeStepSize();
                }

                if (!MixedTrafficLaemmerSignalController.this.regulationQueue.contains(this)) {
                    this.regulationTime = 0.0D;
                    this.stabilize = false;
                    double nCrit = this.determiningArrivalRate * MixedTrafficLaemmerSignalController.this.laemmerConfig.getDesiredCycleTime() * ((MixedTrafficLaemmerSignalController.this.laemmerConfig.getMaxCycleTime() - this.a / (1.0D - this.determiningLoad)) / (MixedTrafficLaemmerSignalController.this.laemmerConfig.getMaxCycleTime() - MixedTrafficLaemmerSignalController.this.laemmerConfig.getDesiredCycleTime()));
                    if (n > 0.0D && n >= nCrit && (!MixedTrafficLaemmerSignalController.this.laemmerConfig.isCheckDownstream() || MixedTrafficLaemmerSignalController.this.downstreamSensor.allDownstreamLinksEmpty(MixedTrafficLaemmerSignalController.this.system.getId(), this.group.getId()))) {
                        MixedTrafficLaemmerSignalController.this.regulationQueue.add(this);
                        this.regulationTime = Math.max(Math.rint(this.determiningLoad * MixedTrafficLaemmerSignalController.this.laemmerConfig.getDesiredCycleTime() + this.signalOutflowCapacityPerS / MixedTrafficLaemmerSignalController.this.systemOutflowCapacity * MixedTrafficLaemmerSignalController.this.tIdle), MixedTrafficLaemmerSignalController.this.laemmerConfig.getMinGreenTime());
                        this.stabilize = true;
                    }

                }
            }
        }

        public void getStatFields(StringBuilder builder) {
            builder.append("state_" + this.group.getId() + ";");
            builder.append("index_" + this.group.getId() + ";");
            builder.append("load_" + this.group.getId() + ";");
            builder.append("a_" + this.group.getId() + ";");
            builder.append("abortionPen_" + this.group.getId() + ";");
            builder.append("regTime_" + this.group.getId() + ";");
            builder.append("nTotal_" + this.group.getId() + ";");
        }

        public void getStepStats(StringBuilder builder, double now) {
            int totalN = 0;
            Iterator var5 = this.group.getSignals().values().iterator();

            while(true) {
                while(var5.hasNext()) {
                    Signal signal = (Signal)var5.next();
                    Id laneId;
                    if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && ((LanesToLinkAssignment)MixedTrafficLaemmerSignalController.this.lanes.getLanesToLinkAssignments().get(signal.getLinkId())).getLanes().size() > 1) {
                        for(Iterator var7 = signal.getLaneIds().iterator(); var7.hasNext(); totalN += MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), laneId)) {
                            laneId = (Id)var7.next();
                        }
                    } else {
                        totalN += MixedTrafficLaemmerSignalController.this.getNumberOfExpectedVehiclesOnLink(now, signal.getLinkId());
                    }
                }

                builder.append(this.group.getState().name() + ";").append(this.index + ";").append(this.determiningLoad + ";").append(this.a + ";").append(this.abortionPenalty + ";").append(this.regulationTime + ";").append(totalN + ";");
                return;
            }
        }
    }

    public static final class LaemmerFactory implements SignalControllerFactory {
        @Inject
        private ControlerListenerManager manager;
        @Inject
        private MixedTrafficLinkSensorManager sensorManager;
        @Inject
        private MixedTrafficDownstreamSensor downstreamSensor;
        @Inject
        private Scenario scenario;

        public LaemmerFactory() {
        }

        public SignalController createSignalSystemController(SignalSystem signalSystem) {
            MixedTrafficLaemmerSignalController controller = new MixedTrafficLaemmerSignalController(this.scenario, this.sensorManager, this.downstreamSensor);
            controller.setSignalSystem(signalSystem);
            controller.setScenario(this.scenario);
            this.manager.addControlerListener(controller);
            return controller;
        }
    }

    private void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }
}
