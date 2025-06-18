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
import org.matsim.core.network.NetworkUtils;
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

public class PRTScheduleGenerator {

    private final static String stopsFile = "./input/haridwarPRT/PRT-Corridors-Stops-Sequence.csv";
    private final static  String inputNetworkFile = "./input/haridwarPRT/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned_20092021.xml"; // Load the corrected network with bicycle on link
    private final static  String outputNetworkFile = "./input/haridwarPRT/Haridwar-PRT-Network.xml.gz"; // Load the corrected network with bicycle on link
    private final static  String outputTransitScheduleFile = "./input/haridwarPRT/Haridwar-PRT-TransitSchedule.xml.gz"; // Load the corrected network with bicycle on link
    private final static  String outputTransitVehiclesFile = "./input/haridwarPRT/Haridwar-PRT-TransitVehicles.xml.gz"; // Load the corrected network with bicycle on link

    private final Map<String, CreateNetworkWithPRTCorridors.PRTCorridor> corridorToSequenceToStop = new HashMap<>();
    private final CoordinateTransformation transformation =
            TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32644");

    public final static String mode = "prt";

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.addCoreModules();
        config.network().setInputFile(inputNetworkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        new PRTScheduleGenerator().run(scenario);
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
            String line = reader.readLine();
            boolean header = true;
            if (header){
                line = reader.readLine();
                header=false;
            }

            while(line!=null) {
                String[] parts = line.split(",");
                String routeName = parts[0];
                String stopId = parts[1];
                String stopName = parts[2];
                double stopLat = Double.parseDouble(parts[3]);
                double stopLong = Double.parseDouble(parts[4]);
                int seq = Integer.parseInt(parts[5]);

                CreateNetworkWithPRTCorridors.PRTCorridor corridor = corridorToSequenceToStop.getOrDefault(routeName, new CreateNetworkWithPRTCorridors.PRTCorridor(routeName));
                corridor.stopList.add(new CreateNetworkWithPRTCorridors.PRTStop(stopId, stopName, stopLat, stopLong, seq));
                corridorToSequenceToStop.put(routeName,corridor);

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("The file "+stopsFile+" it not read. Reason "+e);
        }
    }

    private void addPRTNetwork(Network network){
        // create nodes and links
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

//                    //reverse direction
//                    {
//                        Id<Link> linkId = Id.createLinkId( node.getId().toString() + "_" + lastStop.getId().toString());
//                        Link link = factory.createLink(linkId, node, lastStop);
//                        link.setAllowedModes(Set.of(mode));
//                        link.setLength(CoordUtils.calcEuclideanDistance(node.getCoord(), lastStop.getCoord()));
//                        link.setFreespeed(15.0);
//                        link.setCapacity(9999);
//                        network.addLink(link);
//                    }
                    lastStop = node;
                }
            }
        }
    }

    private void prepareSchedule(Scenario scenario){
        //create transitRoutestop and schedule
        TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory();
        VehicleType vt = scenario.getTransitVehicles().getFactory().createVehicleType(Id.create(mode,VehicleType.class));
        vt.setDescription("Personal Rapid Transit");
        vt.getCapacity().setSeats(4);
        vt.getCapacity().setStandingRoom(0);
        vt.setNetworkMode(mode);
        scenario.getTransitVehicles().addVehicleType(vt);

        Map<String , TransitStopFacility> stopFacilityMap = new HashMap<>();

        for(CreateNetworkWithPRTCorridors.PRTCorridor corridor: this.corridorToSequenceToStop.values()){
            TransitLine line = factory.createTransitLine(Id.create(corridor.id,TransitLine.class));
            List<TransitRouteStop> stops = new ArrayList<>();
            double offset = 0.;
            double offsetInterval = 120;
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
                stops.add(transitStop);
            }

            NetworkRoute networkRoute = scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, corridor.startLink, corridor.endLink);
            List<Id<Link>> links = corridor.links;
            links.remove(corridor.startLink);
            links.remove(corridor.endLink);
            networkRoute.setLinkIds(corridor.startLink,links,corridor.endLink);
            TransitRoute route = factory.createTransitRoute(Id.create(corridor.id+"A", TransitRoute.class), networkRoute, stops, mode);

            //add departures and vehicles
            double startTime = 6*3600.0;
            double headway = 10*60;
            while (startTime<=21*3600.){
                Departure dep = factory.createDeparture(Id.create("prt_"+route.getDepartures().size(), Departure.class), startTime );
                Id<Vehicle> tveh = Id.create("prt_"+route.getDepartures().size(), Vehicle.class);
                dep.setVehicleId(tveh);
                route.addDeparture(dep);
                startTime += headway;
            }
            line.addRoute(route);
            scenario.getTransitSchedule().addTransitLine(line);
        }
//        scenario.getTransitVehicles().addVehicle(scenario.getTransitVehicles().getFactory().createVehicle(Id.create("prt_1", Vehicle.class), vt));
    }

    private Id<Link> createDummyLink(Network network, Node node, String id){

        Link link = network.getFactory().createLink(Id.createLinkId("dummy_"+id), node, node);
        link.setAllowedModes(Set.of(mode));
        link.setLength(100.0);
        link.setFreespeed(15.0);
        link.setCapacity(9999);
        network.addLink(link);

        return link.getId();
    }
}
