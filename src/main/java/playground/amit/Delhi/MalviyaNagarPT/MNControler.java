package playground.amit.Delhi.MalviyaNagarPT;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import playground.amit.utils.FileUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MNControler {

	public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();

        config.network().setInputFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/south_delhi_matsim_network.xml.gz");

        config.plans().setInputFile(  FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/MN_transitDemand_2020-11-01.xml.gz" );

        config.transit().setTransitScheduleFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/SouthDelhi_PT_Schedule.xml.gz" );
        config.transit().setVehiclesFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/TransitVehicles_MN.xml.gz");
        config.transit().setUseTransit(true);
        Set<String> transitModes = new HashSet<>();
        transitModes.add("pt");
        config.transit().setTransitModes(transitModes);
        config.transit().setBoardingAcceptance(TransitConfigGroup.BoardingAcceptance.checkStopOnly);

//        Set<String> modeW = new HashSet<>();
//        modeW.add("walk");
//        config.qsim().setMainModes(modeW);



        config.controler().setOutputDirectory("./output/");

        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);

        PlanCalcScoreConfigGroup scoreConfigGroup= config.planCalcScore();

        QSimConfigGroup qsim =config.qsim();
        qsim.setEndTime(12*3600.);

           //for all activities
           ActivityParams originAct =new ActivityParams ("origin");
           originAct.setTypicalDuration(12*3600.);
           scoreConfigGroup.addActivityParams(originAct);

           ActivityParams destAct =new ActivityParams ("destination");
           destAct.setTypicalDuration(9*3600.);
           scoreConfigGroup.addActivityParams(destAct);

           //for modes
           PlanCalcScoreConfigGroup.ModeParams ptParams =  new PlanCalcScoreConfigGroup.ModeParams(TransportMode.pt);
           ptParams.setConstant(-3);
           ptParams.setMarginalUtilityOfTraveling(-6);
           scoreConfigGroup.addModeParams(ptParams);
           config.plansCalcRoute().removeModeRoutingParams("pt");


//            PlanCalcScoreConfigGroup.ModeParams walkParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.walk);
//            walkParams.setConstant(-1);
//            walkParams.setMarginalUtilityOfTraveling(-12);
//            scoreConfigGroup.addModeParams(walkParams);
//            config.plansCalcRoute().removeModeRoutingParams("walk");


        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.3);
        config.strategy().addStrategySettings(reRoute);
        StrategyConfigGroup strategy= config.strategy();
//        strategy.setFractionOfIterationsToDisableInnovation(0.8);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.run();
	}

}
