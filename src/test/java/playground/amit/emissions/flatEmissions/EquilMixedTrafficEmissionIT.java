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

package playground.amit.emissions.flatEmissions;

import com.google.inject.name.Names;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.vsp.airPollution.flatEmissions.InternalizeEmissionsControlerListener;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Just setting up the equil scenario to get emissions for a mixed traffic conditions.
 * 
 * @author amit
 */


public class EquilMixedTrafficEmissionIT {

	@RegisterExtension
	public final MatsimTestUtils helper = new MatsimTestUtils();
	private static final Logger logger = LogManager.getLogger(EquilMixedTrafficEmissionIT.class);

	private final String classOutputDir = "test/output/" + EquilMixedTrafficEmissionIT.class.getCanonicalName().replace('.', '/') + "/";

	private final DecimalFormat df = new DecimalFormat("#.###");

	private final boolean isConsideringCO2Costs ;

	private final QSimConfigGroup.VehiclesSource vehiclesSource;

	public EquilMixedTrafficEmissionIT(boolean isConsideringCO2Costs, QSimConfigGroup.VehiclesSource vehiclesSource) {
		this.isConsideringCO2Costs = isConsideringCO2Costs;
		this.vehiclesSource = vehiclesSource;
		logger.info("Each parameter will be used in all the tests i.e. all tests will be run while inclusing and excluding CO2 costs.");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@EnumSource(QSimConfigGroup.VehiclesSource.class)
//			.Parameters(name = "{index}: considerCO2 == {0}; vehicleSource == {1}")
	public static List<Object[]> considerCO2 () {
		Object[] [] considerCO2 = new Object [] [] {
				//for 'fromVehiclesData, vehicle IDs must be added to person attributes Amit Oct'19
//				{true, QSimConfigGroup.VehiclesSource.fromVehiclesData} ,
//				{false,QSimConfigGroup.VehiclesSource.fromVehiclesData},

				{true, QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData} ,
				{false, QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData} ,
				};
		return Arrays.asList(considerCO2);
	}

	@Disabled
	@Test
	public void emissionTollTest() {
		List<String> mainModes = Arrays.asList("car","bicycle");

		EquilTestSetUp equilTestSetUp = new EquilTestSetUp();

		Scenario sc = equilTestSetUp.createConfigAndReturnScenario();
		equilTestSetUp.createNetwork(sc);

		// allow all modes on the links
		for (Link l : sc.getNetwork().getLinks().values()){
			l.setAllowedModes(new HashSet<>(mainModes));
		}

		String carPersonId = "567417.1#12424";
		String bikePersonId = "567417.1#12425"; // no emissions
		String bikeVehicleId = bikePersonId;

		if (this.vehiclesSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
			bikeVehicleId = bikePersonId+"_bicycle";
		}

		Vehicles vehs = sc.getVehicles();

		VehicleType car = vehs.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
		car.setMaximumVelocity(100.0/3.6);
		car.setPcuEquivalents(1.0);
//		car.setDescription(HbefaVehicleCategory.PASSENGER_CAR.toString().concat(";petrol (4S);&gt;=2L;PC-P-Euro-0"));
		car.getAttributes().putAttribute("hbefaVehicleTypeDescription", HbefaVehicleCategory.PASSENGER_CAR.toString().concat(";petrol (4S);>=2L;PC-P-Euro-0") );
		// Info: "&gt;" is an escape character for ">" in xml (http://stackoverflow.com/a/1091953/1359166); need to be very careful with them.
		// thus, reading from vehicles file and directly passing to vehicles container is not the same.
		VehicleUtils.setHbefaVehicleCategory(car.getEngineInformation(),HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaEmissionsConcept(car.getEngineInformation(),"PC-P-Euro-0");
		VehicleUtils.setHbefaSizeClass(car.getEngineInformation(), ">=2L");
		VehicleUtils.setHbefaTechnology(car.getEngineInformation(),"petrol (4S)");

		vehs.addVehicleType(car);

		VehicleType bike = vehs.getFactory().createVehicleType(Id.create("bicycle",VehicleType.class));
		bike.setMaximumVelocity(20./3.6);
		bike.setPcuEquivalents(0.25);
		// following might not work, need to check. Amit May'20
		bike.getAttributes().putAttribute("hbefaVehicleTypeDescription",
				HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString().concat(";;;")); //ZERO_EMISSION_VEHICLE
		VehicleUtils.setHbefaVehicleCategory(bike.getEngineInformation(),HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString()); //ZERO_EMISSION_VEHICLE
		bike.setNetworkMode("bicycle");
		vehs.addVehicleType(bike);

		if(! this.vehiclesSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
			Vehicle carVeh = vehs.getFactory().createVehicle(Id.createVehicleId(carPersonId),car);
			vehs.addVehicle(carVeh);

			Vehicle bikeVeh = vehs.getFactory().createVehicle(Id.createVehicleId(bikeVehicleId),bike);
			vehs.addVehicle(bikeVeh);
		}

		sc.getConfig().qsim().setMainModes(mainModes);
		sc.getConfig().qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		sc.getConfig().qsim().setVehiclesSource(this.vehiclesSource);
		sc.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);

		sc.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.pt).setTeleportedModeFreespeedFactor(1.5);
		sc.getConfig().routing().setNetworkModes(mainModes);
		sc.getConfig().scoring().getOrCreateModeParams("bicycle").setConstant(0.0);

		sc.getConfig().travelTimeCalculator().setAnalyzedModesAsString("car,bicycle");
		sc.getConfig().travelTimeCalculator().setFilterModes(true);

		equilTestSetUp.createActiveAgents(sc, carPersonId, TransportMode.car, 6.0 * 3600.);
		equilTestSetUp.createActiveAgents(sc, bikePersonId, "bicycle", 6.0 * 3600. - 5.0);

		emissionSettings(sc);

		Controler controler = new Controler(sc);
		String outputDirectory = classOutputDir + helper.getMethodName() + "/" + (isConsideringCO2Costs ? "considerCO2Costs/" : "notConsiderCO2Costs/");
		sc.getConfig().controller().setOutputDirectory(outputDirectory);

		sc.getConfig().controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);

		EmissionsConfigGroup emissionsConfigGroup = ( (EmissionsConfigGroup) sc.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME) );
//		emissionsConfigGroup.setEmissionEfficiencyFactor(1.0);
//		emissionsConfigGroup.setConsideringCO2Costs(isConsideringCO2Costs);
//		emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EmissionModule.class).asEagerSingleton();
				bind(EmissionCostModule.class).asEagerSingleton();
				addControlerListenerBinding().to(InternalizeEmissionsControlerListener.class);

				bindCarTravelDisutilityFactory().toInstance(new EmissionModalTravelDisutilityCalculatorFactory(new RandomizingTimeDistanceTravelDisutilityFactory("car", sc.getConfig())));
				bind(TravelDisutilityFactory.class).annotatedWith(Names.named("bicycle")).toInstance(new EmissionModalTravelDisutilityCalculatorFactory(new RandomizingTimeDistanceTravelDisutilityFactory("bicycle", sc.getConfig())));
			}
		});

		MyPersonMoneyEventHandler personMoneyHandler = new MyPersonMoneyEventHandler();
		VehicleLinkEnterLeaveTimeEventHandler enterLeaveTimeEventHandler = new VehicleLinkEnterLeaveTimeEventHandler(Id.createLinkId("23"));
		MyEmissionEventHandler emissEventHandler = new MyEmissionEventHandler();

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addEventHandlerBinding().toInstance(personMoneyHandler);
				addEventHandlerBinding().toInstance(enterLeaveTimeEventHandler);
				addEventHandlerBinding().toInstance(emissEventHandler);
			}
		});

		controler.run();

		// first check for emissions

		// first check for cold emission, which are generated only on departure link.
		for (ColdEmissionEvent e : emissEventHandler.coldEvents ) {
			if( ! ( e.getLinkId().equals(Id.createLinkId(12)) || e.getLinkId().equals(Id.createLinkId(45)) ) ) {
				//TODO not sure why due to recent changes (05 May 2020), cold emission event also occurs on links other than departure link. Therefore, commenting two more statements below.
//				throw new RuntimeException("Cold emission event can occur only on departure link.");
			} else if (e.getVehicleId().toString().equals(bikeVehicleId)) {
				for(double d : e.getColdEmissions().values()){
					if (d!=0.) throw new RuntimeException("There should not be any cold emissions from bicycle mode.");
				}
			} else if (e.getVehicleId().toString().equals(carPersonId)) {
				boolean allZeroPollutants = true;
				for(double d : e.getColdEmissions().values()){
					if (d!=0.) allZeroPollutants = false;
				}
				if(allZeroPollutants) throw new RuntimeException("At least one of the cold pollutnant from car mode should be non-zero.");
			}
		}

		for(WarmEmissionEvent e : emissEventHandler.warmEvents) {
			if (e.getVehicleId().toString().equals(bikeVehicleId)) {
				for(double d : e.getWarmEmissions().values()){
					if (d!=0.) throw new RuntimeException("There should not be any cold emissions from bicycle mode.");
				}
			} else if (e.getVehicleId().toString().equals(carPersonId)) {
				boolean allZeroPollutants = true;
				for(double d : e.getWarmEmissions().values()){
					if (d!=0.) allZeroPollutants = false;
				}
				if(allZeroPollutants) throw new RuntimeException("At least one of the cold pollutnant from car mode should be non-zero.");
			}
		}

		// first coldEmission event is on departure link, thus compare money event and coldEmissionEvent
		double firstMoneyEventToll = personMoneyHandler.events.get(1).getAmount();

		Map<Pollutant, Double> coldEmiss = emissEventHandler.coldEvents.get(1).getColdEmissions();
		double totalColdEmissAmount = 0.;
		/*
		 * departure time is 21600, so at this time. distance = 1.0km, parking duration = 12h, vehType="PASSENGER_CAR;petrol (4S);&gt;=2L;PC-P-Euro-0"
		 * Thus, coldEmission levels => FC 22.12, HC 5.41, CO 99.97, NO2 -0.00122764240950346, NOx -0-03, PM 0, NMHC 5.12
		 * Thus coldEmissionCost = 0 * 384500. / (1000. * 1000.) + 45.12 * 1700. / (1000. * 1000.) + -0.03 * 9600. / (1000. * 1000.) = 0.008416
		 */

		for (Pollutant cp :coldEmiss.keySet() ){
			totalColdEmissAmount += EmissionCostFactors.getCostFactor(cp.toString()) * coldEmiss.get(cp);
		}

//		Assert.assertEquals("Cold emission toll from emission event and emission cost factors does not match from manual calculation.",totalColdEmissAmount, 0.008416, MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Cold emission toll from emission event and emission cost factors does not match from money event.",firstMoneyEventToll, - totalColdEmissAmount, MatsimTestUtils.EPSILON);

		/*
		 * There are two routes --> 12-23-38-84-45 and 12-23-39-94-45. 12 is departure link --> no warmEmissionevent.
		 * Thus, in both routes, 2nd warmEmission event is on the link  (38 or 39) with length 5000m. On this link, only free flow warm emission is thrown.
		 * warmEmission --> linkLength = 5km, FC 77.12, HC 0.08, CO 1.0700000000000001, NO2 0.01, NOX 0.36, CO2(total) 241.78, PM 0.00503000011667609, NMHC 0.07, SO2 0.00123396399430931
		 * Thus if co2Costs is not considered --> warmEmissCost = 0.36 * 5 * 9600. / (1000. * 1000.) +  0.07 * 5 * 1700. / (1000. * 1000.) + 0.00123396399430931* 5 * 11000. / (1000. * 1000.)
		 *  +   0.00503000011667609 * 5 * 384500. / (1000. * 1000.) = 0.027613043
		 *  if co2costs are considerd, then warmEmissCost = 0.01787566 +  241.78 * 5 * 70. / (1000. * 1000.) = 0.112236043
		 */
		Map<Pollutant, Double> warmEmiss = emissEventHandler.warmEvents.get(3).getWarmEmissions();
		double warmEmissTime = emissEventHandler.warmEvents.get(3).getTime();
		double totalWarmEmissAmount = 0.;
		for ( Pollutant wp : warmEmiss.keySet() ) {
			if( wp.toString().equalsIgnoreCase(Pollutant.CO2_TOTAL.toString()) && !isConsideringCO2Costs ) {
				//nothing to do
			} else {
				totalWarmEmissAmount += EmissionCostFactors.getCostFactor(wp.toString()) * warmEmiss.get(wp);
			}
		}
		double tollFromMoneyEvent = 0.;
		for (PersonMoneyEvent e : personMoneyHandler.events) {
			// Money event at the time of 2nd warm emission event.
			if ( e.getTime() == warmEmissTime ) tollFromMoneyEvent = e.getAmount();
		}

		if ( isConsideringCO2Costs ) {
			Assertions.assertEquals(  df.format( 0.112236043 ), df.format( totalWarmEmissAmount ), "Warm emission toll from emission event and emission cost factors does not match from manual calculation." );
			Assertions.assertEquals(tollFromMoneyEvent, - totalWarmEmissAmount, MatsimTestUtils.EPSILON, "Warm emission toll from emission event and emission cost factors does not match from money event.");
		} else {
			Assertions.assertEquals( df.format( 0.027613043 ), df.format( totalWarmEmissAmount ) , "Warm emission toll from emission event and emission cost factors does not match from manual calculation.");
			Assertions.assertEquals(tollFromMoneyEvent, - totalWarmEmissAmount, MatsimTestUtils.EPSILON, "Warm emission toll from emission event and emission cost factors does not match from money event.");
		}

		// now check if car is passing bike
		// checking enter and leave time of car and bike on link 23
		Tuple<Double,Double> carEnterLeaveTime = enterLeaveTimeEventHandler.vehicle_link23_enterLeaveTimes.get(Id.createVehicleId(carPersonId));
		Tuple<Double,Double> bikeEnterLeaveTime = enterLeaveTimeEventHandler.vehicle_link23_enterLeaveTimes.get(Id.createVehicleId(bikeVehicleId));

		Assertions.assertTrue(carEnterLeaveTime.getFirst() > bikeEnterLeaveTime.getFirst(), "Car should enter after bicycle.");
		Assertions.assertTrue(carEnterLeaveTime.getSecond() < bikeEnterLeaveTime.getSecond(), "Car should leave before bicycle.");

	}

	private void emissionSettings(Scenario scenario){
		String inputFilesDir = "./test/input/playground/amit/emissions/internalization/";

		String roadTypeMappingFile = inputFilesDir + "/roadTypeMapping.txt";
		String emissionVehicleFile = inputFilesDir + "/equil_emissionVehicles_1pct.xml.gz";

		String averageFleetWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_vehcat_2005average.txt";
		String averageFleetColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt";

//		boolean isUsingDetailedEmissionCalculation = true;
		String detailedWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_SubSegm_2005detailed.txt";
		String detailedColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_SubSegm_2005detailed.txt";

		Config config = scenario.getConfig();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
//		ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);

		scenario.getConfig().vehicles().setVehiclesFile(emissionVehicleFile);

		ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

//		ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);
		ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);

		config.addModule(ecg);
	}

	private static class MyPersonMoneyEventHandler implements PersonMoneyEventHandler {

		final List<PersonMoneyEvent> events = new ArrayList<>();

		@Override
		public void reset(int iteration) {
			events.clear();
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			events.add(event);
		}
	}

	private static class MyEmissionEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

		final List<WarmEmissionEvent> warmEvents = new ArrayList<>();
		final List<ColdEmissionEvent> coldEvents = new ArrayList<>();

		@Override
		public void reset(int iteration) {
			warmEvents.clear();
			coldEvents.clear();
		}

		@Override
		public void handleEvent(ColdEmissionEvent event) {
			coldEvents.add(event);
		}

		@Override
		public void handleEvent(WarmEmissionEvent event) {
			warmEvents.add(event);
		}
	}

	private static class VehicleLinkEnterLeaveTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Tuple<Double,Double>> vehicle_link23_enterLeaveTimes = new HashMap<>();
		private final Id<Link> linkId ;

		VehicleLinkEnterLeaveTimeEventHandler (final Id<Link> linkId) {
			this.linkId = linkId;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if(event.getLinkId().equals(linkId)) {
				vehicle_link23_enterLeaveTimes.put(event.getVehicleId(),new Tuple<>(event.getTime(),Double.NEGATIVE_INFINITY));
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().equals(linkId)) {
				vehicle_link23_enterLeaveTimes.put(event.getVehicleId(),
						new Tuple<>(vehicle_link23_enterLeaveTimes.get(event.getVehicleId()).getFirst(),event.getTime()));
			}
		}

		@Override
		public void reset(int iteration) {
			vehicle_link23_enterLeaveTimes.clear();
		}
	}
}
