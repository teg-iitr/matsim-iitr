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
import org.matsim.contrib.emissions.Pollutant;

import com.google.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by amit on 08.11.17.
 */

public class OnRoadExposureCalculator {

    private static final Logger LOGGER = LogManager.getLogger(OnRoadExposureCalculator.class);

    private final OnRoadExposureConfigGroup config;

    @Inject
    public OnRoadExposureCalculator(OnRoadExposureConfigGroup onRoadExposureConfigGroup) {
        this.config = onRoadExposureConfigGroup;
    }

    /**
     * @param mode
     * @param emissionRate
     * @param travelTime
     * @return
     */
    public Map<Pollutant, Double> calculate(String mode, Map<Pollutant, Double> emissionRate, double travelTime) {
        if (mode==null){
            LOGGER.warn("Mode is null. Using default values of car mode.");
        }
        return emissionRate.entrySet()
                           .stream()
                           .collect(Collectors.toMap(Map.Entry::getKey,
                                   e -> calculateForSinglePollutant(e.getKey(), e.getValue(), mode, travelTime)));
    }

    /**
     * total inhalation in gm = (b * o * r * p * t + e  * o * r * p / d)
     * <p>b --> background concentration</p>
     * <p>e --> emissions in g/m for time bin T</p>
     * <p>d --> dispersion rate</p>
     * <p>o --> occupancy rate</p>
     * <p>r --> breathing rate</p>
     * <p>p --> penetration rate</p>
     * <p>t --> travelTime</p>
     */
    private double calculateForSinglePollutant(Pollutant pollutant, double pollutantValue, String mode, double travelTime) {
        if (config.getPollutantToBackgroundConcentration_gm().get(pollutant) == null) {
        	LOGGER.warn("No background concentration for " + pollutant);
        	return 0.;
        } else {
        	double val = (config.getPollutantToBackgroundConcentration_gm().get(pollutant)
                    * config.getModeToOccupancy().get(mode)
                    * config.getModeToBreathingRate().get(mode)
                    * config.getPollutantToPenetrationRate(mode).get(pollutant)
                    * travelTime)
                    + (pollutantValue / config.getDispersionRate()
                    * config.getModeToOccupancy().get(mode)
                    * config.getModeToBreathingRate().get(mode)
                    * config.getPollutantToPenetrationRate(mode).get(pollutant));

            if (this.config.isUsingMicroGramUnits()) {
                return val * Math.pow(10,6);
            } else {
                return val;
            }
        }

    }

}
