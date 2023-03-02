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
package playground.amit.analysis.emission;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.analysis.emission.filtering.FilteredColdEmissionHandler;
import playground.amit.analysis.emission.filtering.FilteredWarmEmissionHandler;
import playground.amit.munich.utils.MunichPersonFilter;
import playground.amit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.amit.utils.*;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author amit
 *
 */
//TODO :clean it.

public class EmissionLinkAnalyzer extends AbstractAnalysisModule {
	private static final Logger LOG = LogManager.getLogger(EmissionLinkAnalyzer.class);
	private final String emissionEventsFile;
    private final FilteredWarmEmissionHandler warmHandler;
	private final FilteredColdEmissionHandler coldHandler;
	private Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> link2WarmEmissions;
	private Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> link2ColdEmissions;
	private SortedMap<Double, Map<Id<Link>, SortedMap<Pollutant, Double>>> link2TotalEmissions;
	private SortedMap<Pollutant,Double> totalEmissions = new TreeMap<>();

	/**
	 * This will compute the emissions only from links falling inside the given shape and consider the persons belongs to the given user group.
	 */
	public EmissionLinkAnalyzer(final double simulationEndTime, final String emissionEventFile, final int noOfTimeBins, final String shapeFile,
								final Network network, final String userGroup, final PersonFilter personFilter) {
		super(EmissionLinkAnalyzer.class.getSimpleName());
		this.emissionEventsFile = emissionEventFile;
		LOG.info("Aggregating emissions for each "+simulationEndTime/noOfTimeBins+" sec time bin.");

		AreaFilter af = null;
		if(shapeFile!=null)	af = new AreaFilter(shapeFile);

		this.warmHandler = new FilteredWarmEmissionHandler(simulationEndTime, noOfTimeBins, userGroup, personFilter, network, af);
		this.coldHandler = new FilteredColdEmissionHandler(simulationEndTime, noOfTimeBins, userGroup, personFilter, network, af);
	}

	/**
	 * This will compute the emissions only from links falling inside the given shape and persons from all user groups.
	 */
	public EmissionLinkAnalyzer(final double simulationEndTime, final String emissionEventFile, final int noOfTimeBins, final String shapeFile, final Network network ) {
		this(simulationEndTime,emissionEventFile,noOfTimeBins,shapeFile,network,null,null);
	}

	/**
	 * This will compute the emissions for all links and persons from all user groups.
	 */
	public EmissionLinkAnalyzer(final double simulationEndTime, final String emissionEventFile, final int noOfTimeBins) {
		this(simulationEndTime,emissionEventFile,noOfTimeBins,null,null);
	}

	public static void main(String[] args) {
		String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/ijst/output_halfUtilMoney/";
//		String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/ijst/output_doubleUtilMoney/";
		String [] runCases =  {"bau","ei","ei5","ei10"};
		String shapeFileCity = FileUtils.SHARED_SVN+"/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//		String shapeFileMMA = FileUtils.SHARED_SVN+"/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(dir+"/bau/output_network.xml.gz");
//		BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/totalEmissionCosts_metroArea_userGroup.txt");
		BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/totalEmissionCosts_cityArea_userGroup.txt");
		try{
			writer.write("scenario \t userGroup \t totalCostEUR \n");
			for(String str : runCases){
				for(MunichUserGroup ug :MunichUserGroup.values()) {
					String emissionEventFile = dir+str+"/ITERS/it.1500/1500.events.xml.gz";
//					EmissionLinkAnalyzer ela = new EmissionLinkAnalyzer(30*3600, emissionEventFile, 1, shapeFileMMA, sc.getNetwork(), ug.toString(), new MunichPersonFilter());
					EmissionLinkAnalyzer ela = new EmissionLinkAnalyzer(30*3600, emissionEventFile, 1, shapeFileCity, sc.getNetwork(), ug.toString(), new MunichPersonFilter());
					ela.preProcessData();
					ela.postProcessData();
					ela.writeTotalEmissions(dir+str+"/analysis/","MMA_"+ug.toString());
					writer.write(str+"\t"+ug.toString()+"\t"+ela.getTotalEmissionsCosts()+"\n");
					writer.flush();
				}
			}
			writer.close();
		} catch (IOException e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new LinkedList<>();
	}

	@Override
	public void preProcessData() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader reader = new EmissionEventsReader(eventsManager);
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		reader.readFile(this.emissionEventsFile);
	}

	@Override
	public void postProcessData() {
		this.link2WarmEmissions = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		this.link2ColdEmissions = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		this.link2TotalEmissions = sumUpEmissionsPerTimeInterval(this.link2WarmEmissions, this.link2ColdEmissions);
	}

	@Override
	public void writeResults(String outputFolder) {
		SortedMap<Double,Double> time2cost = getTimebinToEmissionsCosts();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/time2totalEmissionCosts.txt");
		try{
			writer.write("timebin \t totalCostEUR \n");
			double totalEmissionCost =0. ;
			for(double timebin : time2cost.keySet()){
				writer.write(timebin+"\t"+time2cost.get(timebin)+"\n");
				totalEmissionCost += time2cost.get(timebin);
			}
			writer.write("totalCost \t"+totalEmissionCost+"\n");
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	public void writeTotalEmissions(String outputFolder, String suffix) {
		SortedMap<Pollutant, Double> emissions = getTotalEmissions();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/totalEmissions_"+suffix+".txt");
		try{
			writer.write("pollutant \t emissionsInGm \n");
			for(Pollutant emiss : emissions.keySet()){
				writer.write(emiss.toString()+"\t"+emissions.get(emiss)+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private SortedMap<Double, Map<Id<Link>, SortedMap<Pollutant, Double>>> sumUpEmissionsPerTimeInterval(
			final Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> time2warmEmissionsTotal,
			final Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> time2coldEmissionsTotal) {

		SortedMap<Double, Map<Id<Link>, SortedMap<Pollutant, Double>>> time2totalEmissions = new TreeMap<>();

		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id<Link>, Map<Pollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);
			Map<Id<Link>, Map<Pollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);

			Map<Id<Link>, Map<Pollutant, Double>> totalEmissions = EmissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			Map<Id<Link>, SortedMap<Pollutant, Double>> sorted_totalEmissions = new TreeMap<>();
			for (Map.Entry<Id<Link>, Map<Pollutant,Double>> e : totalEmissions.entrySet()) {
				sorted_totalEmissions.put(e.getKey(), new TreeMap<>(e.getValue()));
			}
					
			time2totalEmissions.put(endOfTimeInterval, sorted_totalEmissions);

//			this.totalEmissions = MapUtils.mergeMaps(this.totalEmissions, EmissionUtils.getTotalEmissions(sorted_totalEmissions));
			EmissionUtils.getTotalEmissions(sorted_totalEmissions).forEach( (k,v) -> this.totalEmissions.merge(k, v, Double::sum));
		}
		return time2totalEmissions;
	}

	public SortedMap<Double, Map<Id<Link>, SortedMap<Pollutant, Double>>> getLink2TotalEmissions() {
		return this.link2TotalEmissions;
	}

	public Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> getLink2WarmEmissions() {
		return link2WarmEmissions;
	}

	public Map<Double, Map<Id<Link>, Map<Pollutant, Double>>> getLink2ColdEmissions() {
		return link2ColdEmissions;
	}

	public SortedMap<Double,Double> getTimebinToEmissionsCosts(){
		SortedMap<Double, Double> time2cost = new TreeMap<>();
		for(double time : this.link2TotalEmissions.keySet()){
			double cost = 0.;
			for (Id<Link> linkid : this.link2TotalEmissions.get(time).keySet()){
				for(EmissionCostFactors ecf:EmissionCostFactors.values()){
					if ( this.link2TotalEmissions.containsKey(time) && this.link2TotalEmissions.get(time).containsKey(linkid) 
							&& this.link2TotalEmissions.get(time).get(linkid).containsKey(ecf.toString()) )
						cost += this.link2TotalEmissions.get(time).get(linkid).get(ecf.toString()) * ecf.getCostFactor();
					else cost += 0.;
				}
			}
			time2cost.put(time, cost);
		}
		return time2cost;
	}

	public double getTotalEmissionsCosts(){
		double totalEmissionCosts = 0;
		for(EmissionCostFactors ecf:EmissionCostFactors.values()){
			totalEmissionCosts += ecf.getCostFactor() * this.totalEmissions.get(ecf.toString());
		}
		return totalEmissionCosts;
	}

	public SortedMap<Pollutant, Double> getTotalEmissions(){
		return this.totalEmissions;
	}
}
