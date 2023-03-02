/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonAnalysis.java
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
package playground.amit.munich.analysis.userGroup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.munich.utils.MunichPersonFilter;
import playground.amit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.amit.utils.LoadMyScenarios;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

import java.io.BufferedWriter;
import java.util.*;

import static org.matsim.contrib.emissions.Pollutant.*;

/**
 * A class to get emissions and emissions cost for each user group.
 * @author amit
 *
 */
public class EmissionsPerPersonPerUserGroup {

	public static final Logger LOG = LogManager.getLogger(EmissionsPerPersonPerUserGroup.class);
	private int lastIteration;
	private final String outputDir;
	private SortedMap<MunichUserGroup, SortedMap<String, Double>> userGroupToEmissions;
	private Scenario scenario;
	private Map<Id<Person>, SortedMap<Pollutant, Double>> emissionsPerPerson;
	private final MunichPersonFilter pf = new MunichPersonFilter();
	
	public EmissionsPerPersonPerUserGroup(String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String outputDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/exposure/";
		String [] runCases =  {"ExI","5ExI","10ExI","15ExI","20ExI","25ExI"};
		
		EmissionsPerPersonPerUserGroup eppa = new EmissionsPerPersonPerUserGroup(outputDir);
		eppa.run(runCases);
	}
	
	private void init(final String runCase){
		
		this.scenario = LoadMyScenarios.loadScenarioFromOutputDir(this.outputDir+runCase);
		this.lastIteration = this.scenario.getConfig().controler().getLastIteration();
		
		this.userGroupToEmissions = new TreeMap<>();
		this.emissionsPerPerson = new HashMap<>();

		for(MunichUserGroup ug:MunichUserGroup.values()){
			SortedMap<String, Double> pollutantToValue = new TreeMap<>();
			for(Pollutant wm:Pollutant.values()) {
				pollutantToValue.put(wm.toString(), 0.0);
			}
			this.userGroupToEmissions.put(ug, pollutantToValue);
		}
	}

	public void run(final String [] runCases) {
		for(String runCase:runCases){
			init(runCase);
			
			String emissionEventFile = this.outputDir+runCase+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".emission.events.xml.gz";//"/events.xml";//
			EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionEventFile);
			ema.init((MutableScenario) this.scenario);
			ema.preProcessData();
			ema.postProcessData();

			Map<Id<Person>, SortedMap<Pollutant, Double>> totalEmissions = ema.getPerson2totalEmissions();
//			Set<Pollutant> pollutants = new HashSet<Pollutant>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));
			Set<Pollutant> pollutants = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2,PM, SO2));
			emissionsPerPerson = EmissionUtils.setNonCalculatedEmissionsForPopulation(scenario.getPopulation(), totalEmissions, pollutants);

			getTotalEmissionsPerUserGroup(this.emissionsPerPerson);
			writeTotalEmissionsPerUserGroup(this.outputDir+runCase+"/analysis/userGrpEmissions.txt");
			writeTotalEmissionsCostsPerUserGroup(this.outputDir+runCase+"/analysis/userGrpEmissionsCosts.txt");
		}
	}

	private void writeTotalEmissionsCostsPerUserGroup(final String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t");
			for(EmissionCostFactors ecf:EmissionCostFactors.values()){
				writer.write(ecf.toString()+"\t");
			}
			writer.write("total \n");
			for(MunichUserGroup ug:this.userGroupToEmissions.keySet()){
				double totalEmissionCost =0. ;
				writer.write(ug+"\t");
				for(EmissionCostFactors ecf:EmissionCostFactors.values()){
					double ec = this.userGroupToEmissions.get(ug).get(ecf.toString()) * ecf.getCostFactor();
					writer.write(ec+"\t");
					totalEmissionCost += ec;
				}
				writer.write(totalEmissionCost+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		LOG.info("Finished Writing data to file "+outputFile);		
	}

	private void writeTotalEmissionsPerUserGroup(final String outputFile) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t");
			for(String str:this.userGroupToEmissions.get(MunichUserGroup.Urban).keySet()){
				writer.write(str+"\t");
			}
			writer.newLine();
			for(MunichUserGroup ug:this.userGroupToEmissions.keySet()){
				writer.write(ug+"\t");
				for(String str:this.userGroupToEmissions.get(ug).keySet()){
					writer.write(this.userGroupToEmissions.get(ug).get(str)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		LOG.info("Finished Writing files to file "+outputFile);		
	}

	private void getTotalEmissionsPerUserGroup(
			Map<Id<Person>, SortedMap<Pollutant, Double>> emissionsPerPerson) {
		for(Id<Person> personId: scenario.getPopulation().getPersons().keySet()){
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(personId);
			SortedMap<String, Double> emissionsNewValue = new TreeMap<>();
			for(Pollutant poll: emissionsPerPerson.get(personId).keySet()){
				double emissionSoFar = this.userGroupToEmissions.get(ug).get(poll);
				double emissionNewValue = emissionSoFar+emissionsPerPerson.get(personId).get(poll);
				emissionsNewValue.put(poll.toString(), emissionNewValue);
			}
			this.userGroupToEmissions.put(ug, emissionsNewValue);
		}
	}
}
