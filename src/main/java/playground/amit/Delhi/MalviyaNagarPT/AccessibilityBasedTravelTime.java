package playground.amit.Delhi.MalviyaNagarPT;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;

/**
 * Created by Amit on 01/12/2020
 */
public class AccessibilityBasedTravelTime implements TravelTime {
    private static final Logger log = Logger.getLogger(AccessibilityBasedTravelTime.class ) ;

    // Now, this can be 
    @Inject
    public AccessibilityBasedTravelTime() {

    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }
}
