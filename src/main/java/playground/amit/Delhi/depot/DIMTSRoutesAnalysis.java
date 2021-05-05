package playground.amit.Delhi.depot;

import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amit on 05/05/2021.
 */
public class DIMTSRoutesAnalysis {
    public static final String inputFile = "C:/Users/Amit Agarwal/Documents/repos/sl-repos/shared/data/dimts/Mar2021/DIMTS_Cluster_Depot_route_data.txt";
    public static final String outFile = "C:/Users/Amit Agarwal/Documents/repos/sl-repos/shared/data/dimts/Mar2021/DIMTS_Cluster_Depot_route_stats.txt";
    public static final Map<String, Depot> depots = new HashMap<>();
    public static final Map<String, Depot.Bus> buses = new HashMap<>();

    public static void main(String[] args) {

        try(BufferedReader reader = IOUtils.getBufferedReader(inputFile)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null) {
                if (header) {
                    header=false;
                } else{
                    String [] parts = line.split("\t");
                    String depotName = parts[0];
                    String busNumber = parts[1];
                    String routeName = parts[2];
                    String origin = parts[3];
                    double startTime = getTimeSec(parts[4]);
                    String desti = parts[5];
                    double endTime = getTimeSec(parts[6]);

                    Depot depot = depots.getOrDefault(depotName, new Depot(depotName));
                    depot.getBusNumbers().add(busNumber);
                    Depot.DepotRoute dr = depot.getBusRoutes().getOrDefault(routeName, new Depot.DepotRoute(routeName));
                    dr.getOrigins().add(origin);
                    dr.getDestinations().add(desti);
                    dr.getStartTimes().add(startTime);
                    dr.getEndTimes().add(endTime);
                    dr.getDepots().add(depotName);
                    depot.getBusRoutes().put(routeName, dr);
                    depots.put(depotName, depot);

                    Depot.Bus bus = buses.getOrDefault(busNumber, new Depot.Bus(busNumber));
                    bus.getDepots().add(depotName);
                    bus.getRoutes().add(routeName);
                    buses.put(busNumber, bus);
                }
                line = reader.readLine();
            }
        } catch (IOException e){
            throw new RuntimeException("File cannot be read. Reason "+e);
        }

        // checks:
        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)){
            //stats
            writer.write("Total number of depots are "+depots.size()+"\n");
            writer.write("Total number of buses are "+buses.size()+"\n");
            writer.write("Total number of routes are "+depots.values().stream().mapToInt(d->d.getBusRoutes().values().size()).sum()+"\n");
            int trips = 0;
            for (Depot depot : depots.values()) {
                int tripsSoFar = depot.getBusRoutes().values().stream().mapToInt(dr->dr.getStartTimes().size()).sum();
                writer.write("Total number of trips served by depot "+ depot.getDepotName()+" are "+tripsSoFar+"\n");
                trips +=tripsSoFar;
            }
            writer.write("Total number of trips are "+trips+"\n");

            for (Depot.Bus bus : buses.values()) {
                if (bus.getDepots().size()!=1) {
                    writer.write("The bus "+bus.getBusNumber()+" is serving multiple depots.\n");
                }
                writer.write("The bus "+bus.getBusNumber()+" is serving "+bus.getRoutes().size()+" routes.\n");
            }

            for(Depot depot : depots.values()) {
                for (Depot.DepotRoute depotRoute : depot.getBusRoutes().values()) {
                    if (depotRoute.getOrigins().size()!=1) {
                        writer.write("Multiple origins are found for "+depotRoute.getRouteName()+"\n");
                    }

                    if (depotRoute.getDestinations().size()!=1) {
                        writer.write("Multiple destinations are found for "+depotRoute.getRouteName()+"\n");
                    }

                    if (depotRoute.getDepots().size()!=1) {
                        writer.write("The route "+ depotRoute.getRouteName()+" is served by multiple depots."+"\n");
                    }
                    trips += depotRoute.getStartTimes().size();
                }
            }


        }catch (IOException e){
            throw new RuntimeException("File cannot be written. Reason "+e);
        }
    }

    public static double getTimeSec(String hhmm){
        String[] parts = hhmm.split(":");
        return Double.parseDouble(parts[0])*3600. + Double.parseDouble(parts[1])*60.;
    }
}
