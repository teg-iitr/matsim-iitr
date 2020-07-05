package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.activity.departureArrival.FilteredDepartureTimeAnalyzer;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.mixedTraffic.patnaIndia.policies.PatnaPolicyControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.VehicleUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;

import java.io.BufferedWriter;
import java.io.File;
import java.util.*;

public class PatnaControler {

    public static final Logger logger = Logger.getLogger(PatnaControler.class);
    private static final String wfh_walk = "WFHwalk";

    public static void main(String[] args) {

        String outputDir =  "../../patna/output/";
        String inputConfig = "../../patna/input/configBaseCaseCtd_June2020.xml";
        String runCase = "calib_stayHomePlans";
        String filterWorkTrips = "false";
        String wardFile = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/wardFile/Wards.shp";
        String ptDiscountFractionOffPkHr = "0.2";
        String adjustEducationalTripDepartureTimes = "false"; // this needs to better work out
        String addStayHomePlansForCalibration = "true";
        double WFHPenaltyFactor = 0.1;

        if(args.length>0) {
            outputDir = args[0];
            inputConfig = args[1];
            runCase = args[2];
            wardFile = args[3];
            filterWorkTrips = args[4];
            ptDiscountFractionOffPkHr = args[5];
            adjustEducationalTripDepartureTimes = args[6];
            addStayHomePlansForCalibration = args[7];
            WFHPenaltyFactor = Double.parseDouble(args[8]);
        }

        outputDir = outputDir+runCase;

        Config config = ConfigUtils.loadConfig(inputConfig);
        config.controler().setOutputDirectory(outputDir);
        config.controler().setRunId(runCase);

        config.vehicles().setVehiclesFile(null); // vehicle types are added from vehicle file later.
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(50);

        config.travelTimeCalculator().setFilterModes(true);
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(PatnaUtils.ALL_MAIN_MODES)));
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        PatnaPersonFilter patnaPersonFilter = new PatnaPersonFilter();

        if(Boolean.parseBoolean(filterWorkTrips)) {
            logger.info("Filtering work trips with removal probability of 0.5");
            FilterDemandBasedOnTripPurpose filterDemandBasedOnTripPurpose = new FilterDemandBasedOnTripPurpose(scenario.getPopulation(),wardFile,"work");
            filterDemandBasedOnTripPurpose.removePersons(0.5); // work on alternate days...
        }
        if(Boolean.parseBoolean(adjustEducationalTripDepartureTimes)) {
            logger.info("Shifting departure time of educational trips with probability of 0.5. Half of the trips will be departed between 6 to 7 and rest of the trips between 12 to 13.");
            FilterDemandBasedOnTripPurpose filterDemandBasedOnTripPurpose = new FilterDemandBasedOnTripPurpose(scenario.getPopulation(), wardFile, "educational");
            filterDemandBasedOnTripPurpose.shiftDepartureTime(0.5, new Tuple<>(6*3600., 5400), new Tuple<>(12*3600., 5400)); //adjusting time between 6 to 7:30 and 12 to 13:30
        }

        String vehiclesFile = new File(outputDir).getParentFile().getParentFile().getAbsolutePath()+"/input/output_vehicles.xml.gz";
        // following is required to extract only vehicle types and not vehicle info. Amit Nov 2016
        VehicleUtils.addVehiclesToScenarioFromVehicleFile(vehiclesFile, scenario);

        if (!scenario.getVehicles().getVehicles().isEmpty()) throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
                QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
        scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        final Controler controler = new Controler(scenario);

        controler.getConfig().controler().setDumpDataAtEnd(true);
        controler.getConfig().strategy().setMaxAgentPlanMemorySize(10);

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
        final DiscountedPTFareHandler discountedPTFareHandler = new DiscountedPTFareHandler(Double.parseDouble(ptDiscountFractionOffPkHr));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(discountedPTFareHandler);
            }
        });

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().to(PtFareEventHandler.class);
            }
        });
        // for above make sure that util_dist and monetary dist rate for pt are zero.
        PlanCalcScoreConfigGroup.ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
        mp.setMarginalUtilityOfDistance(0.0);
        mp.setMonetaryDistanceRate(0.0);

        // add income dependent scoring function factory
        PatnaPolicyControler.addScoringFunction(controler);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);
            }
        });

        if(Boolean.parseBoolean(addStayHomePlansForCalibration)){
            List<String> actTypes = List.of("work","educational");
            //work-from-home-strategy
            String wfh_name = "WorkFromHome";
            controler.addOverridingModule(new WorkFromHomeModule(wfh_name, wfh_walk, actTypes));

            if (WFHPenaltyFactor!=0.) {
                WFHPricing wfhPricing = new WFHPricing(WFHPenaltyFactor, wfh_walk, actTypes );
                controler.addOverridingModule(new AbstractModule() {
                    @Override
                    public void install() {
                        addEventHandlerBinding().toInstance(wfhPricing);
                    }
                });
            }
        }

        controler.run();

        // delete unnecessary iterations folder here.
        int firstIt = controler.getConfig().controler().getFirstIteration();
        int lastIt = controler.getConfig().controler().getLastIteration();
        FileUtils.deleteIntermediateIterations(outputDir,firstIt,lastIt);

        new File(outputDir+"/analysis/").mkdir();
        String outputEventsFile = outputDir+"/"+runCase+".output_events.xml.gz";
        // write some default analysis
        String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();

        ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, patnaPersonFilter);
        mtta.run();
        mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

        writeModalShare(userGroup, scenario.getConfig().controler());

        ActivityDepartureAnalyzer analyzer = new ActivityDepartureAnalyzer(outputEventsFile);
        analyzer.run();
        analyzer.writeResults(outputDir+"/analysis/activityDepartureCoutners.txt");

        FilteredDepartureTimeAnalyzer lmtdd = new FilteredDepartureTimeAnalyzer(outputEventsFile, 3600.);
        lmtdd.run();
        lmtdd.writeResults(outputDir+"/analysis/departureCounts"+".txt");

        StatsWriter.run(outputDir, runCase);
    }

    private static void writeModalShare(String userGroup, ControlerConfigGroup controlerConfigGroup){
        String outputDir = controlerConfigGroup.getOutputDirectory();
        String runCase = controlerConfigGroup.getRunId();

        int firstIteration = controlerConfigGroup.getFirstIteration();
        String firstIterationEventsFile = outputDir+"/ITERS/it."+ firstIteration +"/"+runCase+"."+ firstIteration +".events.xml.gz";
        String outputEventsFile = outputDir+"/"+runCase+".output_events.xml.gz";

        ModalShareFromEvents msc_firstItEvents = new ModalShareFromEvents(firstIterationEventsFile, userGroup, new PatnaPersonFilter());
        msc_firstItEvents.run();

        ModalShareFromEvents msc_outputEvents = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
        msc_outputEvents.run();

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt")) {
            writeResutls(msc_firstItEvents, writer, firstIteration);
            writeResutls(msc_outputEvents, writer, controlerConfigGroup.getLastIteration());
        } catch (Exception e) {
            throw new RuntimeException("Data can not be written to file. Reason - "+e);
        }
    }

    private static void writeResutls(ModalShareFromEvents msc_outputEvents, BufferedWriter writer, int iteration) {
        try{
            writer.write("iteration\t");
            for(String str:msc_outputEvents.getModeToNumberOfLegs().keySet()){
                writer.write(str+"\t");
            }
            writer.write("total \t");
            writer.newLine();

            writer.write(iteration + "\t");
            for (String str : msc_outputEvents.getModeToNumberOfLegs().keySet()) { // write Absolute No Of Legs
                writer.write(msc_outputEvents.getModeToNumberOfLegs().get(str) + "\t");
            }
            writer.write(MapUtils.intValueSum(msc_outputEvents.getModeToNumberOfLegs()) + "\t");
            writer.newLine();

            writer.write(iteration + "\t");
            for (String str : msc_outputEvents.getModeToPercentOfLegs().keySet()) { // write percentage no of legs
                writer.write(msc_outputEvents.getModeToPercentOfLegs().get(str) + "\t");
            }
            writer.write(MapUtils.doubleValueSum(msc_outputEvents.getModeToPercentOfLegs()) + "\t");
            writer.newLine();
        }
        catch (Exception e) {
            throw new RuntimeException("Data can not be written to file. Reason - "+e);
        }
    }
}
