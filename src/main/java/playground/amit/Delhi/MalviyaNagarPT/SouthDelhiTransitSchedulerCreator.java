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
    private final Scenario scenario;
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

            //transit line 3
            Link startLink3 = this.scenario.getNetwork().getLinks().get(Id.create("5145215660013f", Link.class));
            Link endLink3 = this.scenario.getNetwork().getLinks().get(Id.create("5542886270001f", Link.class));
            NetworkRoute networkRoute3 = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, startLink3.getId(), endLink3.getId());

            ArrayList<Id<Link>> linkList3 = new ArrayList<>();
            linkList3.add(Id.create("5823673530001f", Link.class));
            linkList3.add(Id.create("5823673530003f", Link.class));
            linkList3.add(Id.create("5823673530005f", Link.class));
            linkList3.add(Id.create("5823673530006f", Link.class));
            linkList3.add(Id.create("5823673530007f", Link.class));
            linkList3.add(Id.create("6036480960000f", Link.class));
            linkList3.add(Id.create("6837664700000f", Link.class));
            linkList3.add(Id.create("6837664700001f",Link.class));
            linkList3.add(Id.create("773639460000f",Link.class));
            linkList3.add(Id.create("773639460007f",Link.class));
            linkList3.add(Id.create("773639460008f",Link.class));
            linkList3.add(Id.create("6897059630000f",Link.class));
            linkList3.add(Id.create("5705260650001f",Link.class));
            linkList3.add(Id.create("773639930002f",Link.class));
            linkList3.add(Id.create("5145215680000f",Link.class));
            linkList3.add(Id.create("5145215680001f",Link.class));
            linkList3.add(Id.create("5145215680002f",Link.class));
            linkList3.add(Id.create("5795078100002f",Link.class));
            linkList3.add(Id.create("5705260610000f",Link.class));
            linkList3.add(Id.create("5705260590000f",Link.class));
            linkList3.add(Id.create("5618596580000f",Link.class));
            linkList3.add(Id.create("5618596580003f",Link.class));
            linkList3.add(Id.create("5618596560000f",Link.class));
            linkList3.add(Id.create("5418873140000f",Link.class));
            linkList3.add(Id.create("5418873140001f",Link.class));
            linkList3.add(Id.create("5418873140001f",Link.class));
            linkList3.add(Id.create("5418873140002f",Link.class));
            linkList3.add(Id.create("5418873140004f",Link.class));
            linkList3.add(Id.create("773640050001f",Link.class));
            linkList3.add(Id.create("773640050003f",Link.class));
            linkList3.add(Id.create("773640050005f",Link.class));
            linkList3.add(Id.create("773640050007f",Link.class));
            linkList3.add(Id.create("5577071660001f",Link.class));
            linkList3.add(Id.create("5577071660003f",Link.class));
            linkList3.add(Id.create("5551971890001f",Link.class));
            linkList3.add(Id.create("5551971890003f",Link.class));
            linkList3.add(Id.create("5551971890004f",Link.class));
            linkList3.add(Id.create("5551971890005f",Link.class));
            linkList3.add(Id.create("773639480003f",Link.class));


            networkRoute3.setLinkIds(startLink3.getId(), linkList3, endLink3.getId());

            TransitRoute route = factory.createTransitRoute(Id.create("route_3" + key, TransitRoute.class), networkRoute3, stopList, "bus");
            transitLine.addRoute(route);
            Departure departure = factory.createDeparture(Id.create("dep_" + key, Departure.class), 8 * 3600.);
            departure.setVehicleId(Id.createVehicleId("bus_" + key));
            route.addDeparture(departure);
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
       writeTransitSchedule.writeFile(FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/matsimFiles/SouthDelhi_PT_Schedule.xml.gz");
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
