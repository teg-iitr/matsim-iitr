package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.FacilitiesUtils;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by Amit on 28/11/2020
 */
public class GenerateRandomizedPTRoutes {

    private final String coordinatesFile = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/PT_stops_coordinates_links.csv";
    private static final String outNet = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/PTLines_as_matsimPlainNetwork.xml.gz";
    private final Network network;
    private final NetworkFactory networkFactory;

    GenerateRandomizedPTRoutes(Network network) {
        this.network = network;
        this.networkFactory = this.network.getFactory();
    }

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.plansCalcRoute().setRoutingRandomness(3.0);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        GenerateRandomizedPTRoutes ptLinesAsNetworkGenerator = new GenerateRandomizedPTRoutes(network);
        ptLinesAsNetworkGenerator.addNodes();
        ptLinesAsNetworkGenerator.addLinks();

        GenerateRandomizedPTRoutes.createPerson(scenario, "1","11");
        GenerateRandomizedPTRoutes.createPerson(scenario, "1","19");
        GenerateRandomizedPTRoutes.createPerson(scenario, "19","21");
    }

    private static void createPerson(Scenario scenario, String originId, String destinationId){
        // generate a route between node1 and node11
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        Person person = populationFactory.createPerson(Id.createPersonId("person-WRT-route-between-"+originId+"_"+destinationId));

        Plan plan = populationFactory.createPlan();
        person.addPlan(plan);
        Activity origin = populationFactory.createActivityFromCoord("origin", scenario.getNetwork().getNodes().get(Id.createNodeId(originId)).getCoord());
        plan.addActivity(origin);
        origin.setEndTime(8 * 3600.);

        Leg leg = populationFactory.createLeg(TransportMode.car);
        plan.addLeg(leg);

        Activity destination = populationFactory.createActivityFromCoord("origin", scenario.getNetwork().getNodes().get(Id.createNodeId(destinationId)).getCoord());
        plan.addActivity(destination);

        TripRouter.Builder builder = new TripRouter.Builder(scenario.getConfig());
        builder.setRoutingModule(
                TransportMode.car,
                DefaultRoutingModules.createPureNetworkRouter(
                        TransportMode.car,
                        scenario.getPopulation().getFactory(),
                        scenario.getNetwork(),
                        new FastAStarLandmarksFactory(1).createPathCalculator(scenario.getNetwork(),
                                new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, scenario.getConfig())
                                        .createTravelDisutility(
                                                new AccessibilityBasedTravelTime()),
                                new AccessibilityBasedTravelTime())
                )
        );
        List<? extends PlanElement> routeInfo = builder.build().calcRoute(
                TransportMode.car,
                FacilitiesUtils.toFacility(origin, null),
                FacilitiesUtils.toFacility(destination, null),
                origin.getEndTime().seconds(),
                person);

        System.out.println(((NetworkRoute) ((Leg) routeInfo.get(0)).getRoute()).getLinkIds());
    }

    private void addLinks() {
        createLink("1", "22");
        createLink("22", "2");
        createLink("2", "16");
        createLink("16", "26");
        createLink("26", "19");
        createLink("18", "22");
        createLink("22", "15");
        createLink("15", "23");
        createLink("17", "23");
        createLink("23", "4");
        createLink("4", "24");
        createLink("24", "25");
        createLink("24", "21");
        createLink("21", "13");
        createLink("13", "12");
        createLink("12", "10");
        createLink("10", "11");
        createLink("20", "26");
        createLink("26", "8");
        createLink("8", "28");
        createLink("28", "9");
        createLink("9", "10");
        createLink("2", "3");
        createLink("3", "25");
        createLink("25", "5");
        createLink("5", "14");
        createLink("14", "13");
        createLink("5", "6");
        createLink("6", "7");
        createLink("7", "28");
        createLink("14", "27");
        createLink("27", "9");
    }

    private void createLink(String fromNodeId, String toNodeId) {
        Node fromNode = network.getNodes().get(Id.createNodeId(fromNodeId));
        Node toNode = network.getNodes().get(Id.createNodeId(toNodeId));

        Link link = networkFactory.createLink(Id.createLinkId(fromNode.getId() + "_" + toNode.getId()), fromNode, toNode);
        link.setLength(NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
        link.setCapacity(1000.);
        link.setNumberOfLanes(1);

        network.addLink(link);

        // link in opposite direaction
        //TODO cannot have all reverse links too, program is stuck in the loop. Need to add only the required links.
//        Link linkReverse = networkFactory.createLink(Id.createLinkId(toNode.getId() + "_" + fromNode.getId()), toNode, fromNode);
//        linkReverse.setLength(link.getLength());
//        linkReverse.setCapacity(1000.);
//        linkReverse.setNumberOfLanes(1);
//
//        network.addLink(linkReverse);
    }

    private void addNodes() {
        BufferedReader reader = IOUtils.getBufferedReader(coordinatesFile);
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, MN_TransitDemandGenerator.toCoordinateSystem);
        try {
            String line = reader.readLine();
            boolean isHeader = true;
            while (line != null) {
                if (isHeader) {
                    isHeader = false;
                } else {
                    String[] parts = line.split(",");
                    String index = parts[0];
                    Coord cord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    Coord transformCoord = ct.transform(cord);
                    Node node = networkFactory.createNode(Id.createNodeId(index), transformCoord);
                    network.addNode(node);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason " + e);
        }
    }
}
