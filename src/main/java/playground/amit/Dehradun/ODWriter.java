package playground.amit.Dehradun;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 22-10-2021
 */

public class ODWriter {

    public static void writeOD(Map<Id<OD>,OD> odMap, String outputFile){
        Set<String> zones = new HashSet<>(); // no duplicates
        odMap.values().forEach(od-> {
            zones.add(od.getOrigin());
            zones.add(od.getDestination());
        });

        List<String> zones_sorted = new ArrayList<>(zones);
        Collections.sort(zones_sorted);

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)){
            writer.write("o/d\t");
            for(String z : zones_sorted) {
                writer.write(z+"\t");
            }
            writer.write("\n");

            for (String o: zones_sorted) {
                writer.write(o+"\t");
                for (String d : zones_sorted){
                    writer.write(odMap.get(OD.getID(o,d)).getNumberOfTrips()+"\t");
                }
                writer.write("\n");
            }
        }catch (IOException e){
            throw new RuntimeException("Data is not written to the file "+outputFile+". Reason : "+e);
        }
    }
}
