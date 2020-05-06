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

    private static final String network = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_net_insideZone_fixed.xml.gz";
    private static final String plans = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_plans_test.xml.gz";
    private static final String countsFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_counts.xml.gz";
    private static final String output = "./Ch_output/";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network);
        config.plans().setInputFile(plans);
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);
        config.counts().setInputFile(countsFile);
        config.counts().setWriteCountsInterval(5);
        config.counts().setOutputFormat("all");

        PlanCalcScoreConfigGroup.ActivityParams startAct = new PlanCalcScoreConfigGroup.ActivityParams(ChandigarhConstants.start_act_type);
        startAct.setTypicalDuration(6*3600.);
        PlanCalcScoreConfigGroup.ActivityParams endAct = new PlanCalcScoreConfigGroup.ActivityParams(ChandigarhConstants.end_act_type);
        endAct.setTypicalDuration(16*3600.);
        config.planCalcScore().addActivityParams(startAct);
        config.planCalcScore().addActivityParams(endAct);

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute); // though, this must not have any effect.
        reRoute.setWeight(0.3);
        config.strategy().addStrategySettings(reRoute);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // increase the capacity of tertiary links to 1500
        scenario.getNetwork().getLinks().values().stream().filter(l->l.getCapacity()<=600.).forEach(l->l.setCapacity(1500.));
        scenario.getNetwork().getLinks().values().stream().filter(l->l.getFreespeed()<=60./3.6).forEach(l->l.setFreespeed(60./3.6));

        new Controler(scenario).run();


    }

}
