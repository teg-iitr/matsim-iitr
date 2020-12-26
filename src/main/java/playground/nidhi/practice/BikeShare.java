package playground.nidhi.practice;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BikeShare {


    public static final String inputFile = "C:\\Users\\Nidhi\\Workspace\\MATSimData\\BikeShare\\202011-capitalbikeshare-tripdata.csv";
    public static final String EPSG32643 = "EPSG:32643";


    public Map<String, Coord> bikeStop() {

        Map<String, Coord> bikeStopCoordinate = new HashMap<>();
        BufferedReader br = IOUtils.getBufferedReader(inputFile);
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, EPSG32643);
        try {
            String line = br.readLine();
            boolean isHeader = true;
            while (line != null) {
                if (isHeader) {
                    isHeader = false;
                } else {
                    String[] seperator = line.split(",");

                }
                line = br.readLine();


            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return bikeStopCoordinate;
    }
}
