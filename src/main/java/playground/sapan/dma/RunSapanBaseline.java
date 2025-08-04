package playground.sapan.dma;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.utils.FileUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;


public class RunSapanBaseline {
    public static void main(String[] args) {

        Config config;
        String runId = null;
        String outputDir = "output/";
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenario/Simple_config_working.xml" );
        } else {
            config = ConfigUtils.loadConfig(args[0]);
            runId = args[1];
        }

        config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

        if(runId!=null) {
            config.controller().setRunId(runId);
            outputDir += runId;
        }

        config.controller().setOutputDirectory(outputDir);
        config.controller().setDumpDataAtEnd(true);
        config.replanning().setMaxAgentPlanMemorySize(10);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;
        Controler controler = new Controler( scenario );
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });
        controler.run();

        // delete unnecessary iterations folder here.
//        int firstIt = controler.getConfig().controller().getFirstIteration();
//        int lastIt = controler.getConfig().controller().getLastIteration();
//        FileUtils.deleteIntermediateIterations(outputDir,firstIt,lastIt);
    }
}