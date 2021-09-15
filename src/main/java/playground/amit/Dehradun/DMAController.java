package playground.amit.Dehradun;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.amit.analysis.StatsWriter;
import playground.amit.analysis.modalShare.ModalShareFromEvents;
import playground.amit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.amit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.amit.utils.FileUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

import java.io.File;

/**
 *
 * @author Amit
 *
 */
public class DMAController {

    public static void main(String[] args) {

        String config_file = "/media/amit/hdd/DehMA/input/DehradunMetropolitanArea_config.xml" ;
        String runId = "r101_patna_params";
        boolean useFreeSpeedTravelTimeCalculator_bicycle = false;
        boolean useFreeSpeedTravelTimeCalculator_motorbike = false;

        if (args.length > 0) {
            config_file = args[0];
            runId = args[1];
            if (args.length >2) useFreeSpeedTravelTimeCalculator_bicycle = Boolean.parseBoolean(args[2]);
            if (args.length >3) useFreeSpeedTravelTimeCalculator_motorbike = Boolean.parseBoolean(args[3]);
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

//                this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
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

        if (useFreeSpeedTravelTimeCalculator_bicycle) {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelTimeBinding(DehradunUtils.TravelModes.bicycle.name()).to(FreeSpeedTravelTimeForBicycle.class);
                }
            });
        }

        if(useFreeSpeedTravelTimeCalculator_motorbike) {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelTimeBinding(DehradunUtils.TravelModes.motorbike.name()).to(FreeSpeedTravelTimeForMotorbike.class);
                }
            });
        }

        controler.run();

        String OUTPUT_DIR = config.controler().getOutputDirectory();
        String run = config.controler().getRunId();
        // delete unnecessary iterations folder here.
        int firstIt = controler.getConfig().controler().getFirstIteration();
        int lastIt = controler.getConfig().controler().getLastIteration();
        FileUtils.deleteIntermediateIterations(OUTPUT_DIR, firstIt, lastIt);

        new File(OUTPUT_DIR+"/analysis/").mkdir();
        String outputEventsFile = OUTPUT_DIR+"/"+run+".output_events.xml.gz";
        // write some default analysis

        ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile);
        mtta.run();
        mtta.writeResults(OUTPUT_DIR+"/analysis/modalTravelTime.txt");

        ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
        msc.run();
        msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents.txt");

        StatsWriter.run(OUTPUT_DIR,config.controler().getRunId());

    }

    public static class FreeSpeedTravelTimeForBicycle implements TravelTime {

        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength() / Math.min( DehradunUtils.getSpeed(DehradunUtils.TravelModes.bicycle.name()), link.getFreespeed(time) );
        }
    }

    public static class FreeSpeedTravelTimeForMotorbike implements TravelTime {

        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength() / Math.min( DehradunUtils.getSpeed(DehradunUtils.TravelModes.motorbike.name()), link.getFreespeed(time) );
        }
    }
}

