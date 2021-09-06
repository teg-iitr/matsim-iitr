package playground.amit.Dehradun;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;

public class DMAController {

    public static void main(String[] args) {

        String config_file = "/media/amit/hdd/DehMA/input/DehradunMetropolitanArea_config.xml" ;
        String runId = "r101_patna_params";

        if (args.length > 0) {
            config_file = args[0];
            runId = args[1];
        }

        Config config = ConfigUtils.loadConfig(config_file);
        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(config.controler().getOutputDirectory()+runId);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        DMAVehicleGenerator.generateVehicles(scenario);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
            @Override
            public void install() {
                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
            }
        });

        // adding pt fare system based on distance
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().to(DMAPtFareEventHandler.class);
            }
        });
        // for above make sure that util_dist and monetary dist rate for pt are zero.
        PlanCalcScoreConfigGroup.ModeParams mp = controler.getConfig().planCalcScore().getModes().get(DehradunUtils.TravelModes.bus.name());
        mp.setMarginalUtilityOfDistance(0.0);
        mp.setMonetaryDistanceRate(0.0);

        PlanCalcScoreConfigGroup.ModeParams mp2 = controler.getConfig().planCalcScore().getModes().get(DehradunUtils.TravelModes.IPT.name());
        mp2.setMarginalUtilityOfDistance(0.0);
        mp2.setMonetaryDistanceRate(0.0);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(DehradunUtils.TravelModes.bicycle.name()).to(FreeSpeedTravelTimeForBike.class);
            }
        });

        controler.run();

    }

}

