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
package playground.amit.utils.plans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.utils.FileUtils;
import playground.amit.utils.LoadMyScenarios;

/**
 * Take input plans and write selected plans from choice set of all person in a new file.
 * @author amit
 */
public class SelectedPlansFilter {

	private static final Logger LOG = LogManager.getLogger(SelectedPlansFilter.class);

	private final static String RUN_DIR = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/";
	private final static String INPUT_PLANS = RUN_DIR + "/population_1pct_v2_baseCaseOutputPlans.xml.gz";
	private final static String OUTPUT_PLANS = RUN_DIR + "/population_1pct_v2_baseCaseSelectedPlans.xml.gz";
	
	private Scenario scOut;

	public void run (final String inputPlans){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOut = scOut.getPopulation();
		
		for (Person p :sc.getPopulation().getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();
			PopulationFactory factory = popOut.getFactory();
			Person newP = factory.createPerson(p.getId());
			popOut.addPerson(newP);
			Plan newPlan = factory.createPlan();
			PopulationUtils.copyFromTo(selectedPlan, newPlan);
			newP.addPlan(newPlan);
		}
	}
	
	public Population getPopulation(){
		return scOut.getPopulation();
	}
	
	public void writePlans(final String outputPlans){
		new PopulationWriter(scOut.getPopulation()).write(outputPlans);
		LOG.info("Writing selected plans only successful.");		
	}

	public static void main(String[] args) {
		SelectedPlansFilter spf = new SelectedPlansFilter();
		spf.run(INPUT_PLANS);
		spf.writePlans(OUTPUT_PLANS);
	}
}
