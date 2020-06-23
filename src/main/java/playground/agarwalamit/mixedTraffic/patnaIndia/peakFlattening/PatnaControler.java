package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.VehicleUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

public class PatnaControler {

    public static void main(String[] args) {
        String outputDir =  "../../patna/output/bau/";
        String inputConfig = "../../patna/input/configBaseCaseCtd_June2020.xml";

        Config config = ConfigUtils.loadConfig(inputConfig);
        config.controler().setOutputDirectory(outputDir);

        //==
        // after calibration;  departure time is fixed for urban; check if time choice is not present
        Collection<StrategyConfigGroup.StrategySettings> strategySettings = config.strategy().getStrategySettings();
        for(StrategyConfigGroup.StrategySettings ss : strategySettings){ // departure time is fixed now.
            if ( ss.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString()) ) {
                throw new RuntimeException("Time mutation should not be used; fixed departure time must be used after cadyts calibration.");
            }
        }
        //==

        config.vehicles().setVehiclesFile(null); // vehicle types are added from vehicle file later.
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(50);

        config.travelTimeCalculator().setFilterModes(true);
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(PatnaUtils.ALL_MAIN_MODES)));
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        String vehiclesFile = "../../patna/input/output_vehicles.xml.gz";
        // following is required to extract only vehicle types and not vehicle info. Amit Nov 2016
        VehicleUtils.addVehiclesToScenarioFromVehicleFile(vehiclesFile, scenario);

        if (!scenario.getVehicles().getVehicles().isEmpty()) throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
                QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
        scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        final Controler controler = new Controler(scenario);

        controler.getConfig().controler().setDumpDataAtEnd(true);
        controler.getConfig().strategy().setMaxAgentPlanMemorySize(10);

        controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

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
        PlanCalcScoreConfigGroup.ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
        mp.setMarginalUtilityOfDistance(0.0);
        mp.setMonetaryDistanceRate(0.0);

        // add income dependent scoring function factory
        addScoringFunction(controler);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);
            }
        });

        controler.run();

        // delete unnecessary iterations folder here.
        int firstIt = controler.getConfig().controler().getFirstIteration();
        int lastIt = controler.getConfig().controler().getLastIteration();
        FileUtils.deleteIntermediateIterations(outputDir,firstIt,lastIt);

        new File(outputDir+"/analysis/").mkdir();
        String outputEventsFile = outputDir+"/output_events.xml.gz";
        // write some default analysis
        String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
        ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
        mtta.run();
        mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

        ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
        msc.run();
        msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

        StatsWriter.run(outputDir);
    }

    public static void addScoringFunction(final Controler controler){
        // scoring function
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( controler.getScenario() );
            @Inject
            Network network;
            @Inject
            Population population;
            @Inject
            PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
            @Inject
            ScenarioConfigGroup scenarioConfig;
            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                final ScoringParameters params = parameters.getScoringParameters( person );

                SumScoringFunction sumScoringFunction = new SumScoringFunction();
                sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
                sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                Double ratioOfInc = 1.0;

                if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
                    Double monthlyInc = (Double) person.getAttributes().getAttribute(PatnaUtils.INCOME_ATTRIBUTE);
                    Double avgInc = PatnaUtils.MEADIAM_INCOME;
                    ratioOfInc = avgInc/monthlyInc;
                }

                planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );

                PlanCalcScoreConfigGroup.ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations

                ScoringParameters.Builder builder = new ScoringParameters.Builder(
                        planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
                final ScoringParameters modifiedParams = builder.build();

                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
                sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
                return sumScoringFunction;
            }
        });
    }

}
