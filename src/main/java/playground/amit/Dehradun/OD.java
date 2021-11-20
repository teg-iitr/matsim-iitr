package playground.amit.Dehradun;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author Amit
 */
public class OD  implements Attributable {
    private final String origin;
    private final String destination;
    private final Id<OD> id;
    private double numberOfTrips = 0;
    private final Attributes attributes = new Attributes();
    private Node origin_metro_stop ;
    private Node destination_metro_stop ;

    public static final String total_trips = "total";
    public static final String metro_old = "metro_old";
    public static final String metro_new = "metro_new";

    public OD (String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
        this.id = OD.getID(this.origin, this.destination);
    }

    public void setNumberOfTrips(double trips){
        this.numberOfTrips = trips;
    }

    @Override
    public String toString() {
        return "Origin: "+this.origin+"\t Destination: "+this.destination+"\t number of trips: "+this.numberOfTrips;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public double getNumberOfTrips() {
        return numberOfTrips;
    }

    public Id<OD> getId() {
        return id;
    }

    public static Id<OD> getID(String origin, String destination){
        return Id.create(origin+"_"+destination, OD.class);
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    public Node getOrigin_metro_stop() {
        return origin_metro_stop;
    }

    public void setOrigin_metro_stop(Node origin_metro_stop) {
        this.origin_metro_stop = origin_metro_stop;
    }

    public Node getDestination_metro_stop() {
        return destination_metro_stop;
    }

    public void setDestination_metro_stop(Node destination_metro_stop) {
        this.destination_metro_stop = destination_metro_stop;
    }
}
