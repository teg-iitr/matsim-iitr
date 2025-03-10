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
package playground.amit.mixedTraffic.patnaIndia.simTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.RoutingConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.amit.mixedTraffic.patnaIndia.input.urban.UrbanDemandGenerator;
import playground.amit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.amit.utils.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author amit
 */

public class PatnaSimulationTimeWriter {

	private static final Logger LOG = LogManager.getLogger(PatnaSimulationTimeWriter.class);
	private static final int [] randomSeeds = {4711, 6835, 1847, 4144, 4628, 2632, 5982, 3218, 5736, 7573,4389, 1344} ;
	private static String runDir = FileUtils.RUNS_SVN+"/patnaIndia/run110/";
	private static String inputFilesDir = runDir+"/inputs/";
	
	private static String linkDynamics_CSV = "FIFO,PassingQ,SeepageQ";
	private static String trafficDynamics_CSV = "queue,withHoles";
	private static int numberOfRandomSeedsToUse = randomSeeds.length;
	private static boolean isUsingFastCapacityUpdate = false;
	private static int cloningFactor = 1;

	private static double storageCapacityFactor = 0.03 * cloningFactor;

	public static void main(String[] args) {

		boolean isUsingCluster = false;

		if (args.length != 0) isUsingCluster = true;

		if ( isUsingCluster ) {
			runDir = args[0];
			inputFilesDir = args[1];
			cloningFactor = Integer.valueOf( args[2] );
			linkDynamics_CSV = args[3]; // for 100% scenario, all cases (72) cant be simulated only in one job
			trafficDynamics_CSV = args[4]; // for 100% scenario, all cases (72) cant be simulated only in one job
			numberOfRandomSeedsToUse = Integer.valueOf(args[5]);
			isUsingFastCapacityUpdate = Boolean.valueOf(args[6]);
			storageCapacityFactor = Double.valueOf(args[7]);
		}

		PatnaSimulationTimeWriter pstw = new PatnaSimulationTimeWriter();

		List<String> lds  = Arrays.asList( linkDynamics_CSV.split(",") );
		List<String> tds = Arrays.asList(trafficDynamics_CSV.split(","));
		
		for (String ldString : lds ) {
			LinkDynamics ld = LinkDynamics.valueOf(ldString);
			for ( String tdString : tds){
				TrafficDynamics td = TrafficDynamics.valueOf(tdString);
				pstw.processAndWriteSimulationTime(ld, td);
			}
		}
	}

	private void processAndWriteSimulationTime (LinkDynamics ld, TrafficDynamics td) {
		for (int i = 0; i<randomSeeds.length ;i++) {
			if( i >= numberOfRandomSeedsToUse) continue;
			int randomSeed = randomSeeds[i];
//			MatsimRandom.reset(randomSeed);

			String runSpecificOutputDir = runDir+"/"+cloningFactor+"pct" + "/output_"+ld+"_"+td+"_"+i+"/";
			Controler controler = getControler(ld, td, runSpecificOutputDir, isUsingFastCapacityUpdate);
			controler.getConfig().global().setRandomSeed(randomSeed);

			controler.run();

			// delete unnecessary iterations folder here.
			int firstIt = controler.getConfig().controller().getFirstIteration();
			int lastIt = controler.getConfig().controller().getLastIteration();
			FileUtils.deleteIntermediateIterations(runSpecificOutputDir,firstIt,lastIt);
		}

		// write travel time/distance
		TravelTimeComperator time = new TravelTimeComperator(runDir+"/"+cloningFactor+"pct/");
		time.run();

		TravelDistanceForSimTimeExp dist = new TravelDistanceForSimTimeExp(runDir+"/"+cloningFactor+"pct/",inputFilesDir+"/network.xml.gz");
		dist.run();
	}

	private Controler getControler(LinkDynamics ld, TrafficDynamics td, String runSpecificOutputDir, boolean isUsingFastCapacityUpdate) {
		Config config = createBasicConfigSettings();
		String outPlans = inputFilesDir + "/SelectedPlans_clonedTo"+cloningFactor+".xml.gz";
		
		if (! new File(outPlans).exists() ) { // run only once and not for every case.
			UrbanDemandGenerator udg = new UrbanDemandGenerator(cloningFactor);
			udg.startProcessing(inputFilesDir);
			new PopulationWriter(udg.getPopulation()).write(outPlans);	
		}
		
//		BackwardCompatibilityForOldPlansType bcrt = new BackwardCompatibilityForOldPlansType(inputFilesDir+"/SelectedPlansOnly.xml", PatnaUtils.URBAN_MAIN_MODES);
//		bcrt.extractPlansExcludingLinkInfo();
//		bcrt.writePopOut(outPlans);

		config.plans().setInputFile(outPlans);

		config.network().setInputFile(inputFilesDir+"/network.xml.gz");
		config.counts().setInputFile(inputFilesDir+"counts/countsCarMotorbikeBike.xml");

		config.qsim().setLinkDynamics(ld);
		config.qsim().setTrafficDynamics(td);
		config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);

		if(ld.equals(LinkDynamics.SeepageQ)) {
			config.qsim().setSeepModes(Arrays.asList("bike"));
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);
		}

		config.controller().setCreateGraphs(false);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		Scenario sc = ScenarioUtils.loadScenario(config);

//		//clone persons here.
//		PersonsCloner pc = new PersonsCloner(sc);
//		pc.clonePersons(cloningFactor);
		
		PatnaVehiclesGenerator.createAndAddVehiclesToScenario(sc, PatnaUtils.URBAN_MAIN_MODES);

		final Controler controler = new Controler(sc);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory("bike", config);
		
		controler.addOverridingModule(new AbstractModule() {
			// following must be added in order to get travel time and travel disutility in the router for modes other than car
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder);
			}
		});
		controler.getScenario().getConfig().controller().setOutputDirectory(runSpecificOutputDir);
		return controler;
	}

	private Config createBasicConfigSettings(){

		Config config = ConfigUtils.createConfig();

		config.counts().setWriteCountsInterval(200);
		config.counts().setCountsScaleFactor(100/cloningFactor); 

		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(200);
		//disable writing of the following data
		config.controller().setWriteEventsInterval(200);
		config.controller().setWritePlansInterval(200);

		config.qsim().setFlowCapFactor(0.01*cloningFactor);		
		config.qsim().setStorageCapFactor(storageCapacityFactor);
		config.qsim().setEndTime(36*3600);
		config.qsim().setMainModes(PatnaUtils.URBAN_MAIN_MODES);

		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.85);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);

		StrategySettings timeAllocationMutator	= new StrategySettings();
		timeAllocationMutator.setStrategyName("TimeAllocationMutator");
		timeAllocationMutator.setWeight(0.05);

		config.replanning().addStrategySettings(expChangeBeta);
		config.replanning().addStrategySettings(reRoute);
		config.replanning().addStrategySettings(timeAllocationMutator);

		config.replanning().setFractionOfIterationsToDisableInnovation(0.8);

		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");

		{//activities --> urban
			ActivityParams workAct = new ActivityParams("work");
			workAct.setTypicalDuration(8*3600);
			config.scoring().addActivityParams(workAct);

			ActivityParams homeAct = new ActivityParams("home");
			homeAct.setTypicalDuration(12*3600);
			config.scoring().addActivityParams(homeAct);

			ActivityParams edu = new ActivityParams("educational");
			edu.setTypicalDuration(7*3600);
			config.scoring().addActivityParams(edu);

			ActivityParams soc = new ActivityParams("social");
			soc.setTypicalDuration(5*3600);
			config.scoring().addActivityParams(soc);

			ActivityParams oth = new ActivityParams("other");
			oth.setTypicalDuration(5*3600);
			config.scoring().addActivityParams(oth);

			ActivityParams unk = new ActivityParams("unknown");
			unk.setTypicalDuration(7*3600);
			config.scoring().addActivityParams(unk);
		}

		config.scoring().setMarginalUtlOfWaiting_utils_hr(0);
		config.scoring().setPerforming_utils_hr(6.0);

		// since demand is not calibrated for 10% or 100%, using all zeros, instead.
		ModeParams car = new ModeParams("car");
		car.setConstant(-0.0);
		car.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(car);

		ModeParams bike = new ModeParams("bike");
		bike.setConstant(0.0);
		bike.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(bike);

		ModeParams motorbike = new ModeParams("motorbike");
		motorbike.setConstant(-0.0);
		motorbike.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(motorbike);

		ModeParams pt = new ModeParams("pt");
		pt.setConstant(-0.0);
		pt.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(pt);

		ModeParams walk = new ModeParams("walk");
		walk.setConstant(0.0);
		walk.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(walk);

		config.routing().setNetworkModes(PatnaUtils.URBAN_MAIN_MODES);

		{
			ModeRoutingParams mrp = new ModeRoutingParams("walk");
			mrp.setTeleportedModeSpeed(4./3.6);
			config.routing().addModeRoutingParams(mrp);
		}

		{
			ModeRoutingParams mrp = new ModeRoutingParams("pt");
			mrp.setTeleportedModeSpeed(20./3.6);
			config.routing().addModeRoutingParams(mrp);
		}

		config.travelTimeCalculator().setAnalyzedModesAsString(String.join(",", PatnaUtils.URBAN_MAIN_MODES));
		config.travelTimeCalculator().setFilterModes(true);

		return config;
	}
}
