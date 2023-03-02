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
package playground.amit.analysis.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.VehicleType;

/**
 * @author amit
 */
public class ExperiencedDelayHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
PersonDepartureEventHandler, PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {

	public final static Logger LOG = LogManager.getLogger(ExperiencedDelayHandler.class);
	
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	
	private final SortedMap<Double, Map<Id<Person>, Double>> timebin2PersonId2Delay = new TreeMap<>();
	private final Map<Double, Map<Id<Link>, Double>> timebin2LinkId2Delay = new HashMap<>();
	private final Map<Id<Link>, Map<Id<Person>, Double>> linkId2PersonIdLinkEnterTime = new HashMap<>();
	private final Map<Id<Link>, Map<String,Double>> linkId2FreeSpeedLinkTravelTime = new HashMap<>();
	private final Map<Double, Map<Id<Link>, Integer>> timebin2LinkIdLeaveCount = new HashMap<>();
	private final Map<Id<Person>, PersonDelayInfo> personId2DelayInfo = new HashMap<>();
	private double totalDelay;
	private double warnCount = 0;
	
	private final Map<Id<Person>, String> personId2Mode = new HashMap<>();
	private final SortedMap<String, Double> mode2Speed = new TreeMap<>();

	private double timeBinSize;

	public ExperiencedDelayHandler(final Scenario scenario, final int noOfTimeBins){
		initialize(scenario, noOfTimeBins);
	}

	private void initialize(final Scenario scenario, final int noOfTimeBins){
		double simulationEndTime = scenario.getConfig().qsim().getEndTime().seconds();
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		Network network = scenario.getNetwork();

		for(VehicleType vt :scenario.getVehicles().getVehicleTypes().values()) {
			mode2Speed.put(vt.getId().toString(), vt.getMaximumVelocity());
		}
		
		if (mode2Speed.isEmpty()) {
			mode2Speed.put(TransportMode.car, Double.MAX_VALUE);
		}

		for (Link link : network.getLinks().values()) {
			this.linkId2PersonIdLinkEnterTime.put(link.getId(), new HashMap<>());
			Map<String,Double> mode2freeSpeedTime = new HashMap<>();
			for (String mode : mode2Speed.keySet()) {
				Double freeSpeedLinkTravelTime = Math.floor(link.getLength() / Math.min(link.getFreespeed(), mode2Speed.get(mode))) + 1;
				mode2freeSpeedTime.put(mode, freeSpeedLinkTravelTime);
			}
			this.linkId2FreeSpeedLinkTravelTime.put(link.getId(), mode2freeSpeedTime);
		}

		for(int i =0;i<noOfTimeBins;i++){
			this.timebin2PersonId2Delay.put(this.timeBinSize*(i+1), new HashMap<>());
			this.timebin2LinkId2Delay.put(this.timeBinSize*(i+1), new HashMap<>());
			this.timebin2LinkIdLeaveCount.put(this.timeBinSize*(i+1), new HashMap<>());

			for(Person person : scenario.getPopulation().getPersons().values()){
				Map<Id<Person>, Double>	delayForPerson = this.timebin2PersonId2Delay.get(this.timeBinSize*(i+1));
				delayForPerson.put(person.getId(), 0.);
			}

			for(Link link : network.getLinks().values()) {
				Map<Id<Link>, Double>	delayOnLink = this.timebin2LinkId2Delay.get(this.timeBinSize*(i+1));
				delayOnLink.put(link.getId(), 0.);
				Map<Id<Link>, Integer> countOnLink = this.timebin2LinkIdLeaveCount.get(this.timeBinSize*(i+1));
				countOnLink.put(link.getId(), 0);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.timebin2LinkId2Delay.clear();
		this.timebin2PersonId2Delay.clear();
		LOG.info("Resetting person delays to   " + this.timebin2PersonId2Delay);
		this.linkId2PersonIdLinkEnterTime.clear();
		this.linkId2FreeSpeedLinkTravelTime.clear();
		this.timebin2LinkIdLeaveCount.clear();
		this.transitDriverPersons.clear();
		LOG.info("Resetting linkLeave counter to " + this.timebin2LinkIdLeaveCount);
		this.personId2Mode.clear();
		this.personId2DelayInfo.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if( ! mode2Speed.containsKey(event.getLegMode())) return;

		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = event.getPersonId();
		
		if(this.transitDriverPersons.contains(personId)) return;
		
		personId2Mode.put(event.getPersonId(), event.getLegMode());
		
		if(this.linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId)){
			// Person is already on the link. Cannot happen.
			throw new RuntimeException("Person is already on the link. Cannot happen.");
		} 

		Map<Id<Person>, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		double derivedLinkEnterTime = event.getTime()+1-this.linkId2FreeSpeedLinkTravelTime.get(linkId).get(event.getLegMode());
		personId2LinkEnterTime.put(personId, derivedLinkEnterTime);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);

		this.personId2DelayInfo.putIfAbsent(event.getPersonId(), new PersonDelayInfo(event.getPersonId()));
		this.personId2DelayInfo.get(event.getPersonId()).getTripInfo().add(new TripDelayInfo(event.getLegMode(), event.getTime()));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		double time = event.getTime();
		if(time ==0.0) time = this.timeBinSize;
		double endOfTimeInterval = 0.0;
		endOfTimeInterval = Math.ceil(time/this.timeBinSize)*this.timeBinSize;
		if(endOfTimeInterval<=0.0)endOfTimeInterval=this.timeBinSize;

		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());
		
		if (this.transitDriverPersons.contains(personId)) return;

		double actualTravelTime = event.getTime()-this.linkId2PersonIdLinkEnterTime.get(linkId).remove(personId);
		double freeSpeedTime = this.linkId2FreeSpeedLinkTravelTime.get(linkId).get(personId2Mode.get(personId));

		double currentDelay =	actualTravelTime-freeSpeedTime;
		if(currentDelay<1.)  currentDelay=0.;
		this.totalDelay+=currentDelay;

		Map<Id<Person>, Double> delayForPerson = this.timebin2PersonId2Delay.get(endOfTimeInterval);
		Map<Id<Link>, Double> delayOnLink = this.timebin2LinkId2Delay.get(endOfTimeInterval);
		Map<Id<Link>, Integer> countTotal = this.timebin2LinkIdLeaveCount.get(endOfTimeInterval);

		delayForPerson.put(personId, currentDelay + delayForPerson.get(personId));

		delayOnLink.put(linkId, currentDelay + delayOnLink.get(linkId));

		this.personId2DelayInfo.get(personId).getCurrentTripDelay().addDelay(currentDelay);

		double countsSoFar = countTotal.get(linkId);
		double newValue = countsSoFar + 1.;
		countTotal.put(linkId, (int) newValue);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double time = event.getTime();
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());

		if(this.linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId) && warnCount==0){
			warnCount++;
			LOG.warn("Person "+personId+" is entering on link "+linkId+" two times without leaving from the same. "
					+ "Link enter times are "+this.linkId2PersonIdLinkEnterTime.get(linkId).get(personId)+" and "+time);
			LOG.warn("Reason might be : There is at least one teleport activity departing on the link (and thus derived link "
					+ "enter time) and later person is entering the link with main congested mode. In such cases, the old time will be replaced.");
			LOG.warn(Gbl.ONLYONCE);
		}

		Map<Id<Person>, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		personId2LinkEnterTime.put(personId, time);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if( ! mode2Speed.containsKey(event.getLegMode())) return;

		if(this.transitDriverPersons.remove(event.getPersonId())) return;

		this.linkId2PersonIdLinkEnterTime.get(event.getLinkId()).remove(event.getPersonId());

		this.personId2DelayInfo.get(event.getPersonId()).getCurrentTripDelay().setTravelTime(event.getTime());
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getDelayPerPersonAndTimeInterval(){
		return this.timebin2PersonId2Delay;
	}

	public Map<Double, Map<Id<Link>, Double>> getDelayPerLinkAndTimeInterval(){
		return this.timebin2LinkId2Delay;
	}

	public double getTotalDelayInHours(){
		return this.totalDelay/3600;
	}

	public Map<Double, Map<Id<Link>, Integer>> getTime2linkIdLeaveCount() {
		return this.timebin2LinkIdLeaveCount;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	public Map<Id<Person>, PersonDelayInfo> getPersonId2TripInfo(){
		return this.personId2DelayInfo;
	}

	public static class PersonDelayInfo {
		private final Id<Person> pId;
		final List<TripDelayInfo> tripInfo;
		public PersonDelayInfo(Id<Person> personId){
			this.pId = personId;
			this.tripInfo = new ArrayList<>();
		}

		public List<TripDelayInfo> getTripInfo(){
			return this.tripInfo;
		}

		TripDelayInfo getCurrentTripDelay(){
			return this.tripInfo.get(this.tripInfo.size()-1);
		}

		public Id<Person> getPersonId(){
			return this.pId;
		}
	}

	public static class TripDelayInfo {
		final String travelMode;
		final double departureTime;

		double travelTime = 0.;
		double delay = 0;

		public TripDelayInfo(String travelMode, double departureTime) {
			this.travelMode = travelMode;
			this.departureTime = departureTime;
		}

		void setTravelTime(double arrivalTime) {
			this.travelTime = arrivalTime - this.departureTime;
		}

		void addDelay(double delayPerLink){
			this.delay+=delayPerLink;
		}

		public double getDepartureTime(){
			return this.departureTime;
		}

		public double getDelay(){
			return this.delay;
		}

		public double getTravelTime(){
			return this.travelTime;
		}

		public String getTravelMode(){
			return this.travelMode;
		}
	}
}