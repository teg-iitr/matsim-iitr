/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.amit.emissions.onRoadExposure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.Pollutant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by amit on 15.11.17.
 */

public class OnRoadExposureTable {

    private static final Logger LOG = LogManager.getLogger(OnRoadExposureTable.class);
    private Map<Id<Person>, List<OnRoadTripExposureInfo>> personInfo = new HashMap<>();

    /**
     * @param personId
     * @param linkId
     * @param mode
     * @param time
     */
    public void createTripAndAddInfo(Id<Person> personId, Id<Link> linkId, String mode, Double time) {
        List<OnRoadTripExposureInfo> list = this.personInfo.get(personId);
        if (list == null ) {
            list = new ArrayList<>();
        }
        OnRoadTripExposureInfo tripExposureInfo =  new OnRoadTripExposureInfo(personId, mode);
        tripExposureInfo.addInhaledMass(time, linkId, new HashMap<>());
        list.add(tripExposureInfo);
        this.personInfo.put(personId, list);
    }


        /**
         * @param personId
         * @param linkId
         * @param mode
         * @param time
         * @param inhaledMass if already exists in the table, values will be summed
         */
    public void addInfoToTable(Id<Person> personId, Id<Link> linkId, String mode, Double time, Map<Pollutant, Double> inhaledMass) {
        List<OnRoadTripExposureInfo> list = this.personInfo.get(personId);
        OnRoadTripExposureInfo info = list.get(list.size()-1);
        if (! info.mode.equals(mode)) throw new RuntimeException("A new mode is found for same trip.");
        info.addInhaledMass(time, linkId, inhaledMass);
    }

    public static class OnRoadTripExposureInfo{
        private Id<Person> personId;
        private String mode;

        private final Map<Double,Map<Pollutant,Double>> time2Emissions = new HashMap<>();
        private final Map<Id<Link>,Map<Pollutant,Double>> link2Emissions = new HashMap<>();
//        Table<Double,Id<Link>, Map<String,Double>> time2link2emissions = HashBasedTable.create();

        OnRoadTripExposureInfo(Id<Person> person, String mode) {
            this.personId = person;
            this.mode = mode;
        }

        void addInhaledMass(double time, Id<Link> linkId, Map<Pollutant, Double> inhaledMass){
            {
                Map<Pollutant, Double> soFar = this.time2Emissions.get(time);
                if (soFar==null) this.time2Emissions.put(time, inhaledMass);
                else {
                    Map<Pollutant, Double> outMap = new HashMap<>(inhaledMass);
                    soFar.forEach((k,v) -> outMap.merge(k,v,Double::sum));
                    this.time2Emissions.put(time, outMap);
                }
            }

            {
                Map<Pollutant, Double> soFar = this.link2Emissions.get(linkId);
                if (soFar==null) this.link2Emissions.put(linkId, inhaledMass);
                else {
                    Map<Pollutant, Double> outMap = new HashMap<>(inhaledMass);
                    soFar.forEach((k,v) -> outMap.merge(k,v,Double::sum));
                    this.link2Emissions.put(linkId, outMap);
                }
            }
        }

        public Id<Person> getPersonId() {
            return personId;
        }

        public String getMode() {
            return mode;
        }

        public Map<Id<Link>, Map<Pollutant, Double>> getLink2Emissions() {
            return link2Emissions;
        }
    }

    public void clear(){
        this.personInfo.clear();
    }

    public Map<Pollutant, Double> getTotalInhaledMass(){
        LOG.info("Computing total inhaled mass ...");
        Map<Pollutant, Double> out = new HashMap<>();
        for (List<OnRoadTripExposureInfo> infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                valueMapSum(info.link2Emissions).forEach((k,v) -> out.merge(k,v, Double::sum));
            }
        }
        return out;
    }

    private Map<Pollutant, Double> valueMapSum(Map<Id<Link>, Map<Pollutant, Double>> inMap) {
        return inMap.values()
                .stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingDouble(Map.Entry::getValue)));
    }

    public Map<Id<Person>, Map<Pollutant, Double>> getPersonToInhaledMass(){
        LOG.info("Computing total inhaled mass for each person ...");
        Map<Id<Person>, Map<Pollutant, Double>> out = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                Map<Pollutant, Double> tempSum = out.get(info.personId);
                if (tempSum == null) {
                    tempSum = new HashMap<>();
                }
                final Map<Pollutant, Double> finalMap = new HashMap<>(tempSum);
                valueMapSum(info.link2Emissions).forEach((k,v) -> finalMap.merge(k,v,Double::sum));
                out.put(info.personId, finalMap);
            }
        }
        return out;
    }

    public Map<String, Map<Pollutant,Double>> getModeToInhaledMass(){
        LOG.info("Computing total inhaled mass for each mode ...");
        Map<String, Map<Pollutant, Double>> out = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                final Map<Pollutant, Double> tempOut = out.getOrDefault(info.mode, new HashMap<>());
                valueMapSum(info.link2Emissions).forEach((k,v) -> tempOut.merge(k,v,Double::sum));
                out.put(info.mode, tempOut);
            }
        }
        return out;
    }

    public Map<Id<Link>,Map<Pollutant,Double>> getLinkToInhaledMass(){
        LOG.info("Computing total inhaled mass for each link ...");
        Map<Id<Link>, Map<Pollutant,Double>> outMap = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                outMap = mergeMultiMaps(outMap, info.link2Emissions);
            }
        }
        return outMap;
    }

    public Map<Double,Map<Pollutant,Double>> getTimeToInhaledMass() {
        LOG.info("Computing total inhaled mass in each time bin ...");
        Map<Double, Map<Pollutant,Double>> outMap = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                outMap = mergeMultiMaps(outMap, info.time2Emissions);
            }
        }
        return outMap;
    }

    public static <T> Map<T, Map<Pollutant, Double>> mergeMultiMaps(final Map<T, Map<Pollutant, Double>> m1, final Map<T, Map<Pollutant, Double>> m2) {
        if(m1==null || m2 ==null) throw new NullPointerException("Either of the maps is null. Aborting ...");
        Map<T, Map<Pollutant, Double>> outMap = new HashMap<>(m1);
        m2.forEach(  (k,v) ->  outMap.merge( k, v, OnRoadExposureTable::mergeMaps) );
        return outMap;
    }

    public static Map<Pollutant, Double> mergeMaps(final Map<Pollutant, Double> m1, final Map<Pollutant, Double> m2) {
        if(m1==null || m2 ==null) throw new NullPointerException("Either of the maps is null. Aborting ...");
        Map<Pollutant, Double> outMap = new HashMap<>(m1);
        m2.forEach((k,v) -> outMap.merge(k, v, Double::sum));
        return outMap;
    }

    public Map<Id<Person>, List<OnRoadTripExposureInfo>> getPersonOnRoadExposureInfo() {
        return personInfo;
    }
}
