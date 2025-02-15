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
package playground.amit.munich.inputs;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import playground.amit.utils.LoadMyScenarios;

import java.util.SortedMap;

/**
 * @author amit
 */
public class ReadAndAddSubActivities {

	public ReadAndAddSubActivities(String inputConfig, Scenario sc) {
		this.inputConfig = inputConfig;
		this.sc = sc;
	}

	private final String inputConfig;
	private final Scenario sc;
	
	public static void main(String[] args) {
		
		String initialPlans = "../../../../repos/shared-svn/projects/detailedEval/pop/merged/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
		String initialConfig = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/input/config_munich_1pct_baseCase_fromBK_modified.xml";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndConfig(initialPlans,initialConfig);
		
		String outConfig = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/config_wrappedSubActivities_baseCase.xml";
		String outPlans = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities.xml.gz";
		
		ReadAndAddSubActivities add2Config =  new ReadAndAddSubActivities(initialConfig,sc);
		add2Config.addDataAndWriteConfig(outConfig,outPlans);
	}
	
	private void addDataAndWriteConfig(String outConfig, String outPlans){
		Config config = ConfigUtils.loadConfig(inputConfig);
		
		ActivityClassifier newPlans= new ActivityClassifier(sc);
		newPlans.run();
		newPlans.writePlans(outPlans);
		SortedMap<String, Double> acts = newPlans.getActivityType2TypicalDuration();
		
		for(String act :acts.keySet()){
			ActivityParams params = new ActivityParams();
			
			params.setActivityType(act);
			params.setTypicalDuration(acts.get(act));
			// setting minimal duration does not have any effect in absence of marginalUtilityForEarlyDeparture
			params.setClosingTime(Double.NEGATIVE_INFINITY);
			params.setEarliestEndTime(Double.NEGATIVE_INFINITY);
			params.setLatestStartTime(Double.NEGATIVE_INFINITY);
			params.setOpeningTime(Double.NEGATIVE_INFINITY);
			config.scoring().addActivityParams(params);
		}
		
		config.controller().setOutputDirectory(null);
		config.plans().setInputFile(outPlans);
		config.vspExperimental().setWritingOutputEvents(true);
		
		new ConfigWriter(config).write(outConfig);
	}
}