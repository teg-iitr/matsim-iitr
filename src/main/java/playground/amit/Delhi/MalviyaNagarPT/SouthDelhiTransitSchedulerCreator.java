package playground.amit.Delhi.MalviyaNagarPT;

import com.google.common.collect.Iterables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Amit on 01/11/2020
 */
public class SouthDelhiTransitSchedulerCreator {

    private final Map<String, List<String>> routeId2Stops = new HashMap<>();
    public Scenario scenario;
    private final String coordinatesFile = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/PT_stops_coordinates_links.csv";
    private final String outputVehicleFile = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/TransitVehicles_MN.xml.gz";
    private static final double ptSpeed_MPS = 14.0;


    public SouthDelhiTransitSchedulerCreator(){
        this.routeId2Stops.put("1", List.of("1","22","2","3","25","5","6","7","28","9","10","11"));
        this.routeId2Stops.put("2",List.of("21", "24", "25","5", "6", "7", "28","8","26","19"));
        this.routeId2Stops.put("3",List.of("1","22","2","16","26","19"));

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/south_delhi_matsim_network.xml.gz");

          this.scenario = ScenarioUtils.loadScenario(config);
    }

    public static void main(String[] args) {

        new SouthDelhiTransitSchedulerCreator().run();
    }

    private void run(){
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory factory = schedule.getFactory();
//TO
        //create transitStops
        List<String> remove_stop= List.of("4", "12", "13","14","15","17","18","20","23","27");
        Map<String, Tuple<Coord, String>> busStopToCoordinate = getStopsCoordinates();
        for (Map.Entry<String, Tuple<Coord, String>> entry : busStopToCoordinate.entrySet()) {
            String k = entry.getKey();
            Tuple<Coord, String> v = entry.getValue();
            if (!remove_stop.contains(k)) {
                TransitStopFacility stop = factory.createTransitStopFacility(Id.create(k, TransitStopFacility.class), v.getFirst(), false);
                stop.setLinkId(Id.createLinkId(v.getSecond()));
                schedule.addStopFacility(stop);
            }
        }


        RouteFactories routeFactories = this.scenario.getPopulation().getFactory().getRouteFactories();
        Vehicles transitVehicles = VehicleUtils.createVehiclesContainer();
        VehiclesFactory vehFactory = transitVehicles.getFactory();

        // create vehicle types and vehicle
        VehicleType vehType = vehFactory.createVehicleType(Id.create("bus_type", VehicleType.class));
        vehType.getCapacity().setSeats(40);
        vehType.getCapacity().setStandingRoom(10);
        transitVehicles.addVehicleType(vehType);

        //create routes
        this.routeId2Stops.forEach((key, value) -> {
            TransitLine transitLine =factory.createTransitLine(Id.create("line_" + key, TransitLine.class));

            //transit route
            NetworkRoute networkRoute;
            if (key.equals("1")) {
                networkRoute = routeFactories.createRoute(NetworkRoute.class,  MN_Routes.startLink1, MN_Routes.endLink1 );
                networkRoute.setLinkIds(MN_Routes.startLink1, MN_Routes.linkList1, MN_Routes.endLink1);
            } else if (key.equals("2")){
                networkRoute = routeFactories.createRoute(NetworkRoute.class,  MN_Routes.startLink2, MN_Routes.endLink2);
                networkRoute.setLinkIds(MN_Routes.startLink2, MN_Routes.linkList2 , MN_Routes.endLink2);
            } else if (key.equals("3")) {
                networkRoute = routeFactories.createRoute(NetworkRoute.class,  MN_Routes.startLink3, MN_Routes.endLink3 );
                networkRoute.setLinkIds(MN_Routes.startLink3, MN_Routes.linkList3, MN_Routes.endLink3);
            } else {
                throw new RuntimeException("Transit route ID "+ key+ "not found.");
            }


            //transit route stops
            List<Id<Link>> allLinksInRoute = new ArrayList<>();
            allLinksInRoute.add(networkRoute.getStartLinkId());
            allLinksInRoute.addAll(networkRoute.getLinkIds());
            allLinksInRoute.add(networkRoute.getEndLinkId());
            List<TransitRouteStop> stopList = new ArrayList<>();
            for (String s : value) {
                TransitStopFacility st = schedule.getFacilities().get(Id.create(s, TransitStopFacility.class));
                double length = 0.;
                Id<Link> stopLinkId = st.getLinkId();
                for (Id<Link> linkId : allLinksInRoute) {
                    if (!linkId.equals(stopLinkId)) {
                        double linkLength = this.scenario.getNetwork().getLinks().get(linkId).getLength();
                        length += linkLength;
                    } else {
                        break;
                    }
                }
                double timeOffset = length / ptSpeed_MPS;

                TransitRouteStop transitRouteStop  = factory.createTransitRouteStop(st, timeOffset, timeOffset + 30);
                stopList.add(transitRouteStop);
            }

            TransitRoute route = factory.createTransitRoute(Id.create("route_" + key, TransitRoute.class), networkRoute, stopList, "bus");


            //transit vehicles
            Vehicle[] busVehicles = new Vehicle[12];
            for (int i = 0; i < 12; i++) {

                busVehicles[i]= vehFactory.createVehicle(Id.create("MN_bus"+i+"_line_"+key, Vehicle.class),vehType);
                transitVehicles.addVehicle(busVehicles[i]);
                Departure dep;
                if (key.equals("1")){
                    dep = factory.createDeparture(Id.create("dep_bus" + i+ "_line_" + key, Departure.class), 8 * 3600 + i*600.);
                } else if (key.equals("2")){
                    dep = factory.createDeparture(Id.create("dep_bus" + i+ "_line_" + key, Departure.class), 8.5 * 3600 + i*600.);
                } else if (key.equals("3")) {
                    dep = factory.createDeparture(Id.create("dep_bus" + i+ "_line_" + key, Departure.class), 9 * 3600 + i*600.);
                } else {
                    throw new RuntimeException("Transit route ID "+ key+ "not found.");
                }

                dep.setVehicleId(busVehicles[i].getId());
                route.addDeparture(dep);
            }

            transitLine.addRoute(route);
            schedule.addTransitLine(transitLine);
        });


        MatsimVehicleWriter vehicleWriter = new MatsimVehicleWriter(transitVehicles);
        vehicleWriter.writeFile(outputVehicleFile);

       TransitScheduleWriter writeTransitSchedule = new TransitScheduleWriter(schedule);
       writeTransitSchedule.writeFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/SouthDelhi_PT_Schedule.xml.gz");
}




    public Map<String, Tuple<Coord, String>> getStopsCoordinates() {
        Map<String, Tuple<Coord, String>> busStopToCoordLink = new HashMap<>();
        BufferedReader reader = IOUtils.getBufferedReader(coordinatesFile);
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, MN_TransitDemandGenerator.toCoordinateSystem);
        try {
            String line = reader.readLine();
            boolean isHeader = true;
            while(line!=null) {
                if (isHeader) {
                    isHeader = false;
                } else {
                    String [] parts = line.split(",");
                    String index = parts[0];
                    Coord cord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    Coord transformCoord = ct.transform(cord);
                    String linkID = parts[5];
                    busStopToCoordLink.put(index, new Tuple<>(transformCoord, linkID));
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
        return busStopToCoordLink;
    }


}