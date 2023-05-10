package playground.shivam.signals.config;

import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import playground.shivam.signals.SignalUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

import static playground.shivam.signals.SignalUtils.*;

public class CreateConfig {

    public static Config defineConfig(String outputDirectory, boolean adaptive) throws IOException {
        Config config = ConfigUtils.createConfig();

        // create the path to the output directory if it does not exist yet
        Files.createDirectories(Paths.get(outputDirectory));

        config.controler().setOutputDirectory(outputDirectory);

        config.controler().setLastIteration(ITERATION);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());

        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);


        config.travelTimeCalculator().setMaxTime(24 * 60 * 60);

        config.qsim().setStartTime(0);
        config.qsim().setEndTime(24 * 60 * 60);
        //config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setMainModes(SignalUtils.MAIN_MODES);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setStorageCapFactor(STORAGE_CAPACITY_FACTOR);
        config.qsim().setFlowCapFactor(FLOW_CAPACITY_FACTOR);

        config.plansCalcRoute().setNetworkModes(SignalUtils.MAIN_MODES);

        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(SignalUtils.MAIN_MODES)));
        config.travelTimeCalculator().setCalculateLinkTravelTimes(true);

        config.qsim().setUseLanes(true);

        PlanCalcScoreConfigGroup.ActivityParams dummyAct = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
        dummyAct.setTypicalDuration(3600);
        dummyAct.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(dummyAct);

        StrategyConfigGroup scg = config.strategy();
        {
            StrategyConfigGroup.StrategySettings expChangeBeta = new StrategyConfigGroup.StrategySettings();
            expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
            expChangeBeta.setWeight(0.7);
            scg.addStrategySettings(expChangeBeta);
//
//            StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
//            reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
//            reRoute.setWeight(0.15);
//            scg.addStrategySettings(reRoute);
//
//            StrategyConfigGroup.StrategySettings timeAllocationMutator = new StrategyConfigGroup.StrategySettings();
//            timeAllocationMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
//            timeAllocationMutator.setWeight(0.1);
//            scg.addStrategySettings(timeAllocationMutator);
//
//            config.timeAllocationMutator().setAffectingDuration(false);

//            StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
//            modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
//            modeChoice.setWeight(0.5);
//            scg.addStrategySettings(modeChoice);

//            config.changeMode().setModes(SignalUtils.MAIN_MODES.toArray(new String[0]));
        }

        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);
        // from Jaipur controller
        PlanCalcScoreConfigGroup.ModeParams car = new PlanCalcScoreConfigGroup.ModeParams("car");
        car.setMarginalUtilityOfTraveling(-6.0);
        car.setConstant(-0.5);
        config.planCalcScore().addModeParams(car);

//        PlanCalcScoreConfigGroup.ModeParams truck = new PlanCalcScoreConfigGroup.ModeParams("truck"); // using default for them.
//        truck.setConstant(-1.0);
//        truck.setMarginalUtilityOfTraveling(-7.0);
//        config.planCalcScore().addModeParams(truck);

        if (adaptive) {
            LaemmerConfigGroup laemmerConfigGroup = ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.GROUP_NAME, LaemmerConfigGroup.class);
            laemmerConfigGroup.setDesiredCycleTime(CYCLE);
            laemmerConfigGroup.setMinGreenTime(10);
            //laemmerConfigGroup.setShortenStabilizationAfterIntergreenTime(true);
            laemmerConfigGroup.setDetermineMaxLoadForTIdleGroupedBySignals(true);
            //laemmerConfigGroup.setMinGreenTimeForNonGrowingQueues(true);
            laemmerConfigGroup.setCheckDownstream(true);
            laemmerConfigGroup.setActiveStabilizationStrategy(LaemmerConfigGroup.StabilizationStrategy.HEURISTIC);
            laemmerConfigGroup.setActiveRegime(LaemmerConfigGroup.Regime.COMBINED);
            config.getModules().put(LaemmerConfigGroup.GROUP_NAME, laemmerConfigGroup);
        }

        //write config to file
        String configFile = outputDirectory + "config.xml";
        ConfigWriter configWriter = new ConfigWriter(config);
        configWriter.write(configFile);

        return config;
    }



}
