package playground.shivam.Dadar.evacuation.scenarios;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.shivam.Dadar.evacuation.DadarUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;

import java.util.HashSet;

import static playground.shivam.Dadar.evacuation.DadarUtils.*;

public class DadarWOTeleportedModes {
    public static void createDadarWOTeleportedModesConfig() {
        Config config = ConfigUtils.createConfig();

        config.network().setInputFile(MATSIM_NETWORK);

        config.plans().setInputFile(MATSIM_PLANS);
        config.plans().setRemovingUnneccessaryPlanAttributes(false);

        config.controler().setLastIteration(ITERATIONS);
        config.controler().setOutputDirectory(OUTPUT_WO_TELEPORTED_MODES);
        config.controler().setDumpDataAtEnd(true);
        config.controler().setCreateGraphs(false);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(10);
        config.controler().setWritePlansInterval(10);

        config.qsim().setSnapshotPeriod(5 * 60);
        config.qsim().setEndTime(30 * 3600);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setMainModes(MAIN_MODES);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setNumberOfThreads(4);

        //TODO we are using 0.5 just to see some congestion.
        config.qsim().setFlowCapFactor(0.5);
        config.qsim().setStorageCapFactor(0.5);

        config.vspExperimental().setWritingOutputEvents(true);
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore);

        config.plansCalcRoute().setNetworkModes(MAIN_MODES);

        config.counts().setWriteCountsInterval(5);
        //config.counts().setInputFile(MATSIM_COUNTS);
        config.counts().setFilterModes(true);
        config.counts().setOutputFormat("txt");
        config.counts().setAverageCountsOverIterations(5);

        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(DadarUtils.MAIN_MODES)));

        PlanCalcScoreConfigGroup pcg = config.planCalcScore();
        PlansCalcRouteConfigGroup ptg = config.plansCalcRoute();

        {
            PlanCalcScoreConfigGroup.ActivityParams originAct = new PlanCalcScoreConfigGroup.ActivityParams(ORIGIN_ACTIVITY);
            originAct.setScoringThisActivityAtAll(false);
            originAct.setTypicalDuration(16 * 3600);
            pcg.addActivityParams(originAct);

            PlanCalcScoreConfigGroup.ActivityParams destinationAct = new PlanCalcScoreConfigGroup.ActivityParams(DESTINATION_ACTIVITY);
            destinationAct.setScoringThisActivityAtAll(false);
            destinationAct.setTypicalDuration(8 * 3600);
            pcg.addActivityParams(destinationAct);
        }

        for (String mode : MAIN_MODES) {
            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(mode);
            modeParams.setConstant(-1 * DadarUtils.setConstant(mode));
            modeParams.setMarginalUtilityOfTraveling(-1 * DadarUtils.setMarginalUtilityOfTraveling(mode));
            modeParams.setMarginalUtilityOfDistance(-1 * DadarUtils.setMarginalUtilityOfDistance(mode));
            pcg.addModeParams(modeParams);
        }

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
            modeChoice.setWeight(0.1);
            scg.addStrategySettings(modeChoice);

            config.changeMode().setModes(DadarUtils.MAIN_MODES.toArray(new String[DadarUtils.MAIN_MODES.size()]));
        }

        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);


        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();

        for (String mode : MAIN_MODES) {
            VehicleType veh = VehicleUtils.createVehicleType(Id.create(mode, VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.setNetworkMode(mode);
            vehicles.addVehicleType(veh);
        }

        Controler controler = new Controler(scenario);

        controler.getConfig().strategy().setMaxAgentPlanMemorySize(5);

        controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations

            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
            }
        });

        controler.run();
    }
}
