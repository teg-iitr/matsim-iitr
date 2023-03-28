package playground.shivam.signals.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import playground.shivam.signals.SignalUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CreateFixedConfig {
    public static Config defineFixedConfig(String outputDirectory) throws IOException {
        Config config = ConfigUtils.createConfig();
        // create the path to the output directory if it does not exist yet
        Files.createDirectories(Paths.get(outputDirectory));

        config.controler().setOutputDirectory(outputDirectory);
        config.controler().setLastIteration(100);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());

        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);


        config.travelTimeCalculator().setMaxTime(5 * 60 * 60);

        config.qsim().setStartTime(0);
        config.qsim().setEndTime(5 * 60 * 60);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setNodeOffset(20.0);
        config.qsim().setMainModes(SignalUtils.MAIN_MODES);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        config.qsim().setUseLanes(true);

        config.plansCalcRoute().setNetworkModes(SignalUtils.MAIN_MODES);

        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(SignalUtils.MAIN_MODES)));

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

            StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
            reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
            reRoute.setWeight(0.15);
            scg.addStrategySettings(reRoute);

            StrategyConfigGroup.StrategySettings timeAllocationMutator = new StrategyConfigGroup.StrategySettings();
            timeAllocationMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
            timeAllocationMutator.setWeight(0.05);
            scg.addStrategySettings(timeAllocationMutator);

            config.timeAllocationMutator().setAffectingDuration(false);

            StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
            modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
            modeChoice.setWeight(0.5);
            scg.addStrategySettings(modeChoice);

            config.changeMode().setModes(SignalUtils.MAIN_MODES.toArray(new String[0]));
        }

        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

        // from Jaipur controller
        PlanCalcScoreConfigGroup.ModeParams car = new PlanCalcScoreConfigGroup.ModeParams("car");
        car.setMarginalUtilityOfTraveling(-6.0);
        car.setConstant(-0.5);
        config.planCalcScore().addModeParams(car);

        PlanCalcScoreConfigGroup.ModeParams truck = new PlanCalcScoreConfigGroup.ModeParams("truck"); // using default for them.
        truck.setConstant(-1.0);
        truck.setMarginalUtilityOfTraveling(-7.0);
        config.planCalcScore().addModeParams(truck);

        QSimConfigGroup qsim = config.qsim();
        List<String> mainModes = Arrays.asList("car", "truck");
        qsim.setMainModes(mainModes);

        ChangeModeConfigGroup changeTripMode = config.changeMode();
        changeTripMode.setModes(new String[]{"car", "truck"});
        config.plansCalcRoute().setNetworkModes(mainModes);

        //write config to file
        String configFile = outputDirectory + "config.xml";
        ConfigWriter configWriter = new ConfigWriter(config);
        configWriter.write(configFile);
        return config;
    }
}
