package playground.amit.Delhi.MalviyaNagarPT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amit on 01/12/2020
 */
public class AccessibilityBasedTravelTime implements TravelTime {
    private static final Logger log = LogManager.getLogger(AccessibilityBasedTravelTime.class ) ;

    private static final String odMatrix = FileUtils.getLocalGDrivePath()+"project_data/delhiMalviyaNagar_PT/2016-10_MalviyaNagarODSurveyData.txt";

    //passenger exchange = boarding + alighting
    private final Map<String, Double> stop2PassengerExchange = new HashMap<>();

    // Now, this can be
//    @Inject
    public AccessibilityBasedTravelTime() {
        BufferedReader reader = IOUtils.getBufferedReader(odMatrix);
        try {
            String line = reader.readLine();
            String [] destinations = null;
            while(line!=null) {
                String [] parts = line.split("\t");
                if (destinations==null) { // origins --> store as labels
                    destinations = parts;
                } else {
                    String origin = parts[0];
                    double totalOriginatedTrips = 0.;
                    for(int index = 1; index < destinations.length ; index++){
                        String destination = destinations[index];
                        double trips = Double.parseDouble(parts[index]);
                        totalOriginatedTrips += trips;
                        stop2PassengerExchange.put(destination, trips+stop2PassengerExchange.getOrDefault(destination, 0.));
                    }
                    stop2PassengerExchange.put(origin, totalOriginatedTrips+stop2PassengerExchange.getOrDefault(origin, 0.));
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }

    /*
     * We need to use the cost function in terms of the passenger exchange (i.e., persons boarding and alighting at toNode of that link).
     */
    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        String toNodeId = link.getToNode().getId().toString();
        if (stop2PassengerExchange.get(toNodeId)==null) return 0.;
        else {
            double cost = - stop2PassengerExchange.get(toNodeId);
//            System.out.println("Cost for the link: "+ link.getId()+" is "+cost);
            return cost;// more is the passenger exchange, lesser is the cost
        }
//        return link.getLength() / link.getFreespeed(time);
    }
}
