package org.matsim.contrib.signals.sensor;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;

final class MixedTrafficLinkSensor {
    private static final Logger log = Logger.getLogger(LinkSensor.class);
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
    private int countTrucks = 0;
    private int countCars = 0;
    public MixedTrafficLinkSensor(Link link) {
        this.link = link;
    }

    public void registerDistanceToMonitor(Double distanceMeter) {
        if (!this.doDistanceMonitoring) {
            this.enableDistanceMonitoring();
        }

        this.distanceMeterCarLocatorMap.put(distanceMeter, new HashMap());
    }

    public void registerAverageVehiclesPerSecondToMonitor() {
        this.registerAverageVehiclesPerSecondToMonitor(1.0D / 0.0, 1.0D / 0.0);
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
        Iterator var6 = distSpecificCarLocators.values().iterator();
        while (var6.hasNext()) {
            CarLocator cl = (CarLocator) var6.next();
            if (cl.isCarinDistance(now)) {
                ++count;
            }
        }

        return count;
    }

    public double getAvgVehiclesPerSecond(double now) {
        double avgVehPerSecond = 0.0D;
        if (now > this.monitoringStartTime) {
            if (this.lookBackTime == 1.0D / 0.0) {
                avgVehPerSecond = (this.countCars * MixedTrafficVehiclesUtils.getPCU("car") + this.countTrucks * MixedTrafficVehiclesUtils.getPCU("truck")) / (now - this.monitoringStartTime + 1.0D);
//                avgVehPerSecond = this.totalVehicles / (now - this.monitoringStartTime + 1.0D);
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
        ++this.vehiclesOnLink;
        if (this.doAverageVehiclesPerSecondMonitoring) {
            if (this.lookBackTime != 1.0D / 0.0) {
                this.updateBucketsUntil(event.getTime());
                this.currentBucket.incrementAndGet();
            }
            if (event.getVehicleId().toString().contains("truck"))
                ++this.countTrucks;
            else
                ++this.countCars;
            ++this.totalVehicles;
            if (this.totalVehicles == 1.0D) {
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
        ++this.vehiclesOnLink;
        if (this.doAverageVehiclesPerSecondMonitoring) {
            if (this.lookBackTime != 1.0D / 0.0) {
                this.updateBucketsUntil(event.getTime());
                this.currentBucket.incrementAndGet();
            }
            if (event.getVehicleId().toString().contains("truck"))
                ++this.countTrucks;
            else
                ++this.countCars;
            ++this.totalVehicles;
            if (this.totalVehicles == 1.0D) {
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
    }

    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehicleLeftLink(event.getVehicleId());
        if (this.doAverageVehiclesPerSecondMonitoring) {
            --this.totalVehicles;
            if (event.getVehicleId().toString().contains("truck"))
                --this.countTrucks;
            else
                --this.countCars;
        }

    }

    private void vehicleLeftLink(Id<Vehicle> vehId) {
        --this.vehiclesOnLink;
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
