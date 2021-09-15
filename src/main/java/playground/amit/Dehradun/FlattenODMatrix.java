package playground.amit.Dehradun;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.demand.DMADemandGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Amit
 *
 */
public class FlattenODMatrix {
    private final Map<Id<OD>, OD> odMap = new HashMap<>();

    public static void main(String[] args) {
        String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/atIITR/";

        String [] in_files = {"FinalTripMatrix.txt","FinalTripMatrix_bus.txt","FinalTripMatrix_rail.txt","OD_2021_all.txt", "OD_2021_metro.txt"};
        String [] out_files = {"flattened_OD_all_2017.txt","flattened_OD_bus_2017.txt","flattened_OD_rail_2017.txt","flattened_OD_all_2021.txt","flattened_OD_metro_2021.txt"};

        for (int i = 0; i < in_files.length; i++){
            String inFile = SVN_repo + in_files[i];
            String out_file = SVN_repo + "/flowMapVisualization/" + out_files[i];

            FlattenODMatrix flattenODMatrix = new FlattenODMatrix();
            flattenODMatrix.flattenOD(inFile);
            flattenODMatrix.writeFlattenOD(out_file);
        }
    }

    public void writeFlattenOD(String file){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(file)) {
            writer.write("origin\tdest\tcount\n");
            for(OD od : this.odMap.values()) {
                writer.write(od.getOrigin()+"\t"+od.getDestination()+"\t"+od.getNumberOfTrips()+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written to file. Reason "+ e);
        }
    }

    public void flattenOD(String ODMatrixFile){
        BufferedReader reader = IOUtils.getBufferedReader(ODMatrixFile);
        try {
            String line = reader.readLine();
            List<String> destinations = null;
            while (line!=null){
                String [] parts = line.split("\t");
                if (destinations == null ){
                    destinations = Arrays.asList(parts);
                } else {
                    String origin = parts[0];
                    for (int index = 1; index<destinations.size()-1;index++){
                        OD od = new OD(origin, destinations.get(index));
                        od.setNumberOfTrips(Integer.parseInt(parts[index]));
                        odMap.put(od.getId(), od);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
