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

package playground.amit.flowDynamics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * @author amit after {playground.andreas.demo.AccessEgressDemoSimple}
 */

public class CarPassingBusTest {

	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private static final boolean isUsingOTFVis = false;
	
	@Test
	public void runTest() {
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createVehicles();
		createPopulation();
		
		LinkEnterLeaveTimeEventHandler lelteh = new LinkEnterLeaveTimeEventHandler();
		
		runSim(lelteh);
		
		Id<Vehicle> busId = Id.createVehicleId("bus_1");
		Id<Vehicle> carId = Id.createVehicleId("carUser");
		
		Id<Link> linkId = Id.createLinkId("1011");
		//	first make sure car enter after bus
		double busEnterTime = lelteh.vehicleEnterLeaveTimes.get(busId).get(linkId).getFirst();
		double carEnterTime = lelteh.vehicleEnterLeaveTimes.get(carId).get(linkId).getFirst();
		
		Assertions.assertEquals( busEnterTime < carEnterTime, true, "Bus should enter before car.");
		
		// now check car left before bus
		
		double busLeaveTime = lelteh.vehicleEnterLeaveTimes.get(busId).get(linkId).getSecond();
		double carLeaveTime = lelteh.vehicleEnterLeaveTimes.get(carId).get(linkId).getSecond();

		Assertions.assertEquals( busLeaveTime > carLeaveTime, true, "Car should leave before bus.");
		
		// now check for travel times
		double busTravelTime = busLeaveTime - busEnterTime; // should be = 500/5+1 = 101
		double carTravelTime = carLeaveTime - carEnterTime; // should be = 500/10+1 = 51

		Assertions.assertEquals( busTravelTime, 101, MatsimTestUtils.EPSILON, "Wrong bus travel time");
		Assertions.assertEquals( carTravelTime, 51, MatsimTestUtils.EPSILON, "Wrong car travel time");
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue );
		config.qsim().setEndTime(24.0*3600);
		config.qsim().setMainModes(Arrays.asList(TransportMode.car));
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
	}

	private void createNetwork() {
		Network network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);

		Node n01, n10, n11, n12, n13, n14;
		double x1 = -500;
		network.addNode(n01 = network.getFactory().createNode(Id.create("01", Node.class), new Coord(x1, -100)));
		network.addNode(n10 = network.getFactory().createNode(Id.create("10", Node.class), new Coord(0, 0)));
		network.addNode(n11 = network.getFactory().createNode(Id.create("11", Node.class), new Coord(500, 0)));
		network.addNode(n12 = network.getFactory().createNode(Id.create("12", Node.class), new Coord(1000, 0)));
		network.addNode(n13 = network.getFactory().createNode(Id.create("13", Node.class), new Coord(1500, 0)));
		network.addNode(n14 = network.getFactory().createNode(Id.create("14", Node.class), new Coord(2000, 50)));

		Link l;
		l = network.getFactory().createLink(Id.create("0110", Link.class), n01, n10); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1011", Link.class), n10, n11); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1112", Link.class), n11, n12); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1213", Link.class), n12, n13); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1314", Link.class), n13, n14); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);

	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		ArrayList<TransitRouteStop> stopListA = new ArrayList<>();

		// create stops
		TransitStopFacility stopFac;
		TransitRouteStop stop;

		stopFac = builder.createTransitStopFacility(Id.create("11", TransitStopFacility.class), new Coord(500, 0), false);
		stopFac.setLinkId(Id.create("1011", Link.class));
		schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 0, 10);
		stopListA.add(stop);

		stopFac = builder.createTransitStopFacility(Id.create("13", TransitStopFacility.class), new Coord(1500, 0), false);
		stopFac.setLinkId(Id.create("1213", Link.class));
		schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 50, 60);
		stopListA.add(stop);

		// transit line A		
		Link startLinkA = this.scenario.getNetwork().getLinks().get(Id.create("0110", Link.class));
		Link endLinkA = this.scenario.getNetwork().getLinks().get(Id.create("1314", Link.class));
		NetworkRoute networkRouteA = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, startLinkA.getId(), endLinkA.getId());

		ArrayList<Id<Link>> linkListA = new ArrayList<>();
		linkListA.add(Id.create("1011", Link.class)); 
		linkListA.add(Id.create("1112", Link.class)); 
		linkListA.add(Id.create("1213", Link.class));

		networkRouteA.setLinkIds(startLinkA.getId(), linkListA, endLinkA.getId());
		TransitRoute tRouteA = builder.createTransitRoute(Id.create("A", TransitRoute.class), networkRouteA, stopListA, "bus");
		TransitLine tLineA = builder.createTransitLine(Id.create("line A", TransitLine.class));
		tLineA.addRoute(tRouteA);
		schedule.addTransitLine(tLineA);

		Departure dep = builder.createDeparture(Id.create("bus_1", Departure.class), 7*3600.0 );
		dep.setVehicleId(Id.create("bus_1", Vehicle.class));
		tRouteA.addDeparture(dep);
	}

	private void createVehicles() {
		{
			Vehicles vehicles = this.scenario.getTransitVehicles();
			VehiclesFactory vb = vehicles.getFactory();

			// bus like
			VehicleType busType = vb.createVehicleType(Id.create("bus", VehicleType.class));
			busType.setMaximumVelocity(5.0);
			busType.setPcuEquivalents(3.);
			VehicleCapacity capacity = busType.getCapacity();
			capacity.setSeats(Integer.valueOf(9999));
			capacity.setStandingRoom(Integer.valueOf(0));
			vehicles.addVehicleType(busType);
			vehicles.addVehicle( vb.createVehicle(Id.create("bus_1", Vehicle.class), busType));
		}

		{
			Vehicles vehs = this.scenario.getVehicles();
			VehicleType carType = vehs.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
			carType.setMaximumVelocity(10.);
			carType.setPcuEquivalents(1.);
			vehs.addVehicleType(carType);

			vehs.addVehicle(vehs.getFactory().createVehicle(Id.create("carUser", Vehicle.class), carType) );
		}
	}

	private void createPopulation() {
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();

		Person person = pb.createPerson(Id.create("carUser", Person.class));
		Plan plan = pb.createPlan();

		Link startLinkA = this.scenario.getNetwork().getLinks().get(Id.create("0110", Link.class));
		Link endLinkA = this.scenario.getNetwork().getLinks().get(Id.create("1314", Link.class));

		Activity act1 = pb.createActivityFromLinkId("home", startLinkA.getId());
		act1.setEndTime(7*3600. + 49.);
		Leg leg = pb.createLeg(TransportMode.car);

		NetworkRoute networkRouteA = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, startLinkA.getId(), endLinkA.getId());

		ArrayList<Id<Link>> linkListA = new ArrayList<>();
		linkListA.add(Id.create("1011", Link.class)); 
		linkListA.add(Id.create("1112", Link.class)); 
		linkListA.add(Id.create("1213", Link.class));

		networkRouteA.setLinkIds(startLinkA.getId(), linkListA, endLinkA.getId());
		leg.setRoute(networkRouteA);

		Activity act2 = pb.createActivityFromLinkId("work", endLinkA.getId());

		population.addPerson(person);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);
	}

	private void runSim(LinkEnterLeaveTimeEventHandler eventHandler) {
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(eventHandler);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim qSim = new QSimBuilder(this.scenario.getConfig()).useDefaults().build(this.scenario, events);

		if (isUsingOTFVis) {
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
			OTFClientLive.run(scenario.getConfig(), server);
		}

		qSim.run();
	}
	
	private static class LinkEnterLeaveTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Tuple<Double, Double>>> vehicleEnterLeaveTimes = new HashMap<>();

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Tuple<Double, Double>> times = this.vehicleEnterLeaveTimes.get(event.getVehicleId());
			if (times == null) {
				times = new HashMap<>();
				times.put(event.getLinkId(), new Tuple<>(0., Double.NEGATIVE_INFINITY));
				this.vehicleEnterLeaveTimes.put(event.getVehicleId(), times);
			}
			
			Tuple<Double, Double> d = times.get(event.getLinkId());
			if (d == null) {
				d = new Tuple<>(event.getTime(), Double.NEGATIVE_INFINITY);
			}
			
			times.put(event.getLinkId(), d);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Tuple<Double, Double>> times = this.vehicleEnterLeaveTimes.get(event.getVehicleId());
			if (times == null) {
				times = new HashMap<>();
				times.put(event.getLinkId(), new Tuple<>(Double.NEGATIVE_INFINITY, 0.));
				this.vehicleEnterLeaveTimes.put(event.getVehicleId(), times);
			}
			
			Tuple<Double, Double> d = times.get(event.getLinkId());
			d = new Tuple<>(d.getFirst(), event.getTime());
			times.put(event.getLinkId(), d);
		}

		@Override
		public void reset(int iteration) {
		}
	}
}