/* *********************************************************************** *
 * project: org.matsim.*
 * DreieckNModes													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.amit.fundamentalDiagrams.core;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.amit.fundamentalDiagrams.core.pointsToRun.FDAgentsGenerator;
import playground.amit.fundamentalDiagrams.core.pointsToRun.FDAgentsGeneratorControlerListner;
import playground.amit.fundamentalDiagrams.core.pointsToRun.FDDistributionAgentsGeneratorImpl;
import playground.amit.fundamentalDiagrams.core.pointsToRun.FDAgentsGeneratorImpl;
import playground.shivam.trafficChar.TrafficCharQSimModule;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

/**
 * @author amit after ssix
 */

public class FDModule extends AbstractModule {

	public static final Logger LOG = Logger.getLogger(FDModule.class);

	public static final double MAX_ACT_END_TIME = 1800.;

	private String runDir ;
	static boolean isUsingLiveOTFVis = false;
	private final Scenario scenario;
	private static FDNetworkGenerator fdNetworkGenerator;

	private String[] travelModes;
	private final FDConfigGroup FDConfigGroup;

	public FDModule(final Scenario scenario, String linkDynamics){
		FDConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FDConfigGroup.class);
		fdNetworkGenerator = new FDNetworkGenerator(FDConfigGroup);
		this.scenario = scenario;
		fdNetworkGenerator.createNetwork(this.scenario.getNetwork());

		changeLinkDynamics(scenario.getNetwork(), linkDynamics);
		checkForConsistencyAndInitialize();
		setUpConfig();

		new ConfigWriter(scenario.getConfig()).write(this.runDir+"/output_config.xml");
		new NetworkWriter(scenario.getNetwork()).write(this.runDir+"/output_network.xml");
		new MatsimVehicleWriter(scenario.getVehicles()).writeFile(this.runDir+"/output_vehicles.xml");
		new PopulationWriter(scenario.getPopulation()).write(this.runDir + "/output_plans.xml");
	}
	public FDModule(final Scenario scenario){
		FDConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FDConfigGroup.class);
		fdNetworkGenerator = new FDNetworkGenerator(FDConfigGroup);
		this.scenario = scenario;
		fdNetworkGenerator.createNetwork(this.scenario.getNetwork());
		checkForConsistencyAndInitialize();
		setUpConfig();

		new ConfigWriter(scenario.getConfig()).write(this.runDir+"/output_config.xml");
		new NetworkWriter(scenario.getNetwork()).write(this.runDir+"/output_network.xml");
		new MatsimVehicleWriter(scenario.getVehicles()).writeFile(this.runDir+"/output_vehicles.xml");
		new PopulationWriter(scenario.getPopulation()).write(this.runDir + "/output_plans.xml");
	}
	private void changeLinkDynamics(Network network, String linkDynamics) {
		for (Link link: network.getLinks().values()) {
			if (link.getId().equals(Id.createLinkId("1")) | link.getId().equals(Id.createLinkId("2")))
				link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, linkDynamics);
			else
				link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, TrafficCharConfigGroup.ROAD_TYPE_DEFAULT);
		}
	}
	private void checkForConsistencyAndInitialize(){
		this.runDir = scenario.getConfig().controler().getOutputDirectory();
		if(runDir==null) throw new RuntimeException("Location to write data for FD is not set. Aborting...");

		createLogFile();

		if(FDConfigGroup.getReduceDataPointsByFactor() != 1) {
			LOG.info("===============");
			LOG.warn("Number of modes for each mode type in FD will be reduced by a factor of "+ FDConfigGroup.getReduceDataPointsByFactor()+". This will not change the traffic dynamics.");
			if (scenario.getConfig().qsim().getTrafficDynamics()== QSimConfigGroup.TrafficDynamics.queue) LOG.warn("Make sure this is what you want because it will be more likely to have less or no points in congested regime in absence of queue model with holes.");
			LOG.info("===============");
		}

		travelModes = scenario.getConfig().qsim().getMainModes().toArray(new String[0]);

		if (scenario.getVehicles().getVehicleTypes().isEmpty()) {
			if (travelModes.length==1 && travelModes [0].equals("car")) {
				LOG.warn("No vehicle information is provided for "+this.travelModes[0]+". Using default vehicle (i.e. car) with maximum speed same as" +
						"allowed speed on the link.");

				VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
            	car.setPcuEquivalents(1.0);
            	car.setMaximumVelocity( FDConfigGroup.getTrackLinkSpeed() );
            	scenario.getVehicles().addVehicleType(car);
			} else {
				throw new RuntimeException("Vehicle type information for modes "+ Arrays.toString(travelModes)+" is not provided. Aborting...");
			}
		}

		if (scenario.getConfig().controler().getOverwriteFileSetting().equals(OverwriteFileSetting.deleteDirectoryIfExists)) {
			LOG.warn("Overwrite file setting is set to "+scenario.getConfig().controler().getOverwriteFileSetting() 
					+ ", which will also remove the fundamental diagram data file. Setting it back to "+OverwriteFileSetting.overwriteExistingFiles);
			scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		}
	}

	private void setUpConfig() {
		// TODO: following need to updated.
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		// required when using controler
		PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
		home.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(home);

		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(work);

		scenario.getConfig().controler().setCreateGraphs(false);
		scenario.getConfig().controler().setDumpDataAtEnd(true);

		scenario.getConfig().qsim().setEndTime(100*3600.); // qsim should not go beyond 100 hrs it stability is not achieved.

		// following is necessary, in order to achieve the data points at high density
		if (this.travelModes.length==1 && this.travelModes[0].equals("car"))
			scenario.getConfig().qsim().setStuckTime(60.);
		else  if (this.travelModes.length==1 && this.travelModes[0].equals("truck"))
			scenario.getConfig().qsim().setStuckTime(180.);

		//TODO probably, following is not required anymore.
//		if ( scenario.getConfig().network().isTimeVariantNetwork() ) {
//			Network netImpl = scenario.getNetwork();
//			netImpl.getFactory().(new VariableIntervalTimeVariantLinkFactory());
//		}

		StrategyConfigGroup.StrategySettings ss = new StrategyConfigGroup.StrategySettings();
		ss.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
		ss.setWeight(1.0);
		scenario.getConfig().strategy().addStrategySettings(ss);
		scenario.getConfig().strategy().setFractionOfIterationsToDisableInnovation(1.0);
	}

	@Override
	public void install() {
		this.bind(FDNetworkGenerator.class).toInstance(fdNetworkGenerator); // required for FDTrackMobsimAgent
//		this.bind(PopulationAgentSource.class).asEagerSingleton();
		this.bindMobsim().toProvider(FDQSimProvider.class);

		this.bind(GlobalFlowDynamicsUpdator.class).asEagerSingleton(); //provide same instance everywhere
		this.addEventHandlerBinding().to(GlobalFlowDynamicsUpdator.class);

		if (FDConfigGroup.isRunningDistribution()) {
			this.bind(FDAgentsGenerator.class).to(FDDistributionAgentsGeneratorImpl.class);
		} else {
			this.bind(FDAgentsGenerator.class).to(FDAgentsGeneratorImpl.class);
		}

		this.bind(FDAgentsGeneratorControlerListner.class).asEagerSingleton(); //probably, not really necessary, since there is no shared information wherever it is required.
		this.addControlerListenerBinding().to(FDAgentsGeneratorControlerListner.class);
		this.bind(TerminationCriterion.class).to(FDAgentsGeneratorControlerListner.class);

		this.bind(FDDataWriter.class).asEagerSingleton();// necessary to access constructor arguments
		this.addControlerListenerBinding().to(FDDataWriter.class);

		this.bind(FDStabilityTester.class).asEagerSingleton();
		this.bind(FDDataContainer.class).asEagerSingleton();
	}

	private void createLogFile(){
		// do check the logging here. The logger has been changed. Amit, May'20
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		final boolean appendToExistingFile = false;

		String conversionPattern = " %d %4p %c{1} %L %m%n";
		PatternLayout layout =  PatternLayout.newBuilder().withPattern(conversionPattern).build();
		String fd_log = "/fdlogfile.log";
		String filename = runDir + fd_log;
		FileAppender appender = FileAppender.newBuilder().setName(fd_log).setLayout(layout).withFileName(filename).withAppend(appendToExistingFile).build();;
		appender.start();
		config.getRootLogger().addAppender(appender, Level.ALL, null);
		ctx.updateLoggers();
	}

}
