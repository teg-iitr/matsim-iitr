package playground.amit.Delhi.MalviyaNagarPT;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import playground.amit.utils.FileUtils;

import java.util.HashSet;
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

//        Set<String> modeW = new HashSet<>();
//        modeW.add("walk");
//        config.qsim().setMainModes(modeW);


        config.transit().setBoardingAcceptance(TransitConfigGroup.BoardingAcceptance.checkStopOnly);

        config.controller().setOutputDirectory("./output/");

        config.controller().setLastIteration(10);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setDumpDataAtEnd(true);

        ScoringConfigGroup scoreConfigGroup= config.scoring();

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
           ScoringConfigGroup.ModeParams ptParams =  new ScoringConfigGroup.ModeParams(TransportMode.pt);
           ptParams.setConstant(-3);
           ptParams.setMarginalUtilityOfTraveling(-6);
           scoreConfigGroup.addModeParams(ptParams);
           config.routing().removeModeRoutingParams("pt");

//
//            ScoringConfigGroup.ModeParams walkParams = new ScoringConfigGroup.ModeParams(TransportMode.walk);
//            walkParams.setConstant(-1);
//            walkParams.setMarginalUtilityOfTraveling(-12);
//            scoreConfigGroup.addModeParams(walkParams);
//            config.routing().removeModeRoutingParams("walk");


        ReplanningConfigGroup.StrategySettings reRoute = new ReplanningConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.3);
        config.replanning().addStrategySettings(reRoute);
        ReplanningConfigGroup strategy= config.replanning();
//        strategy.setFractionOfIterationsToDisableInnovation(0.8);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.run();
	}

}
