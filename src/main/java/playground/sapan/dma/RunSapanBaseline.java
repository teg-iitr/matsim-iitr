package playground.sapan.dma;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


public class RunSapanBaseline {
    public static void main(String[] args) {

        Config config;
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenario/Simple_config_working.xml" );
        } else {
            config = ConfigUtils.loadConfig( args );
        }

        config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

        config.controller().setOutputDirectory("output");

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Controler controler = new Controler( scenario ) ;

        controler.run();
    }
}