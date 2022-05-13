package org.matsim.contrib.signals.sensor;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

final class MixedTrafficLaneSensor {
    private Link link;
    private Lane lane;
    private int agentsOnLane = 0;
    private boolean doDistanceMonitoring = false;
    private boolean doAverageVehiclesPerSecondMonitoring = false;
    private double totalVehicles = 0.0D;
    private double monitoringStartTime;
    private Map<Double, Map<Id<Vehicle>, CarLocator>> distanceMeterCarLocatorMap = null;
    private double lookBackTime;
    private LinkedList<AtomicInteger> timeBuckets;
    private double timeBucketSize;
    private double currentBucketStartTime;
    private AtomicInteger currentBucket;
    private int numOfBucketsNeededForLookback;
    private double volume = 0.0D;
    private Map<Id<Vehicle>, Vehicle> vehicles;

    public MixedTrafficLaneSensor(Link link, Lane lane, Map<Id<Vehicle>, Vehicle> vehicles) {
        this.link = link;
        this.lane = lane;
        this.vehicles = vehicles;
    }

    public void registerDistanceToMonitor(Double distanceMeter) {
        if (!this.doDistanceMonitoring) {
            this.doDistanceMonitoring = true;
            this.distanceMeterCarLocatorMap = new HashMap();
        }

        this.distanceMeterCarLocatorMap.put(distanceMeter, new HashMap());
    }

    public void handleEvent(LaneEnterEvent event) {
        ++this.agentsOnLane;
        if (this.doAverageVehiclesPerSecondMonitoring) {
            if (this.lookBackTime != 1.0D / 0.0) {
                this.updateBucketsUntil(event.getTime());
                this.currentBucket.incrementAndGet();
            }

            ++this.totalVehicles;
            this.volume += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
            if (this.totalVehicles == 1.0D) {
                this.monitoringStartTime = event.getTime();
            }
        }

        if (this.doDistanceMonitoring) {

            for (Double distance : this.distanceMeterCarLocatorMap.keySet()) {
                Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = (Map) this.distanceMeterCarLocatorMap.get(distance);
                carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.lane, this.link, event.getTime(), distance));
            }
        }

    }

    public void handleEvent(LaneLeaveEvent event) {
        --this.agentsOnLane;
        if (this.doDistanceMonitoring) {

            for (Double distance : this.distanceMeterCarLocatorMap.keySet()) {
                Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = (Map) this.distanceMeterCarLocatorMap.get(distance);
                carLocatorPerVehicleId.remove(event.getVehicleId());
            }
        }

    }

    public int getNumberOfCarsOnLane() {
        return this.agentsOnLane;
    }

    public int getNumberOfCarsInDistance(Double distanceMeter, double now) {
        Map<Id<Vehicle>, CarLocator> distSpecificCarLocators = (Map)this.distanceMeterCarLocatorMap.get(distanceMeter);
        int count = 0;

        for (var entry: distSpecificCarLocators.entrySet()) {
            if (entry.getValue().isCarinDistance(now)) {
                count = (int) (count + this.vehicles.get(entry.getKey()).getType().getPcuEquivalents());
            }
        }
//        for (CarLocator cl : distSpecificCarLocators.values()) {
//            if (cl.isCarinDistance(now)) {
//                ++count;
//            }
//        }

        return count;
    }

    public double getAvgVehiclesPerSecond(double now) {
        double avgVehPerSecond = 0.0D;
        if (now > this.monitoringStartTime) {
            if (this.lookBackTime == 1.0D / 0.0) {
//                avgVehPerSecond = this.totalVehicles / (now - this.monitoringStartTime + 1.0D);
                avgVehPerSecond = this.volume / (now - this.monitoringStartTime + 1.0D);
            } else {
                this.updateBucketsUntil(now);
                if (this.timeBuckets.size() > 0) {
                    avgVehPerSecond = (double)this.timeBuckets.stream().mapToInt(AtomicInteger::intValue).sum() / ((double)this.timeBuckets.size() * this.timeBucketSize);
                }

                if ((this.timeBuckets.size() == 0 || avgVehPerSecond == 0.0D) && this.currentBucket != null && this.currentBucket.get() > 0) {
                    avgVehPerSecond = (double)this.currentBucket.get() / (now - this.currentBucketStartTime + 1.0D);
                }
            }
        }

        return this.volume / (now - this.monitoringStartTime + 1.0D);
    }

    public void registerAverageVehiclesPerSecondToMonitor() {
        this.registerAverageVehiclesPerSecondToMonitor(1.0D / 0.0, 1.0D / 0.0);
    }

    public void registerAverageVehiclesPerSecondToMonitor(double lookBackTime, double timeBucketSize) {
        if (!this.doAverageVehiclesPerSecondMonitoring) {
            this.doAverageVehiclesPerSecondMonitoring = true;
            this.lookBackTime = lookBackTime;
            this.timeBucketSize = timeBucketSize;
            this.timeBuckets = new LinkedList();
            this.currentBucketStartTime = 0.0D;
            this.currentBucket = new AtomicInteger(0);
            this.numOfBucketsNeededForLookback = (int)Math.ceil(lookBackTime / timeBucketSize);
        }

    }

    private void queueFullBucket(AtomicInteger bucket) {
        this.timeBuckets.add(bucket);
        if (this.timeBuckets.size() > this.numOfBucketsNeededForLookback) {
            this.timeBuckets.poll();
        }

    }

    private void updateBucketsUntil(double now) {
        if (now >= this.currentBucketStartTime + this.timeBucketSize) {
            this.queueFullBucket(this.currentBucket);

            for(this.currentBucketStartTime += this.timeBucketSize; this.currentBucketStartTime <= now - this.timeBucketSize; this.currentBucketStartTime += this.timeBucketSize) {
                this.queueFullBucket(new AtomicInteger(0));
            }

            this.currentBucket = new AtomicInteger(0);
        }

    }
}

