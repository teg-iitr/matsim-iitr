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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by amit on 14.11.17.
 */
@RunWith(Parameterized.class)
public class OnRoadExposureForMixedTrafficTest {

    @Rule
    public final MatsimTestUtils helper = new MatsimTestUtils();
    private static final Logger logger = Logger.getLogger(OnRoadExposureForMixedTrafficTest.class);

    private final boolean isConsideringCO2Costs = false; // no local exposure for co2

    private final QSimConfigGroup.VehiclesSource vehiclesSource;

    public OnRoadExposureForMixedTrafficTest(QSimConfigGroup.VehiclesSource vehiclesSource) {
        this.vehiclesSource = vehiclesSource;
        logger.info("Each parameter will be used in all the tests i.e. all tests will be run while inclusing and excluding CO2 costs.");
    }

    @Parameterized.Parameters(name = "{index}: vehicleSource == {0};")
    public static List<Object[]> considerCO2 () {
        Object[] [] considerCO2 = new Object [] [] {
                //fro 'frommVEhiclesData' vehicle Ids must be provided with person attributes, this vehicle source is not imp. here. Amit Oct'19
//                { QSimConfigGroup.VehiclesSource.fromVehiclesData},
                {QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData}
        };
        return Arrays.asList(considerCO2);
    }

    /**
     * See the event handler for the details about the manual exposure calculation.
     *
     * TODO : should include following situation: (a) cold emissions except departure link is thrown at later time but on the departure link until distance travelled is more than 1km.
     */
    @Ignore
    @Test
    public void excludeAgentsLeavingInSameTimeStepTest() {
        EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
        Scenario sc = equilTestSetUp.createConfigAndReturnScenario();

        Controler controler = getControler(sc, equilTestSetUp);

        OnRoadExposureConfigGroup onRoadExposureConfigGroup = (OnRoadExposureConfigGroup) ConfigUtils.addOrGetModule( sc.getConfig(), OnRoadExposureConfigGroup.class);
        OnRoadExposureHandler onRoadExposureEventHandler = new OnRoadExposureHandler(onRoadExposureConfigGroup, sc.getNetwork());

        EmissionAggregator emissionAggregator = new EmissionAggregator(sc.getNetwork());

        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                addEventHandlerBinding().toInstance(onRoadExposureEventHandler);
                addEventHandlerBinding().toInstance(emissionAggregator);
            }
        });

        controler.run();

        // offline calculation
        Map<Pollutant, Double> totalInhaledMass_manual = new HashMap<>();
        {
            OnRoadExposureCalculator onRoadExposureCalculator = new OnRoadExposureCalculator(onRoadExposureConfigGroup);

            {
                //car driver is exposed of cold emiss
                Map<Pollutant, Double> inhaledByCarEmiss = mergeMaps ( emissionAggregator.warmEmissions.get("car"), emissionAggregator.coldEmissions.get("car") );

                // since the background concentration=0; travel time does not matter.
                Map<Pollutant, Double> inhaledMass_car = onRoadExposureCalculator.calculate("car", inhaledByCarEmiss, 0.);
                totalInhaledMass_manual = inhaledMass_car;
            }
            {
                Map<Pollutant, Double> inhaledByBicycleEmiss = mergeMaps(emissionAggregator.warmEmissions.get("bicycle"),
                        emissionAggregator.coldEmissions.get("bicycle"));

                Map<Pollutant, Double> inhaledMass_bicycle = onRoadExposureCalculator.calculate("bicycle", inhaledByBicycleEmiss, 0.);

                totalInhaledMass_manual =  mergeMaps(totalInhaledMass_manual, inhaledMass_bicycle);
            }
        }
        totalInhaledMass_manual.remove("CO2_TOTAL");

        Map<Pollutant, Double> totalInhaledMass_sim = onRoadExposureEventHandler.getOnRoadExposureTable().getTotalInhaledMass();
        for (Pollutant str : totalInhaledMass_sim.keySet()) {
        	if (totalInhaledMass_manual.get(str) == null) {
        		logger.warn("Skip test for " + str);
        	} else {
                Assert.assertEquals("Calculation of inhaled mass of "+str+" is wrong.", totalInhaledMass_manual.get(str), totalInhaledMass_sim.get(str), Math.pow(10,-5));
        	}
        }
        totalInhaledMass_sim.entrySet().stream().forEach(e-> System.out.println(e.getKey() + " \t" + e.getValue() ));

    }

    private Controler getControler(Scenario sc, EquilTestSetUp equilTestSetUp){
        List<String> mainModes = Arrays.asList("car", "bicycle");
        equilTestSetUp.createNetwork(sc);

        // allow all modes on the links
        for (Link l : sc.getNetwork().getLinks().values()) {
            l.setAllowedModes(new HashSet<>(mainModes));
        }

        String carPersonId = "567417.1#12424";
        String bikePersonId = "567417.1#12425"; // no emissions
        String bikeVehicleId = bikePersonId;

        if (this.vehiclesSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
            bikeVehicleId = bikePersonId + "_bicycle";
        }

        Vehicles vehs = sc.getVehicles();

        VehicleType car = vehs.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(100.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.getAttributes().putAttribute("hbefaVehicleTypeDescription",
                 HbefaVehicleCategory.PASSENGER_CAR.toString().concat(";petrol (4S);>=2L;PC-P-Euro-0")
                 );
        // Info: "&gt;" is an escape character for ">" in xml (http://stackoverflow.com/a/1091953/1359166); need to be very careful with them.
        // thus, reading from vehicles file and directly passing to vehicles container is not the same.
        VehicleUtils.setHbefaVehicleCategory(car.getEngineInformation(),HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaEmissionsConcept(car.getEngineInformation(),"PC-P-Euro-0");
        VehicleUtils.setHbefaSizeClass(car.getEngineInformation(), ">=2L");
        VehicleUtils.setHbefaTechnology(car.getEngineInformation(),"petrol (4S)");

        vehs.addVehicleType(car);

        VehicleType bike = vehs.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
        bike.setMaximumVelocity(20. / 3.6);
        bike.setPcuEquivalents(0.25);
        bike.getAttributes().putAttribute("hbefaVehicleTypeDescription",
                HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString().concat(";;;") );//ZERO_EMISSION_VEHICLE
        VehicleUtils.setHbefaVehicleCategory(bike.getEngineInformation(),HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());//ZERO_EMISSION_VEHICLE
        bike.setNetworkMode("bicycle");
        vehs.addVehicleType(bike);

        if (!this.vehiclesSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
            Vehicle carVeh = vehs.getFactory().createVehicle(Id.createVehicleId(carPersonId), car);
            vehs.addVehicle(carVeh);

            Vehicle bikeVeh = vehs.getFactory().createVehicle(Id.createVehicleId(bikeVehicleId), bike);
            vehs.addVehicle(bikeVeh);
        }

        sc.getConfig().qsim().setMainModes(mainModes);
        sc.getConfig().qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        sc.getConfig().qsim().setVehiclesSource(this.vehiclesSource);
        sc.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);

        sc.getConfig()
          .plansCalcRoute()
          .getOrCreateModeRoutingParams(TransportMode.pt)
          .setTeleportedModeFreespeedFactor(1.5);
        sc.getConfig().plansCalcRoute().setNetworkModes(mainModes);
        sc.getConfig().planCalcScore().getOrCreateModeParams("bicycle").setConstant(0.0);

        sc.getConfig().travelTimeCalculator().setAnalyzedModesAsString("car,bicycle");
        sc.getConfig().travelTimeCalculator().setFilterModes(true);

        equilTestSetUp.createActiveAgents(sc, carPersonId, TransportMode.car, 6.0 * 3600.);
        equilTestSetUp.createActiveAgents(sc, bikePersonId, "bicycle", 6.0 * 3600. - 5.0);

        emissionSettings(sc);

        Controler controler = new Controler(sc);
        sc.getConfig().controler().setOutputDirectory(helper.getOutputDirectory());

        EmissionsConfigGroup emissionsConfigGroup = ((EmissionsConfigGroup) sc.getConfig()
                                                                              .getModules()
                                                                              .get(EmissionsConfigGroup.GROUP_NAME));
//        emissionsConfigGroup.setEmissionEfficiencyFactor(1.0);
        emissionsConfigGroup.setConsideringCO2Costs(isConsideringCO2Costs);
        emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
            }
        });
        return controler;
    }

    private void emissionSettings(Scenario scenario){
        String inputFilesDir = "./test/input/playground/amit/emissions/internalization/";

        String roadTypeMappingFile = inputFilesDir + "/roadTypeMapping.txt";
        String emissionVehicleFile = inputFilesDir + "/equil_emissionVehicles_1pct.xml.gz";

        String averageFleetWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_vehcat_2005average.txt";
        String averageFleetColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt";

        boolean isUsingDetailedEmissionCalculation = true;
        String detailedWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_SubSegm_2005detailed.txt";
        String detailedColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_SubSegm_2005detailed.txt";

        Config config = scenario.getConfig();
        EmissionsConfigGroup ecg = new EmissionsConfigGroup();
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);

        scenario.getConfig().vehicles().setVehiclesFile(emissionVehicleFile);

        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

//        ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
        ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);
        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
        config.addModule(ecg);

        OnRoadExposureConfigGroup onRoadExposureConfigGroup = new OnRoadExposureConfigGroup();
        onRoadExposureConfigGroup.setUsingMicroGramUnits(false);
        onRoadExposureConfigGroup.getModeToBreathingRate().put("bicycle",3.06/3600.);
        onRoadExposureConfigGroup.getModeToOccupancy().put("bicycle",1.0);
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("bicycle"); // this will set the default values
        config.addModule(onRoadExposureConfigGroup);
    }

    private class EmissionAggregator implements ColdEmissionEventHandler, WarmEmissionEventHandler {

        private Map<String, Map<Pollutant, Double>> coldEmissions = new HashMap<>();
        private Map<String, Map<Pollutant, Double>> warmEmissions = new HashMap<>();

        private final Network network;

        EmissionAggregator(Network network){
            this.network = network;
        }

        @Override
        public void reset(int iteration){
            this.coldEmissions.put("car", new HashMap<>());
            this.coldEmissions.put("bicycle", new HashMap<>());
            this.warmEmissions.put("car", new HashMap<>());
            this.warmEmissions.put("bicycle", new HashMap<>());
        }

        @Override
        public void handleEvent(ColdEmissionEvent event) {
            Map<Pollutant, Double> emiss = event.getColdEmissions()
                                                     .entrySet()
                                                     .stream()
                                                     .collect(Collectors.toMap(e -> e.getKey(),
                                                                     e -> e.getValue() / this.network.getLinks()
                                                                                                     .get(event.getLinkId())
                                                                                                     .getLength()));
            if (    (event.getLinkId().toString().equals("12") && event.getTime()==21595.0) || // self exposed bicycle
                    (event.getLinkId().toString().equals("45") && event.getTime()==50995.0) //self exposed bicycle
                    ) {
                coldEmissions.put("bicycle", mergeMaps(coldEmissions.get("bicycle"), emiss));
            } else if ( (event.getLinkId().toString().equals("12") && event.getTime()==21600.0) || //self exposed car
                    (event.getLinkId().toString().equals("45") && event.getTime()==51000.0)  //self exposed car
                    ) {
                coldEmissions.put("car", mergeMaps(coldEmissions.get("car"), emiss));
            } else
//                if (this.eventsOrder.equals(EventsComparatorForEmissions.EventsOrder.EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT) )
                {
                if ( event.getLinkId().toString().equals("12")  ) {
                    // no one is on the link

                } else {
                    coldEmissions.put("car", mergeMaps(coldEmissions.get("car"), emiss)); // only car is exposed by its own emissions
                }
            }
        }

        @Override
        public void handleEvent(WarmEmissionEvent event) {
            Map<Pollutant, Double> emiss = event.getWarmEmissions()
                                                             .entrySet()
                                                             .stream()
                                                             .collect(Collectors.toMap(e -> e.getKey(),
                                                                     e -> e.getValue() / this.network.getLinks()
                                                                                                     .get(event.getLinkId())
                                                                                                     .getLength()));
            //when car leave link 23 at 21674.0, bicycle is on the link,--> exposed
            // car leave link 56 at 51038, bicycle is on the link, --> exposed
            if  ( (event.getLinkId().toString().equals("23") && event.getTime()==21674.0 ) || // car emissions--> bicycle exposed.
                    (event.getLinkId().toString().equals("56") && event.getTime()==51038.0 ) // car emissions -> bicycle exposed
                    ) {

                warmEmissions.put("bicycle", mergeMaps(warmEmissions.get("bicycle"), emiss));

                warmEmissions.put("car", mergeMaps(warmEmissions.get("car"), emiss));

            } else
//                if (this.eventsOrder.equals(EventsComparatorForEmissions.EventsOrder.EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT))
                {
                warmEmissions.put("car", mergeMaps(warmEmissions.get("car"), emiss)); // only car is exposed by its own emissions; bicycle does not produce any
            }

        }


    }

    private Map<Pollutant, Double> mergeMaps(Map<Pollutant, Double> car, Map<Pollutant, Double> emiss) {
        Map<Pollutant, Double> out = new HashMap<>(car);
        emiss.forEach((k,v) -> out.merge(k,v, Double::sum));
        return out;
    }
}
