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

public class FlattenODMatrix {
    private final Map<Id<DMADemandGenerator.OD>, DMADemandGenerator.OD> odMap = new HashMap<>();

    public static void main(String[] args) {
        String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
        String OD_all_file = SVN_repo + "atIITR/FinalTripMatrix.txt";
        String zone_centroid_file = SVN_repo + "atIITR/flowMapVisualization/flatten_OD.txt";

        FlattenODMatrix flattenODMatrix = new FlattenODMatrix();
        flattenODMatrix.flattenOD(OD_all_file);
        flattenODMatrix.writeFlattenOD(zone_centroid_file);

    }

    public void writeFlattenOD(String file){
        try(BufferedWriter writer = IOUtils.getBufferedWriter(file)) {
            writer.write("origin\tdest\tcount\n");
            for(DMADemandGenerator.OD od : this.odMap.values()) {
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
                        DMADemandGenerator.OD od = new DMADemandGenerator.OD(origin, destinations.get(index));
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
