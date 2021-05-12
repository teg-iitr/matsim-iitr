package playground.amit.Delhi.gtfs;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import org.matsim.pt2matsim.gtfs.lib.Route;
import org.matsim.pt2matsim.gtfs.lib.StopTime;
import org.matsim.pt2matsim.gtfs.lib.Trip;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by Amit on 11/05/2021.
 */
public class GTFSRouteStopsWriter {

    private static final String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_DIMTS_18042021.zip";
    private static final String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\18042021\\GTFS_route_stops.txt";

    public static void main(String[] args) {
        GtfsFeed gtfsFeed = new GtfsFeedImpl(GTFS_PATH);

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFilePath)){
            writer.write("routeId\trouteLongName\trouteShortName\ttripId\tstopSequencePosition\tstopId\tstopName\n");
            for (Route route : gtfsFeed.getRoutes().values()){
                for (Trip trip: route.getTrips().values()) {
                    for (StopTime stopTime: trip.getStopTimes()){
                        writer.write(route.getId()+"\t"+route.getLongName()+"\t"+route.getShortName()+"\t");
                        writer.write(trip.getId()+"\t");
                        writer.write(stopTime.getSequencePosition()+"\t");
                        writer.write(stopTime.getStop().getId()+"\t"+stopTime.getStop().getName()+"\n");
                    }
                }
            }
        }catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason :"+e);
        }




    }

}
