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

package playground.agarwalamit.emissions.onRoadExposure;

import java.util.Map;
import java.util.stream.Collectors;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * Created by amit on 15.11.17.
 */

public class OnRoadExposureTable {

    private Table<Id<Person>, String, OnRoadTripExposureInfo> personInfo = HashBasedTable.create();

    /**
     * @param personId
     * @param linkId
     * @param mode
     * @param time
     * @param inhaledMass if already exists in the table, values will be summed
     */
    public void addInfoToTable(Id<Person> personId, Id<Link> linkId, String mode, Double time, Map<String, Double> inhaledMass) {
        OnRoadTripExposureInfo tripExposureInfo = this.personInfo.get(personId, mode);
        if (tripExposureInfo==null) {
                tripExposureInfo = new OnRoadTripExposureInfo(personId, mode);
        }
        tripExposureInfo.addInhaledMass(time, linkId, inhaledMass);
        this.personInfo.put(personId, mode, tripExposureInfo);
    }

    public class OnRoadTripExposureInfo{
        private Id<Person> personId;
        private String mode;

        Table<Double,Id<Link>, Map<String,Double>> time2link2emissions = HashBasedTable.create();

        OnRoadTripExposureInfo(Id<Person> person, String mode) {
            this.personId = person;
            this.mode = mode;
        }

        void addInhaledMass(double time, Id<Link> linkId, Map<String, Double> inhaledMass){
            Map<String, Double> massSoFar = this.time2link2emissions.get(time, linkId);
            if (massSoFar == null) {
                this.time2link2emissions.put(time, linkId, inhaledMass);
            } else {
                this.time2link2emissions.put(time, linkId, massSoFar.entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(e -> e.getKey(),
                                                                            e -> e.getValue() + inhaledMass.get(e.getKey()))));


            }
        }
    }

    public void clear(){
        this.personInfo.clear();
    }

    //TODO :complete following methods.

    public Map<String, Double> getTotalInhaledMass(){
        throw new RuntimeException("not implemented yet.");
    }

    public Map<String, Map<String,Double>> getModeToInhaledMass(){
        throw new RuntimeException("not implemented yet.");
    }

    public Map<Id<Link>,Map<String,Double>> getLinkToInhaledMass(){
        throw new RuntimeException("not implemented yet.");
    }

    public Map<Double,Map<String,Double>> getTimeToInhaledMass(){
        throw new RuntimeException("not implemented yet.");
    }
}