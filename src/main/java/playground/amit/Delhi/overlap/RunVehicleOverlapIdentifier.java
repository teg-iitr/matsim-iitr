package playground.amit.Delhi.overlap;

import playground.amit.Delhi.overlap.algo.elements.SigmoidFunction;
import playground.amit.Delhi.overlap.algo.optimizer.OverlapOptimizer;

/**
 * @author Amit
 */
public class RunVehicleOverlapIdentifier {

    public static void main(String[] args) {

        /**
         * Required Input data
         */
        String GTFS_PATH = "../path/GTFS.zip";
        String outFilePath = "output/path";
        /**
         * tab separated fields: Depot Name	Vehicle No.	Route Name	Origin	Sch. Trip Start	Destination	Sch. Trip End
         */
        String vehicles_file = "../path/vehicles.txt";
        int timebinSize = 24*60*60;
        int minDevicesPerTimeBin = 40;

        /**
         * Optional input data
         * excluded_vehicles_file the first column should have the vehicle no which are to be excluded.
         */
        String excluded_vehicles_file = "../path/excluded_vehicles.txt";

        OverlapOptimizer optimizer = new OverlapOptimizer(timebinSize, outFilePath, SigmoidFunction.BipolarSigmoid, minDevicesPerTimeBin);
        optimizer.initializeWithGTFS(GTFS_PATH);

        /**
         * Use the following if you want to optimize till a given probability
         */
//        optimizer.optimizeTillProb(0.0);

        /**
         * Use the following if you want to optimize till the coverage reaches to a given threshold
         */
//        optimizer.optimizeTillCoverage(0.50);

        /**
         * Use the following if you want to optimize till remaining vehicles are as per the input numbers
         */
        optimizer.optimizeTillVehicles(40);

        OverlapOptimizer.LOG.info("Completed.");
    }
}
