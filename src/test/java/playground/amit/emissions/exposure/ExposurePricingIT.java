/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
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
package playground.amit.emissions.exposure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.amit.analysis.emission.AirPollutionExposureAnalysisControlerListener;
import playground.amit.analysis.emission.experienced.ExperiencedEmissionCostHandler;
import playground.amit.munich.utils.MunichPersonFilter;
import playground.amit.utils.PersonFilter;
import playground.vsp.airPollution.exposure.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * @author julia, benjamin, amit
 *
 */

public class ExposurePricingIT {
	private Logger logger = LogManager.getLogger(ExposurePricingIT.class);

	private Double xMin = 0.0;

	private Double xMax = 20000.;

	private Double yMin = 0.0;

	private Double yMax = 12500.;

	private Integer noOfXCells = 32;

	private Integer noOfYCells = 20;

	@RegisterExtension
	public MatsimTestUtils helper = new MatsimTestUtils(); 

	private DecimalFormat df = new DecimalFormat("#.###");

	private boolean isConsideringCO2Costs;

	private int noOfTimeBins;

	public ExposurePricingIT(boolean isConsideringCO2Costs, int noOfTimeBins) {
		this.isConsideringCO2Costs = isConsideringCO2Costs;
		this.noOfTimeBins = noOfTimeBins;
	}

	@ParameterizedTest
//			(name = "{index}: considerCO2 == {0}; noOfTimeBins == {1}")
	@ValueSource(booleans = {false, true})
	public static Collection<Object[]> considerCO2 () {
		Object[][] object = new Object [][] { 
				{true, 24},
				{false, 24},
				{true, 1},
				{false, 1}
		};
		return Arrays.asList(object);
	}

	@Disabled
	@Test
	public void noPricingTest () {
		logger.info("isConsideringCO2Costs = "+ this.isConsideringCO2Costs);
		logger.info("Number of time bins are "+ this.noOfTimeBins);
		Scenario sc = minimalControlerSetting(isConsideringCO2Costs);
		
//		sc.getConfig().routing().setInsertingAccessEgressWalk(true);

		String outputDirectory = helper.getOutputDirectory() + "/" + (isConsideringCO2Costs ? "considerCO2Costs" : "notConsiderCO2Costs") + "_"+this.noOfTimeBins+"_timeBins/";
		sc.getConfig().controller().setOutputDirectory(outputDirectory);

		sc.getConfig().controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);

		Controler controler = new Controler(sc);

		/* 
		 *******************************************************************************************
		 * first check the route without any pricing which should be shortest i.e. route with link 39.
		 ********************************************************************************************
		 */
		controler.run();

		Person activeAgent = controler.getScenario().getPopulation().getPersons().get(Id.create("567417.1#12424", Person.class));
		Plan selectedPlan = activeAgent.getSelectedPlan();

		// check with the first leg
		NetworkRoute route = (NetworkRoute) ( (Leg) selectedPlan.getPlanElements().get(3) ).getRoute() ;
		// Agent should take longer route to avoid exposure toll
		Assertions.assertTrue(route.getLinkIds().contains(Id.create("39", Link.class)),"Wrong route is selected. Agent should have used route with shorter link (i.e. 39) instead.");
	}

	@Disabled
	@Test
	public void pricingExposure_ReRouteTest() {
		logger.info("isConsideringCO2Costs = "+ this.isConsideringCO2Costs);
		logger.info("Number of time bins are "+ this.noOfTimeBins);

		Scenario sc = minimalControlerSetting(isConsideringCO2Costs);
		ScenarioUtils.loadScenario(sc); // need to load vehicles. Amit Sep 2016

//		sc.getConfig().routing().setInsertingAccessEgressWalk(true);
		sc.getConfig().controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);

		String outputDirectory = helper.getOutputDirectory() + "/" + (isConsideringCO2Costs ? "considerCO2Costs" : "notConsiderCO2Costs") + "_"+this.noOfTimeBins+"_timeBins/";
		sc.getConfig().controller().setOutputDirectory(outputDirectory);

		sc.getConfig().controller().setLastIteration(1);
		addReRoutingStrategy(sc);

		Controler controler = new Controler(sc);
		/* 
		 *******************************************************************************************
		 * Now price for exposure and check the route which should be longer i.e. route with link 38.
		 ********************************************************************************************
		 */

		GridTools gt = new GridTools(sc.getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
		Double timeBinSize = controler.getScenario().getConfig().qsim().getEndTime().seconds() / this.noOfTimeBins ;

		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);


		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(
        	new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, sc.getConfig())
				);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(GridTools.class).toInstance(gt);
				bind(ResponsibilityGridTools.class).toInstance(rgt);
				bind(EmissionModule.class).asEagerSingleton();
				bind(EmissionResponsibilityCostModule.class).asEagerSingleton();
				addControlerListenerBinding().to(InternalizeEmissionResponsibilityControlerListener.class);

				bindCarTravelDisutilityFactory().toInstance(emfac);
				bind(ExperiencedEmissionCostHandler.class);
				bind(PersonFilter.class).to(MunichPersonFilter.class);
			}
		});

		MyPersonMoneyEventHandler personMoneyEventHandler = new MyPersonMoneyEventHandler();
		controler.getEvents().addHandler(personMoneyEventHandler);

		controler.run();

		Person activeAgent = sc.getPopulation().getPersons().get(Id.create("567417.1#12424", Person.class));
		Plan selectedPlan = activeAgent.getSelectedPlan();

		// check with the first leg
		NetworkRoute route = (NetworkRoute) ( (Leg) selectedPlan.getPlanElements().get(3) ).getRoute() ;
		// Agent should take longer route to avoid exposure toll
		Assertions.assertTrue(route.getLinkIds().contains(Id.create("38", Link.class)),"Wrong route is selected. Agent should have used route with link 38 (longer) instead.");
	}
	
	@Disabled
	@Test
	public void pricingExposure_TollTest() {
		logger.info("isConsideringCO2Costs = "+ this.isConsideringCO2Costs);
		logger.info("Number of time bins are "+ this.noOfTimeBins);

		Scenario sc = minimalControlerSetting(isConsideringCO2Costs);
		ScenarioUtils.loadScenario(sc); // need to load vehicles. Amit Sep 2016
		
//		sc.getConfig().routing().setInsertingAccessEgressWalk(false);
		// yy otherwise, the scenario consumes walk time from activity to link, somewhat modifying the results. kai, jun'16

		String outputDirectory = helper.getOutputDirectory() + "/" + (isConsideringCO2Costs ? "considerCO2Costs" : "notConsiderCO2Costs") + "_"+this.noOfTimeBins+"_timeBins/";
		sc.getConfig().controller().setOutputDirectory(outputDirectory);

		sc.getConfig().controller().setLastIteration(1);
		sc.getConfig().controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);

		Controler controler = new Controler(sc);
		/* 
		 *******************************************************************************************
		 * Now price for exposure but without re-routing and 
		 * check the route which should be shorter i.e. route with link 39.
		 ********************************************************************************************
		 */

		GridTools gt = new GridTools(sc.getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
		Double timeBinSize = controler.getScenario().getConfig().qsim().getEndTime().seconds() / this.noOfTimeBins ;

		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
//		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, sc.getConfig().scoring());
        final playground.vsp.airPollution.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new playground.vsp.airPollution.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory(
                new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig())
		);
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(GridTools.class).toInstance(gt);
				bind(ResponsibilityGridTools.class).toInstance(rgt);
				bind(EmissionModule.class).asEagerSingleton();
				bind(EmissionResponsibilityCostModule.class).asEagerSingleton();
				addControlerListenerBinding().to(InternalizeEmissionResponsibilityControlerListener.class);

				bindCarTravelDisutilityFactory().toInstance(emfac);

				bind(ExperiencedEmissionCostHandler.class);
				addControlerListenerBinding().to(AirPollutionExposureAnalysisControlerListener.class);
			}
		});

		MyPersonMoneyEventHandler personMoneyEventHandler = new MyPersonMoneyEventHandler();
		controler.getEvents().addHandler(personMoneyEventHandler);

		controler.run();

		Assertions.assertTrue(personMoneyEventHandler.events.size()>0, "Money events are not thrown,check if emission costs are internlized.");

		if (noOfTimeBins == 1) {
			/*
			 * Manual calculation:
			 * 20 passive agents and 1 active agent will be exposed due to emission of 1 active agent on the link 39.
			 * All passive agent perform home activity for 86399 whereas active agent perform 6*3600+86400-51148 (=56852) home activity 
			 * and 51000-22107 (=28893) work activity. 	Total cells = 32*20 = 640.
			 * Emission costs (only warm emission since cold emission exists only upto 2 km on departure link) on link 39 = 0.027613043 
			 * without considering co2 costs (see EquilEmissionTest.class)
			 * cell of Link 39 = 13,14; Thus only cells with 9 < x < 17 and 10 < y < 18 matters to get the dispersion weight.
			 * 15,13; 15,12; 15,11; 14,13; 14,12; 14,11; 13,13; 13,12; 13,11; 12,13; 12,12; 12,11; 11,13; 11,12; 11,11; 
			 * noOfCells with zero distance (13,14;) = 0; noOfCells with 1 distance (13,13;) = 1;
			 * noOfCells with 2 distance (13,12; 12,13; 14,13 ) = 3; noOfCells with 3 distance (13,11; 12,12; 11,13; 14,12; 15,13) = 5;
			 * avg Exposed duration = (20*86399+(56852+28893))/640 = 2833.945313; 
			 * Thus total relevant duration = (0.132+3*0.029+5*0.002)*86399 = 19785.371
			 * Thus total toll in iteration 1 on link 39 = 19785.371 * 0.027613043 / 2833.945313 = 0.192782
			 */

			if (! isConsideringCO2Costs ) {
				if(personMoneyEventHandler.link39LeaveTime == Double.POSITIVE_INFINITY) {
					throw new RuntimeException("Active agent does not pass through link 39.");
				}

				for (PersonMoneyEvent e : personMoneyEventHandler.events){
					if (e.getTime() == personMoneyEventHandler.link39LeaveTime) {
						Assertions.assertEquals(  df.format( -0.19278 ), df.format( e.getAmount() ), "Exposure toll on link 39 from Manual calculation does not match from money event." );
					}
				}
			} else {
				//flat co2 costs = 0.084623
				// exposure costs from other pollutants = 0.19278
				for(PersonMoneyEvent e : personMoneyEventHandler.events) {
					if(e.getTime() == personMoneyEventHandler.link39LeaveTime) {
						Assertions.assertEquals(  df.format( -0.19278 - 0.084623 ), df.format( e.getAmount() ), "Exposure toll on link 39 from Manual calculation does not match from money event." );
					}
				}
			}
		} else if(this.noOfTimeBins == 24 ) {
			/*
			 * Total activity duration between 6-7 = 20*3600 + 3600 - 507 = 75093
			 * Thus, avg exposed duration = 75093/640 = 117.3328125
			 * Thus relavant duration = (0.132+3*0.029+5*0.002)*3600 = 824.4
			 * Thus, total toll = 824.4 * 0.027613043 / 117.3328125 = 0.1940138667
			 */
			
			if (! isConsideringCO2Costs ) {
				if(personMoneyEventHandler.link39LeaveTime == Double.POSITIVE_INFINITY) {
					throw new RuntimeException("Active agent does not pass through link 39.");
				}

				for (PersonMoneyEvent e : personMoneyEventHandler.events){
					if (e.getTime() == personMoneyEventHandler.link39LeaveTime) {
						Assertions.assertEquals(  df.format( -0.194013 ), df.format( e.getAmount() ), "Exposure toll on link 39 from manual calculation does not match from money event." );
					}
				}
			} else {
				//flat co2 costs = 0.084623
				// exposure costs from other pollutants = 0.194013
				for(PersonMoneyEvent e : personMoneyEventHandler.events) {
					if(e.getTime() == personMoneyEventHandler.link39LeaveTime) {
						Assertions.assertEquals(  df.format( -0.194013 - 0.084623 ), df.format( e.getAmount() ), "Exposure toll on link 39 from Manual calculation does not match from money event." );
					}
				}
			}
		}
	}

	private Scenario minimalControlerSetting(boolean isConsideringCO2Costs) {
	
//		// since same files are used for multiple test, files are added to ONE MORE level up then the test package directory
//		String packageInputDir = helper.getPackageInputDirectory();
//		String inputFilesDir = packageInputDir.substring(0, packageInputDir.lastIndexOf('/') );
//		inputFilesDir = inputFilesDir.substring(0, inputFilesDir.lastIndexOf('/') + 1);
		String inputFilesDir = "./test/input/playground/amit/emissions/internalization/";

		String roadTypeMappingFile = inputFilesDir + "/roadTypeMapping.txt";
		String emissionVehicleFile = inputFilesDir + "/equil_emissionVehicles_1pct.xml.gz";

		String averageFleetWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_vehcat_2005average.txt";
		String averageFleetColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt";

		boolean isUsingDetailedEmissionCalculation = true;
		String detailedWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_SubSegm_2005detailed.txt";
		String detailedColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_SubSegm_2005detailed.txt";
		
		
		EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
		Scenario sc = equilTestSetUp.createConfigAndReturnScenario();
		// Info: I have used link speed as 100/3.6 m/s instead of 100 m/s thus check the difference in the result
		equilTestSetUp.createNetwork(sc);
		equilTestSetUp.createActiveAgents(sc);
		equilTestSetUp.createPassiveAgents(sc);

		Config config = sc.getConfig();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
//		ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		config.vehicles().setVehiclesFile(emissionVehicleFile);

		ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

//		ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);
		ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
//		ecg.setUsingVehicleTypeIdAsVehicleDescription(true);

//		ecg.setEmissionEfficiencyFactor(1.0);
//		ecg.setConsideringCO2Costs(isConsideringCO2Costs);
//		ecg.setEmissionCostMultiplicationFactor(1.0);

		config.addModule(ecg);

		config.controller().setLastIteration(0);
		return sc;
	}

	private void addReRoutingStrategy(Scenario sc) {
		ReplanningConfigGroup scg = sc.getConfig().replanning();
		StrategySettings strategySettingsR = new StrategySettings();
		strategySettingsR.setStrategyName("ReRoute");
		strategySettingsR.setWeight(1000);
		strategySettingsR.setDisableAfter(10);
		scg.addStrategySettings(strategySettingsR);
	}

	static class MyPersonMoneyEventHandler implements PersonMoneyEventHandler, LinkLeaveEventHandler  {

		List<PersonMoneyEvent> events = new ArrayList<PersonMoneyEvent>();
		double link39LeaveTime = Double.POSITIVE_INFINITY;

		@Override
		public void reset(int iteration) {
			events.clear();
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			events.add(event);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().toString().equals("39")) link39LeaveTime = event.getTime();
		}
	}
}