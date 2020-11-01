package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
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

    private void main(){
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory factory = schedule.getFactory();

        //create transitStops
        Map<String, Coord> busStopToCoordinate = getStopsCoordinates();
        busStopToCoordinate.entrySet().stream().forEach(e ->{
            TransitStopFacility stop = factory.createTransitStopFacility(Id.create(e.getKey(),TransitStopFacility.class),e.getValue(), false);
            // TODO: not sure, if a link must also be added to the stop using stop.setLinkId(...).
            schedule.addStopFacility(stop);
        });

        //create routes
        this.routeId2Stops.entrySet().stream().forEach(e -> {
            TransitLine transitLine = factory.createTransitLine(Id.create("line_"+e.getKey(), TransitLine.class));
            List<TransitRouteStop> stopList = e.getValue()
                    .stream()
                    .map(s -> schedule.getFacilities().get(Id.create(s,TransitStopFacility.class))).map(st->factory.createTransitRouteStop(st, 0., 60.)).collect(Collectors.toList());
            //TODO do we really need to create networkRoute
            TransitRoute route = factory.createTransitRoute(Id.create("route_"+e.getKey(), TransitRoute.class), null, stopList, "bus");
            transitLine.addRoute(route);
            Departure departure = factory.createDeparture(Id.create("dep_"+e.getKey(), Departure.class), 8*3600.);
            departure.setVehicleId(Id.createVehicleId("bus_"+e.getKey()));
            route.addDeparture(departure);
            schedule.addTransitLine(transitLine);
        });

        // create vehicle types and vehicle
        //TODO need to create vehicles
        

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
