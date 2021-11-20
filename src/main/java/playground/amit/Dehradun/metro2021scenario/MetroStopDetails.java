package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.network.Node;
import playground.amit.utils.NumberUtils;

/**
 * @author Amit, created on 23-10-2021
 */

public class MetroStopDetails{
    private final Node stop;
    private double boarding_before = 0;
    private double alighting_before = 0;
    private double boarding_after = 0;
    private double alighting_after = 0.;
    private boolean lock = false;

    MetroStopDetails(Node n){
        this.stop = n;
    }

    public Node getStop() {
        return stop;
    }

    public double getBoarding_before() {
        return boarding_before;
    }

    public void addBoarding_before(double boarding_before) {
        if(lock) throw new RuntimeException("Pct Change in boarding/alighting is already called, thus cannot make further changes.");
        this.boarding_before += boarding_before;
    }

    public double getAlighting_before() {
        return alighting_before;
    }

    public void addAlighting_before(double alighting_before) {
        if(lock) throw new RuntimeException("Pct Change in boarding/alighting is already called, thus cannot make further changes.");
        this.alighting_before += alighting_before;
    }

    public double getBoarding_after() {
        return boarding_after;
    }

    public void addBoarding_after(double boarding_after) {
        if(lock) throw new RuntimeException("Pct Change in boarding/alighting is already called, thus cannot make further changes.");
        this.boarding_after += boarding_after;
    }

    public double getAlighting_after() {
        return alighting_after;
    }

    public void addAlighting_after(double alighting_after) {
        if(lock) throw new RuntimeException("Pct Change in boarding/alighting is already called, thus cannot make further changes.");
        this.alighting_after += alighting_after;
    }

    public double getBoarding_pct_change(){
        lock=true;
        return NumberUtils.round((boarding_after-boarding_before)*100/boarding_before,2);
    }

    public double getAlighting_pct_change(){
        lock=true;
        return NumberUtils.round((alighting_after-alighting_before)*100/alighting_before,2);
    }

    public String getMetroLine(){
        return (String) this.stop.getAttributes().getAttribute(MetroStopsQuadTree.metro_line_name);
    }
}
