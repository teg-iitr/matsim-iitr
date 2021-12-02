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

    public static final double tripStartThreshold = 600.;

    public GTFSVehicleIntegrator(String vehicle_file){
        readVehicleFile(vehicle_file); // the file should have vehicle number, route number, start time
    }

    public String keepTheTrip(double tripStartTime, String routeName){
        // just check if this trip is served by any of the vehicle based on the route name and trip start time
        List<VehicleDetails> vehicleDetailsList = this.routeId2VehicleDetails.get(routeName);
        if(vehicleDetailsList == null) return null;

        for(VehicleDetails vehicleDetails : vehicleDetailsList){
            List<Double> startTimes = vehicleDetails.getStartTimes();
            double closestTime = closestNumberFromList(tripStartTime, startTimes);
            if (closestTime < tripStartThreshold) return vehicleDetails.vehicleNumber;
        }
        return null;
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
                    VehicleDetails vehicleDetails = this.vehicleNumberToVehicleDetails.get(vehicleNumber);
                    if (vehicleDetails==null){
                        vehicleDetails = new VehicleDetails(parts[0], vehicleNumber);
                    }
                    vehicleDetails.getRouteNames().add(parts[2]);
                    vehicleDetails.getStartTimes().add(toSeconds(parts[4]));
                    vehicleDetails.getEndTimes().add(toSeconds(parts[4]));
                    this.vehicleNumberToVehicleDetails.put(vehicleNumber, vehicleDetails);

                    List<VehicleDetails> vds = this.routeId2VehicleDetails.get(parts[2]);
                    if(vds==null){
                        vds = new ArrayList<>();
                    }
                    vds.add(vehicleDetails);
                    this.routeId2VehicleDetails.put(parts[2], vds);
                }
                line = reader.readLine();
            }
        } catch (IOException e){
            throw new RuntimeException("The file is not read. Reason "+e);
        }
    }

    public static class VehicleDetails{
        final String depotName;
        final String vehicleNumber;
        final List<String> routeNames = new ArrayList<>();
        final List<Double> startTimes = new ArrayList<>();
        final List<Double> endTimes = new ArrayList<>();

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

        public List<String> getRouteNames() {
            return this.routeNames;
        }

        public List<Double> getStartTimes() {
            return startTimes;
        }

        public List<Double> getEndTimes() {
            return endTimes;
        }
    }

    private double toSeconds(String hh_mm){
        String [] parts = hh_mm.split(":");
        return Double.parseDouble(parts[0])*3600.+Double.parseDouble(parts[1])*60;
    }

}
