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

package playground.shivam.linkDynamics.core;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import javax.validation.constraints.Positive;
import java.util.*;

public class LDConfigGroup extends ReflectiveConfigGroup {

    public LDConfigGroup() {
        super(GROUP_NAME);
        this.flowCapFactor1 = 1.0D;
        this.storageCapFactor1 = 1.0D;
        this.trafficDynamics1 = LDConfigGroup.TrafficDynamics.queue;
        this.linkDynamics1 = LDConfigGroup.LinkDynamics.FIFO;
        this.linkWidth1 = 30.0F;
        this.isRestrictingSeepage1 = true;
        this.useLanes1 = false;
        this.seepModes1 = List.of("bike");

        this.flowCapFactor2 = 1.0D;
        this.storageCapFactor2 = 1.0D;
        this.trafficDynamics2 = LDConfigGroup.TrafficDynamics.queue;
        this.linkDynamics2 = LDConfigGroup.LinkDynamics.FIFO;
        this.linkWidth2 = 30.0F;
        this.isRestrictingSeepage2 = true;
        this.useLanes2 = false;
        this.seepModes2 = List.of("bike");
    }

    private static final Logger log = Logger.getLogger(LDConfigGroup.class);

    public static final String GROUP_NAME = "linkDynamics";

    private static final String LINK_DYNAMICS_1 = "linkDynamics1";
    private static final String TRAFFIC_DYNAMICS_1 = "trafficDynamics1";
    private static final String FLOW_CAPACITY_FACTOR_1 = "flowCapacityFactor1";
    private static final String STORAGE_CAPACITY_FACTOR_1 = "storageCapacityFactor1";
    private static final String USE_LANES_1 = "useLanes1";
    private boolean useLanes1;
    private static final String SEEP_MODE_1 = "seepMode1";
    private Collection<String> seepModes1;
    private static final String IS_RESTRICTING_SEEPAGE_1 = "isRestrictingSeepage1";
    private boolean isRestrictingSeepage1;
    private float linkWidth1;
    public static final String LINK_WIDTH_1 = "linkWidth1";
    private LDConfigGroup.LinkDynamics linkDynamics1;
    private LDConfigGroup.TrafficDynamics trafficDynamics1;
    private boolean isSeepModeStorageFree1;
    private static final String IS_SEEP_MODE_STORAGE_FREE_1 = "isSeepModeStorageFree1";
    private LDConfigGroup.TrafficDynamicsCorrectionApproach trafficDynamicsCorrectionApproach1;

    private static final String LINK_DYNAMICS_2 = "linkDynamics2";
    private static final String TRAFFIC_DYNAMICS_2 = "trafficDynamics2";
    private static final String FLOW_CAPACITY_FACTOR_2 = "flowCapacityFactor2";
    private static final String STORAGE_CAPACITY_FACTOR_2 = "storageCapacityFactor2";
    private static final String USE_LANES_2 = "useLanes2";
    private boolean useLanes2;
    private static final String SEEP_MODE_2 = "seepMode2";
    private Collection<String> seepModes2;
    private static final String IS_RESTRICTING_SEEPAGE_2 = "isRestrictingSeepage2";
    private boolean isRestrictingSeepage2;
    private float linkWidth2;
    public static final String LINK_WIDTH_2 = "linkWidth2";
    private LDConfigGroup.LinkDynamics linkDynamics2;
    private LDConfigGroup.TrafficDynamics trafficDynamics2;
    private boolean isSeepModeStorageFree2;
    private static final String IS_SEEP_MODE_STORAGE_FREE_2 = "isSeepModeStorageFree2";
    private LDConfigGroup.TrafficDynamicsCorrectionApproach trafficDynamicsCorrectionApproach2;

    @Positive
    private double flowCapFactor1;
    @Positive
    private double storageCapFactor1;
    @StringSetter(FLOW_CAPACITY_FACTOR_1)
    public void setFlowCapFactor1(double flowCapFactor1) {
        this.flowCapFactor1 = flowCapFactor1;
    }
    @StringGetter(FLOW_CAPACITY_FACTOR_1)
    public double getFlowCapFactor1() {
        return this.flowCapFactor1;
    }
    @StringSetter(STORAGE_CAPACITY_FACTOR_1)
    public void setStorageCapFactor1(double storageCapFactor1) {
        this.storageCapFactor1 = storageCapFactor1;
    }
    @StringGetter(STORAGE_CAPACITY_FACTOR_1)
    public double getStorageCapFactor1() {
        return this.storageCapFactor1;
    }
    @StringSetter(TRAFFIC_DYNAMICS_1)
    public void setTrafficDynamics1(LDConfigGroup.TrafficDynamics str) {
        this.trafficDynamics1 = str;
    }
    @StringGetter(TRAFFIC_DYNAMICS_1)
    public LDConfigGroup.TrafficDynamics getTrafficDynamics1() {
        return this.trafficDynamics1;
    }
    @StringSetter(LINK_DYNAMICS_1)
    public void setLinkDynamics1(LDConfigGroup.LinkDynamics linkDynamics1) {
        this.linkDynamics1 = linkDynamics1;
    }
    @StringGetter(LINK_DYNAMICS_1)
    public LDConfigGroup.LinkDynamics getLinkDynamics1() {return this.linkDynamics1;}
    @StringSetter(LINK_WIDTH_1)
    public void setLinkWidthForVis1(float linkWidth1) {
        this.linkWidth1 = linkWidth1;
    }
    @StringGetter(LINK_WIDTH_1)
    public float getLinkWidthForVis1() {
        return this.linkWidth1;
    }
    @StringGetter(USE_LANES_1)
    public boolean isUseLanes1() {
        return this.useLanes1;
    }
    @StringSetter(USE_LANES_1)
    public void setUseLanes1(boolean useLanes1) {
        this.useLanes1 = useLanes1;
    }
    @StringGetter(SEEP_MODE_1)
    private String getSeepModesAsString1() {
        return CollectionUtils.setToString(new HashSet(this.getSeepModes1()));
    }
    @StringSetter(SEEP_MODE_1)
    private void setSeepModes1(String value) {
        this.setSeepModes1(Arrays.asList(value.split(",")));
    }
    public Collection<String> getSeepModes1() {
        return this.seepModes1;
    }
    public void setSeepModes1(Collection<String> seepModes1) {
        this.seepModes1 = seepModes1;
    }
    @StringGetter(IS_SEEP_MODE_STORAGE_FREE_1)
    public boolean isSeepModeStorageFree1() {
        return this.isSeepModeStorageFree1;
    }
    @StringSetter(IS_SEEP_MODE_STORAGE_FREE_1)
    public void setSeepModeStorageFree1(boolean isSeepModeStorageFree1) {this.isSeepModeStorageFree1 = isSeepModeStorageFree1;}
    @StringGetter(IS_RESTRICTING_SEEPAGE_1)
    public boolean isRestrictingSeepage1() {
        return this.isRestrictingSeepage1;
    }
    @StringSetter(IS_RESTRICTING_SEEPAGE_1)
    public void setRestrictingSeepage1(boolean isRestrictingSeepage1) {this.isRestrictingSeepage1 = isRestrictingSeepage1;}
    public LDConfigGroup.TrafficDynamicsCorrectionApproach getTrafficDynamicsCorrectionApproach1() {return this.trafficDynamicsCorrectionApproach1;}
    public void setTrafficDynamicsCorrectionApproach1(LDConfigGroup.TrafficDynamicsCorrectionApproach trafficDynamicsCorrectionApproach1) { this.trafficDynamicsCorrectionApproach1 = trafficDynamicsCorrectionApproach1;}



    @Positive
    private double flowCapFactor2;
    @Positive
    private double storageCapFactor2;
    @StringSetter(FLOW_CAPACITY_FACTOR_2)
    public void setFlowCapFactor2(double flowCapFactor2) {
        this.flowCapFactor2 = flowCapFactor2;
    }
    @StringGetter(FLOW_CAPACITY_FACTOR_2)
    public double getFlowCapFactor2() {
        return this.flowCapFactor2;
    }
    @StringSetter(STORAGE_CAPACITY_FACTOR_2)
    public void setStorageCapFactor2(double storageCapFactor2) {
        this.storageCapFactor2 = storageCapFactor2;
    }
    @StringGetter(STORAGE_CAPACITY_FACTOR_2)
    public double getStorageCapFactor2() {
        return this.storageCapFactor2;
    }
    @StringSetter(TRAFFIC_DYNAMICS_2)
    public void setTrafficDynamics2(LDConfigGroup.TrafficDynamics str) {
        this.trafficDynamics2 = str;
    }
    @StringGetter(TRAFFIC_DYNAMICS_2)
    public LDConfigGroup.TrafficDynamics getTrafficDynamics2() {
        return this.trafficDynamics2;
    }
    @StringSetter(LINK_DYNAMICS_2)
    public void setLinkDynamics2(LDConfigGroup.LinkDynamics linkDynamics2) {
        this.linkDynamics2 = linkDynamics2;
    }
    @StringGetter(LINK_DYNAMICS_2)
    public LDConfigGroup.LinkDynamics getLinkDynamics2() {return this.linkDynamics2;}
    @StringSetter(LINK_WIDTH_2)
    public void setLinkWidthForVis2(float linkWidth2) {
        this.linkWidth2 = linkWidth2;
    }
    @StringGetter(LINK_WIDTH_2)
    public float getLinkWidthForVis2() {
        return this.linkWidth2;
    }
    @StringGetter(USE_LANES_2)
    public boolean isUseLanes2() {return this.useLanes2;}
    @StringSetter(USE_LANES_2)
    public void setUseLanes2(boolean useLanes2) {this.useLanes2 = useLanes2;}
    @StringGetter(SEEP_MODE_2)
    private String getSeepModesAsString2() {return CollectionUtils.setToString(new HashSet(this.getSeepModes2()));}
    @StringSetter(SEEP_MODE_2)
    private void setSeepModes2(String value) {this.setSeepModes2(Arrays.asList(value.split(",")));}
    public Collection<String> getSeepModes2() {return this.seepModes2;}
    public void setSeepModes2(Collection<String> seepModes2) {this.seepModes2 = seepModes2;}
    @StringGetter(IS_SEEP_MODE_STORAGE_FREE_2)
    public boolean isSeepModeStorageFree2() {return this.isSeepModeStorageFree2;}
    @StringSetter(IS_SEEP_MODE_STORAGE_FREE_2)
    public void setSeepModeStorageFree2(boolean isSeepModeStorageFree2) {this.isSeepModeStorageFree2 = isSeepModeStorageFree2;}
    @StringGetter(IS_RESTRICTING_SEEPAGE_2)
    public boolean isRestrictingSeepage2() {return this.isRestrictingSeepage2; }
    @StringSetter(IS_RESTRICTING_SEEPAGE_2)
    public void setRestrictingSeepage2(boolean isRestrictingSeepage2) {this.isRestrictingSeepage2 = isRestrictingSeepage2;}
    public LDConfigGroup.TrafficDynamicsCorrectionApproach getTrafficDynamicsCorrectionApproach2() {return this.trafficDynamicsCorrectionApproach2;}
    public void setTrafficDynamicsCorrectionApproach2(LDConfigGroup.TrafficDynamicsCorrectionApproach trafficDynamicsCorrectionApproach2) { this.trafficDynamicsCorrectionApproach2 = trafficDynamicsCorrectionApproach2;}

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        StringBuilder stb = new StringBuilder();
        stb = new StringBuilder(60);
        LDConfigGroup.TrafficDynamics[] trafficDynamics = LDConfigGroup.TrafficDynamics.values();
        for (LDConfigGroup.TrafficDynamics td : trafficDynamics) {
            stb.append(td).append(",");
        }
        map.put(TRAFFIC_DYNAMICS_1, "options: " + stb);
        map.put(TRAFFIC_DYNAMICS_2, "options: " + stb);
        stb = new StringBuilder();
        LDConfigGroup.LinkDynamics[] linkDynamics = LDConfigGroup.LinkDynamics.values();
        for(LDConfigGroup.LinkDynamics ld: linkDynamics) {
            stb.append(ld).append(",");
        }
        map.put(LINK_DYNAMICS_1, "default: FIFO; options:" + stb);
        map.put(LINK_DYNAMICS_2, "default: FIFO; options:" + stb);

        map.put(USE_LANES_1, "Set this parameter to true if lanes should be used, false if not.");
        map.put(IS_SEEP_MODE_STORAGE_FREE_1, "If link dynamics is set as " + LDConfigGroup.LinkDynamics.SeepageQ + ", set to true if seep mode do not consumes any space on the link. Default is false.");
        map.put(IS_RESTRICTING_SEEPAGE_1, "If link dynamics is set as " + LDConfigGroup.LinkDynamics.SeepageQ + ", set to false if all seep modes should perform seepage. Default is true (better option).");
        map.put(LINK_WIDTH_1, "The (initial) width of the links of the network. Use positive floating point values. This is used only for visualisation.");

        map.put(USE_LANES_2, "Set this parameter to true if lanes should be used, false if not.");
        map.put(IS_SEEP_MODE_STORAGE_FREE_2, "If link dynamics is set as " + LDConfigGroup.LinkDynamics.SeepageQ + ", set to true if seep mode do not consumes any space on the link. Default is false.");
        map.put(IS_RESTRICTING_SEEPAGE_2, "If link dynamics is set as " + LDConfigGroup.LinkDynamics.SeepageQ + ", set to false if all seep modes should perform seepage. Default is true (better option).");
        map.put(LINK_WIDTH_2, "The (initial) width of the links of the network. Use positive floating point values. This is used only for visualisation.");

        return map;
    }

    public static enum LinkDynamics {
        FIFO,
        PassingQ,
        SeepageQ;

        private LinkDynamics() {
        }
    }

    public static enum TrafficDynamics {
        queue,
        withHoles,
        kinematicWaves;

        private TrafficDynamics() {
        }
    }

    public static enum TrafficDynamicsCorrectionApproach {
        REDUCE_FLOW_CAPACITY,
        INCREASE_NUMBER_OF_LANES;

        private TrafficDynamicsCorrectionApproach() {
        }
    }
}