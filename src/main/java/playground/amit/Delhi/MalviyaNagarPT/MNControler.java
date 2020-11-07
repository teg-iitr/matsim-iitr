package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.utils.FileUtils;

public class MNControler {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();

        config.network().setInputFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/south_delhi_matsim_network.xml.gz");
        config.plans().setInputFile(  FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/MN_transitDemand_2020-11-01.xml.gz" );
        config.controler().setOutputDirectory("./output/");


        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);
        
        PlanCalcScoreConfigGroup scoreConfigGroup= config.planCalcScore();
        //for all activities
       ActivityParams originAct =new ActivityParams ("origin");
       originAct.setTypicalDuration(12*3600.);
       scoreConfigGroup.addActivityParams(originAct);

       ActivityParams destAct =new ActivityParams ("destination");
       destAct.setTypicalDuration(9*3600.);
       scoreConfigGroup.addActivityParams(destAct);

        config.planCalcScore().addActivityParams(originAct);
        config.planCalcScore().addActivityParams(destAct);

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.3);
        config.strategy().addStrategySettings(reRoute);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // increase the capacity of tertiary links to 1500
//        scenario.getNetwork().getLinks().values().stream().filter(l->l.getCapacity()<=600.).forEach(l->l.setCapacity(1500.));
//        scenario.getNetwork().getLinks().values().stream().filter(l->l.getFreespeed()<=60./3.6).forEach(l->l.setFreespeed(60./3.6));

        new Controler(scenario).run();


	}

}
