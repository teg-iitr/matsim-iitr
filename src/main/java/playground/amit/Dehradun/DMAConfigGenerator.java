package playground.amit.Dehradun;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import playground.amit.utils.FileUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author Amit
 *
 */
public class DMAConfigGenerator {

    private static final String config_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/matsim/DehradunMetropolitanArea_config.xml";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.global().setNumberOfThreads(8);

        config.network().setInputFile("DehradunMetropolitanArea_matsim_network_fromPBF_cleaned.xml.gz");
        config.plans().setInputFile("DehradunMetropolitanArea_plans_10sample.xml.gz");

        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(100);
        config.controller().setOutputDirectory("output/");
        config.controller().setDumpDataAtEnd(true);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

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

        ScoringConfigGroup.ActivityParams first = new ScoringConfigGroup.ActivityParams("FirstAct");
        first.setTypicalDuration(8*2600.);
        config.scoring().addActivityParams(first);

        ScoringConfigGroup.ActivityParams second = new ScoringConfigGroup.ActivityParams("SecondAct");
        second.setTypicalDuration(8*2600.);
        config.scoring().addActivityParams(second);

        config.scoring().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.car.name());
        config.scoring().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.motorbike.name());

        config.scoring().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.bus.name()).setConstant(0.);
        config.scoring().getOrCreateModeParams(DehradunUtils.TravelModesBaseCase2017.IPT.name()).setConstant(0.);

        config.routing().setNetworkModes(modes);
        config.routing().setClearingDefaultModeRoutingParams(true);
        config.routing().getOrCreateModeRoutingParams(DehradunUtils.TravelModesBaseCase2017.bus.name()).setTeleportedModeFreespeedFactor(1.5);
        config.routing().getOrCreateModeRoutingParams(DehradunUtils.TravelModesBaseCase2017.IPT.name()).setTeleportedModeFreespeedFactor(1.3);
        config.scoring().setFractionOfIterationsToStartScoreMSA(0.8);

        ReplanningConfigGroup.StrategySettings reRoute = new ReplanningConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.2);
        config.replanning().addStrategySettings(reRoute);

        ReplanningConfigGroup.StrategySettings modeChoice = new ReplanningConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.1);
        config.replanning().addStrategySettings(modeChoice);
        config.changeMode().setModes(new String [] {DehradunUtils.TravelModesBaseCase2017.car.name(),
                DehradunUtils.TravelModesBaseCase2017.motorbike.name(),
                DehradunUtils.TravelModesBaseCase2017.IPT.name(), DehradunUtils.TravelModesBaseCase2017.bus.name()});

        ReplanningConfigGroup.StrategySettings timeMutation = new ReplanningConfigGroup.StrategySettings();
        timeMutation.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
        timeMutation.setWeight(0.1);
        config.replanning().addStrategySettings(timeMutation);

        config.timeAllocationMutator().setMutationRange(3600.0*2);

        ReplanningConfigGroup.StrategySettings planSelection = new ReplanningConfigGroup.StrategySettings();
        planSelection.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        planSelection.setWeight(0.6);
        config.replanning().addStrategySettings(planSelection);

        config.replanning().setMaxAgentPlanMemorySize(10);
        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        new ConfigWriter(config).write(config_file);
    }
}
