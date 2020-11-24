package playground.amit.Delhi.MalviyaNagarPT;

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

    public SouthDelhiTransitSchedulerCreator(){
        this.routeId2Stops.put("1", List.of("1","22","2","3","25","5","6","7","28","9","10","11"));
        this.routeId2Stops.put("2",List.of("19","26","8","28","7","6","5","25","24","21"));
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

        //create transitStops

        Map<String, Tuple<Coord, String>> busStopToCoordinate = getStopsCoordinates();
        for (Map.Entry<String, Tuple<Coord, String>> entry : busStopToCoordinate.entrySet()) {
            String k = entry.getKey();
            Tuple<Coord, String> v = entry.getValue();
            TransitStopFacility stop = factory.createTransitStopFacility(Id.create(k, TransitStopFacility.class), v.getFirst(), false);
            stop.setLinkId(Id.createLinkId(v.getSecond()));
            schedule.addStopFacility(stop);
        }

        RouteFactories routeFactories = this.scenario.getPopulation().getFactory().getRouteFactories();
        Vehicles transitVehicles = VehicleUtils.createVehiclesContainer();
        VehiclesFactory vehFactory = transitVehicles.getFactory();
        VehicleType vehType = vehFactory.createVehicleType(Id.create("bus_type", VehicleType.class));
        vehType.getCapacity().setSeats(40);
        vehType.getCapacity().setStandingRoom(10);
        transitVehicles.addVehicleType(vehType);

        //create routes
        this.routeId2Stops.forEach((key, value) -> {
            TransitLine transitLine1 =factory.createTransitLine(Id.create("line_" + key, TransitLine.class));

            List<TransitRouteStop> stopList = new ArrayList<>();
            for (String s : value) {
                TransitStopFacility st = schedule.getFacilities().get(Id.create(s, TransitStopFacility.class));
                TransitRouteStop transitRouteStop = factory.createTransitRouteStop(st, 0., 60.);
                stopList.add(transitRouteStop);
            }

            //transit route
            NetworkRoute networkRoute;
            if (key=="1") {
                networkRoute = routeFactories.createRoute(NetworkRoute.class,  MN_Routes.startLink1, MN_Routes.endLink1 );
                networkRoute.setLinkIds(MN_Routes.startLink1, MN_Routes.getLinkList1(), MN_Routes.endLink1);
            } else if (key=="2"){
                networkRoute = routeFactories.createRoute(NetworkRoute.class,  MN_Routes.startLink2, MN_Routes.endLink2);
                networkRoute.setLinkIds(MN_Routes.startLink2, MN_Routes.getLinkList2(), MN_Routes.endLink2);
            } else if (key =="3") {
                networkRoute = routeFactories.createRoute(NetworkRoute.class,  MN_Routes.startLink3, MN_Routes.endLink3 );
                networkRoute.setLinkIds(MN_Routes.startLink3, MN_Routes.getLinkList3(), MN_Routes.endLink3);
            } else {
                throw new RuntimeException("Transit route ID "+ key+ "not found.");
            }

            TransitRoute route_1 = factory.createTransitRoute(Id.create("route_" + key, TransitRoute.class), networkRoute, stopList, "bus");

            // create vehicle types and vehicle


            Vehicle[] busVehicles = new Vehicle[12];
            for (int i = 0; i < 12; i++) {
                busVehicles[i]= vehFactory.createVehicle(Id.create("MN_bus"+i+key, Vehicle.class),vehType);
                transitVehicles.addVehicle(busVehicles[i]);
                Departure dep = factory.createDeparture(Id.create("dep_bus" + i+key, Departure.class), 8 * 3600 + i*300.);
                dep.setVehicleId(busVehicles[i].getId());
                route_1.addDeparture(dep);
            }

            transitLine1.addRoute(route_1);
            schedule.addTransitLine(transitLine1);
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