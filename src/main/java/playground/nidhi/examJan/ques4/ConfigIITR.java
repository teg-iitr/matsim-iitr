package playground.nidhi.examJan.ques4;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import java.util.Arrays;

public class ConfigIITR {
    public static void main(String[] args) {
        Config config= ConfigUtils.createConfig();
        config.network().setInputFile("C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_matsim_network.xml.gz");
        config.plans().setInputFile( "C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_population.xml.gz");
        config.vehicles().setVehiclesFile("C:\\Users\\Nidhi\\Workspace\\MATSimData\\TEST\\iitr_vehicle.xml.gz");

        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);

        QSimConfigGroup qsim =config.qsim();
//        qsim.setEndTime(14*3600.);

        qsim.setMainModes(Arrays.asList(TransportMode.car));

//PlanCalcScore config group
        PlanCalcScoreConfigGroup scoreConfigGroup= config.planCalcScore();
        //for all activities
        PlanCalcScoreConfigGroup.ActivityParams home =new PlanCalcScoreConfigGroup.ActivityParams("origin");
        home.setTypicalDuration(22*3600.);
        scoreConfigGroup.addActivityParams(home);

        PlanCalcScoreConfigGroup.ActivityParams work =new PlanCalcScoreConfigGroup.ActivityParams("destination");
        work.setTypicalDuration(10*3600.);
        scoreConfigGroup.addActivityParams(work);

        //for all modes
        PlanCalcScoreConfigGroup.ModeParams car =  new PlanCalcScoreConfigGroup.ModeParams(TransportMode.car);
        car.setConstant(0);
        car.setMarginalUtilityOfTraveling(-6);
        scoreConfigGroup.addModeParams(car);


//  /PlanCalcRoute config group
        PlansCalcRouteConfigGroup routeConfigGroup=config.plansCalcRoute();
        routeConfigGroup.setNetworkModes(Arrays.asList(TransportMode.car));

        PlansCalcRouteConfigGroup.ModeRoutingParams walk= new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.walk);
        walk.setBeelineDistanceFactor(1.0);
        walk.setTeleportedModeFreespeedFactor(2.0);
        routeConfigGroup.addModeRoutingParams(walk);



        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.3);
        config.strategy().addStrategySettings(reRoute);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

    }
}
