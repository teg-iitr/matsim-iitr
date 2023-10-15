package playground.amit.Delhi.overlap.vehicles;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.lib.Route;
import org.matsim.pt2matsim.gtfs.lib.Trip;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 30-11-2021
 */

public class GTFSVehicleIntegrator {

    private final Map<String, VehicleDetails> vehicleNumberToVehicleDetails = new HashMap<>();
    private final Map<String, List<VehicleDetails>> routeId2VehicleDetails = new HashMap<>();
    private final Set<String> excludedVehicles = new HashSet<>(); // cant install device due to design/ warranty issues, Oct'23

    public static final double tripStartThreshold = 600.;

    /**
     *
     * @param vehicle_file tab separated fields: Depot Name	Vehicle No.	Route Name	Origin	Sch. Trip Start	Destination	Sch. Trip End
     * @param excluded_vehicles_file the first column should have the vehicle no which are to be excluded.
     */
    public GTFSVehicleIntegrator(String vehicle_file, String excluded_vehicles_file){
        if (excluded_vehicles_file!=null) {
            excludeVehicles(excluded_vehicles_file);
        }
        readVehicleFile(vehicle_file); // the file should have vehicle number, route number, start time
    }

    /**
     *
     * @param vehicle_file tab separated fields: Depot Name	Vehicle No.	Route Name	Origin	Sch. Trip Start	Destination	Sch. Trip End
     */
    public GTFSVehicleIntegrator(String vehicle_file){
        readVehicleFile(vehicle_file); // the file should have vehicle number, route number, start time
    }

    private void excludeVehicles(String excluded_vehicles_file){
        try(BufferedReader reader = IOUtils.getBufferedReader(excluded_vehicles_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(header){
                    header = false;
                } else{
                    // Vehicle No.
                    String [] parts = line.split("\t");
                    String vehicleNumber = parts[0]; //TODO assuming that the first column is having vehicle numbers
                    this.excludedVehicles.add(vehicleNumber);
                }
                line = reader.readLine();
            }
        } catch (IOException e){
            throw new RuntimeException("The file is not read. Reason "+e);
        }
    }

    public String keepTheTrip(double tripStartTime, String routeName){
        // just check if this trip is served by any of the vehicle based on the route name and trip start time
        List<VehicleDetails> vehicleDetailsList = this.routeId2VehicleDetails.get(routeName);
        if(vehicleDetailsList == null) return null;

        String vehicleNumber = null;
        double minDiff = Double.POSITIVE_INFINITY;
        for(VehicleDetails vehicleDetails : vehicleDetailsList){
            List<Double> startTimes = vehicleDetails.getRouteNamesToStartTimes().get(routeName);
            double closestTime = closestNumberFromList(tripStartTime, startTimes);
            double diff = Math.abs(closestTime-tripStartTime);
            if ( diff <= tripStartThreshold && diff < minDiff ) {
                minDiff = diff;
                vehicleNumber = vehicleDetails.getVehicleNumber();
            }
        }
        return vehicleNumber;
    }

    public double closestNumberFromList(double number, List<Double> list){
        return list.stream()
                .min(Comparator.comparingDouble(i -> Math.abs(i - number)))
                .orElseThrow(() -> new NoSuchElementException("No value present."));
    }

    public void readVehicleFile(String vehicle_file){
        try(BufferedReader reader = IOUtils.getBufferedReader(vehicle_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(header){
                    header = false;
                } else{
                    // Depot Name	Vehicle No.	Route Name	Origin	Sch. Trip Start	Destination	Sch. Trip End
                    String [] parts = line.split("\t");
                    String vehicleNumber = parts[1];
                    if (this.excludedVehicles.contains(vehicleNumber)) continue;
                    else{
                        VehicleDetails vehicleDetails = this.vehicleNumberToVehicleDetails.get(vehicleNumber);
                        if (vehicleDetails==null){
                            vehicleDetails = new VehicleDetails(parts[0], vehicleNumber);
                        }
                        List<Double> startTimes = vehicleDetails.getRouteNamesToStartTimes().getOrDefault(parts[2], new ArrayList<>());
                        startTimes.add(toSeconds(parts[4]));
                        vehicleDetails.getRouteNamesToStartTimes().put(parts[2], startTimes);

                        List<Double> endTimes = vehicleDetails.getRouteNamesToEndTimes().getOrDefault(parts[2], new ArrayList<>());
                        endTimes.add(toSeconds(parts[6]));
                        vehicleDetails.getRouteNamesToEndTimes().put(parts[2], endTimes);

                        this.vehicleNumberToVehicleDetails.put(vehicleNumber, vehicleDetails);

                        List<VehicleDetails> vds = this.routeId2VehicleDetails.get(parts[2]);
                        if(vds==null){
                            vds = new ArrayList<>();
                        }
                        vds.add(vehicleDetails);
                        this.routeId2VehicleDetails.put(parts[2], vds);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e){
            throw new RuntimeException("The file is not read. Reason "+e);
        }
    }

    public static class VehicleDetails{
        private final String depotName;
        private final String vehicleNumber;
        private final Map<String, List<Double>> routeNamesToStartTimes = new HashMap<>();
        private final Map<String, List<Double>> routeNamesToEndTimes = new HashMap<>();

        public VehicleDetails(String depotName, String vehicleNumber) {
            this.depotName = depotName;
            this.vehicleNumber = vehicleNumber;
        }

        public String getDepotName() {
            return depotName;
        }

        public String getVehicleNumber() {
            return vehicleNumber;
        }

        public Map<String, List<Double>> getRouteNamesToStartTimes() {
            return this.routeNamesToStartTimes;
        }

        public Map<String, List<Double>> getRouteNamesToEndTimes() {
            return this.routeNamesToEndTimes;
        }
    }

    private double toSeconds(String hh_mm){
        String [] parts = hh_mm.split(":");
        return Double.parseDouble(parts[0])*3600.+Double.parseDouble(parts[1])*60;
    }

}
