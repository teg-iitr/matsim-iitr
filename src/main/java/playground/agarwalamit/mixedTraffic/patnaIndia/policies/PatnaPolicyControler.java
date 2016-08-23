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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.plans.SelectedPlansFilter;

/**
 * @author amit
 */

public class PatnaPolicyControler {

	private static String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/";
	private static String configFile = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/input/configBaseCaseCtd.xml";
	private static boolean applyTrafficRestrain = false;
	private static boolean addBikeTrack = true;

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();

		if(args.length>0){
			configFile = args[0];
			outputDir = args[1];

			applyTrafficRestrain = Boolean.valueOf(args[2]);
			addBikeTrack = Boolean.valueOf(args[3]);
			ConfigUtils.loadConfig(config, configFile);
		} else {
			ConfigUtils.loadConfig(config, configFile);
			if(applyTrafficRestrain && addBikeTrack) config.controler().setOutputDirectory(outputDir+"/both/");
			else if(addBikeTrack) config.controler().setOutputDirectory(outputDir+"/bikeTrack/");
			else if(applyTrafficRestrain && addBikeTrack) config.controler().setOutputDirectory(outputDir+"/trafficRestrain/");
			else config.controler().setOutputDirectory(outputDir+"/baseCaseCtd/");
		}

		//==
		// after calibration;  departure time is fixed for urban; remove time choice
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for(StrategySettings ss : strategySettings){ // departure time is fixed now.
			if ( ss.getStrategyName().equals(DefaultStrategy.TimeAllocationMutator.toString()) ) {
				ss.setWeight(0.0);
			}
		}
		//==
		
		//==
		// take only selected plans so that time for urban and location for external traffic is fixed.
		String configPath = config.getContext().getPath();
		String inPlans = "baseCaseOutput_plans.xml.gz";
		String outPlans = "selectedPlansOnly.xml.gz";
		
		SelectedPlansFilter plansFilter = new SelectedPlansFilter();
		plansFilter.run(configPath + inPlans);
		plansFilter.writePlans(config.getContext().getPath() + outPlans);
		config.plans().setInputFile(outPlans);
		//==
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(1);

		// policies if any
		if (applyTrafficRestrain && addBikeTrack ) {
			config.network().setInputFile(configPath + "/networkWithTrafficRestricationAndBikeTrack.xml.gz");
		} else if (applyTrafficRestrain ) {
			config.network().setInputFile(configPath + "/networkWithTrafficRestrication.xml.gz");
		} else if(addBikeTrack) {
			config.network().setInputFile(configPath + "/networkWithBikeTrack.xml.gz");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);

		if(applyTrafficRestrain ) removeRoutes(scenario); // removal of some links may lead to exception if routes are not removed from leg.

		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.getConfig().strategy().setMaxAgentPlanMemorySize(10);

		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		final RandomizingTimeDistanceTravelDisutilityFactory builder_truck =  new RandomizingTimeDistanceTravelDisutilityFactory("truck", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);

				addTravelTimeBinding("truck").to(FreeSpeedTravelTimeForTruck.class);
				addTravelDisutilityFactoryBinding("truck").toInstance(builder_truck);

				addTravelTimeBinding("motorbik").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbik").to(carTravelDisutilityFactoryKey());					
			}
		});

		controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListner.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListner.class);

				this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
			}
		});

		// adding pt fare system based on distance 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
			}
		});
		// for above make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// add income dependent scoring function factory
		addScoringFunction(controler);

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = outputDir+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectory(new File(dirToDel),false);
		}

		new File(outputDir+"/analysis/").mkdir();
		String outputEventsFile = outputDir+"/output_events.xml.gz";
		// write some default analysis
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile);
		mtta.run();
		mtta.writeResults(outputDir+"/analysis/modalTravelTime.txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents.txt");

		StatsWriter.run(outputDir);
	}

	private static void addScoringFunction(final Controler controler){
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
			@Inject Network network;
			@Inject Population population;
			@Inject PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
			@Inject ScenarioConfigGroup scenarioConfig;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				Double ratioOfInc = 1.0;

				if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
					Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
					Double avgInc = PatnaUtils.MEADIAM_INCOME;
					ratioOfInc = avgInc/monthlyInc;
				}

				planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );				

				ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 

				CharyparNagelScoringParameters.Builder builder = new CharyparNagelScoringParameters.Builder(
						planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
				final CharyparNagelScoringParameters modifiedParams = builder.build();

				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
				return sumScoringFunction;
			}
		});
	}

	private static void removeRoutes(Scenario scenario){
		//since some links are now removed, route in the plans will throw exception, remove them.
		for (Person p : scenario.getPopulation().getPersons().values()){
			List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
			for (PlanElement pe :pes ){
				if (pe instanceof Activity) { 
					Activity act = ((Activity)pe);
					Id<Link> linkId = act.getLinkId();
					Coord cord = act.getCoord();

					if (cord == null && linkId == null) throw new RuntimeException("Activity "+act.toString()+" do not have either of link id or coord. Aborting...");
					else if (linkId == null ) { /*nothing to do*/ }
					else if (cord==null && ! scenario.getNetwork().getLinks().containsKey(linkId)) throw new RuntimeException("Activity "+act.toString()+" do not have cord and link id is not present in network. Aborting...");
					else {
						cord = scenario.getNetwork().getLinks().get(linkId).getCoord();
						act.setCoord(cord);
					}
				} else if ( pe instanceof Leg){
					Leg leg = (Leg) pe;
					leg.setRoute(null);
				}
			}
		}
	}
}
