package playground.agarwalamit.Chandigarh;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

public class RunControler {

    private static final String network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone.xml.gz";
    private static final String plans = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_plans.xml.gz";
    private static final String output = "./Ch_output/";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network);
        config.plans().setInputFile(plans);
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);

        PlanCalcScoreConfigGroup.ActivityParams startAct = new PlanCalcScoreConfigGroup.ActivityParams("start");
        startAct.setTypicalDuration(06*3600.);
        PlanCalcScoreConfigGroup.ActivityParams endAct = new PlanCalcScoreConfigGroup.ActivityParams("end");
        endAct.setTypicalDuration(16*3600.);
        config.planCalcScore().addActivityParams(startAct);
        config.planCalcScore().addActivityParams(endAct);

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute); // though, this must not have any effect.
        reRoute.setWeight(0.3);
        config.strategy().addStrategySettings(reRoute);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        new Controler(scenario).run();


    }

}
