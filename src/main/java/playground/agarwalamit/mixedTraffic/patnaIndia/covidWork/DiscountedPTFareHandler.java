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

package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;

import com.google.inject.Inject;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * The fare calculation is taken from KumarEtc2004PTCost.
 *
 * @author amit
 */

public class DiscountedPTFareHandler implements PersonDepartureEventHandler, TeleportationArrivalEventHandler{

    public static final Logger logger = Logger.getLogger(DiscountedPTFareHandler.class);

    private final Map<Id<Person>,String> person2mode = new HashMap<>();
    //peak is 7 to 10 and 15 to 18; in the form of 1 to 24.
    private final List<Integer> discountedHours = List.of(7,11,15,19); // i.e. 6 to 7, 10 to 11, 14-15 and 18 to 19
    private final double discount ; //10%
    private final double timebinsize = 3600.;

    DiscountedPTFareHandler(double discountOffPkHr){
        discount = discountOffPkHr;
        logger.info("Reducing the PT fare by "+discount+" factor for hours "+discountedHours);
    }

    DiscountedPTFareHandler(){
        this(0.1);
    }

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

        int timebin = (int)  Math.ceil(event.getTime()/timebinsize);
        if(timebin==0) timebin=1;

        if (discountedHours.contains(timebin)) {
           amount2pay = amount2pay*(1.-discount);
        }

        Event moneyEvent = new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount2pay, "ptFare","operator");
        events.processEvent(moneyEvent);
    }

    private double getPTFareFromDistance(final double distance) {
        double fareInRs = Double.NEGATIVE_INFINITY;
        if(distance < 4000) fareInRs = 0.01 * 300;
        else fareInRs = 0.01* ( 300 + (distance/1000 - 4)*31 );
        return fareInRs/PatnaUtils.INR_USD_RATE;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if( ! event.getLegMode().equals(TransportMode.pt) ) return;
        person2mode.put(event.getPersonId(), event.getLegMode());
    }
}
