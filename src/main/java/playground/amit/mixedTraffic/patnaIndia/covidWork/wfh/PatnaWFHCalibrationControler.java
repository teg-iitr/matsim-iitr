package playground.amit.mixedTraffic.patnaIndia.covidWork.wfh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.analysis.StatsWriter;
import playground.amit.analysis.activity.departureArrival.FilteredDepartureTimeAnalyzer;
import playground.amit.mixedTraffic.patnaIndia.covidWork.ActivityDepartureAnalyzer;
import playground.amit.mixedTraffic.patnaIndia.covidWork.MyChangeTripMode;
import playground.amit.mixedTraffic.patnaIndia.covidWork.PatnaCovidPolicyControler;
import playground.amit.mixedTraffic.patnaIndia.policies.PatnaPolicyControler;
import playground.amit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.amit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.amit.utils.FileUtils;
import playground.amit.utils.PersonFilter;
import playground.amit.utils.VehicleUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
/**
 * @author amit
 */

public class PatnaWFHCalibrationControler {

    public static final Logger logger = LogManager.getLogger(PatnaWFHCalibrationControler.class);
//    private static final String wfh_walk = "WFHwalk";

    public static void main(String[] args) {

        String outputDir =  "../../patna/output/";
        String inputConfig = "../../patna/input/configBaseCaseCtd_June2020.xml";
        String runCase = "calib_stayHomePlans";
        String addStayHomePlansForCalibration = "true";
        double WFHPenaltyFactor = 0.5;

        if(args.length>0) {
            outputDir = args[0];
            inputConfig = args[1];
            runCase = args[2];
            addStayHomePlansForCalibration = args[3];//7
            WFHPenaltyFactor = Double.parseDouble(args[4]);//8
        }

        outputDir = outputDir+runCase;

        Config config = ConfigUtils.loadConfig(inputConfig);
        config.controller().setOutputDirectory(outputDir);
        config.controller().setRunId(runCase);

        config.vehicles().setVehiclesFile(null); // vehicle types are added from vehicle file later.
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setWriteEventsInterval(50);

        config.travelTimeCalculator().setFilterModes(true);
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(PatnaUtils.ALL_MAIN_MODES)));
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        PatnaPersonFilter patnaPersonFilter = new PatnaPersonFilter();

        String vehiclesFile = new File(outputDir).getParentFile().getParentFile().getAbsolutePath()+"/input/output_vehicles.xml.gz";
        // following is required to extract only vehicle types and not vehicle info. Amit Nov 2016
        VehicleUtils.addVehiclesToScenarioFromVehicleFile(vehiclesFile, scenario);

        if (!scenario.getVehicles().getVehicles().isEmpty()) throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
                QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
        scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        final Controler controler = new Controler(scenario);

        controler.getConfig().controller().setDumpDataAtEnd(true);
        controler.getConfig().replanning().setMaxAgentPlanMemorySize(10);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().to(PtFareEventHandler.class);
                this.addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode).toProvider(MyChangeTripMode.class);
            }
        });
        // for above make sure that util_dist and monetary dist rate for pt are zero.
        ScoringConfigGroup.ModeParams mp = controler.getConfig().scoring().getModes().get("pt");
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
            scenario.getPopulation().getPersons().values()
                    .stream()
                    .filter(p->patnaPersonFilter.getUserGroupAsStringFromPersonId(p.getId()).equals(PatnaPersonFilter.PatnaUserGroup.urban.toString()))
                    .forEach(p->{
                        p.getPlans().stream().forEach(plan -> plan.setType("non-WFH_"+( (Leg)plan.getPlanElements().get(1)).getMode()) );

                        Activity secondAc = ((Activity)p.getSelectedPlan().getPlanElements().get(2));
                        if (actTypes.contains(secondAc.getType())) {
                            Activity firstAct = null ; // for location
                            Plan plan = scenario.getPopulation().getFactory().createPlan();
                            plan.setType(WFHActivity.WFH_PLAN_TYPE);

                            for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
                                if (pe instanceof Activity) {
                                    Activity act = ((Activity)pe);
                                    if(firstAct==null) firstAct = act;

                                    Activity wfhAct = scenario.getPopulation().getFactory().createActivityFromLinkId(wfh_name+"_"+act.getType(), firstAct.getLinkId());
                                    wfhAct.setCoord(firstAct.getCoord());
                                    wfhAct.setFacilityId(firstAct.getFacilityId());
                                    if (act.getEndTime().isDefined()) {
                                        wfhAct.setEndTime(act.getEndTime().seconds());
                                    }
                                    plan.addActivity(wfhAct);
                                }
                            }
                            p.addPlan(plan);
                            p.setSelectedPlan(plan);
                        }
                    });

            if (WFHPenaltyFactor!=0.) {
                WFHPricing wfhPricing = new WFHPricing(WFHPenaltyFactor);
                controler.addOverridingModule(new AbstractModule() {
                    @Override
                    public void install() {
                        addEventHandlerBinding().toInstance(wfhPricing);
                        addControlerListenerBinding().toInstance(wfhPricing);
                    }
                });
            }

            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    this.bind(WFHActivity.class).toInstance(actType -> actType.startsWith(wfh_name));
                    this.bind(PersonFilter.class).toInstance(patnaPersonFilter);
                    this.addControlerListenerBinding().to(WFHCounterControlerListner.class);
                }
            });
        }

        controler.run();

        // delete unnecessary iterations folder here.
        int firstIt = controler.getConfig().controller().getFirstIteration();
        int lastIt = controler.getConfig().controller().getLastIteration();
        FileUtils.deleteIntermediateIterations(outputDir,firstIt,lastIt);

        new File(outputDir+"/analysis/").mkdir();
        String outputEventsFile = outputDir+"/"+runCase+".output_events.xml.gz";
        // write some default analysis
        String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();

        PatnaCovidPolicyControler.writeModalShareFromEvents(userGroup, scenario.getConfig().controller());

//        ModalShareFromPlans modalShareFromPlans = new ModalShareFromPlans(outputDir+"/"+runCase+".output_experienced_plans.xml.gz", userGroup, patnaPersonFilter);
//        modalShareFromPlans.run();
//        modalShareFromPlans.writeResults(outputDir+"/analysis/urbanModalShare_outputExpPlans.txt");

        ActivityDepartureAnalyzer analyzer = new ActivityDepartureAnalyzer(outputEventsFile);
        analyzer.run();
        analyzer.writeResults(outputDir+"/analysis/activityDepartureCoutners.txt");

        FilteredDepartureTimeAnalyzer lmtdd = new FilteredDepartureTimeAnalyzer(outputEventsFile, 3600.);
        lmtdd.run();
        lmtdd.writeResults(outputDir+"/analysis/departureCounts"+".txt");

        StatsWriter.run(outputDir, runCase);
    }
}
