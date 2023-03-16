package playground.shivam.Dadar.evacuation;


import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.*;
import org.matsim.evacuationgui.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import playground.amit.Dehradun.OD;
import playground.amit.jaipur.plans.ODMatrixGenerator;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.amit.utils.LoadMyScenarios;
import playground.amit.utils.geometry.GeometryUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * @author Shivam
 */
public class RunDadarEvacScenario {
    private final String INPUT_FILES_PATH = "input/evacDadar/";
    private final String OUTPUT_EVAC_SIM_FILES_PATH = "output/evacDadar/";
    private final String OUTPUT_MODE_SHARE_CALIBRATION_FILES_PATH = "output/dadarModeShareCalibration";
    private final Map<Id<Link>, Geometry> SAFE_POINTS = new HashMap<>();
    private Collection<Id<Node>> safeNodeAIds = new ArrayList<>();
    private final String BOUNDARY_SHAPEFILE = INPUT_FILES_PATH + "boundaryDadar.shp";
    private final String EVACUATION_ZONES_SHAPEFILE = INPUT_FILES_PATH + "evacuationZones.shp";
    private final String ZONES_SHAPEFILE = INPUT_FILES_PATH + "zonesDadar.shp";
    //    private static final String boundaryShapeFile = "input/evacDadar/boundaryDadar.shp";
    private final String ORIGIN_ACTIVITY = "origin";
    private final String DESTINATION_ACTIVITY = "destination";
    private final String MATSIM_NETWORK = INPUT_FILES_PATH + "dadar-network_smaller.xml.gz";
    private final String EVACUATION_NETWORK = INPUT_FILES_PATH + "dadar_evac_network.xml.gz";

    private final String SAFE_POINT_SHAPEFILE = INPUT_FILES_PATH + "dadarSafePoints.shp";
    private final String OD_MATRIX = INPUT_FILES_PATH + "dadar_od_10_10_22.csv";

    private final String modeShareFilePath = INPUT_FILES_PATH + "dadar_mode_share/";
    private final String MATSIM_COUNTS = INPUT_FILES_PATH + "dadar-counts.xml.gz";
    private final String PSEUDO_COUNTS_FROM_STATION = INPUT_FILES_PATH + "pseudoCounts.txt";

    private final String MATSIM_PLANS = INPUT_FILES_PATH + "dadar-plans.xml.gz";
    private final String EVACUATION_PLANS = INPUT_FILES_PATH + "dadar_evac_plans.xml.gz";
    private final Collection<String> DADAR_ALL_MODES = DadarUtils.ALL_MAIN_MODES;
    private Scenario scenario;
    private Geometry evacuationArea;
    private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Dadar");
    private final Map<Tuple<Id<Link>, String>, Map<Integer, Double>> countStation2time2countInfo = new HashMap<>();


    public void run() {

        scenario = LoadMyScenarios.loadScenarioFromNetwork(MATSIM_NETWORK);

        safePoints();

        createDadarEvacNetwork(scenario);

        createDadarEvacPlans(LoadMyScenarios.loadScenarioFromPlans(MATSIM_PLANS));

        createDadarEvacConfig();

    }
    private void runModeShareCalibration() {
        createDadarNetworkFromOSM();

        createPlansFromDadarOD();

        createDadarPseudoCounts();

        createModeShareCalibrationConfig();
    }
    public void readCountStationData(final String file) {
        try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
            String line = reader.readLine();

            while (line != null) {
                if (line.startsWith("countStation")) {
                    line = reader.readLine();
                    continue;
                }
                String parts[] = line.split("\t");
                String surveyLocation = parts[0];
                Id<Link> linkId = Id.createLinkId(parts[1]);
                Integer time = Integer.valueOf(parts[2]);
                double sum = 0.;
                for (int index = 3; index < parts.length - 1; index++) {
                    sum += Double.valueOf(parts[index]);
                }

                double count = Double.valueOf(parts[parts.length - 1]);
                if (sum != count)
                    throw new RuntimeException("sum of individual modal counts does not match total count.");

                Tuple<Id<Link>, String> myCountStationInfo = new Tuple<>(linkId, surveyLocation);
                if (countStation2time2countInfo.containsKey(myCountStationInfo)) {
                    Map<Integer, Double> time2count = countStation2time2countInfo.get(myCountStationInfo);
                    time2count.put(time, count);
                } else {
                    Map<Integer, Double> time2count = new HashMap<>();
                    time2count.put(time, count);
                    countStation2time2countInfo.put(myCountStationInfo, time2count);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not Read. Reason :" + e);
        }
    }

    private void createDadarPseudoCounts() {
        readCountStationData(PSEUDO_COUNTS_FROM_STATION);

        Counts<Link> counts = new Counts<>();
        counts.setName("Dadar_Multi_Modal_Counts");
        counts.setYear(2014);
        counts.setDescription("DadarAllModesCountBasedOnCMP_Mumbai_2016");

        for (Tuple<Id<Link>, String> mcs : countStation2time2countInfo.keySet()) {
            Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
            for (Integer i : countStation2time2countInfo.get(mcs).keySet()) {
                c.createVolume(i, countStation2time2countInfo.get(mcs).get(i));
            }
        }

        new CountsWriter(counts).write(MATSIM_COUNTS);
    }


    private void createDadarEvacPlans(Scenario scenarioPop) {
        Population dadarPop = scenarioPop.getPopulation();

        Population evacPop = this.scenario.getPopulation();
        PopulationFactory popFact = evacPop.getFactory();

        Person evacPerson;
        for (Person person : dadarPop.getPersons().values()) {
            PlanElement actPe = person.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
            Activity originExisting = (Activity) actPe;
            Coord originExistingCoord = originExisting.getCoord();

            Link link = NetworkUtils.getNearestLink(scenario.getNetwork(), originExisting.getCoord());
            Activity origin = popFact.createActivityFromLinkId(originExisting.getType(), link.getId());

            //check if the person is in the area shape, if not leave them out
            if (originExistingCoord != null && !evacuationArea.contains(MGC.coord2Point(originExistingCoord)))
                continue;

            // also exclude any origin activity starting on link which is not included in evac network
            if (!scenario.getNetwork().getLinks().containsKey(origin.getLinkId()))
                continue;

            // should skip when coord is one of the safeNodeAId
            if (safeNodeAIds.contains(link.getToNode().getId()))
                continue;

            evacPerson = popFact.createPerson(person.getId());
            for (Id<Link> safeLinkIdFromSafePoint : SAFE_POINTS.keySet()) {
                Plan planOut = popFact.createPlan();

                planOut.addActivity(origin);
                // TODO: check in simulation
                origin.setEndTime(9 * 3600);

                PlanElement legPe = person.getSelectedPlan().getPlanElements().get(1);
                Leg leg = popFact.createLeg(((Leg) legPe).getMode());
                planOut.addLeg(leg);

                Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkIdFromSafePoint);
                planOut.addActivity(evacAct);

                evacPerson.addPlan(planOut);
                if (DADAR_ALL_MODES.contains(leg.getMode())) {
                    TripRouter.Builder builder = new TripRouter.Builder(scenario.getConfig());
                    builder.setRoutingModule(
                            leg.getMode(),
                            DefaultRoutingModules.createPureNetworkRouter(
                                    leg.getMode(),
                                    popFact,
                                    scenario.getNetwork(),
                                    new DijkstraFactory().createPathCalculator(scenario.getNetwork(),
                                            new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()),
                                            new FreeSpeedTravelTime())
                            )
                    );
                    List<? extends PlanElement> routeInfo = builder.build().calcRoute(
                            leg.getMode(),
                            FacilitiesUtils.toFacility(origin, null),
                            FacilitiesUtils.toFacility(evacAct, null),
                            origin.getEndTime().seconds(),
                            evacPerson,
                            new Attributes());

                    Route route = ((Leg) routeInfo.get(0)).getRoute();
                    route.setStartLinkId(origin.getLinkId());
                    route.setEndLinkId(evacAct.getLinkId());

                    leg.setRoute(route);
                    leg.setTravelTime(((Leg) routeInfo.get(0)).getTravelTime().seconds());
                } else
                    continue;
            }
            evacPop.addPerson(evacPerson);

        }
        new PopulationWriter(evacPop).write(EVACUATION_PLANS);
    }

    private void createDadarEvacNetwork(Scenario scenario) {
        // taking the zones that
        Geometry transformEvacuationArea = (Geometry) ShapeFileReader.getAllFeatures(EVACUATION_ZONES_SHAPEFILE).iterator().next().getDefaultGeometry();// --> WGS84

        try {
            evacuationArea = JTS.transform(transformEvacuationArea, CRS.findMathTransform(MGC.getCRS(TransformationFactory.WGS84), MGC.getCRS(DadarUtils.Dadar_EPSG), true));
        } catch (TransformException | FactoryException e) {
            throw new RuntimeException("Transformation isn't successful" + e);
        }

        EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(scenario, evacuationArea, safeLinkId);
        net.run(SAFE_POINTS);
        this.safeNodeAIds = net.getSafeNodeAIds();

        for (Link l : scenario.getNetwork().getLinks().values()) {
            Set<String> allowedModes = new HashSet<>(DADAR_ALL_MODES);
            l.setAllowedModes(allowedModes);
        }

        new NetworkWriter(scenario.getNetwork()).write(EVACUATION_NETWORK);
    }

    private void createModeShareCalibrationConfig() {
        Config config = ConfigUtils.createConfig();

        config.network().setInputFile(MATSIM_NETWORK);

        config.plans().setInputFile(MATSIM_PLANS);
        config.plans().setRemovingUnneccessaryPlanAttributes(false);

        config.controler().setLastIteration(0);
        config.controler().setLastIteration(100);
        config.controler().setOutputDirectory(OUTPUT_MODE_SHARE_CALIBRATION_FILES_PATH);
        config.controler().setDumpDataAtEnd(false);
        config.controler().setCreateGraphs(false);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setWriteEventsInterval(10);
        config.controler().setWritePlansInterval(10);

        config.qsim().setSnapshotPeriod(5 * 60);
        config.qsim().setEndTime(30 * 3600);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setMainModes(DADAR_ALL_MODES);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        //TODO we are using 0.5 just to see some congestion.
        config.qsim().setFlowCapFactor(0.5);
        config.qsim().setStorageCapFactor(0.5);

        config.vspExperimental().setWritingOutputEvents(true);
        config.plansCalcRoute().setNetworkModes(DADAR_ALL_MODES);

        config.counts().setWriteCountsInterval(5);
        config.counts().setInputFile(MATSIM_COUNTS);
        config.counts().setFilterModes(true);
        config.counts().setOutputFormat("txt");
        config.counts().setAverageCountsOverIterations(5);

        config.travelTimeCalculator().setFilterModes(true);
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(DadarUtils.ALL_MAIN_MODES)));


        PlanCalcScoreConfigGroup pcg = config.planCalcScore();
        {
            PlanCalcScoreConfigGroup.ActivityParams originAct = new PlanCalcScoreConfigGroup.ActivityParams(ORIGIN_ACTIVITY);
            originAct.setScoringThisActivityAtAll(false);
            pcg.addActivityParams(originAct);

            PlanCalcScoreConfigGroup.ActivityParams destinationAct = new PlanCalcScoreConfigGroup.ActivityParams(DESTINATION_ACTIVITY);
            destinationAct.setScoringThisActivityAtAll(false);
            destinationAct.setTypicalDuration(8 * 3600);
            pcg.addActivityParams(destinationAct);

        }

        for (String mode : DADAR_ALL_MODES) {
            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(mode);
            modeParams.setConstant(DadarUtils.setConstant(mode));
            modeParams.setMarginalUtilityOfTraveling(DadarUtils.setMarginalUtilityOfTraveling(mode));
            pcg.addModeParams(modeParams);
        }

        StrategyConfigGroup scg = config.strategy();
        {
            StrategyConfigGroup.StrategySettings expChangeBeta = new StrategyConfigGroup.StrategySettings();
            expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
            expChangeBeta.setWeight(0.7);
            scg.addStrategySettings(expChangeBeta);

            StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
            reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
            reRoute.setWeight(0.15);
            scg.addStrategySettings(reRoute);

            StrategyConfigGroup.StrategySettings timeAllocationMutator = new StrategyConfigGroup.StrategySettings();
            timeAllocationMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
            timeAllocationMutator.setWeight(0.05);
            scg.addStrategySettings(timeAllocationMutator);

            config.timeAllocationMutator().setAffectingDuration(false);

            StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
            modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
            modeChoice.setWeight(0.8);
            scg.addStrategySettings(modeChoice);

            config.changeMode().setModes(DadarUtils.ALL_MAIN_MODES.toArray(new String[DadarUtils.ALL_MAIN_MODES.size()]));
        }

        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();

        for (String mode : DADAR_ALL_MODES) {
            VehicleType veh = VehicleUtils.createVehicleType(Id.create(mode, VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getStuckTime(mode));
            veh.setNetworkMode(mode);
            vehicles.addVehicleType(veh);
        }

        new ConfigWriter(config).write(INPUT_FILES_PATH + "config.xml");

        Controler controler = new Controler(scenario);

        controler.getConfig().strategy().setMaxAgentPlanMemorySize(5);

        controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations

            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
            }
        });

        controler.run();
    }

    private void createDadarEvacConfig() {
        Config config = scenario.getConfig();

        config.network().setInputFile(EVACUATION_NETWORK);

        config.plans().setInputFile(EVACUATION_PLANS);
        config.plans().setRemovingUnneccessaryPlanAttributes(true);

        config.controler().setLastIteration(0);
        config.controler().setLastIteration(100);
        config.controler().setOutputDirectory(OUTPUT_EVAC_SIM_FILES_PATH);
        config.controler().setDumpDataAtEnd(false);
        config.controler().setCreateGraphs(false);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setWriteEventsInterval(10);
        config.controler().setWritePlansInterval(10);

        config.qsim().setSnapshotPeriod(5 * 60);
        config.qsim().setEndTime(30 * 3600);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setMainModes(DADAR_ALL_MODES);
        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        //TODO we are using 0.5 just to see some congestion.
        config.qsim().setFlowCapFactor(0.5);
        config.qsim().setStorageCapFactor(0.5);

        config.vspExperimental().setWritingOutputEvents(true);

        config.plansCalcRoute().setNetworkModes(DADAR_ALL_MODES);

        config.travelTimeCalculator().setFilterModes(true);
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes((new HashSet<>(DadarUtils.ALL_MAIN_MODES)));


        PlanCalcScoreConfigGroup pcg = config.planCalcScore();
        {
            PlanCalcScoreConfigGroup.ActivityParams evacAct = new PlanCalcScoreConfigGroup.ActivityParams("evac");
            evacAct.setTypicalDuration(3600);
            pcg.addActivityParams(evacAct);
        }

        for (String mode : DADAR_ALL_MODES) {
            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(mode);
            modeParams.setConstant(DadarUtils.setConstant(mode));
            modeParams.setMarginalUtilityOfTraveling(DadarUtils.setMarginalUtilityOfTraveling(mode));
            pcg.addModeParams(modeParams);
        }

        StrategyConfigGroup scg = config.strategy();
        {
            StrategyConfigGroup.StrategySettings expChangeBeta = new StrategyConfigGroup.StrategySettings();
            expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
            expChangeBeta.setWeight(0.7);
            scg.addStrategySettings(expChangeBeta);

            StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
            reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
            reRoute.setWeight(0.15);
            scg.addStrategySettings(reRoute);

            StrategyConfigGroup.StrategySettings timeAllocationMutator = new StrategyConfigGroup.StrategySettings();
            timeAllocationMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
            timeAllocationMutator.setWeight(0.05);
            scg.addStrategySettings(timeAllocationMutator);

            config.timeAllocationMutator().setAffectingDuration(false);

            config.changeMode().setModes(DadarUtils.ALL_MAIN_MODES.toArray(new String[DadarUtils.ALL_MAIN_MODES.size()]));
        }

        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();

        for (String mode : DADAR_ALL_MODES) {
            VehicleType veh = VehicleUtils.createVehicleType(Id.create(mode, VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getStuckTime(mode));
            veh.setNetworkMode(mode);
            vehicles.addVehicleType(veh);
        }

        new ConfigWriter(config).write(INPUT_FILES_PATH + "config.xml");

        Controler controler = new Controler(scenario);

        controler.getConfig().strategy().setMaxAgentPlanMemorySize(5);


        controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations

            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        controler.run();
    }

    public static void main(String[] args) {
        new RunDadarEvacScenario().runModeShareCalibration();
        //new RunDadarEvacScenario().run();
    }



    private void safePoints() {
        int numberOfSafePointsNeeded = 2;

        Collection<SimpleFeature> safePoints = ShapeFileReader.getAllFeatures(SAFE_POINT_SHAPEFILE);

        safePoints.removeIf(e -> (e.getDefaultGeometry() == null));

        Collection<SimpleFeature> duplicate = new ArrayList<>(safePoints);
        int c = 0;

        for (SimpleFeature simpleFeature : duplicate) {
            safePoints.removeIf(e -> (Objects.equals(e.getID(), simpleFeature.getID())));

            c++;

            if (c == (duplicate.size() - numberOfSafePointsNeeded))
                break;
        }

        String fromSafePointSystem = "EPSG:32643";

        String toSafePointSystem = DadarUtils.Dadar_EPSG;


        for (SimpleFeature safePoint : safePoints) {
            Geometry safePointDefaultGeometry = (Geometry) safePoint.getDefaultGeometry();

            try {
                Geometry transformedSafePoint = JTS.transform(safePointDefaultGeometry, CRS.findMathTransform(MGC.getCRS(fromSafePointSystem), MGC.getCRS(toSafePointSystem), true));
                SAFE_POINTS.put(Id.createLinkId(safeLinkId.toString() + safePoint.getID()), transformedSafePoint);
            } catch (TransformException | FactoryException e) {
                throw new RuntimeException("Transformation isn't successful" + e);
            }
        }
    }

    private void createPlansFromDadarOD() {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        Map<Id<OD>, OD> tripMatrix = ODMatrixGenerator.generateOD(OD_MATRIX);
        Collection<SimpleFeature> zones = ShapeFileReader.getAllFeatures(ZONES_SHAPEFILE);

        for (OD tripOd : tripMatrix.values()) {

            String originID = tripOd.getOrigin();
            String destinationID = tripOd.getDestination();
            double numberOfTrips = tripOd.getNumberOfTrips();

            if (numberOfTrips == 0) continue;

            SimpleFeature origin_zone = null;
            SimpleFeature destination_zone = null;

            for (SimpleFeature zone : zones) {
                String zone_key = "name";
                String zoneID = String.valueOf(zone.getAttribute(zone_key));
                if (origin_zone == null && zoneID.equals(originID)) origin_zone = zone;
                else if (destination_zone == null && zoneID.equals(destinationID)) destination_zone = zone;
            }

            if (origin_zone == null || destination_zone == null) {
                System.out.println("Either of the origin zone " + originID + " or destination zone " + destinationID + " is null.");
                continue;
            }

            for (int i = 0; i < numberOfTrips; i++) {

//                create matsim plans --> origin coord, destination coord, time, mode,

                Point originPoint = GeometryUtils.getRandomPointInsideFeature(origin_zone);
                Point destinationPoint = GeometryUtils.getRandomPointInsideFeature(destination_zone);

                Coord origin = DadarUtils.TRANSFORMATION_FROM_WSG_84.transform(MGC.point2Coord(originPoint));
                Coord destination = DadarUtils.TRANSFORMATION_FROM_WSG_84.transform(MGC.point2Coord(destinationPoint));

                Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = factory.createPlan();

                Activity origAct = factory.createActivityFromCoord(ORIGIN_ACTIVITY, origin);
                // all agents leave around 6 am.
                origAct.setEndTime(6 * 3600 + MatsimRandom.getRandom().nextInt(3600));
                plan.addActivity(origAct);

                Leg leg = factory.createLeg(getTravelMode(MatsimRandom.getLocalInstance().nextInt(100)));
                plan.addLeg(leg);

                Activity destinAct = factory.createActivityFromCoord(DESTINATION_ACTIVITY, destination);
                plan.addActivity(destinAct);

                person.addPlan(plan);
                population.addPerson(person);

            }
        }
        new PopulationWriter(population).write(MATSIM_PLANS);
    }

    private static String getTravelMode(int number) {
        if (number <= 45) return DadarUtils.DadarTrafficCountMode2023.motorbike.toString();
        else if (number <= 85) {
            return DadarUtils.DadarTrafficCountMode2023.car.toString();
        } else if (number <= 89) {
            return DadarUtils.DadarTrafficCountMode2023.bus.toString();
        } else if (number <= 91) {
            return DadarUtils.DadarTrafficCountMode2023.lcv.toString();
        } else if (number <= 95) {
            return DadarUtils.DadarTrafficCountMode2023.truck.toString();
        } else if (number <= 97) {
            return DadarUtils.DadarTrafficCountMode2023.bicycle.toString();
        } else if (number <= 99) {
            return DadarUtils.DadarTrafficCountMode2023.auto.toString();
        } else return DadarUtils.DadarTrafficCountMode2023.cart.toString();
    }

    /**
     * Creates a Network from OSM
     *
     * @return path of MATSIm network
     */
    private void createDadarNetworkFromOSM() {
        String inputOSMFile = INPUT_FILES_PATH + "dadar.osm.pbf";

        Geometry wholeGeometry = (Geometry) ShapeFileReader.getAllFeatures(BOUNDARY_SHAPEFILE).iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            if (hierarchyLevel <= 4)
                return true; //keep all roads upto level 4.
            else
                return (hierarchyLevel <= 5 && wholeGeometry.contains(MGC.coord2Point(DadarUtils.TRANSFORMATION_FROM_WSG_84.transform(cord))));
        };
        Set<String> modes = new HashSet<>(Collections.singletonList(String.join("", DADAR_ALL_MODES)));

        Network network = (new SupersonicOsmNetworkReader.Builder())
                .setCoordinateTransformation(DadarUtils.TRANSFORMATION_FROM_WSG_84)
//                .addOverridingLinkProperties(OsmTags.MOTORWAY, new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 2, 120.0 / 3.6, 2000, true))
//                .addOverridingLinkProperties(OsmTags.MOTORWAY_LINK, new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 2, 120.0 / 3.6, 1800, true))
//                .addOverridingLinkProperties(OsmTags.TRUNK, new LinkProperties(LinkProperties.LEVEL_TRUNK, 2, 120.0 / 3.6, 2000, false))
//                .addOverridingLinkProperties(OsmTags.TRUNK_LINK, new LinkProperties(LinkProperties.LEVEL_TRUNK, 2, 80.0 / 3.6, 1800, false))
//                .addOverridingLinkProperties(OsmTags.PRIMARY, new LinkProperties(LinkProperties.LEVEL_PRIMARY, 2, 80.0 / 3.6, 1800, false))
//                .addOverridingLinkProperties(OsmTags.PRIMARY_LINK, new LinkProperties(LinkProperties.LEVEL_PRIMARY, 2, 80.0 / 3.6, 1800, false))
//                .addOverridingLinkProperties(OsmTags.SECONDARY, new LinkProperties(LinkProperties.LEVEL_SECONDARY, 2, 4, 800, false))
//                .addOverridingLinkProperties(OsmTags.SECONDARY_LINK, new LinkProperties(LinkProperties.LEVEL_SECONDARY, 2, 4, 800, false))
//                .addOverridingLinkProperties(OsmTags.TERTIARY, new LinkProperties(LinkProperties.LEVEL_TERTIARY, 2, 4, 600, false))
//                .addOverridingLinkProperties(OsmTags.TERTIARY_LINK, new LinkProperties(LinkProperties.LEVEL_TERTIARY, 2, 4, 600, false))
                .setIncludeLinkAtCoordWithHierarchy(includeLinkAtCoordWithHierarchy)
                .setAfterLinkCreated((link, osmTags, isReversed) -> {
                    link.setAllowedModes(modes);
                })
                .build()
                .read(inputOSMFile);
        // adding the missingLink
        {
            Node n1 = network.getNodes().get(Id.createNodeId("6165604798"));
            Node n2 = network.getNodes().get(Id.createNodeId("7371092120"));
            Link link = network.getFactory().createLink(Id.createLinkId("missingLink"), n1, n2);
            link.setCapacity(1500.0);
            link.setFreespeed(22.22222222222222);
            link.setAllowedModes(modes);
            link.setLength(1000);
            network.addLink(link);
        }
        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(MATSIM_NETWORK);
    }
}
