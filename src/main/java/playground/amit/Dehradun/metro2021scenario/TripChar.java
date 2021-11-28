package playground.amit.Dehradun.metro2021scenario;

/**
 * @author Amit, created on 28-11-2021
 */

public class TripChar {

    public final double tripDist;
    public double tripTime;

    public TripChar(double tripDist){
        this(tripDist, 0.);
    }

    public TripChar(double tripDist, double tripTime){
        this.tripDist = tripDist;
        this.tripTime = tripTime;
    }

    public double accessDist;
    public double accessTime;
    public double egressDist;
    public double egressTime;
}
