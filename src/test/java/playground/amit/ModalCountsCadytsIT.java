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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.MobsimType;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.amit.mixedTraffic.counts.CountsInserter;
import playground.vsp.cadyts.multiModeCadyts.ModalCountsCadytsContext;
import playground.vsp.cadyts.multiModeCadyts.ModalCountsLinkIdentifier;
import playground.vsp.cadyts.multiModeCadyts.ModalCountsReader;
import playground.vsp.cadyts.multiModeCadyts.MultiModalCountsCadytsModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * amit after CadytsCarIT in cadyts contrib.
 */
public class ModalCountsCadytsIT {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testCalibrationAsScoring() throws IOException {

		final double beta=30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		final Config config = createTestConfig(inputDir, outputDir);

		List<String> mainModes = Arrays.asList("car","bike");
		config.qsim().setMainModes(mainModes );
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		config.controller().setLastIteration(lastIteration);

		config.scoring().setBrainExpBeta(beta);

		StrategySettings strategySettings = new StrategySettings() ;
		strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		strategySettings.setWeight(1.0);
		config.replanning().addStrategySettings(strategySettings);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(100.0/3.6);
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(50.0/3.6);
		bike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bike);

		// ===

		CountsInserter jcg = new CountsInserter();		
		jcg.processInputFile( inputDir+"/countsCarBike.txt" );
		jcg.run();
		Counts<ModalCountsLinkIdentifier> modalLinkCounts = jcg.getModalLinkCounts();
		Map<Id<ModalCountsLinkIdentifier>, ModalCountsLinkIdentifier> modalLinkContainer = jcg.getModalLinkContainer();

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new MultiModalCountsCadytsModule(modalLinkCounts, modalLinkContainer));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindScoringFunctionFactory().toInstance(new ScoringFunctionFactory() {
					@Inject
					private ScoringParametersForPerson parameters;
					@Inject private Network network;
					@Inject
					ModalCountsCadytsContext cadytsContext;
					@Override
					public ScoringFunction createNewScoringFunction(Person person) {
						final ScoringParameters params = parameters.getScoringParameters(person);

						SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
						scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
						scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
						scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

						final CadytsScoring<ModalCountsLinkIdentifier> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
						final double cadytsScoringWeight = beta*30.;
						scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
						scoringFunctionAccumulator.addScoringFunction(scoringFunction );

						return scoringFunctionAccumulator;
					}
				});
			}
		});

		controler.run();

		//scenario data  test
		Assertions.assertEquals( scenario.getNetwork().getLinks().size() , 23, "Different number of links in network.");
		Assertions.assertEquals( scenario.getNetwork().getNodes().size() , 15 , "Different number of nodes in network.");

		Assertions.assertNotNull(scenario.getPopulation(), "Population is null.");

		Assertions.assertEquals( scenario.getPopulation().getPersons().size(), 9,"Num. of persons in population is wrong.");
		Assertions.assertEquals( scenario.getConfig().counts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON, "Scale factor is wrong.");

		//counts
		Assertions.assertEquals( scenario.getConfig().counts().getCountsFileName(), inputDir + "countsCarBike.xml", "Count file is wrong.");

		// check for car
		String mode = mainModes.get(0);
		Count<ModalCountsLinkIdentifier> count =  modalLinkCounts.getCount( Id.create( mode.concat(
				ModalCountsLinkIdentifier.getModeLinkSplitter()).concat("19") , ModalCountsLinkIdentifier.class ) );
		Assertions.assertEquals("link_19", "CsId is wrong.", count.getCsLabel() );
		Assertions.assertEquals(count.getVolume(7).getValue(), 5.0 , MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( count.getMaxVolume().getValue(), 5.0 , MatsimTestUtils.EPSILON, "Max count volume is wrong.");

		String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".multiMode_hourlyCounts.txt";
		ModalCountsReader reader = new ModalCountsReader(outCounts);
		double[] simValues;
		double realValue;

		Id<Link> locId11 = Id.create(11, Link.class);
		simValues = reader.getSimulatedValues(locId11, mode);
		realValue = getCountRealValue(modalLinkCounts, locId11, mode, 7); //6-7 am
		Assertions.assertEquals( 0.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( 0.0, realValue, MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		Id<Link> locId12 = Id.create("12", Link.class);
		simValues = reader.getSimulatedValues(locId12, mode);
		realValue = getCountRealValue(modalLinkCounts, locId12, mode, 7);
		Assertions.assertEquals( 0.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals(0.0, realValue , MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		Id<Link> locId19 = Id.create("19", Link.class);
		simValues = reader.getSimulatedValues(locId19, mode);
		realValue = getCountRealValue(modalLinkCounts, locId19, mode, 7);
		Assertions.assertEquals( 5.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( 5.0, realValue, MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		Id<Link> locId21 = Id.create("21", Link.class);
		simValues = reader.getSimulatedValues(locId21, mode);
		realValue = getCountRealValue(modalLinkCounts, locId21, mode, 7);
		Assertions.assertEquals( 5.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( 5.0, realValue, MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		// check for bike 
		mode = mainModes.get(1);
		count =  modalLinkCounts.getCount( Id.create( mode.concat(ModalCountsLinkIdentifier.getModeLinkSplitter()).concat("11") , ModalCountsLinkIdentifier.class ) );
		//		Assert.assertEquals("Occupancy counts description is wrong", modalLinkCounts.getDescription(), "counts values for equil net");
		Assertions.assertEquals( count.getCsLabel() , "link_11", "CsId is wrong.");
		Assertions.assertEquals( count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON, "Max count volume is wrong.");

		outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".multiMode_hourlyCounts.txt";
		reader = new ModalCountsReader(outCounts);

		locId11 = Id.create(11, Link.class);
		simValues = reader.getSimulatedValues(locId11, mode);
		realValue = getCountRealValue(modalLinkCounts, locId11, mode, 7);
		Assertions.assertEquals( 4.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( 4.0, realValue, MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		locId12 = Id.create(12, Link.class);
		simValues = reader.getSimulatedValues(locId12, mode);
		realValue = getCountRealValue(modalLinkCounts, locId12, mode, 7);
		Assertions.assertEquals(0.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( 0.0, realValue , MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		locId19 = Id.create(19, Link.class);
		simValues = reader.getSimulatedValues(locId19, mode);
		realValue = getCountRealValue(modalLinkCounts, locId19, mode, 7);
		Assertions.assertEquals( 0.0, simValues[6], MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");
		Assertions.assertEquals( 0.0, realValue, MatsimTestUtils.EPSILON, "Volume of hour 6 is wrong");

		locId21 = Id.create(21, Link.class);
		simValues = reader.getSimulatedValues(locId21, mode);
		realValue = getCountRealValue(modalLinkCounts, locId21, mode, 8); // bike is slow; will enter link 21 after 7am
		Assertions.assertEquals( 4.0, simValues[7], MatsimTestUtils.EPSILON, "Volume of hour 7 is wrong");
		Assertions.assertEquals( 4.0, realValue, MatsimTestUtils.EPSILON, "Volume of hour 7 is wrong");
	}

	//--------------------------------------------------------------
	private double getCountRealValue(Counts<ModalCountsLinkIdentifier> counts, Id<Link> linkId, String mode, int hour) {
		Count<ModalCountsLinkIdentifier> count =  counts.getCount( Id.create( mode.concat(ModalCountsLinkIdentifier.getModeLinkSplitter()).concat(linkId.toString()) , ModalCountsLinkIdentifier.class ) );
		return count.getVolume(hour).getValue();
	}


	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		config.global().setRandomSeed(4711) ;
		config.network().setInputFile(inputDir + "network.xml") ;
		config.plans().setInputFile(inputDir + "plans.xml") ;
		config.controller().setFirstIteration(1) ;
		config.controller().setLastIteration(10) ;
		config.controller().setOutputDirectory(outputDir) ;
		config.controller().setWriteEventsInterval(1) ;
		config.controller().setMobsim(MobsimType.qsim.toString()) ;
		config.qsim().setFlowCapFactor(1.) ;
		config.qsim().setStorageCapFactor(1.) ;
		config.qsim().setStuckTime(10.) ;
		config.qsim().setRemoveStuckVehicles(false) ;
		{
			ActivityParams params = new ActivityParams("h") ;
			config.scoring().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}
		{
			ActivityParams params = new ActivityParams("w") ;
			config.scoring().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		config.counts().setInputFile(inputDir + "countsCarBike.xml");
		return config;
	}
}