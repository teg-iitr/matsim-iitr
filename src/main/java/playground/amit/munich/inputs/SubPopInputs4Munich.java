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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import playground.amit.munich.utils.MunichPersonFilter;
import playground.amit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.amit.utils.LoadMyScenarios;

import java.util.Collection;
import java.util.List;

/**
 * @author amit
 */

public class SubPopInputs4Munich {

	final MunichPersonFilter pf = new MunichPersonFilter();
	private final String subPopAttributeName = "userGroup";
	private final String outPopAttributeFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/personsAttributes_1pct_usrGrp.xml.gz";

	public static void main(String[] args) {
		SubPopInputs4Munich inputs = new SubPopInputs4Munich();
		inputs.writePersonAttributes();
		inputs.modifyConfig();
	}

	private void writePersonAttributes(){

		// read plans with subActivities (basically these are inital plans from different sources + subActivities)
		String initialPlans = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities.xml.gz";
		String outPlansFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities_usrGrp.xml.gz";

		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(initialPlans);
		Population pop = sc.getPopulation();	

		for(Person p : pop.getPersons().values()){
			String ug = pf.getUserGroupAsStringFromPersonId(p.getId());
			p.getAttributes().putAttribute(subPopAttributeName, ug);

			//pt of commuter and rev_commuter need to be replaced by some other mode.
			if(pf.isPersonInnCommuter(p.getId()) || pf.isPersonOutCommuter(p.getId())){
				List<PlanElement> pes = p.getSelectedPlan().getPlanElements(); // only one plan each person in initial plans
				for(PlanElement pe : pes){
					if(pe instanceof Leg){
						if( ((Leg)pe).getMode().equals(TransportMode.pt) ){
							((Leg)pe).setMode(TransportMode.pt.concat("_").concat(ug));
						}
					}
				}
			}
		}

		new PopulationWriter(pop).write(outPlansFile);
	}

	private void modifyConfig(){

		// I think, config with all sub activities info can be taken.
		String existingConfig = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/config_wrappedSubActivities_baseCase.xml"; 
		String outConfigFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/config_wrappedSubActivities_usrGrp_baseCase.xml"; // need manual verification later

		Config config =  ConfigUtils.loadConfig(existingConfig);

//		config.plans().setSubpopulationAttributeName(subPopAttributeName); // if this is set then, one have to set same strategy for all sub pops.
		config.plans().setInputPersonAttributeFile(outPopAttributeFile); // this should be included in the population input file. theresa, aug'19

		// get the existing strategies and add others user grp to it.
		Collection<StrategySettings> strategySettings = config.replanning().getStrategySettings();

		for(StrategySettings ss : strategySettings){
			ss.setSubpopulation(MunichUserGroup.Urban.toString());
		}
		{
			// once subPop attribute is set, strategy for all sub pop groups neet to set seprately.
			String ug = MunichUserGroup.Rev_Commuter.toString();
			StrategySettings reroute = new StrategySettings();
			reroute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
			reroute.setSubpopulation(ug);
			reroute.setWeight(0.15);
			config.replanning().addStrategySettings(reroute);

			StrategySettings expBeta = new StrategySettings();
			expBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
			expBeta.setSubpopulation(ug);
			expBeta.setWeight(0.7);
			config.replanning().addStrategySettings(expBeta);

			StrategySettings modeChoiceComm = new StrategySettings();
			modeChoiceComm.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.concat("_").concat(ug));
			modeChoiceComm.setWeight(0.15);
			modeChoiceComm.setSubpopulation(ug);
			config.replanning().addStrategySettings(modeChoiceComm);

			// first use existing pt mode parameters and set them as new pt mode parameters
			ModeParams ptParams = config.scoring().getModes().get(TransportMode.pt);

			config.scoring().getOrCreateModeParams("pt_".concat(ug)).setConstant(-0.3);
			config.scoring().getOrCreateModeParams("pt_".concat(ug)).setMarginalUtilityOfDistance(ptParams.getMarginalUtilityOfDistance());
			config.scoring().getOrCreateModeParams("pt_".concat(ug)).setMarginalUtilityOfTraveling(ptParams.getMarginalUtilityOfTraveling());
			config.scoring().getOrCreateModeParams("pt_".concat(ug)).setMonetaryDistanceRate(ptParams.getMonetaryDistanceRate());

			// teleportation speeds for different pts
			config.routing().getOrCreateModeRoutingParams("pt_".concat(ug)).setTeleportedModeSpeed(50.0/3.6);
		}
		{
			String ug = MunichUserGroup.Freight.toString();
			StrategySettings reroute = new StrategySettings();
			reroute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
			reroute.setSubpopulation(ug);
			reroute.setWeight(0.30);
			config.replanning().addStrategySettings(reroute);

			StrategySettings expBeta = new StrategySettings();
			expBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
			expBeta.setSubpopulation(ug);
			expBeta.setWeight(0.70);
			config.replanning().addStrategySettings(expBeta);
		}

		config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
		new ConfigWriter(config).write(outConfigFile);
	}
}
