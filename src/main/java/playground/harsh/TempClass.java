//package playground.harsh;
//import cadyts.measurements.SingleLinkMeasurement;
//import cadyts.utilities.io.tabularFileParser.TabularFileParser;
//import cadyts.utilities.misc.DynamicData;
//import org.junit.Assert;
//import org.junit.Rule;
//import org.junit.Test;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
//import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
//import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
//import org.matsim.contrib.cadyts.general.CadytsScoring;
//import org.matsim.contrib.cadyts.utils.CalibrationStatReader;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.config.ConfigWriter;
//import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
//import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
//import org.matsim.core.controler.AbstractModule;
//import org.matsim.core.controler.ControlerDefaultsModule;
//import org.matsim.core.controler.ControlerI;
//import org.matsim.core.controler.Injector;
//import org.matsim.core.controler.NewControlerModule;
//import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
//import org.matsim.core.mobsim.framework.Mobsim;
//import org.matsim.core.replanning.PlanStrategy;
//import org.matsim.core.replanning.PlanStrategyImpl;
//import org.matsim.core.scenario.ScenarioByConfigModule;
//import org.matsim.core.scoring.ScoringFunction;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//import org.matsim.core.scoring.SumScoringFunction;
//import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
//import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
//import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
//import org.matsim.core.scoring.functions.ScoringParameters;
//import org.matsim.core.scoring.functions.ScoringParametersForPerson;
//import org.matsim.counts.Count;
//import org.matsim.counts.Counts;
//import org.matsim.counts.MatsimCountsReader;
////import org.matsim.testcases.MatsimTestUtils;
//
//
//
//
//
//public class TempClass{
//	public static void main(String [] args) {
//		Config config=TempClass.createTestConfig("C:\\Users\\DELL\\git\\matsim-iitr-main\\test\\input\\playground\\agarwalamit\\ModalCountsCadytsIT\\", "C:\\Users\\DELL\\Desktop\\output3");
//		config.controler().setLastIteration(0);
//		final String CADYTS_STRATEGY_NAME = "ccc";
//		StrategySettings strategySettings = new StrategySettings(Id.create(1, StrategySettings.class));
//		strategySettings.setStrategyName(CADYTS_STRATEGY_NAME) ;
//		strategySettings.setWeight(1.0) ;
//		config.strategy().addStrategySettings(strategySettings);
//		CadytsConfigGroup cadytsCar = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
////		cadytsCar.addParam("startTime", "04:00:00");
//		cadytsCar.setStartTime( 4*3600 );
////		cadytsCar.addParam("endTime", "20:00:00");
//		cadytsCar.setEndTime( 20*3600 );
//		cadytsCar.addParam("regressionInertia", "0.95");
//		cadytsCar.addParam("useBruteForce", "true");
//		cadytsCar.addParam("minFlowStddevVehH", "8");
//		cadytsCar.addParam("preparatoryIterations", "1");
//		cadytsCar.addParam("timeBinSize", "3600");
//		new ConfigWriter(config).writeFileV2("C:\\\\Users\\\\DELL\\\\Desktop\\\\output1");
//	}
//	private static Config createTestConfig(String inputDir, String outputDir) {
//		Config config = ConfigUtils.createConfig() ;
//		config.global().setRandomSeed(4711) ;
//		config.network().setInputFile(inputDir + "network.xml") ;
//		config.plans().setInputFile(inputDir + "plans.xml") ;
//		config.controler().setFirstIteration(1) ;
//		config.controler().setLastIteration(10) ;
//		config.controler().setOutputDirectory(outputDir) ;
////		config.controler().setWriteEventsInterval(1) ;
////		config.controler().setMobsim(MobsimType.qsim.toString()) ;
////		config.qsim().setFlowCapFactor(1.) ;
////		config.qsim().setStorageCapFactor(1.) ;
////		config.qsim().setStuckTime(10.) ;
////		config.qsim().setRemoveStuckVehicles(false) ;
//
//
//
////		{
////			ActivityParams params = new ActivityParams("h") ;
////			config.planCalcScore().addActivityParams(params ) ;
////			params.setTypicalDuration(12*60*60.) ;
////		}{
////			ActivityParams params = new ActivityParams("w") ;
////			config.planCalcScore().addActivityParams(params ) ;
////			params.setTypicalDuration(8*60*60.) ;
////		}
//		config.counts().setInputFile(inputDir + "countsCarBike.xml");
//		
//		return config;
//	}
//}
