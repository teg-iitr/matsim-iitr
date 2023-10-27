package playground.amit.Delhi.overlap.vehicles;

import playground.amit.Delhi.overlap.algo.optimizer.OverlapOptimizer;
import playground.amit.Delhi.overlap.algo.elements.SigmoidFunction;
import playground.amit.utils.FileUtils;

/**
 * @author Amit, created on 30-11-2021
 */

public class DelhiVehicleOverlapIdentifier {



    public static void main(String[] args) {
        String GTFS_PATH = "C:\\Users\\amit2\\Downloads\\bug-fix-JTRG\\input\\GTFS09072022Edit.zip";
        String vehicles_file = "C:\\Users\\amit2\\Downloads\\bug-fix-JTRG\\input\\VehicleSampleData1.txt";

        int timebinSize = 24*60*60;
        int minDevicesPerTimeBin = 100;


        String outFilePath = "C:\\Users\\amit2\\Downloads\\bug-fix-JTRG\\output\\";

        OverlapOptimizer optimizer = new OverlapOptimizer(timebinSize, outFilePath, SigmoidFunction.BipolarSigmoid, minDevicesPerTimeBin);
        optimizer.initializeWithGTFSAndVehicles(GTFS_PATH, vehicles_file);
//      optimizer.initializeWithGTFSAndVehicles(GTFS_PATH, vehicles_file, excluded_vehicles_file);
//		optimizer.run(10);
//        optimizer.optimizeTillProb(0.0);
//		optimizer.optimizeTillRoutes(50);
        optimizer.optimizeTillCoverage(0.95);

        OverlapOptimizer.LOG.info("Completed.");
    }
}
