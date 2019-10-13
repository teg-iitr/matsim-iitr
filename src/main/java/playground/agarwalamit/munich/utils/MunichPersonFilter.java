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
package playground.agarwalamit.munich.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit
 */

public class MunichPersonFilter implements playground.agarwalamit.utils.PersonFilter{

	private static final Logger LOG = Logger.getLogger(MunichPersonFilter.class);
	public enum MunichUserGroup {Urban, Rev_Commuter, Freight}

	public MunichPersonFilter (){}

    @Override
	public String getUserGroupAsStringFromPersonId (final Id<Person> personId) {
		return getMunichUserGroupFromPersonId(personId).toString();
	}

	@Override
	public List<String> getUserGroupsAsStrings() {
		return Arrays.stream(MunichUserGroup.values()).map(Enum::toString).collect(Collectors.toList());
	}

	/**
	 * @return Urban or (Rev) commuter or Freight from person id.
	 */
	public MunichUserGroup getMunichUserGroupFromPersonId(final Id<Person> personId) {
		if (isPersonFreight(personId) ) return MunichUserGroup.Freight;
		else if (isPersonFromMID(personId)) return MunichUserGroup.Urban;
		else return MunichUserGroup.Rev_Commuter;
	}
	
	/**
	 * A translation between UserGroup and MunichUserGroup 
	 * Helpful for writing data to files.
	 */
	public MunichUserGroup getMunichUserGroup(final MunichUserGroup ug){
		switch(ug){
			case Rev_Commuter: return MunichUserGroup.Rev_Commuter;
			case Freight: return MunichUserGroup.Freight;
			case Urban: return MunichUserGroup.Urban;
			default: throw new RuntimeException("User group "+ug+" is not recongnised. Aborting ...");
		}
	}

	/*
	 * Taken from BK's personFilter
	 */

	public boolean isPersonFromMID(Id personId) {
		boolean isFromMID = false;
		if(personId.toString().startsWith("gv_")); //do nothing
		else if(personId.toString().startsWith("pv_")); //do nothing
		else isFromMID = true;
		return isFromMID;
	}

	public boolean isPersonFromMunich(Id personId) {
		boolean isFromMunich = false;
		if(isPersonFromMID(personId) || isPersonOutCommuter(personId)){
			isFromMunich = true;
		}
		return isFromMunich;
	}

	public boolean isPersonInnCommuter(Id personId) {
		boolean isInnCommuter = false;
		if(personId.toString().startsWith("pv_")){
			if(personId.toString().startsWith("pv_car_9162")); //do nothing
			else if(personId.toString().startsWith("pv_pt_9162")); //do nothing
			else isInnCommuter = true;
		}
		return isInnCommuter;
	}

	public boolean isPersonOutCommuter(Id personId) {
		boolean isOutCommuter = false;
		if(personId.toString().startsWith("pv_car_9162") || personId.toString().startsWith("pv_pt_9162")){
			isOutCommuter = true;
		}
		return isOutCommuter;
	}

	public boolean isPersonFreight(Id personId) {
		boolean isFreight = false;
		if(personId.toString().startsWith("gv_")){
			isFreight = true;
		}
		return isFreight;
	}

	public boolean isPersonIdFromUserGroup(Id personId, MunichUserGroup userGroup) {
		boolean isFromUserGroup = false;

		if(isPersonFromMID(personId)){
			if(userGroup.equals(MunichUserGroup.Urban)) isFromUserGroup = true ;
		}
		else if(isPersonInnCommuter(personId) || isPersonOutCommuter(personId)){
			if(userGroup.equals(MunichUserGroup.Rev_Commuter)) isFromUserGroup = true;
		}
		else if(isPersonFreight(personId)){
			if(userGroup.equals(MunichUserGroup.Freight)) isFromUserGroup = true;
		}
		else{
			LOG.warn("Cannot match person " + personId + " to any user group defined in " + MunichPersonFilter.class);
		}
		return isFromUserGroup;
	}

	public Population getPopulation(Population population, MunichUserGroup userGroup) {
		Population filteredPopulation = null;
		if(userGroup.equals(MunichUserGroup.Urban)) filteredPopulation = getMiDPopulation(population);
		else if(userGroup.equals(MunichUserGroup.Freight)) filteredPopulation = getFreightPopulation(population);
		else if(userGroup.equals(MunichUserGroup.Rev_Commuter)) filteredPopulation = getInOutCommuter(population);

		return filteredPopulation;
	}

	public Population getMiDPopulation(Population population) {
		Population filteredPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		population.getPersons().values().stream().filter(person -> isPersonFromMID(person.getId())).forEach(filteredPopulation::addPerson);
		return filteredPopulation;
	}

	public Population getInOutCommuter(Population population){
		Population filteredPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		population.getPersons().values().stream().filter(person -> isPersonInnCommuter(person.getId()) || isPersonOutCommuter(person.getId()) ).forEach(filteredPopulation::addPerson);
		return filteredPopulation;
	}

	public Population getFreightPopulation(Population population){
		Population filteredPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		population.getPersons().values().stream().filter(person -> isPersonFreight(person.getId())).forEach(filteredPopulation::addPerson);
		return filteredPopulation;
	}
}