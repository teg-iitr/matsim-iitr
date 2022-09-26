///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2016 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.amit.opdyts.equil;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Random;
//import java.util.Set;
//import com.google.common.io.Files;
//import floetteroed.opdyts.DecisionVariableRandomizer;
//import floetteroed.opdyts.ObjectiveFunction;
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
//import org.matsim.contrib.opdyts.MATSimSimulator2;
//import org.matsim.contrib.opdyts.MATSimStateFactoryImpl;
//import org.matsim.contrib.opdyts.useCases.modeChoice.EveryIterationScoringParameters;
//import org.matsim.contrib.opdyts.utils.MATSimOpdytsControler;
//import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.config.groups.VspExperimentalConfigGroup;
//import org.matsim.core.controler.AbstractModule;
//import org.matsim.core.controler.OutputDirectoryHierarchy;
//import org.matsim.core.controler.events.ShutdownEvent;
//import org.matsim.core.controler.listener.ShutdownListener;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.scoring.functions.ScoringParametersForPerson;
//import org.matsim.core.utils.io.IOUtils;
//import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
//import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
//import playground.amit.clustering.ClusterAlgorithm;
//import playground.amit.clustering.ClusterUtils;
//import playground.amit.opdyts.DistanceDistribution;
//import playground.amit.opdyts.ModeChoiceDecisionVariable;
//import playground.amit.opdyts.ModeChoiceObjectiveFunction;
//import playground.amit.opdyts.ModeChoiceRandomizer;
//import playground.amit.opdyts.OpdytsScenario;
//import playground.amit.opdyts.RandomizedUtilityParametersChoser;
//import playground.amit.opdyts.analysis.OpdytsModalStatsControlerListener;
//import playground.amit.opdyts.plots.BestSolutionVsDecisionVariableChart;
//import playground.amit.opdyts.plots.OpdytsConvergenceChart;
//import playground.amit.opdyts.teleportationModes.TeleportationODCoordAnalyzer;
//import playground.amit.opdyts.teleportationModes.Zone;
//import playground.amit.utils.FileUtils;
//
///**
// * @author amit
// */
//
//public class MatsimOpdytsEquilIntegration {
//
//	private static String EQUIL_DIR = FileUtils.RUNS_SVN+"/opdyts/equil/carPt/inputs/";
//	private static final OpdytsScenario EQUIL = OpdytsScenario.EQUIL;
//
//	public static void main(String[] args) {
//		String configFile;
//		String OUT_DIR ;
//		String relaxedPlans;
//		ModeChoiceRandomizer.ASCRandomizerStyle ascRandomizeStyle;
//		double stepSize = 1.0;
//		int iterations2Convergence = 300;
//		double selfTuningWt = 1.0;
//		int warmUpItrs = 5;
//
//		if (args.length > 0) {
//			EQUIL_DIR = args[0];
//			OUT_DIR = args[1];
//			relaxedPlans = args[2];
//			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.valueOf(args[3]);
//
//			// opdyts params
//			stepSize = Double.valueOf(args[4]);
//			iterations2Convergence = Integer.valueOf(args[5]);
//			selfTuningWt = Double.valueOf(args[6]);
//			warmUpItrs = Integer.valueOf(args[7]);
//		} else {
//			OUT_DIR = FileUtils.RUNS_SVN+"/opdyts/equil/carPt/testCalib/";
//			relaxedPlans = FileUtils.RUNS_SVN+"/opdyts/equil/carPt/relaxedPlans/output_plans.xml.gz";
//			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.axial_fixedVariation;
//		}
//
//		configFile = EQUIL_DIR+"/config.xml";
//		OUT_DIR += ascRandomizeStyle+"/";
//
//		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters
//		Config config = ConfigUtils.loadConfig(configFile, new OpdytsConfigGroup());
//		config.plans().setInputFile(relaxedPlans);
//		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things
//		config.controler().setOutputDirectory(OUT_DIR);
//
//		// from GF, every run should have a different random seed.
//		int randomSeed = new Random().nextInt(9999);
//		config.global().setRandomSeed(randomSeed);
//
//		OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.GROUP_NAME, OpdytsConfigGroup.class ) ;
//		opdytsConfigGroup.setOutputDirectory(OUT_DIR);
//		opdytsConfigGroup.setDecisionVariableStepSize(stepSize);
//		opdytsConfigGroup.setNumberOfIterationsForConvergence(iterations2Convergence);
//		opdytsConfigGroup.setSelfTuningWeight(selfTuningWt);
//		opdytsConfigGroup.setWarmUpIterations(warmUpItrs);
//
//		List<String> modes2consider = Arrays.asList("car","pt");
//
//		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//		config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
//
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//
//		//****************************** mainly opdyts settings ******************************
//		MATSimOpdytsControler<ModeChoiceDecisionVariable> opdytsControler = new MATSimOpdytsControler<>(scenario);
//
//		DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL);
//		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,distanceDistribution);
//
//		// following is the  entry point to start a matsim controler together with opdyts
//		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(),
//				scenario);
//
//		EquilZoneIdentifier equilZoneIdentifier = new EquilZoneIdentifier(scenario.getPopulation(), ClusterUtils.getBoundingBox(scenario.getNetwork()), 1000,
//				ClusterAlgorithm.ClusterType.K_MEANS);
//		Set<Zone> relevantZones = equilZoneIdentifier.getZones();
//		simulator.addSimulationStateAnalyzer(
//				new TeleportationODCoordAnalyzer.Provider(opdytsControler.getTimeDiscretization(), new HashSet<>(Arrays.asList("pt")), relevantZones, scenario));
//
//		String finalOUT_DIR = OUT_DIR;
//		simulator.addOverridingModule(new AbstractModule() {
//
//			@Override
//			public void install() {
//				// some stats
//				addControlerListenerBinding().to(KaiAnalysisListener.class);
//				addControlerListenerBinding().toInstance(stasControlerListner);
//
//				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
//
//				this.bind(ModalShareEventHandler.class);
//				this.addControlerListenerBinding().to(ModalShareControlerListener.class);
//
//				addControlerListenerBinding().toInstance(new ShutdownListener() {
//					@Override
//					public void notifyShutdown(ShutdownEvent event) {
//						// copy the state vector elements files before removing ITERS dir
//						String outDir = event.getServices().getControlerIO().getOutputPath()+"/vectorElementSizeFiles/";
//						new File(outDir).mkdirs();
//
//						int firstIt = event.getServices().getConfig().controler().getFirstIteration();
//						int lastIt = event.getServices().getConfig().controler().getLastIteration();
//						int plotEveryItr = 50;
//
//						for (int itr = firstIt+1; itr <=lastIt; itr++) {
//							if ( (itr == firstIt+1 || itr%plotEveryItr ==0) && new File(event.getServices().getControlerIO().getIterationPath(itr)).exists() ) {
//								{
//									String sourceFile = event.getServices().getControlerIO().getIterationFilename(itr,"stateVector_networkModes.txt");
//									String sinkFile =  outDir+"/"+itr+".stateVector_networkModes.txt";
//									try {
//										Files.copy(new File(sourceFile), new File(sinkFile));
//									} catch (IOException e) {
//										LogManager.getLogger(MatsimOpdytsEquilIntegration.class).warn("Data is not copied. Reason : " + e);
//									}
//								}
//								{
//									String sourceFile = event.getServices().getControlerIO().getIterationFilename(itr,"stateVector_teleportationModes.txt");
//									String sinkFile =  outDir+"/"+itr+".stateVector_teleportationModes.txt";
//									try {
//										Files.copy(new File(sourceFile), new File(sinkFile));
//									} catch (IOException e) {
//										LogManager.getLogger(MatsimOpdytsEquilIntegration.class).warn("Data is not copied. Reason : " + e);
//									}
//								}
//							}
//						}
//
//						// remove the unused iterations
//						String dir2remove = event.getServices().getControlerIO().getOutputPath()+"/ITERS/";
//						IOUtils.deleteDirectoryRecursively(new File(dir2remove).toPath());
//
//						// post-process
//						String opdytsConvergencefile = finalOUT_DIR +"/opdyts.con";
//						if (new File(opdytsConvergencefile).exists()) {
//							OpdytsConvergenceChart opdytsConvergencePlotter = new OpdytsConvergenceChart();
//							opdytsConvergencePlotter.readFile(finalOUT_DIR +"/opdyts.con");
//							opdytsConvergencePlotter.plotData(finalOUT_DIR +"/convergence.png");
//						}
//
//						BestSolutionVsDecisionVariableChart bestSolutionVsDecisionVariableChart = new BestSolutionVsDecisionVariableChart(new ArrayList<>(modes2consider));
//						bestSolutionVsDecisionVariableChart.readFile(finalOUT_DIR +"/opdyts.log");
//						bestSolutionVsDecisionVariableChart.plotData(finalOUT_DIR +"/decisionVariableVsASC.png");
//					}
//				});
//			}
//		});
//
//		// this is the objective Function which returns the value for given SimulatorState
//		// in my case, this will be the distance based modal split
//		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(distanceDistribution);
//
//		// randomize the decision variables (for e.g.\ utility parameters for modes)
//		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
//				RandomizedUtilityParametersChoser.ONLY_ASC, EQUIL, null, modes2consider, ascRandomizeStyle);
//
//		// what would be the decision variables to optimize the objective function.
//		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(), scenario,modes2consider, EQUIL);
//
//		opdytsControler.addNetworkModeOccupancyAnalyzr(simulator);
//		opdytsControler.run(simulator, decisionVariableRandomizer,  initialDecisionVariable, objectiveFunction);
//	}
//}
