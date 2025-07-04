package playground.sapan.dma;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class PRTScheduleGeneratorBothDirection {

    private final static String stopsFile = "scenario/haridwarPRT/PRT-Corridors-Stops-Sequence.csv";
    private final static  String inputNetworkFile = "scenario/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned_14052025.xml.gz";
    private final static  String outputNetworkFile = "scenario/HaridwarPT/Haridwar-PT-Network9.xml.gz";
    private final static  String outputTransitScheduleFile = "scenario/HaridwarPT/Haridwar-PT-TransitSchedule9.xml.gz";
    private final static  String outputTransitVehiclesFile = "scenario/HaridwarPT/Haridwar-PT-TransitVehicles9.xml.gz";

    private final Map<String, CreateNetworkWithPRTCorridors.PRTCorridor> corridorToSequenceToStop = new HashMap<>();
    private final CoordinateTransformation transformation =
            TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32644");

    public final static String mode = "pt";

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.addCoreModules();
        config.network().setInputFile(inputNetworkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        new PRTScheduleGeneratorBothDirection().run(scenario);
        new NetworkWriter(scenario.getNetwork()).write(outputNetworkFile);
        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputTransitScheduleFile);
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(outputTransitVehiclesFile);
    }

    private void run(Scenario scenario){
        readFile();
        addPRTNetwork(scenario.getNetwork());
        prepareSchedule(scenario);
    }

    private void readFile() {
        try (BufferedReader reader = IOUtils.getBufferedReader(stopsFile)) {
            String line = reader.readLine(); // skip header
            Map<String, List<CreateNetworkWithPRTCorridors.PRTStop>> tempCorridors = new HashMap<>();
            Set<String> usedStopIds = new HashSet<>();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String routeName = parts[0].trim();
                String stopId = parts[1].trim();
                String stopName = parts[2].trim();
                double stopLat = Double.parseDouble(parts[3].trim());
                double stopLong = Double.parseDouble(parts[4].trim());
                int seq = Integer.parseInt(parts[5].trim());

                // stopId has already been used in another corridor, rename it
                while (usedStopIds.contains(stopId)) {
                    stopId = stopId + "_new";
                }
                usedStopIds.add(stopId);

                CreateNetworkWithPRTCorridors.PRTCorridor corridor = corridorToSequenceToStop.getOrDefault(
                        routeName, new CreateNetworkWithPRTCorridors.PRTCorridor(routeName));
                CreateNetworkWithPRTCorridors.PRTStop stop = new CreateNetworkWithPRTCorridors.PRTStop(
                        stopId, stopName, stopLat, stopLong, seq);

                corridor.stopList.add(stop);
                corridorToSequenceToStop.put(routeName, corridor);

                tempCorridors.computeIfAbsent(routeName, k -> new ArrayList<>()).add(stop);
            }

            // Generate reverse corridors with stop_id named as stop_id_b and reverse link joining them.
            // The Node is same but with two stops for both the direction

            for (Map.Entry<String, List<CreateNetworkWithPRTCorridors.PRTStop>> entry : tempCorridors.entrySet()) {
                String forwardCorridor = entry.getKey(); // e.g., "Corridor_1"
                List<CreateNetworkWithPRTCorridors.PRTStop> forwardStops = entry.getValue();

                String reverseCorridor = forwardCorridor + "b"; // "Corridor_1b"
                CreateNetworkWithPRTCorridors.PRTCorridor revCorridor = new CreateNetworkWithPRTCorridors.PRTCorridor(reverseCorridor);

                int reverseSeq = 1;
                for (int i = forwardStops.size() - 1; i >= 0; i--) {
                    CreateNetworkWithPRTCorridors.PRTStop fStop = forwardStops.get(i);
                    String revStopId = fStop.stopId + "b";
                    CreateNetworkWithPRTCorridors.PRTStop revStop = new CreateNetworkWithPRTCorridors.PRTStop(
                            revStopId, fStop.stopName, fStop.lat, fStop.lon, reverseSeq);
                    revCorridor.stopList.add(revStop);
                    reverseSeq++;
                }

                corridorToSequenceToStop.put(reverseCorridor, revCorridor);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading stops file: " + stopsFile, e);
        }
    }

    private void addPRTNetwork(Network network){
        NetworkFactory factory = network.getFactory();
        Map<Id<Node> , Node> stop2Node = new HashMap<>();

        for(CreateNetworkWithPRTCorridors.PRTCorridor c : corridorToSequenceToStop.values()){
            Node lastStop = null;
            for (CreateNetworkWithPRTCorridors.PRTStop stop : c.stopList){
                Id<Node> nodeId = Id.createNodeId(mode + stop.stopId);
                Node node = stop2Node.get(nodeId);
                if (node==null){
                    Coord transformedCoord = transformation.transform(new Coord(stop.lon, stop.lat));
                    node = factory.createNode(nodeId, transformedCoord);
                    network.addNode(node);
                    stop2Node.put(nodeId,node);
                }

                if (lastStop==null) lastStop=node;
                else {
                    {
                        Id<Link> linkId = Id.createLinkId( lastStop.getId().toString() + "_" + node.getId().toString());
                        Link link = factory.createLink(linkId, lastStop, node);
                        link.setAllowedModes(Set.of(mode));
                        link.setLength(CoordUtils.calcEuclideanDistance(lastStop.getCoord(), node.getCoord()));
                        link.setFreespeed(15.0);
                        link.setCapacity(9999);
                        c.links.add(linkId);
                        network.addLink(link);

                        if(c.startLink==null){
                            c.startLink = linkId;
                        }
                        c.endLink = linkId;
                    }

                    lastStop = node;
                }
            }
        }
    }

    private void prepareSchedule(Scenario scenario){
        TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory();
        VehicleType vt = scenario.getTransitVehicles().getFactory().createVehicleType(Id.create(mode,VehicleType.class));
        vt.setDescription("Personal Rapid Transit");
        vt.getCapacity().setSeats(4);
        vt.getCapacity().setStandingRoom(0);
        vt.setNetworkMode(mode);
        scenario.getTransitVehicles().addVehicleType(vt);

        Map<String , TransitStopFacility> stopFacilityMap = new HashMap<>();
        int depCount = 0;

        for(CreateNetworkWithPRTCorridors.PRTCorridor corridor: this.corridorToSequenceToStop.values()){
            TransitLine line = factory.createTransitLine(Id.create(corridor.id,TransitLine.class));
            List<TransitRouteStop> stops = new ArrayList<>();
            double offset = 0.;
            double offsetInterval = 120; // Change the offset based on the distance or anything as required
            int index = 0;
            for (CreateNetworkWithPRTCorridors.PRTStop stop : corridor.stopList) {
                TransitStopFacility stopFac = stopFacilityMap.get(stop.stopId);
                if (stopFac == null) {
                    stopFac = factory.createTransitStopFacility(Id.create(mode+stop.stopId, TransitStopFacility.class),
                            transformation.transform(new Coord(stop.lon, stop.lat)), false);
                    Id<Link> linkId;
                    if(index==0){
                        linkId =  createDummyLink(scenario.getNetwork(),scenario.getNetwork().getLinks().get(corridor.startLink).getFromNode(),stop.stopId);
                    } else{
                        linkId = corridor.links.get(index-1);
                    }
                    index++;
                    stopFac.setLinkId(linkId);
                    scenario.getTransitSchedule().addStopFacility(stopFac);
                    stopFacilityMap.put(stop.stopId, stopFac);
                }
                TransitRouteStop transitStop = factory.createTransitRouteStop(stopFac, offset + offsetInterval * stops.size(), offset + offsetInterval * (stops.size() + 1));
                transitStop.setAwaitDepartureTime(true);
                stops.add(transitStop);
            }

//            NetworkRoute networkRoute2 = scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, corridor.startLink, corridor.endLink);
            List<Id<Link>> fullRouteLinks = new ArrayList<>();

            // Use the dummy link as the first link and Intermediate links are from corridor.links (between stops) and the final stop’s link as end link
            Id<Link> firstLinkId = stopFacilityMap.get(corridor.stopList.get(0).stopId).getLinkId();
            fullRouteLinks.add(firstLinkId);

            fullRouteLinks.addAll(corridor.links);

            Id<Link> lastLinkId = stopFacilityMap.get(
                    corridor.stopList.get(corridor.stopList.size() - 1).stopId).getLinkId();

            NetworkRoute networkRoute = scenario.getPopulation().getFactory()
                    .getRouteFactories().createRoute(NetworkRoute.class, fullRouteLinks.get(0), lastLinkId);

            List<Id<Link>> interior = new ArrayList<>(fullRouteLinks.subList(1, fullRouteLinks.size() - 1));
            networkRoute.setLinkIds(fullRouteLinks.get(0), interior, lastLinkId);
            TransitRoute route = factory.createTransitRoute(Id.create(corridor.id+"A", TransitRoute.class), networkRoute, stops, mode);

            //add departures and vehicles
            // Change the headway if needed
            double startTime = 6*3600.0;
            double headway = 10*60;
            Set<Id<Vehicle>> createdVehicleIds = new HashSet<>();
            while (startTime<=21*3600.){
                Departure dep = factory.createDeparture(Id.create("pt_"+depCount, Departure.class), startTime );
                Id<Vehicle> tveh = Id.create("pt_"+depCount, Vehicle.class);
                dep.setVehicleId(tveh);
                route.addDeparture(dep);

                if (!createdVehicleIds.contains(tveh)) {
                    Vehicle vehicle = scenario.getTransitVehicles().getFactory().createVehicle(tveh, vt);
                    scenario.getTransitVehicles().addVehicle(vehicle);
                    createdVehicleIds.add(tveh);
                }

                startTime += headway;
                depCount++;
            }
            line.addRoute(route);
            scenario.getTransitSchedule().addTransitLine(line);
        }
    }

    private Id<Link> createDummyLink(Network network, Node node, String id){

        Link link = network.getFactory().createLink(Id.createLinkId("dummy_"+id), node, node);
        link.setAllowedModes(Set.of(mode));
        link.setLength(10.0);
        link.setFreespeed(9999);
        link.setCapacity(9999);
        network.addLink(link);

        return link.getId();
    }
}
