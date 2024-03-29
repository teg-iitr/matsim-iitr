package playground.amit.Dehradun.metro2021scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 21-10-2021
 */

public class MetroTripsComparator {

    private final Map<Id<OD>, OD> odId2OD = new HashMap<>();
    private final DMAZonesProcessor zonesProcessor;
    private final MetroStopsQuadTree metroStopsQuadTree;

    private final Map<Id<Node>, MetroStopDetails> stop_details = new HashMap<>();

    public MetroTripsComparator(DMAZonesProcessor zonesProcessor, MetroStopsQuadTree metroStopsQuadTree){
        this.zonesProcessor = zonesProcessor;
        this.metroStopsQuadTree = metroStopsQuadTree;
    }

    public static void main(String[] args) {
        String metro_trips_file = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_trips_comparison_gh-router_NH-only_28-11-2021.txt";
        String stop_metro_share = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/metro_share_change_at_stops_NH-only_28-11-2021.txt";

        new MetroTripsComparator(new DMAZonesProcessor(), new MetroStopsQuadTree()).run(metro_trips_file, stop_metro_share, OD2MetroTripCharsWriter.readMetroData(HaridwarRishikeshScenarioRunner.OD_2_metro_trips_characteristics));
    }

    public void run(String metro_trips_file, String stop_metro_share, Map<Id<OD>, TripChar> od2MetroTripChar) {
        readODFile(metro_trips_file);
        setNearestStopToZone(od2MetroTripChar);
        writeFile(stop_metro_share);
    }

    private void writeFile(String stop_metro_share){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(stop_metro_share)){
            writer.write("stopId\tmetroStopName\tmetroLine" +
                    "\tboarding_before\tboarding_after\tboarding_pct_change" +
                    "\talighting_before\talighting_after\talighting_pct_change\n");
            List<Id<Node>> stops = new ArrayList<>(stop_details.keySet());
            Collections.sort(stops);
            for(Id<Node> stop : stops){
                writer.write(stop+"\t");
                writer.write(this.stop_details.get(stop).getStop().getAttributes().getAttribute(MetroStopsQuadTree.node_name)+"\t");
                writer.write(this.stop_details.get(stop).getStop().getAttributes().getAttribute(MetroStopsQuadTree.metro_line_name)+"\t");
                writer.write(this.stop_details.get(stop).getBoarding_before()+"\t");
                writer.write(this.stop_details.get(stop).getBoarding_after()+"\t");
                writer.write(this.stop_details.get(stop).getBoarding_pct_change()+"\t");
                writer.write(this.stop_details.get(stop).getAlighting_before()+"\t");
                writer.write(this.stop_details.get(stop).getAlighting_after()+"\t");
                writer.write(this.stop_details.get(stop).getAlighting_pct_change()+"\t");
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file "+stop_metro_share+". Possible reason "+e);
        }
    }

    private void setNearestStopToZone(Map<Id<OD>, TripChar> od2MetroTripChar){
        for(OD od : this.odId2OD.values()){
//            Coord origin = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(od.getOrigin(), 1).get(0));
//            Coord destination = DehradunUtils.Reverse_transformation.transform(this.zonesProcessor.getRandomCoords(od.getDestination(), 1).get(0));
//
//            Node [] nearestMetroStops_origin = metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(origin);
//            Node [] nearestMetroStops_destination = metroStopsQuadTree.getNearestNodeAndNodeInOppositeDirection(destination);
//            nearestMetroStops_destination = MetroStopsQuadTree.arrangeMetroStopsAsPerOriginLines(nearestMetroStops_origin, nearestMetroStops_destination);

            //do a check in advance to reduce the number of calls for GH routing API
//            double [] accessDists = new double[] {NetworkUtils.haversineDistanceKm(origin.getY(), origin.getX(), nearestMetroStops_origin[0].getCoord().getY(), nearestMetroStops_origin[0].getCoord().getX()),
//                    NetworkUtils.haversineDistanceKm(origin.getY(), origin.getX(), nearestMetroStops_origin[1].getCoord().getY(), nearestMetroStops_origin[1].getCoord().getX())};
//
//            double [] egressDists = new double[] {NetworkUtils.haversineDistanceKm(destination.getY(), destination.getX(), nearestMetroStops_destination[0].getCoord().getY(), nearestMetroStops_destination[0].getCoord().getX()),
//                    NetworkUtils.haversineDistanceKm(origin.getY(), origin.getX(), nearestMetroStops_destination[1].getCoord().getY(), nearestMetroStops_destination[1].getCoord().getX())};

            //following still might be a problem due to first processing and then filtering...this can be instead applied in metro asc calibration
            // no need for check at this level, because this has been introduced in metro asc calibration now. 28 Nov 2021 AA
//            if ((accessDists[0] > 2.0 && egressDists [0] > 2.0) && (accessDists[1] > 2.0 && egressDists [1] > 2.0)) continue;

            //do another check in advance if the metro stops (entry/ exit) are same
//            if ( nearestMetroStops_origin[0].getId().equals(nearestMetroStops_destination[0].getId()) ) continue;

//            double shortestDist = Double.POSITIVE_INFINITY;
//            int final_index = 0;
//            for (int i = 0; i<nearestMetroStops_origin.length; i++) {
//                double dist =  GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(nearestMetroStops_origin[i].getCoord(), nearestMetroStops_destination[i].getCoord(),"metro",null).tripDist;
//                if (dist < shortestDist) {
//                    final_index = i;
//                    shortestDist = dist;
//                }
//            }
            TripChar tc = od2MetroTripChar.get(od.getId());
            if(tc==null) continue;

            Node nearest_origin = this.metroStopsQuadTree.getMetroStopsNetwork().getNodes().get(Id.createNodeId(tc.access_stop));
            Node nearest_destination = this.metroStopsQuadTree.getMetroStopsNetwork().getNodes().get(Id.createNodeId(tc.egress_stop));

//            double access_dist = accessDists[final_index];
//            double egress_dist = egressDists[final_index];

//            if (access_dist <= 2. && egress_dist <= 2.) {
                od.setOrigin_metro_stop(nearest_origin);
                od.setDestination_metro_stop(nearest_destination);

                {
                    MetroStopDetails metroStopDetails_origin = this.stop_details.getOrDefault(nearest_origin.getId(), new MetroStopDetails(nearest_origin));
                    metroStopDetails_origin.addBoarding_before((Double) od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old));
                    metroStopDetails_origin.addBoarding_after((Double) od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_new));
                    stop_details.put(nearest_origin.getId(),metroStopDetails_origin);
                }
                {
                    MetroStopDetails metroStopDetails_destination = this.stop_details.getOrDefault(nearest_destination.getId(), new MetroStopDetails(nearest_destination));
                    metroStopDetails_destination.addAlighting_before((Double) od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old));
                    metroStopDetails_destination.addAlighting_after((Double) od.getAttributes().getAttribute(HaridwarRishikeshScenarioRunner.metro_trips_new));
                    stop_details.put(nearest_destination.getId(),metroStopDetails_destination);
                }
//            }
        }
    }

    private void readODFile(String metro_trips_file){
        try(BufferedReader reader = IOUtils.getBufferedReader(metro_trips_file)){
            String line = reader.readLine();
            boolean header = true;
            while(line!=null){
                if(!header){
                    String [] parts = line.split("\t");
                    Id<OD> odID = OD.getID(parts[0],parts[1]);

                    OD od = this.odId2OD.getOrDefault(odID, new OD(parts[0],parts[1]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.total_trips,Double.parseDouble(parts[2]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_old,Double.parseDouble(parts[3]));
                    od.getAttributes().putAttribute(HaridwarRishikeshScenarioRunner.metro_trips_new,Double.parseDouble(parts[5]));

                    odId2OD.put(odID, od);
                } else{
                    header=false;
                }
                line = reader.readLine();
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not read. Reason "+e);
        }
    }
}
