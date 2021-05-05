package playground.amit.Delhi.depot;

import java.util.*;

/**
 * Created by Amit on 05/05/2021.
 *
 * Assumptions (These must be verified):
 * <li>A route is served from one depot only.</li>
 * <li>A bus serving to a route would not serve any other route.</li>
 *
 */
public class Depot {
    private final String depotName;
    private Map<String, DepotRoute> busRoutes = new HashMap<>();
    private List<String> busNumbers = new ArrayList<>();

    public Depot(String name){
        this.depotName = name;
    }

    public String getDepotName() {
        return depotName;
    }

    public Map<String, DepotRoute> getBusRoutes() {
        return busRoutes;
    }

    public List<String> getBusNumbers() {
        return busNumbers;
    }

    public static class Bus {
        private final String busNumber;
        private final Set<String> depots = new HashSet<>();
        private final Set<String> routes = new HashSet<>();

        public Bus(String busNumber) {
            this.busNumber = busNumber;
        }

        public String getBusNumber() {
            return busNumber;
        }

        public Set<String> getDepots() {
            return depots;
        }

        public Set<String> getRoutes() {
            return routes;
        }
    }

    public static class DepotRoute{

        private final String routeName;
        private final Set<String> origins = new HashSet<>();
        private final Set<String> destinations= new HashSet<>();
        private List<Double> startTimes  = new ArrayList<>();
        private List<Double> endTimes = new ArrayList<>();
        private List<String> busNumbers = new ArrayList<>();
        private Set<String> depots = new HashSet<>();

        public DepotRoute(String name) {
            this.routeName = name;
        }

        public Set<String> getDepots() {
            return depots;
        }

        public String getRouteName() {
            return routeName;
        }

        public final Set<String> getOrigins() {
            return origins;
        }

        public final Set<String> getDestinations() {
            return destinations;
        }

        public List<Double> getStartTimes() {
            return startTimes;
        }

        public List<Double> getEndTimes() {
            return endTimes;
        }

        public List<String> getBusNumbers() {
            return busNumbers;
        }
    }
}
