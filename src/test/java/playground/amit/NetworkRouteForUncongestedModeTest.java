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
package playground.amit;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author amit
 */

public class NetworkRouteForUncongestedModeTest {

	@RegisterExtension
	public MatsimTestUtils helper = new MatsimTestUtils();

	private final static URL EQUIL_NETWORK = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml");

	/**
	 * Every link must allow car and ride mode if networkModes are car and ride. 
	 * Using overriding modules to get network route for ride mode.  
	 */
	@Test
	public void testWithAllowedModesOnLink(){

		Scenario sc = createSceanrio();
		sc.getConfig().controller().setOutputDirectory(helper.getOutputDirectory());
		sc.getConfig().routing().removeModeRoutingParams("ride");
		// set allowed modes on each link
		for (Link l : sc.getNetwork().getLinks().values()) {
			l.setAllowedModes(new HashSet<>(Arrays.asList("car","ride")));
		}
		
		Controler controler = new Controler(sc);
		controler.getConfig().controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		
		//overriding module to get network route for ride mode
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {// same must be assigned for non-car main modes.
				addTravelTimeBinding("ride").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
			}
		});
		controler.run();
		Assertions.assertTrue(true);
	}

	private Scenario createSceanrio () {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(EQUIL_NETWORK.toString());
		config.controller().setLastIteration(1);

		{
			ActivityParams ap = new ActivityParams();
			ap.setActivityType("h");
			ap.setTypicalDuration(12*3600);
			config.scoring().addActivityParams(ap);
		}
		{
			ActivityParams ap = new ActivityParams();
			ap.setActivityType("w");
			ap.setTypicalDuration(8*3600);
			config.scoring().addActivityParams(ap);
		}
		config.qsim().setMainModes(Arrays.asList("car"));
		config.routing().setNetworkModes(Arrays.asList("car","ride"));

		{
			StrategySettings reRoute = new StrategySettings();
			reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
			reRoute.setWeight(0.2);
			config.replanning().addStrategySettings(reRoute);

			StrategySettings changeExpBetaStrategySettings = new StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setWeight(0.8);
			config.replanning().addStrategySettings(changeExpBetaStrategySettings);
		}

		Scenario sc = ScenarioUtils.loadScenario(config);
		//create plans with car and ride mode
		for ( int ii = 1; ii<5; ii++) {
			Id<Person> personId = Id.createPersonId(ii);
			Person p = sc.getPopulation().getFactory().createPerson(personId);
			Plan plan = sc.getPopulation().getFactory().createPlan();
			p.addPlan(plan);

			Activity home = sc.getPopulation().getFactory().createActivityFromLinkId("h", Id.createLinkId("1"));
			home.setEndTime(6 *3600);
			Leg leg ;

			if ( ii%2==0 ) leg = sc.getPopulation().getFactory().createLeg("car");
			else leg = sc.getPopulation().getFactory().createLeg("ride");

			plan.addActivity(home);
			plan.addLeg(leg);

			Activity work = sc.getPopulation().getFactory().createActivityFromLinkId("w", Id.createLinkId("20"));
			work.setEndTime(16*3600);

			plan.addActivity(work);

			sc.getPopulation().addPerson(p);
		}
		return sc;
	}
}