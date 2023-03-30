package org.matsim.contrib.signals.sensor;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

final class MixedTrafficLinkSensor {
    private static final Logger log = LogManager.getLogger(LinkSensor.class);
    private Link link = null;
    public int vehiclesOnLink = 0;
    private double totalVehicles = 0.0D;
    private boolean doDistanceMonitoring = false;
    private boolean doAverageVehiclesPerSecondMonitoring = false;
    private Map<Double, Map<Id<Vehicle>, CarLocator>> distanceMeterCarLocatorMap = null;
    private double monitoringStartTime;
    private double lookBackTime;
    private double timeBucketSize;
    private Queue<AtomicInteger> timeBuckets;
    private double currentBucketStartTime;
    private AtomicInteger currentBucket;
    private int numOfBucketsNeededForLookback;
    private Map<Id<Vehicle>, Vehicle> vehicles;
    private double volume = 0.0D;
    private Map<String, Double> mode2PCUs;

    public Map<String, Double> getMode2PCUs() {
        return mode2PCUs;
    }


    public MixedTrafficLinkSensor(Link link, Map<Id<Vehicle>, Vehicle> vehicles) {
        this.link = link;
        this.vehicles = vehicles;
        this.mode2PCUs = vehicles
                        .values()
                        .stream()
                        .collect(Collectors.toMap(v -> v.getId().toString(), vehicle -> vehicle.getType().getPcuEquivalents()));
    }


    public void registerDistanceToMonitor(Double distanceMeter) {
        if (!this.doDistanceMonitoring) {
            this.enableDistanceMonitoring();
        }

        this.distanceMeterCarLocatorMap.put(distanceMeter, new HashMap());
    }

    public void registerAverageVehiclesPerSecondToMonitor(double lookBackTime, double timeBucketCollectionDuration) {
        if (!this.doAverageVehiclesPerSecondMonitoring) {
            this.doAverageVehiclesPerSecondMonitoring = true;
            this.lookBackTime = lookBackTime;
            this.timeBucketSize = timeBucketCollectionDuration;
            this.timeBuckets = new LinkedList();
            this.currentBucketStartTime = 0.0D;
            this.currentBucket = new AtomicInteger(0);
            this.numOfBucketsNeededForLookback = (int)Math.ceil(lookBackTime / timeBucketCollectionDuration);
        }

    }

    private void enableDistanceMonitoring() {
        this.doDistanceMonitoring = true;
        this.distanceMeterCarLocatorMap = new HashMap();
    }

    public int getNumberOfCarsOnLink() {
        return this.vehiclesOnLink;
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
                avgVehPerSecond = this.volume / (now - this.monitoringStartTime + 1.0D);
                avgVehPerSecond = this.totalVehicles / (now - this.monitoringStartTime + 1.0D);
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

        return avgVehPerSecond;
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

    private void queueFullBucket(AtomicInteger bucket) {
        this.timeBuckets.add(bucket);
        if (this.timeBuckets.size() > this.numOfBucketsNeededForLookback) {
            this.timeBuckets.poll();
        }

    }

    public void handleEvent(LinkEnterEvent event) {
        this.vehiclesOnLink += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();;
        if (this.doAverageVehiclesPerSecondMonitoring) {
            if (this.lookBackTime != 1.0D / 0.0) {
                this.updateBucketsUntil(event.getTime());
                this.currentBucket.incrementAndGet();
            }
            this.volume += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
            this.totalVehicles += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();;
            if (this.volume == 1.0D || totalVehicles == 1) {
                this.monitoringStartTime = event.getTime();
            }
        }
        if (this.doDistanceMonitoring) {
            Iterator var2 = this.distanceMeterCarLocatorMap.keySet().iterator();

            while(var2.hasNext()) {
                Double distance = (Double)var2.next();
                Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = (Map)this.distanceMeterCarLocatorMap.get(distance);
                carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.link, event.getTime(), distance));
            }
        }

    }

    public void handleEvent(PersonEntersVehicleEvent event) {
        this.vehiclesOnLink += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
        if (this.doAverageVehiclesPerSecondMonitoring) {
            if (this.lookBackTime != 1.0D / 0.0) {
                this.updateBucketsUntil(event.getTime());
                this.currentBucket.incrementAndGet();
            }
            this.volume += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
            this.totalVehicles += this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
            if (this.volume == 1.0D || totalVehicles == 1) {
                this.monitoringStartTime = event.getTime();
            }
        }

        if (this.doDistanceMonitoring) {
            Iterator var2 = this.distanceMeterCarLocatorMap.keySet().iterator();

            while(var2.hasNext()) {
                Double distance = (Double)var2.next();
                Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = (Map)this.distanceMeterCarLocatorMap.get(distance);
                double fs_tt = this.link.getLength() / this.link.getFreespeed();
                carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.link, event.getTime() - fs_tt, distance));
            }
        }

    }

    public void handleEvent(LinkLeaveEvent event) {
        this.vehicleLeftLink(event.getVehicleId());
        this.volume -= this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
    }

    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehicleLeftLink(event.getVehicleId());
        if (this.doAverageVehiclesPerSecondMonitoring) {
            this.totalVehicles -= this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();;
            this.volume -= this.vehicles.get(event.getVehicleId()).getType().getPcuEquivalents();
        }

    }

    private void vehicleLeftLink(Id<Vehicle> vehId) {
        this.vehiclesOnLink -= this.vehicles.get(vehId).getType().getPcuEquivalents();
        if (this.doDistanceMonitoring) {
            Iterator var2 = this.distanceMeterCarLocatorMap.keySet().iterator();

            while(var2.hasNext()) {
                Double distance = (Double)var2.next();
                Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = (Map)this.distanceMeterCarLocatorMap.get(distance);
                carLocatorPerVehicleId.remove(vehId);
            }
        }

    }
}
