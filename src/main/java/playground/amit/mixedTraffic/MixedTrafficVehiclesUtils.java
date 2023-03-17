/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.amit.mixedTraffic;

import org.matsim.api.core.v01.TransportMode;

/**
 * @author amit, shivam
 */
public class MixedTrafficVehiclesUtils {

    /**
     * @param travelMode for which PCU value is required
     */
    public static double getPCU(final String travelMode) {
        double pcu;
        switch (travelMode) {
            case TransportMode.car:
                pcu = 1.0;
                break;
            case "bicycle":
            case TransportMode.bike:
                pcu = 0.25;
                break;
            case "motorbike":
                pcu = 0.25;
                break;
            case TransportMode.walk:
                pcu = 0.10;
                break;
            case "lcv":
                pcu = 0.75;
                break;
            case "bus":
                pcu = 3;
                break;
            case "cart":
                pcu = 0.15;
                break;
            case TransportMode.pt:
                pcu = 20./3.6;
                break;
            case "truck":
                pcu = 3.0;
                break;
            case "auto":
                pcu = 3.0;
                break;
            case "cycle":
                pcu = 0.5;
                break;
            default:
                throw new RuntimeException("No PCU is set for travel mode " + travelMode + ".");
        }
        return pcu;
    }

    /**
     * @param travelMode for which speed is required
     */
    public static double getSpeed(final String travelMode) {
        double speed;
        switch (travelMode) {
            case TransportMode.car:
                speed = 16.67;
                break;
            case "bicycle":
            case TransportMode.bike:
                speed = 4.17;
                break;
            case "motorbike":
                speed = 16.67;
                break;
            case TransportMode.walk:
                speed = 1.2;
                break;
            case "lcv":
                speed = 15;
                break;
            case "cart":
                speed = 2;
                break;
            case TransportMode.pt:
                speed = 20;
                break;
            case "truck":
                speed = 8.33;
                break;
            case "bus":
                speed = 10;
                break;
            case "auto":
                speed = 8.33;
                break;
            default:
                throw new RuntimeException("No speed is set for travel mode " + travelMode + ".");
        }
        return speed;
    }

    /**
     * @param travelMode for which effective cell size is required
     * @return physical road space occupied based on PCU unit
     * default is cell size for car (7.5 m)
     */
    public static double getCellSize(final String travelMode) {
        double matsimCellSize = 7.5;
        return matsimCellSize * getPCU(travelMode);
    }

    public static double getLength(final String travelMode) {
        switch (travelMode) {
            case TransportMode.car:
                return 3.72;
            case "bicycle":
            case TransportMode.bike:
            case "motorbike":
                return 1.9;
            case "lcv":
                return 3.5;
            case "cart":
                return 2.3;
            case "truck":
                return 7.5;
            case "bus":
                return 7.5;
            case "auto":
                return 2.5;
            case TransportMode.pt:
                return 10;

        }
        throw new RuntimeException("Length fot " + travelMode + " is not found.");
    }

    public static double getStuckTime(final String travelMode) {
        double stuckTime;
        switch (travelMode) {
            case TransportMode.car:
            case "motorbike":
                stuckTime = 60.;
                break;
            case "bicycle":
            case TransportMode.bike:
                stuckTime = 15.;
                break;
            case TransportMode.walk:
                stuckTime = 6.;
                break;
            case "lcv":
                stuckTime = 50;
                break;
            case "cart":
                stuckTime = 10;
                break;
            case TransportMode.pt:
                stuckTime = 360;
                break;
            case "truck":
                stuckTime = 180.;
                break;
            case "bus":
                stuckTime = 150;
                break;
            case "auto":
                stuckTime = 40;
                break;
            default:
                throw new RuntimeException("No stuckTime is set for travel mode " + travelMode + ".");
        }
        return stuckTime;
    }
}
