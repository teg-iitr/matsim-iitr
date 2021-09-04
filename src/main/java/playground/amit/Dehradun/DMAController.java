package playground.amit.Dehradun;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import playground.amit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.amit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;

import javax.inject.Inject;
import java.io.File;

public class DMAController {
    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";

    public static void main(String[] args) {

        String config_file = "/media/amit/hdd/DehMA/input/DehradunMetropolitanArea_config.xml" ;
        String runId = "r101_patna_params";

        if (args.length > 0) {
            config_file = args[0];
            runId = args[1];
        }

        System.out.println(new File(config_file).exists());

        Config config = ConfigUtils.loadConfig(config_file);
        config.controler().setRunId(runId);

        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.rail.name()).setConstant(-2.0);
        config.planCalcScore().getOrCreateModeParams(DehradunUtils.TravelModes.motorbike.name()).setConstant(1.0);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        DMAVehicleGenerator.generateVehicles(scenario);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

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

        PlanCalcScoreConfigGroup.ModeParams mp3 = controler.getConfig().planCalcScore().getModes().get(DehradunUtils.TravelModes.rail.name());
        mp3.setMarginalUtilityOfDistance(0.0);
        mp3.setMonetaryDistanceRate(0.0);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(DehradunUtils.TravelModes.bicycle.name()).to(FreeSpeedTravelTimeForBike.class);
            }
        });

        controler.run();

    }

}

