package playground.amit.Delhi.overlap.vehicles;

import playground.amit.Delhi.overlap.algo.optimizer.OverlapOptimizer;
import playground.amit.Delhi.overlap.algo.elements.SigmoidFunction;
import playground.amit.utils.FileUtils;

/**
 * @author Amit, created on 30-11-2021
 */

public class DelhiVehicleOverlapIdentifier {



    public static void main(String[] args) {
        String GTFS_PATH = FileUtils.SVN_PROJECT_DATA_DRIVE+"\\delhi\\gtfs_files\\\\12052021\\GTFS_DIMTS_12052021.zip";
        String vehicles_file = FileUtils.SVN_PROJECT_DATA_DRIVE+"\\delhi\\dimts\\Mar2021\\VehicleSampleData.txt";

        int timebinSize = 24*60*60;
        int minDevicesPerTimeBin = 40;


        String outFilePath = FileUtils.SVN_PROJECT_DATA_DRIVE+"\\delhi\\gtfs_files\\02122021\\vehicles_overlap_prob_24hTimebin_excludingSelfTrips\\";

        OverlapOptimizer optimizer = new OverlapOptimizer(timebinSize, outFilePath, SigmoidFunction.BipolarSigmoid, minDevicesPerTimeBin);
        optimizer.initializeWithGTFSAndVehicles(GTFS_PATH, vehicles_file);
//      optimizer.initializeWithGTFSAndVehicles(GTFS_PATH, vehicles_file, excluded_vehicles_file);
//		optimizer.run(10);
        optimizer.optimizeTillProb(0.0);
//		optimizer.optimizeTillRoutes(50);
//		optimizer.done();
        OverlapOptimizer.LOG.info("Completed.");
    }
}
