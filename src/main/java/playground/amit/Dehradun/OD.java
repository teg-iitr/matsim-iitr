package playground.amit.Dehradun;

import org.matsim.api.core.v01.Id;

/**
 * @author Amit
 */
public class OD  {
    private final String origin;
    private final String destination;
    private final Id<OD> id;
    private int numberOfTrips = 0;

    public OD (String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
        this.id = OD.getID(this.origin, this.destination);
    }

    public void setNumberOfTrips(int trips){
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

    public int getNumberOfTrips() {
        return numberOfTrips;
    }

    public Id<OD> getId() {
        return id;
    }

    public static Id<OD> getID(String origin, String destination){
        return Id.create(origin+"_"+destination, OD.class);
    }
}
