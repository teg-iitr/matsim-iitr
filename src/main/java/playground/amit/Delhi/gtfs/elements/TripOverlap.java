package playground.amit.Delhi.gtfs.elements;

import org.matsim.api.core.v01.Id;
import org.matsim.pt2matsim.gtfs.lib.Trip;
import playground.amit.Delhi.gtfs.SigmoidFunction;
import playground.amit.Delhi.gtfs.SigmoidFunctionUtils;

import java.util.*;

/**
 * Created by Amit on 10/05/2021.
 */
public class TripOverlap {

    private final List<Segment> segments = new ArrayList<>();
    private final Id<Trip> tripId;
    private String routeId;
    private final Map<Segment, SegmentalOverlap> seg2overlaps = new LinkedHashMap<>();
    private final Map<SigmoidFunction, Double> sigmoidFunction2Probs = new HashMap<>();

    TripOverlap(Id<Trip> tripId) {
        this.tripId = tripId;
    }
    public List<Segment> getSegments() {
        return this.segments;
    }

    public Id<Trip> getTripId(){
        return this.tripId;
    }

    public Map<Segment, SegmentalOverlap> getSeg2overlaps() {
        return seg2overlaps;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public Map<SigmoidFunction, Double> getSigmoidFunction2Probs() {
        if (this.sigmoidFunction2Probs.isEmpty()) {
            for(SigmoidFunction sigmoidFunction : SigmoidFunction.values()) {
                double prob = 1.0;
                for (SegmentalOverlap so : getSeg2overlaps().values()) {
                    prob *= SigmoidFunctionUtils.getValue(sigmoidFunction, so.getCount());
                }
                this.sigmoidFunction2Probs.put(sigmoidFunction, prob);
            }
        }
        return sigmoidFunction2Probs;
    }
}
