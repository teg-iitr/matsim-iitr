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

package playground.amit.Dehradun;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;

import com.google.inject.Inject;

import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * The fare calculation is taken from Table 6-20 in Dehradun Metropolitan Area CMP.
 *
 * @author amit
 */

public class DMAPtFareEventHandler implements PersonDepartureEventHandler, TeleportationArrivalEventHandler{

    private final Map<Id<Person>,String> person2mode = new HashMap<>();
    private final List<String> ptModes = Arrays.asList(DehradunUtils.TravelModesBaseCase2017.bus.name(), DehradunUtils.TravelModesBaseCase2017.IPT.name());

    @Inject
    private EventsManager events;

    @Override
    public void reset(int iteration) {
        person2mode.clear();
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if( ! person2mode.containsKey(event.getPersonId()) ) return;

        double dist = event.getDistance();
        double fare = getPTFareFromDistance(dist);
        double amount2pay = -fare;

        Event moneyEvent = new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount2pay, "PT-Fare", "Operator", null);
        events.processEvent(moneyEvent);
    }

    private double getPTFareFromDistance(final double distance) {
        //y = 1.035x + 4.2734; using
        double fareInRs = 1.035 * (distance/ 1000.) + 4.2734;
        return fareInRs / PatnaUtils.INR_USD_RATE ;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if( ! ptModes.contains(event.getLegMode()) ) return;
        person2mode.put(event.getPersonId(), event.getLegMode());
    }
}
