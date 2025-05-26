package playground.amit.Chandigarh;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

import com.google.inject.Inject;
public class CadytsInt{
	private static final String network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone_fixed.xml.gz";
	private static final String plans = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_plans_test.xml.gz";
	private static final String countsFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_counts.xml.gz";
	private static final String output = "./Ch_output/";
    
	public static void main(String [] args) {

		Config config = ConfigUtils.createConfig();

        config.network().setInputFile(network);

        config.plans().setInputFile(plans);

        config.controller().setOutputDirectory(output);
        config.controller().setLastIteration(50);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setDumpDataAtEnd(true);

        config.counts().setInputFile(countsFile);
        config.counts().setWriteCountsInterval(5);
        config.counts().setOutputFormat("all");

        ScoringConfigGroup.ActivityParams startAct = new ScoringConfigGroup.ActivityParams(ChandigarhConstants.start_act_type);
        startAct.setTypicalDuration(6*3600.);
        ScoringConfigGroup.ActivityParams endAct = new ScoringConfigGroup.ActivityParams(ChandigarhConstants.end_act_type);
        endAct.setTypicalDuration(16*3600.);
        config.scoring().addActivityParams(startAct);
        config.scoring().addActivityParams(endAct);
		config.scoring().setFractionOfIterationsToStartScoreMSA(0.7);

        ReplanningConfigGroup.StrategySettings reRoute = new ReplanningConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute); // though, this must not have any effect.
        reRoute.setWeight(0.2);
        config.replanning().addStrategySettings(reRoute);

		ReplanningConfigGroup.StrategySettings changeExpBeta = new ReplanningConfigGroup.StrategySettings();
		changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		changeExpBeta.setWeight(0.8);
		config.replanning().addStrategySettings(changeExpBeta);
		config.replanning().setFractionOfIterationsToDisableInnovation(0.7);
		config.replanning().setMaxAgentPlanMemorySize(10);

		ModeParams carParams = new ModeParams(TransportMode.car);
		carParams.setMarginalUtilityOfTraveling(0.);
		carParams.setConstant(0.);
		carParams.setMonetaryDistanceRate(0.);
		carParams.setMarginalUtilityOfDistance(0.);
		config.scoring().addModeParams(carParams);

		CadytsConfigGroup cadytsConfigGroup = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfigGroup.setStartTime(9*3600);
		cadytsConfigGroup.setEndTime(11*3600-1);

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		scenario.getNetwork().getLinks().values().stream().filter(l->l.getCapacity()<=600.).forEach(l->l.setCapacity(1500.));
        scenario.getNetwork().getLinks().values().stream().filter(l->l.getFreespeed()<=60./3.6).forEach(l->l.setFreespeed(60./3.6));
		// ---

		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CadytsCarModule());

		// include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final double cadytsWeight = 30.;
			@Inject CadytsContext cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork(), config.transit().getTransitModes()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsWeight * config.scoring().getBrainExpBeta()) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
		controler.run() ;
	}
}