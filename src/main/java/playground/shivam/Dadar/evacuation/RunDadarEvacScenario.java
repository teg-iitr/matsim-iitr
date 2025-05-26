package playground.shivam.Dadar.evacuation;


import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.geotools.api.feature.simple.SimpleFeature;
import playground.amit.Dehradun.OD;
import playground.amit.jaipur.plans.ODMatrixGenerator;
import playground.amit.utils.geometry.GeometryUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * @author Shivam
 */
public class RunDadarEvacScenario {
    private static final String filesPath = "C:\\Users\\amit2\\Downloads\\evacDadar\\evacDadar\\";
    private static final String boundaryShapeFile = filesPath+"boundaryDadar.shp";
//    private static final String boundaryShapeFile = "input/evacDadar/boundaryDadar.shp";
    public static final String ORIGIN_ACTIVITY = "origin";
    public static final String DESTINATION_ACTIVITY = "destination";

    public static final String outputMATSimNetworkFile = filesPath+"dadar-network_smaller.xml.gz";
    public static final String ODMatrixFile = filesPath+"dadar_od_10_10_22.csv";
    public static final String plansFile = filesPath+"dadar-plans.xml.gz";

    public static void main(String[] args) {
        // getMATSimNetworkFromOSM();
//        getPlansFromOD();

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(outputMATSimNetworkFile);
        config.plans().setInputFile(plansFile);
        config.controller().setLastIteration(10);
        config.controller().setOutputDirectory(filesPath+"output");
        config.controller().setDumpDataAtEnd(true);
        config.controller().setCreateGraphs(true);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        //TODO we are using 0.5 just to see some congestion.
        config.qsim().setFlowCapFactor(0.5);
        config.qsim().setStorageCapFactor(0.5);

        config.vspExperimental().setWritingOutputEvents(true);

        ScoringConfigGroup pcg = config.scoring();
        ScoringConfigGroup.ActivityParams originAct = new ScoringConfigGroup.ActivityParams(ORIGIN_ACTIVITY);
        originAct.setScoringThisActivityAtAll(false);
        pcg.addActivityParams(originAct);

        ScoringConfigGroup.ActivityParams destinationAct = new ScoringConfigGroup.ActivityParams(DESTINATION_ACTIVITY);
        destinationAct.setScoringThisActivityAtAll(false);
        pcg.addActivityParams(destinationAct);

        ReplanningConfigGroup scg = config.replanning();
        ReplanningConfigGroup.StrategySettings reRoute = new ReplanningConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        reRoute.setWeight(0.2);
        scg.addStrategySettings(reRoute);

        ReplanningConfigGroup.StrategySettings tam = new ReplanningConfigGroup.StrategySettings();
        tam.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);
        tam.setWeight(0.1);
        scg.addStrategySettings(tam);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        //clean the network
        new NetworkCleaner().run(scenario.getNetwork());

        Controler controler = new Controler(scenario);
        controler.run();
    }

    private static void getPlansFromOD() {
//        String ODMatrixFile = "input/evacDadar/dadar_od_10_10_22.csv";


        Map<Id<OD>, OD> remaining_OD = ODMatrixGenerator.generateOD(ODMatrixFile);

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShapeFile);

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        for (OD od : remaining_OD.values()) {
            String originID = od.getOrigin();
            String destinationID = od.getDestination();
            double numberOfTrips = od.getNumberOfTrips();

            if (numberOfTrips ==0 ) continue;

            SimpleFeature origin_feature = null;
            SimpleFeature destination_feature = null;

            for (SimpleFeature feature : features) {
                String zone_key = "name";
                String zoneID = String.valueOf(feature.getAttribute(zone_key));
                if (origin_feature == null && zoneID.equals(originID)) origin_feature = feature;
                else if (destination_feature == null && zoneID.equals(destinationID)) destination_feature = feature;
            }

            if (origin_feature == null || destination_feature == null) {
                System.out.println("Either of the origin zone " + originID + " or destination zone "+destinationID+" is null.");
                continue;
            }

            for (int i = 0; i < numberOfTrips; i++) {

//                create matsim plans --> origin coord, destination coord, time, mode,

                Point originPoint = GeometryUtils.getRandomPointInsideFeature(origin_feature);
                Point destinationPoint = GeometryUtils.getRandomPointInsideFeature(destination_feature);

                Coord origin = DadarUtils.transformation.transform(MGC.point2Coord(originPoint));
                Coord destination = DadarUtils.transformation.transform(MGC.point2Coord(destinationPoint));

                Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = factory.createPlan();

                Activity origAct = factory.createActivityFromCoord(ORIGIN_ACTIVITY, origin);
                origAct.setEndTime(6. * 3600. + MatsimRandom.getRandom().nextInt( 3600));
                plan.addActivity(origAct);

                Leg leg = factory.createLeg("car");
                plan.addLeg(leg);

                Activity destinAct = factory.createActivityFromCoord(DESTINATION_ACTIVITY, destination);
                plan.addActivity(destinAct);

                person.addPlan(plan);
                population.addPerson(person);

            }
        }

        new PopulationWriter(population).write(plansFile);

    }

    private static void getMATSimNetworkFromOSM() {
        Set<String> modes = EnumSet.allOf(DadarUtils.DadarTrafficCountMode2023.class).stream().map(DadarUtils.DadarTrafficCountMode2023::toString).collect(Collectors.toSet());


        String inputOSMFile = filesPath+"dadar.osm.pbf";


        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(boundaryShapeFile);

        Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry(); // --> WGS:84

        BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy = (cord, hierarchyLevel) -> {
            if (hierarchyLevel <= 4)
                return true; //keep all roads upto level 4.
            else
                return (hierarchyLevel <= 5 && geometry.contains(MGC.coord2Point(DadarUtils.transformation.transform(cord))));
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
                    link.setAllowedModes(modes);
                })
                .build()
                .read(inputOSMFile);

        new NetworkWriter(network).write(outputMATSimNetworkFile);
    }
}
