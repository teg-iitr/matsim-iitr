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
import playground.amit.utils.FileUtils;

import java.util.Arrays;
import java.util.List;

public class MNControler {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();


        config.network().setInputFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/south_delhi_matsim_network.xml.gz");
        config.plans().setInputFile(  FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/MN_transitDemand_2020-11-01.xml.gz" );
        config.transit().setTransitScheduleFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/SouthDelhi_PT_Schedule.xml.gz" );
        config.transit().setVehiclesFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/OutputVehicles_MN_VR.xml.gz");
        config.transit().setUseTransit(true);
        config.controler().setOutputDirectory("./output/");


        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);

        List<String> mainModes = Arrays.asList(TransportMode.pt);
        config.qsim().setMainModes(mainModes );
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);


        PlanCalcScoreConfigGroup scoreConfigGroup= config.planCalcScore();
        //for all activities
       ActivityParams originAct =new ActivityParams ("origin");
       originAct.setTypicalDuration(12*3600.);
       scoreConfigGroup.addActivityParams(originAct);

       ActivityParams destAct =new ActivityParams ("destination");
       destAct.setTypicalDuration(9*3600.);
       scoreConfigGroup.addActivityParams(destAct);


//       PlanCalcScoreConfigGroup.ModeParams PT =  new PlanCalcScoreConfigGroup.ModeParams(TransportMode.pt);
//        PT.setConstant(0);
//        PT.setMarginalUtilityOfTraveling(-6);
//        scoreConfigGroup.addModeParams(PT);

//
//        PlansCalcRouteConfigGroup routeConfigGroup=config.plansCalcRoute();
//        PlansCalcRouteConfigGroup.ModeRoutingParams pt= new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.pt);
//        pt.setBeelineDistanceFactor(1.3);
//        pt.setTeleportedModeFreespeedFactor(1.5);
//        routeConfigGroup.addModeRoutingParams(pt);

        config.planCalcScore().addActivityParams(originAct);
        config.planCalcScore().addActivityParams(destAct);
//        config.plansCalcRoute().addModeRoutingParams(pt);
//        config.planCalcScore().addModeParams(PT);

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.3);
        config.strategy().addStrategySettings(reRoute);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // increase the capacity of tertiary links to 1500
//        scenario.getNetwork().getLinks().values().stream().filter(l->l.getCapacity()<=600.).forEach(l->l.setCapacity(1500.));
//        scenario.getNetwork().getLinks().values().stream().filter(l->l.getFreespeed()<=60./3.6).forEach(l->l.setFreespeed(60./3.6));

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.run();


	}

}
