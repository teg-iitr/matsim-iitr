package playground.amit.Dehradun;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author Amit
 *
 */
public class DMAConfigGenerator {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String config_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_config.xml";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.global().setNumberOfThreads(8);

        config.network().setInputFile("DehradunMetropolitanArea_matsim_network_fromPBF_cleaned.xml.gz");
        config.plans().setInputFile("DehradunMetropolitanArea_plans_10sample.xml.gz");

        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(100);
        config.controler().setOutputDirectory("output/");
        config.controler().setDumpDataAtEnd(true);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        config.qsim().setFlowCapFactor(DehradunUtils.sampleSize);
        config.qsim().setStorageCapFactor(DehradunUtils.sampleSize*1.5);
        config.qsim().setEndTime(30*3600.);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setNumberOfThreads(8);

        Collection<String> modes = Arrays.asList(DehradunUtils.TravelModesBaseCase2017.car.name(),
                DehradunUtils.TravelModesBaseCase2017.motorbike.name());
        config.qsim().setMainModes(modes);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        PlanCalcScoreConfigGroup.ActivityParams first = new PlanCalcScoreConfigGroup.ActivityParams("FirstAct");
        first.setTypicalDuration(8*2600.);
        config.planCalcScore().addActivityParams(first);

        PlanCalcScoreConfigGroup.ActivityParams second = new PlanCalcScoreConfigGroup.ActivityParams("SecondAct");
        second.setTypicalDuration(8*2600.);
        config.planCalcScore().addActivityParams(second);

        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.car.name());
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.motorbike.name());

        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.bus.name()).setConstant(0.);
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.IPT.name()).setConstant(0.);

        config.plansCalcRoute().setNetworkModes(modes);
        config.plansCalcRoute().setClearingDefaultModeRoutingParams(true);
        config.plansCalcRoute().getOrCreateModeRoutingParams(DehradunUtils.TravelModesBaseCase2017.bus.name()).setTeleportedModeFreespeedFactor(1.5);
        config.plansCalcRoute().getOrCreateModeRoutingParams(DehradunUtils.TravelModesBaseCase2017.IPT.name()).setTeleportedModeFreespeedFactor(1.3);
        config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.2);
        config.strategy().addStrategySettings(reRoute);

        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.1);
        config.strategy().addStrategySettings(modeChoice);
        config.changeMode().setModes(new String [] {DehradunUtils.TravelModesBaseCase2017.car.name(),
                DehradunUtils.TravelModesBaseCase2017.motorbike.name(),
                DehradunUtils.TravelModesBaseCase2017.IPT.name(), DehradunUtils.TravelModesBaseCase2017.bus.name()});

        StrategyConfigGroup.StrategySettings timeMutation = new StrategyConfigGroup.StrategySettings();
        timeMutation.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
        timeMutation.setWeight(0.1);
        config.strategy().addStrategySettings(timeMutation);

        config.timeAllocationMutator().setMutationRange(3600.0*2);

        StrategyConfigGroup.StrategySettings planSelection = new StrategyConfigGroup.StrategySettings();
        planSelection.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        planSelection.setWeight(0.6);
        config.strategy().addStrategySettings(planSelection);

        config.strategy().setMaxAgentPlanMemorySize(10);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        new ConfigWriter(config).write(config_file);
    }
}
