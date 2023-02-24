package playground.shivam.Dadar.evacuation;


import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuationgui.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import playground.amit.Dehradun.OD;
import playground.amit.jaipur.plans.ODMatrixGenerator;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.amit.utils.LoadMyScenarios;
import playground.amit.utils.geometry.GeometryUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * @author Shivam
 */
public class RunDadarEvacScenario {
    private static final String filesPath = "input/evacDadar/";
    private static final String boundaryShapeFileWGS84 = filesPath + "boundaryDadar.shp";
    private static final String zonesShapeFile = filesPath + "zonesDadar.shp";
    //    private static final String boundaryShapeFile = "input/evacDadar/boundaryDadar.shp";
    private static final String ORIGIN_ACTIVITY = "origin";
    private static final String DESTINATION_ACTIVITY = "destination";

    private static final String outputMATSimNetworkFile = filesPath + "dadar-network_smaller.xml.gz";
    private static final String outputEvacNetworkFile = filesPath + "dadar_evac_network.xml.gz";
    private static final String ODMatrixFile = filesPath + "dadar_od_10_10_22.csv";

    private static final String outputMATSimPlansFile = filesPath + "dadar-plans.xml.gz";

    private static final String outputEvacPlansFile = filesPath + "dadar_evac_plans.xml.gz";
    private Set<String> dadarModes = EnumSet.allOf(DadarUtils.DadarTrafficCountMode2023.class).stream().map(DadarUtils.DadarTrafficCountMode2023::toString).collect(Collectors.toSet());

    private Scenario scenario;
    private static Geometry evacuationArea;
    private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Dadar");

    public void run() {
        createDadarNetworkFromOSM();
        scenario = LoadMyScenarios.loadScenarioFromNetwork(outputMATSimNetworkFile);
        
        createDadarEvacNetwork(scenario);

        createPlansFromDadarOD();

        createDadarEvacPopulation(LoadMyScenarios.loadScenarioFromPlans(outputMATSimPlansFile));

        createDadarEvacConfig();
    }

    private void createDadarEvacPopulation(Scenario scenarioPop) {
        Population dadarPop = scenarioPop.getPopulation();
        PopulationFactory popFact = dadarPop.getFactory();

        Population evacPop = this.scenario.getPopulation();

        Person evacPerson;
        for (Person person : dadarPop.getPersons().values()) {
            PlanElement actPe = person.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
            Activity homeExisting = (Activity) actPe;
            Link link;
            Coord actCoord = homeExisting.getCoord();

            link = NetworkUtils.getNearestLink(scenario.getNetwork(), homeExisting.getCoord());
            Activity home = popFact.createActivityFromLinkId(homeExisting.getType(), link.getId());

            //check if the person is in the area shape, if not leave them out

            if (actCoord != null && !evacuationArea.contains(MGC.coord2Point(actCoord)))
                continue;

            // also exclude any home activity starting on link which is not included in evac network
            if (!scenario.getNetwork().getLinks().containsKey(home.getLinkId()))
                continue;

            evacPerson = popFact.createPerson(person.getId());
            Plan planOut = popFact.createPlan();
            evacPerson.addPlan(planOut);

            planOut.addActivity(home);
            home.setEndTime(9 * 3600);

            PlanElement legPe = person.getSelectedPlan().getPlanElements().get(1);
            Leg leg = popFact.createLeg(((Leg) legPe).getMode());
            planOut.addLeg(leg);

            Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkId);
            planOut.addActivity(evacAct);

            if (dadarModes.contains(leg.getMode())) {
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
                        FacilitiesUtils.toFacility(home, null),
                        FacilitiesUtils.toFacility(evacAct, null),
                        home.getEndTime().seconds(),
                        evacPerson,
                        new Attributes());

                Route route = ((Leg) routeInfo.get(0)).getRoute();
                route.setStartLinkId(home.getLinkId());
                route.setEndLinkId(evacAct.getLinkId());

                leg.setRoute(route);
                leg.setTravelTime(((Leg) routeInfo.get(0)).getTravelTime().seconds());
            }
            else
                continue;
            evacPop.addPerson(evacPerson);
        }
        new PopulationWriter(evacPop).write(outputEvacPlansFile);
    }

    private void createDadarEvacNetwork(Scenario scenario) {
        // TODO: right now we are testing the whole network as evac zone
        Geometry transformEvacuationArea = (Geometry) ShapeFileReader.getAllFeatures(boundaryShapeFileWGS84).iterator().next().getDefaultGeometry();// --> EPSG:7767

        try {
            evacuationArea = JTS.transform(transformEvacuationArea, CRS.findMathTransform(MGC.getCRS(TransformationFactory.WGS84), MGC.getCRS(DadarUtils.Dadar_EPSG), true));
        } catch (TransformException | FactoryException e) {
            throw new RuntimeException("Transformation isn't successful" + e);
        }

        EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(scenario, evacuationArea, safeLinkId);
        net.run();

        for (Link l : scenario.getNetwork().getLinks().values()) {
            Set<String> allowedModes = new HashSet<>(dadarModes);
            l.setAllowedModes(allowedModes);
        }

        new NetworkWriter(scenario.getNetwork()).write(outputEvacNetworkFile);
    }

    private void createDadarEvacConfig() {
        Config config = scenario.getConfig();
        config.network().setInputFile(outputEvacNetworkFile);
        config.plans().setInputFile(outputEvacPlansFile);
        config.controler().setLastIteration(10);
        config.controler().setOutputDirectory(filesPath + "output");
        config.controler().setDumpDataAtEnd(true);
        config.controler().setCreateGraphs(true);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.vspExperimental().setWritingOutputEvents(true);

        // config.global().setCoordinateSystem(DadarUtils.Dadar_EPSG);

        // TODO: testing with half flow
        config.qsim().setStorageCapFactor(0.5);
        config.qsim().setFlowCapFactor(0.5);

        config.qsim().setSnapshotPeriod(5 * 60);
        config.qsim().setEndTime(30 * 3600);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        config.qsim().setMainModes(dadarModes);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);

        PlanCalcScoreConfigGroup pcg = config.planCalcScore();
        PlanCalcScoreConfigGroup.ActivityParams originAct = new PlanCalcScoreConfigGroup.ActivityParams(ORIGIN_ACTIVITY);
        originAct.setScoringThisActivityAtAll(false);
        pcg.addActivityParams(originAct);

        PlanCalcScoreConfigGroup.ActivityParams destinationAct = new PlanCalcScoreConfigGroup.ActivityParams(DESTINATION_ACTIVITY);
        destinationAct.setScoringThisActivityAtAll(false);
        pcg.addActivityParams(destinationAct);

        PlanCalcScoreConfigGroup.ActivityParams evacAct = new PlanCalcScoreConfigGroup.ActivityParams("evac");
        evacAct.setTypicalDuration(3600);
        config.planCalcScore().addActivityParams(evacAct);

        StrategyConfigGroup scg = config.strategy();

        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.2);
        scg.addStrategySettings(reRoute);

        StrategyConfigGroup.StrategySettings tam = new StrategyConfigGroup.StrategySettings();
        tam.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
        tam.setWeight(0.1);
        scg.addStrategySettings(tam);

//        StrategyConfigGroup.StrategySettings ceb = new StrategyConfigGroup.StrategySettings();
//        ceb.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
//        scg.addStrategySettings(ceb);

//        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
//        tam.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
//        tam.setWeight(0.1);
//        scg.addStrategySettings(tam);

        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        new ConfigWriter(config).write(filesPath + "config.xml");

        Controler controler = new Controler(scenario);


        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                addTravelTimeBinding("bike").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());

                addTravelTimeBinding("motorbike").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());

                addControlerListenerBinding().toInstance(new IterationStartsListener() {
                    @Override
                    public void notifyIterationStarts(IterationStartsEvent event) {
                        if (event.getIteration() == 8) {
                            System.out.println(event.getIteration());
                        }
                    }
                });
            }
        });

        controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
            @Override
            public void install() {
                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });
        controler.run();
    }

    public static void main(String[] args) {
        new RunDadarEvacScenario().run();
    }

    private void createPlansFromDadarOD() {
        Map<Id<OD>, OD> remaining_OD = ODMatrixGenerator.generateOD(ODMatrixFile);

        Collection<SimpleFeature> zones = ShapeFileReader.getAllFeatures(zonesShapeFile);

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        for (OD od : remaining_OD.values()) {
            String originID = od.getOrigin();
            String destinationID = od.getDestination();
            double numberOfTrips = od.getNumberOfTrips();

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

                Coord origin = DadarUtils.transformation.transform(MGC.point2Coord(originPoint));
                Coord destination = DadarUtils.transformation.transform(MGC.point2Coord(destinationPoint));

                Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = factory.createPlan();

                Activity origAct = factory.createActivityFromCoord(ORIGIN_ACTIVITY, origin);
                origAct.setEndTime(6. * 3600. + MatsimRandom.getRandom().nextInt(2 * 3600));
                plan.addActivity(origAct);

                Leg leg = factory.createLeg("car");
                plan.addLeg(leg);

                Activity destinAct = factory.createActivityFromCoord(DESTINATION_ACTIVITY, destination);
                plan.addActivity(destinAct);

                person.addPlan(plan);
                population.addPerson(person);

            }
        }

        new PopulationWriter(population).write(outputMATSimPlansFile);
    }

    /**
     * Creates a Network from OSM
     *
     * @return path of MATSIm network
     */
    private void createDadarNetworkFromOSM() {
        String inputOSMFile = filesPath + "dadar.osm.pbf";

        Geometry wholeGeometry = (Geometry) ShapeFileReader.getAllFeatures(boundaryShapeFileWGS84).iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            if (hierarchyLevel <= 4)
                return true; //keep all roads upto level 4.
            else
                return (hierarchyLevel <= 5 && wholeGeometry.contains(MGC.coord2Point(DadarUtils.transformation.transform(cord))));
        };

        Network network = (new SupersonicOsmNetworkReader.Builder())
                .setCoordinateTransformation(DadarUtils.transformation)
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
                    link.setAllowedModes(dadarModes);
                })
                .build()
                .read(inputOSMFile);

        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(outputMATSimNetworkFile);
    }
}
