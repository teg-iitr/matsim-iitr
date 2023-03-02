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

package playground.shivam.trafficChar.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;

import java.util.*;

public class TrafficCharConfigGroup extends ReflectiveConfigGroup {

    public TrafficCharConfigGroup() {
        super(GROUP_NAME);
    }
    public static TrafficCharConfigGroup get(Config config) {
        return (TrafficCharConfigGroup) config.getModules().get(TrafficCharConfigGroup.GROUP_NAME);
    }

    private static final Logger log = LogManager.getLogger(TrafficCharConfigGroup.class);

    public static final String GROUP_NAME = "trafficChar";
    public static final String ROAD_TYPE = "roadType";
    public static final String ROAD_TYPE_DEFAULT = "default";

    private final Map<String, QSimConfigGroup> roadType2TrafficChar = new HashMap<>();

    public String getRoadType(QSimConfigGroup qSimConfigGroup) {
        for (var entry : this.roadType2TrafficChar.entrySet()) {
            if (Objects.equals(qSimConfigGroup, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void setRoadType(String oldRoadType, String newRoadType) {
        this.roadType2TrafficChar.put(newRoadType, this.roadType2TrafficChar.get(oldRoadType));
    }

//    private String roadType;
//
//    private QSimConfigGroup qSimConfigGroup;

    public void setQSimConfigGroup(String roadType, QSimConfigGroup qSimConfigGroup) {
        this.roadType2TrafficChar.replace(roadType, qSimConfigGroup);
    }

    public QSimConfigGroup getQSimConfigGroup(String roadType) {
        return this.roadType2TrafficChar.get(roadType);
    }

    public void addQSimConfigGroup(String roadType, QSimConfigGroup qSimConfigGroup){
        this.roadType2TrafficChar.put(roadType, qSimConfigGroup);
        super.addParameterSet(super.createParameterSet(roadType));
    }

    public Map<String, QSimConfigGroup> getRoadType2TrafficChar() {
        return Collections.unmodifiableMap(roadType2TrafficChar);
    }

}