package org.matsim.contrib.signals.analysis;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

@Singleton
public class MixedTrafficSignalAnalysisTool implements SignalGroupStateChangedEventHandler, AfterMobsimListener, ActivityStartEventHandler, ActivityEndEventHandler {
    private final SignalsData signalsData;
    private Map<Id<SignalGroup>, Double> totalSignalGreenTime;
    private List<Double> cycleTimes;
    private Map<Id<SignalSystem>, Integer> numberOfCyclesPerSystem;
    private Map<Id<SignalSystem>, Double> sumOfSystemCycleTimes;
    private Map<Double, Map<Id<SignalGroup>, Double>> summedBygoneSignalGreenTimesPerSecond;
    private Map<Id<SignalGroup>, Double> lastSwitchesToGreen;
    private Map<Id<SignalGroup>, Double> lastSwitchesToRed;
    private Map<Id<SignalSystem>, Double> lastCycleStartPerSystem;
    private Map<Id<SignalGroup>, Id<SignalSystem>> signalGroup2signalSystemId;
    private Map<Id<SignalSystem>, Id<SignalGroup>> firstSignalGroupOfSignalSystem;
    private double lastActStartTime;
    private Double firstActEndTime;


    public Map<Id<SignalGroup>, Id<SignalSystem>> getSignalGroup2signalSystemId() {
        return signalGroup2signalSystemId;
    }
    public Map<Id<SignalSystem>, Double> getSumOfSystemCycleTimes() {
        return sumOfSystemCycleTimes;
    }
    public List<Double> getCycleTimes() {
        return cycleTimes;
    }
    public MixedTrafficSignalAnalysisTool(SignalsData signalsData) {
        this.signalsData = signalsData;
    }

    @Inject
    public MixedTrafficSignalAnalysisTool(EventsManager em, ControlerListenerManager clm, SignalsData signalsData) {
        this(signalsData);
        em.addHandler(this);
        clm.addControlerListener(this);
    }

    public void reset(int iteration) {
        this.totalSignalGreenTime = new HashMap();
        this.numberOfCyclesPerSystem = new HashMap();
        this.signalGroup2signalSystemId = new HashMap();
        this.firstSignalGroupOfSignalSystem = new HashMap();
        this.sumOfSystemCycleTimes = new HashMap();
        this.summedBygoneSignalGreenTimesPerSecond = new TreeMap();
        this.lastSwitchesToGreen = new HashMap();
        this.lastSwitchesToRed = new HashMap();
        this.lastCycleStartPerSystem = new HashMap();
        this.cycleTimes = new ArrayList<>();
    }

    public void handleEvent(ActivityStartEvent event) {
        this.lastActStartTime = event.getTime();
    }

    public void handleEvent(ActivityEndEvent event) {
        if (this.firstActEndTime == null) {
            this.firstActEndTime = event.getTime();
        }
    }

    public void handleEvent(SignalGroupStateChangedEvent event) {

        if (!this.signalGroup2signalSystemId.containsKey(event.getSignalGroupId())) {
            this.signalGroup2signalSystemId.put(event.getSignalGroupId(), event.getSignalSystemId());
        }

        switch(event.getNewState()) {
            case RED:
                this.lastSwitchesToRed.put(event.getSignalGroupId(), event.getTime());
                Double lastSwitchToGreen = (Double)this.lastSwitchesToGreen.remove(event.getSignalGroupId());
                this.doBygoneGreenTimeAnalysis(event, lastSwitchToGreen);
                break;
            case GREEN:
                this.lastSwitchesToGreen.put(event.getSignalGroupId(), event.getTime());
                this.doCycleAnalysis(event);
                Double lastSwitchToRed = (Double)this.lastSwitchesToRed.remove(event.getSignalGroupId());
                this.doBygoneGreenTimeAnalysis(event, lastSwitchToRed);
        }

    }


    public void notifyAfterMobsim(AfterMobsimEvent event) {
        double simEndTime = this.lastActStartTime;
        Iterator var4 = this.lastSwitchesToGreen.keySet().iterator();

        Id signalSystemId;
        while(var4.hasNext()) {
            signalSystemId = (Id)var4.next();
            this.calculateLastGreenTimeOfTheGroupAndAddToTotalGreen(signalSystemId, simEndTime, (Double)this.lastSwitchesToGreen.get(signalSystemId));
            this.fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(signalSystemId, simEndTime, (Double)this.lastSwitchesToGreen.get(signalSystemId), 1);
        }

        this.lastSwitchesToGreen.clear();
        var4 = this.lastSwitchesToRed.keySet().iterator();

        while(var4.hasNext()) {
            signalSystemId = (Id)var4.next();
            this.fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(signalSystemId, simEndTime, (Double)this.lastSwitchesToRed.get(signalSystemId), 0);
        }

        this.lastSwitchesToRed.clear();
        var4 = this.lastCycleStartPerSystem.keySet().iterator();
        while (var4.hasNext()) {
            signalSystemId = (Id) var4.next();
            this.addLastSystemCycleTime(signalSystemId, simEndTime);
        }
    }

    private void doBygoneGreenTimeAnalysis(SignalGroupStateChangedEvent event, Double lastSwitch) {
        if (lastSwitch == null) {
            if (!this.summedBygoneSignalGreenTimesPerSecond.containsKey(event.getTime())) {
                this.summedBygoneSignalGreenTimesPerSecond.put(event.getTime(), new HashMap());
            }

            ((Map)this.summedBygoneSignalGreenTimesPerSecond.get(event.getTime())).put(event.getSignalGroupId(), 0.0D);
        } else {
            int increment = 0;
            if (event.getNewState().equals(SignalGroupState.RED)) {
                increment = 1;
                this.calculateLastGreenTimeOfTheGroupAndAddToTotalGreen(event.getSignalGroupId(), event.getTime(), lastSwitch);
            }

            this.fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(event.getSignalGroupId(), event.getTime(), lastSwitch, increment);
        }

    }

    private void fillBygoneGreenTimeMapForEverySecondSinceLastSwitch(Id<SignalGroup> signalGroupId, double thisSwitch, double lastSwitch, int increment) {
        double lastBygoneSignalGreenTimeInsideMap = (Double)((Map)this.summedBygoneSignalGreenTimesPerSecond.get(lastSwitch)).get(signalGroupId);

        for(double time = lastSwitch + 1.0D; time <= thisSwitch; ++time) {
            if (!this.summedBygoneSignalGreenTimesPerSecond.containsKey(time)) {
                this.summedBygoneSignalGreenTimesPerSecond.put(time, new HashMap());
            }

            lastBygoneSignalGreenTimeInsideMap += (double)increment;
            ((Map)this.summedBygoneSignalGreenTimesPerSecond.get(time)).put(signalGroupId, lastBygoneSignalGreenTimeInsideMap);
        }

    }

    private void calculateLastGreenTimeOfTheGroupAndAddToTotalGreen(Id<SignalGroup> signalGroupId, double redSwitch, double lastGreenSwitch) {
        if (!this.totalSignalGreenTime.containsKey(signalGroupId)) {
            this.totalSignalGreenTime.put(signalGroupId, 0.0D);
        }

        double greenTime = redSwitch - lastGreenSwitch;
        this.totalSignalGreenTime.put(signalGroupId, (Double)this.totalSignalGreenTime.get(signalGroupId) + greenTime);
    }

    private void doCycleAnalysis(SignalGroupStateChangedEvent event) {
        if (!this.firstSignalGroupOfSignalSystem.containsKey(event.getSignalSystemId())) {
            this.firstSignalGroupOfSignalSystem.put(event.getSignalSystemId(), event.getSignalGroupId());
            this.numberOfCyclesPerSystem.put(event.getSignalSystemId(), 0);
        }

        if (event.getSignalGroupId().equals(this.firstSignalGroupOfSignalSystem.get(event.getSignalSystemId()))) {
            this.numberOfCyclesPerSystem.put(event.getSignalSystemId(), (Integer)this.numberOfCyclesPerSystem.get(event.getSignalSystemId()) + 1);
            if (this.lastCycleStartPerSystem.containsKey(event.getSignalSystemId())) {
                this.addLastSystemCycleTime(event.getSignalSystemId(), event.getTime());
            }

            this.lastCycleStartPerSystem.put(event.getSignalSystemId(), event.getTime());
        }

    }

    private void addLastSystemCycleTime(Id<SignalSystem> signalSystemId, double cycleStartTime) {
        if (!this.sumOfSystemCycleTimes.containsKey(signalSystemId)) {
            this.sumOfSystemCycleTimes.put(signalSystemId, 0.0D);
        }

        double lastCycleTime = cycleStartTime - (Double)this.lastCycleStartPerSystem.get(signalSystemId);
        double cycleTime = this.sumOfSystemCycleTimes.get(signalSystemId) + lastCycleTime;
        this.sumOfSystemCycleTimes.put(signalSystemId, cycleTime);
        this.cycleTimes.add(cycleTime);
    }

    public Map<Id<SignalGroup>, Double> getTotalSignalGreenTime() {
        return this.totalSignalGreenTime;
    }

    public Map<Id<SignalGroup>, Double> calculateAvgSignalGreenTimePerFlexibleCycle() {
        Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = new HashMap();
        Iterator var2 = this.totalSignalGreenTime.keySet().iterator();

        while(var2.hasNext()) {
            Id<SignalGroup> signalGroupId = (Id)var2.next();
            Id<SignalSystem> signalSystemId = (Id)this.signalGroup2signalSystemId.get(signalGroupId);
            double avgSignalGreenTime = (Double)this.totalSignalGreenTime.get(signalGroupId) / (double)(Integer)this.numberOfCyclesPerSystem.get(signalSystemId);
            avgSignalGreenTimePerCycle.put(signalGroupId, avgSignalGreenTime);
        }

        return avgSignalGreenTimePerCycle;
    }

    public Map<Id<SignalSystem>, Double> calculateAvgFlexibleCycleTimePerSignalSystem() {
        Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = new HashMap();
        Iterator var2 = this.sumOfSystemCycleTimes.keySet().iterator();

        while(var2.hasNext()) {
            Id<SignalSystem> signalSystemId = (Id)var2.next();
            double avgSystemCylceTime = (Double)this.sumOfSystemCycleTimes.get(signalSystemId) / (double)(Integer)this.numberOfCyclesPerSystem.get(signalSystemId);
            avgCycleTimePerSystem.put(signalSystemId, avgSystemCylceTime);
        }

        return avgCycleTimePerSystem;
    }

    public Map<Double, Map<Id<SignalGroup>, Double>> getSumOfBygoneSignalGreenTime() {
        return this.summedBygoneSignalGreenTimesPerSecond;
    }

    public Map<Id<SignalGroup>, Double> calculateSignalGreenTimeRatios() {
        Map<Id<SignalGroup>, Double> signalGreenTimeRatios = new HashMap();
        Iterator var2 = this.totalSignalGreenTime.keySet().iterator();

        while(var2.hasNext()) {
            Id<SignalGroup> signalGroupId = (Id)var2.next();
            double avgSignalGreenTime = (Double)this.totalSignalGreenTime.get(signalGroupId) / (this.lastActStartTime - this.firstActEndTime);
            signalGreenTimeRatios.put(signalGroupId, avgSignalGreenTime);
        }

        return signalGreenTimeRatios;
    }
}
