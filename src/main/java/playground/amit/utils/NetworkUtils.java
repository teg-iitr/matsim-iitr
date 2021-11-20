/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.amit.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Created by amit on 24/11/2016.
 */


public class NetworkUtils {

    public static void removeIsolatedNodes(final Network network ){
        List<Node> nodes2remove = network.getNodes()
                                         .values()
                                         .stream()
                                         .filter(n -> n.getInLinks().isEmpty() && n.getOutLinks().isEmpty())
                                         .collect(Collectors.toList());

        for(Node n : nodes2remove) {network.removeNode(n.getId());}
    }

    public static void copy(Link from, Link to) {
        to.setCapacity(from.getCapacity());
        to.setNumberOfLanes(from.getNumberOfLanes());
        to.setAllowedModes(from.getAllowedModes());
        to.setFreespeed(from.getFreespeed());
        to.setLength(from.getLength());
    }

    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c;
    }
}
