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

package playground.amit;

import com.google.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.amit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Just setting up the equil scenario for mixed traffic conditions.
 *
 * @author amit
 */

public class EquilMixedTrafficIT {

    private static final URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil-mixedTraffic");

    @RegisterExtension
    public MatsimTestUtils helper = new MatsimTestUtils();

    @Test
    public void runSameVehiclesTypesInTrips() {
        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(EQUIL_DIR,"config-with-mode-vehicles.xml"));
        config.controller().setOutputDirectory(helper.getOutputDirectory());

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Vehicles vehs = scenario.getVehicles();

        scenario.getConfig().controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

        Controler controler = new Controler(scenario);

        final VehicleLinkTravelTimeEventHandler handler = new VehicleLinkTravelTimeEventHandler();
        ModalMaxSpeedEventHandler speedEventHandler = new ModalMaxSpeedEventHandler();

        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                // add event handler to test...
                addEventHandlerBinding().toInstance(handler);
                addEventHandlerBinding().toInstance(speedEventHandler);
            }
        });

        controler.run();

        final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicle2link2enterTime = handler.getVehicleId2LinkEnterTime();
        final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicle2link2leaveTime = handler.getVehicleId2LinkLeaveTime();

        Id<Vehicle> bikeVeh = Id.createVehicleId("10_bicycle");
        Id<Vehicle> carVeh = Id.createVehicleId(1);

        Id<Link> link5 = Id.createLinkId(5);
        Id<Link> link22 = Id.createLinkId(22);

        Assertions.assertEquals( Math.floor(10000 / 4.17) + 1.0, vehicle2link2leaveTime.get(bikeVeh).get(link5) - vehicle2link2enterTime.get(bikeVeh).get(link5), MatsimTestUtils.EPSILON, "Wrong travel time of agent 9 on link 5");
        Assertions.assertEquals( Math.floor(10000 / 16.677) + 1.0, vehicle2link2leaveTime.get(carVeh).get(link5) - vehicle2link2enterTime.get(carVeh).get(link5), MatsimTestUtils.EPSILON, "Wrong travel time of agent 2 on link 5");

        Tuple<Double,Double> bikeSpeed = speedEventHandler.mode2minmaxSpeed.get("bicycle");
        Tuple<Double,Double> carSpeed = speedEventHandler.mode2minmaxSpeed.get("car");

        VehicleType bicycle = vehs.getVehicleTypes().get(Id.create("bicycle",VehicleType.class));
        VehicleType car = vehs.getVehicleTypes().get(Id.create("car",VehicleType.class));

        Assertions.assertTrue( bikeSpeed.getSecond() < bicycle.getMaximumVelocity(), "The spped of bike should not be larger than assigned speed. It is "+bikeSpeed.getSecond());
        Assertions.assertTrue( carSpeed.getSecond() < car.getMaximumVelocity(), "The spped of car should not be larger than assigned speed. It is "+carSpeed.getSecond());

        Assertions.assertTrue( bikeSpeed.getFirst() > Math.floor(bicycle.getMaximumVelocity()), "The spped of bike should not be less than assigned speed. It is "+bikeSpeed.getFirst());
        Assertions.assertTrue( carSpeed.getFirst() > Math.floor(car.getMaximumVelocity()), "The spped of car should not be larger than assigned speed. It is "+carSpeed.getFirst());
    }

    /*
     * Multiple main modes, a person make multiple trips with different modes. Mode choice is allowed.
     * For e.g. In initial plans of Munich, person 555524.1#3699 uses bike in all trip modes,
     * however, a vehicle (passenger car) is still created for this person with vehicle id same as person id.
     */
    @Test
    public void runDifferentVehiclesTypesInTrips() {
        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(EQUIL_DIR,"config-with-mode-vehicles.xml"));
        String outDir = helper.getOutputDirectory();
        config.controller().setOutputDirectory(outDir);

        // is using VehiclesSource.modeVehicleTypesFromVehiclesData, only vehicleTypes are required.
        config.vehicles().setVehiclesFile(null);

        config.qsim().setUsePersonIdForMissingVehicleId(true);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
//        config.plans().setInputFile(EQUIL_DIR+"/plans2000.xml.gz");

        config.scoring().getOrCreateModeParams("bicycle").setMarginalUtilityOfTraveling(0.);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Vehicles vehs = scenario.getVehicles();

        VehicleType car = vehs.getFactory().createVehicleType(Id.create("car", VehicleType.class));
        car.setMaximumVelocity(16.67);
        car.setPcuEquivalents(1.0);
        vehs.addVehicleType(car);

        VehicleType bicycle = vehs.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
        bicycle.setMaximumVelocity(4.17);
        bicycle.setPcuEquivalents(0.25);
        bicycle.setNetworkMode("bicycle");
        vehs.addVehicleType(bicycle);

        // get bicycle-car or car-bicycle in plan.
        for (Person p : scenario.getPopulation().getPersons().values()) {
            List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
            String firstMode = null;
            for (PlanElement pe : pes) {
                if (pe instanceof Leg) {
                    Leg leg = (Leg) pe;
                    if (firstMode == null) {
                        firstMode = leg.getMode();
                    } else {
                        String mode = firstMode.equals("car") ? "bicycle" : "car";
                        leg.setMode(mode);
                    }
                }
            }
            // check
            Assertions.assertFalse( ((Leg)pes.get(1)).getMode().equals(((Leg)pes.get(3)).getMode()),"Assign different modes to the legs of person "+ p.getId());
        }

        //
        ReplanningConfigGroup.StrategySettings ss = new ReplanningConfigGroup.StrategySettings();
        ss.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
        ss.setWeight(0.2);

        scenario.getConfig().replanning().addStrategySettings(ss);
        scenario.getConfig().changeMode().setModes(new String [] {"car","bicycle"});

        scenario.getConfig().replanning().setFractionOfIterationsToDisableInnovation(0.8);

        scenario.getConfig().controller().setDumpDataAtEnd(true);
        scenario.getConfig().controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

        ModalMaxSpeedEventHandler speedEventHandler = new ModalMaxSpeedEventHandler();

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("bicycle").to(FreeSpeedTravelTimeForBike.class);
                addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());

                addEventHandlerBinding().toInstance(speedEventHandler);
            }
        });
        controler.run();

        Tuple<Double,Double> bikeSpeed = speedEventHandler.mode2minmaxSpeed.get("bicycle");
        Tuple<Double,Double> carSpeed = speedEventHandler.mode2minmaxSpeed.get("car");

        Assertions.assertTrue( bikeSpeed.getSecond() < bicycle.getMaximumVelocity(), "The spped of bike should not be larger than assigned speed. It is "+bikeSpeed.getSecond());
        Assertions.assertTrue( carSpeed.getSecond() < car.getMaximumVelocity(), "The spped of car should not be larger than assigned speed. It is "+carSpeed.getSecond());

        Assertions.assertTrue( bikeSpeed.getFirst() > Math.floor(bicycle.getMaximumVelocity()), "The spped of bike should not be less than assigned speed. It is "+bikeSpeed.getFirst());
        Assertions.assertTrue(carSpeed.getFirst() > Math.floor(car.getMaximumVelocity()), "The spped of car should not be larger than assigned speed. It is "+carSpeed.getFirst());
    }

    private static class ModalMaxSpeedEventHandler implements VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {

        @Inject
        Network network;

        private final Map<String, Tuple<Double,Double>> mode2minmaxSpeed = new HashMap<>();
        private final Map<Id<Vehicle>, Double> vehicleEnterTime = new HashMap<>();
        private final Map<Id<Vehicle>, String> vehicleId2Mode = new HashMap<>();

        @Override
        public void handleEvent(LinkEnterEvent event) {
            vehicleEnterTime.put(event.getVehicleId(), event.getTime());
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            if (vehicleEnterTime.containsKey(event.getVehicleId())) {
                String mode = vehicleId2Mode.get(event.getVehicleId());
                double linkLength = network.getLinks().get(event.getLinkId()).getLength();
                double travelTime = event.getTime() - vehicleEnterTime.remove(event.getVehicleId());
                double speed = linkLength / travelTime; // m/s

                if ( mode2minmaxSpeed.get(mode).getFirst() > speed) { // min speed
                    mode2minmaxSpeed.put(mode, new Tuple<>(speed,mode2minmaxSpeed.get(mode).getSecond()));
                }
                else if (mode2minmaxSpeed.get(mode).getSecond() < speed) { // max speed
                    mode2minmaxSpeed.put(mode, new Tuple<>(mode2minmaxSpeed.get(mode).getFirst(),speed));
                }
            }
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            if(! mode2minmaxSpeed.containsKey(event.getNetworkMode())) {
                mode2minmaxSpeed.put(event.getNetworkMode(), new Tuple<>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
            }
            vehicleId2Mode.put(event.getVehicleId(), event.getNetworkMode());
        }

        @Override
        public void reset(int iteration) {
            mode2minmaxSpeed.clear();
            vehicleEnterTime.clear();
            vehicleId2Mode.clear();
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            vehicleId2Mode.remove(event.getVehicleId());
            vehicleEnterTime.remove(event.getVehicleId());
        }
    }

    private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

        private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkLeaveTimes = new HashMap<>();
        private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkEnterTimes = new HashMap<>();

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            Map<Id<Link>, Double> leaveTime = this.vehicleLinkLeaveTimes.get(event.getVehicleId());
            if (leaveTime == null) {
                leaveTime = new HashMap<>();
                this.vehicleLinkLeaveTimes.put(event.getVehicleId(), leaveTime);
            }
            leaveTime.put(event.getLinkId(), Double.valueOf(event.getTime()));
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            Map<Id<Link>, Double> enterTime = this.vehicleLinkEnterTimes.get(event.getVehicleId());
            if (enterTime == null) {
                enterTime = new HashMap<>();
                this.vehicleLinkEnterTimes.put(event.getVehicleId(), enterTime);
            }
            enterTime.put(event.getLinkId(), Double.valueOf(event.getTime()));
        }

        @Override
        public void reset(int iteration) {
            this.vehicleLinkEnterTimes.clear();
            this.vehicleLinkLeaveTimes.clear();
        }

        public Map<Id<Vehicle>, Map<Id<Link>, Double>> getVehicleId2LinkEnterTime() {
            return this.vehicleLinkEnterTimes;
        }

        public Map<Id<Vehicle>, Map<Id<Link>, Double>> getVehicleId2LinkLeaveTime() {
            return this.vehicleLinkLeaveTimes;
        }
    }
}
