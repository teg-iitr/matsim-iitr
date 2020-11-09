package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Amit on 01/11/2020
 */
public class SouthDelhiTransitSchedulerCreator {

    private final Map<String, List<String>> routeId2Stops = new HashMap<>();
    public static Scenario scenario;
    private final String coordinatesFile = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/PT_stops_coordinates.csv";

   
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
        Map<String, Coord> busStopToCoordinate = getStopsCoordinates();
        busStopToCoordinate.forEach((key, value) -> {
            TransitStopFacility stop = factory.createTransitStopFacility(Id.create(key, TransitStopFacility.class), value, false);
            // TODO: not sure, if a link must also be added to the stop using stop.setLinkId(...).
            schedule.addStopFacility(stop);
        });

        //create routes
        this.routeId2Stops.forEach((key, value) -> {
            TransitLine transitLine = factory.createTransitLine(Id.create("line_" + key, TransitLine.class));
            List<TransitRouteStop> stopList = new ArrayList<>();
            for (String s : value) {
                TransitStopFacility st = schedule.getFacilities().get(Id.create(s, TransitStopFacility.class));
                TransitRouteStop transitRouteStop = factory.createTransitRouteStop(st, 0., 60.);
                stopList.add(transitRouteStop);
            }
            //TODO do we really need to create networkRoute

            //transit line 1

            NetworkRoute networkRoute1 = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class,  MN_Routes.startLink1.getId(), MN_Routes.endLink1.getId());
            networkRoute1.setLinkIds(MN_Routes.startLink1.getId(), MN_Routes.getLinkList1(), MN_Routes.endLink1.getId());


            //transit line 2

            NetworkRoute networkRoute2 = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, MN_Routes.startLink2.getId(), MN_Routes.endLink2.getId());
            networkRoute2.setLinkIds(MN_Routes.startLink2.getId(), MN_Routes.getLinkList2(), MN_Routes.endLink2.getId());

            //transit line 3
            NetworkRoute networkRoute3 = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, MN_Routes.startLink3.getId(), MN_Routes.endLink3.getId());
            networkRoute3.setLinkIds(MN_Routes.startLink3.getId(), MN_Routes.getLinkList3(), MN_Routes.endLink3.getId());


            TransitRoute route_1 = factory.createTransitRoute(Id.create("route_1" + key, TransitRoute.class), networkRoute1, stopList, "bus");
            TransitRoute route_2 = factory.createTransitRoute(Id.create("route_2" + key, TransitRoute.class), networkRoute2, stopList, "bus");
            TransitRoute route_3 = factory.createTransitRoute(Id.create("route_3" + key, TransitRoute.class), networkRoute3, stopList, "bus");
            transitLine.addRoute(route_1);
            transitLine.addRoute(route_2);
            transitLine.addRoute(route_3);
            Departure departure = factory.createDeparture(Id.create("dep_" + key, Departure.class), 8 * 3600.);
            departure.setVehicleId(Id.createVehicleId("bus_" + key));
            route_1.addDeparture(departure);
            route_2.addDeparture(departure);
            route_3.addDeparture(departure);
            schedule.addTransitLine(transitLine);

        });


        // create vehicle types and vehicle
        //TODO need to create vehicles
     
//        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
//        VehiclesFactory vehFactory = vehicles.getFactory();
//		VehicleType busType = vehFactory.createVehicleType(Id.create("busType", VehicleType.class));
//		busType.getCapacity().setSeats(40);
//		busType.getCapacity().setStandingRoom(10);
//		scenario.getTransitVehicles().addVehicleType(busType);
//
//		for (TransitLine line : schedule.getTransitLines().values()) {
//			for (TransitRoute route : line.getRoutes().values()) {
//				for (Departure departure : route.getDepartures().values()) {
//					if (!vehicles.getVehicles().keySet().contains(departure.getVehicleId())) {
//						Vehicle vehicle = vehFactory.createVehicle(departure.getVehicleId(), busType);
//						vehicles.addVehicle( vehicle);
//					}
//				}
//			}
//    }

       TransitScheduleWriter writeTransitSchedule = new TransitScheduleWriter(schedule);
       writeTransitSchedule.writeFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/Test_SouthDelhi_PT_Schedule.xml.gz");
}

    
    
    
    public Map<String, Coord> getStopsCoordinates() {
        Map<String, Coord> busStopToCoordinate = new HashMap<>();
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
                    busStopToCoordinate.put(index, transformCoord);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
        return busStopToCoordinate;
    }

  
}
