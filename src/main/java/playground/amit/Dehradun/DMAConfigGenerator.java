package playground.amit.Dehradun;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.Arrays;
import java.util.Collection;

public class DMAConfigGenerator {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String plans_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_plans.xml.gz";
    private static final String network_file = SVN_repo + "atIITR/matsim/road-network-osm/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned.xml.gz";
    private static final String config_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_config.xml.gz";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network_file);
        config.plans().setInputFile(plans_file);

        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(100);
        config.controler().setOutputDirectory("../output/");
        config.controler().setDumpDataAtEnd(true);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        config.qsim().setFlowCapFactor(2.0);
        config.qsim().setStorageCapFactor(3.0);
        config.qsim().setEndTime(30*3600.);
        Collection<String> modes = Arrays.asList(DehradunUtils.TravelModes.car.name(), DehradunUtils.TravelModes.auto.name(),
                DehradunUtils.TravelModes.motorbike.name(), DehradunUtils.TravelModes.bicycle.name());
        config.qsim().setMainModes(modes);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        PlanCalcScoreConfigGroup.ActivityParams first = new PlanCalcScoreConfigGroup.ActivityParams("FirstAct");
        first.setTypicalDuration(8*2600.);
        config.planCalcScore().addActivityParams(first);

        PlanCalcScoreConfigGroup.ActivityParams second = new PlanCalcScoreConfigGroup.ActivityParams("SecondAct");
        second.setTypicalDuration(8*2600.);
        config.planCalcScore().addActivityParams(second);

        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.car.name());
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.auto.name());
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.motorbike.name());
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.bicycle.name());

        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.bus.name()).setConstant(0.);
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.walk.name()).setConstant(0.);
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.IPT.name()).setConstant(0.);
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.rail.name()).setConstant(-1.0);

        config.plansCalcRoute().setNetworkModes(modes);
        config.plansCalcRoute().setClearingDefaultModeRoutingParams(true);
        config.plansCalcRoute().getOrCreateModeRoutingParams(DehradunUtils.TravelModes.bus.name()).setTeleportedModeFreespeedFactor(1.5);
        config.plansCalcRoute().getOrCreateModeRoutingParams(DehradunUtils.TravelModes.walk.name()).setTeleportedModeFreespeedFactor(1.0);
        config.plansCalcRoute().getOrCreateModeRoutingParams(DehradunUtils.TravelModes.IPT.name()).setTeleportedModeFreespeedFactor(1.3);
        config.plansCalcRoute().getOrCreateModeRoutingParams(DehradunUtils.TravelModes.rail.name()).setTeleportedModeFreespeedFactor(1.1);

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.25);
        config.strategy().addStrategySettings(reRoute);

        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.15);
        config.strategy().addStrategySettings(modeChoice);
        config.changeMode().setModes(new String [] {DehradunUtils.TravelModes.car.name(), DehradunUtils.TravelModes.auto.name(),
                DehradunUtils.TravelModes.motorbike.name(), DehradunUtils.TravelModes.bicycle.name(),
                DehradunUtils.TravelModes.bus.name(), DehradunUtils.TravelModes.walk.name(), DehradunUtils.TravelModes.IPT.name(),
                DehradunUtils.TravelModes.rail.name()});

        StrategyConfigGroup.StrategySettings timeMutation = new StrategyConfigGroup.StrategySettings();
        timeMutation.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
        timeMutation.setWeight(0.1);
        config.strategy().addStrategySettings(timeMutation);

        config.timeAllocationMutator().setMutationRange(3600.0*2);

        StrategyConfigGroup.StrategySettings planSelection = new StrategyConfigGroup.StrategySettings();
        planSelection.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        planSelection.setWeight(0.5);
        config.strategy().addStrategySettings(planSelection);

        config.strategy().setMaxAgentPlanMemorySize(10);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        new ConfigWriter(config).write(config_file);

    }

}
