/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkColdEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.amit.analysis.emission;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author benjamin
 * 
 */
public class EmissionsPerLinkColdEventHandler implements
		ColdEmissionEventHandler {
	private static final Logger logger = LogManager
			.getLogger(EmissionsPerLinkColdEventHandler.class);

	Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> time2coldEmissionsTotal = new HashMap<>();

	final int noOfTimeBins;
	final double timeBinSize;

	public EmissionsPerLinkColdEventHandler(double simulationEndTime,
			int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	@Override
	public void reset(int iteration) {
		this.time2coldEmissionsTotal.clear();
		logger.info("Resetting cold emission aggregation to "
				+ this.time2coldEmissionsTotal);
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Double time = event.getTime();
		Id<Link> linkId = event.getLinkId();
		Map<Pollutant, Double> coldEmissionsOfEvent = event
				.getColdEmissions();
		double endOfTimeInterval = 0.0;

		int numberOfInterval = (int) Math.ceil(time / timeBinSize);
		if (numberOfInterval == 0)
			numberOfInterval = 1; // only happens if time = 0.0
		endOfTimeInterval = numberOfInterval * timeBinSize;

		Map<Id<Link>, Map<Pollutant, Double>> coldEmissionsTotal = new HashMap<>();

		if (endOfTimeInterval < this.noOfTimeBins * this.timeBinSize+1) {
			if (this.time2coldEmissionsTotal.get(endOfTimeInterval) != null) {
				coldEmissionsTotal = this.time2coldEmissionsTotal
						.get(endOfTimeInterval);

				if (coldEmissionsTotal.get(linkId) != null) {
					Map<Pollutant, Double> coldEmissionsSoFar = coldEmissionsTotal
							.get(linkId);
					for (Entry<Pollutant, Double> entry : coldEmissionsOfEvent
							.entrySet()) {
						Pollutant pollutant = entry.getKey();
						Double eventValue = entry.getValue();

						Double previousValue = coldEmissionsSoFar
								.get(pollutant);
						Double newValue = previousValue + eventValue;
						coldEmissionsSoFar.put(pollutant, newValue);
					}
					coldEmissionsTotal.put(linkId, coldEmissionsSoFar);
				} else {
					coldEmissionsTotal.put(linkId, coldEmissionsOfEvent);
				}
			} else {
				coldEmissionsTotal.put(linkId, coldEmissionsOfEvent);
			}
			this.time2coldEmissionsTotal.put(endOfTimeInterval,
					coldEmissionsTotal);
		}
	}

	public Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> getColdEmissionsPerLinkAndTimeInterval() {
		return time2coldEmissionsTotal;
	}

}
