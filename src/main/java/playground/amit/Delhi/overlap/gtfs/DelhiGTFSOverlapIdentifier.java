package playground.amit.Delhi.overlap.gtfs;

import playground.amit.Delhi.overlap.algo.elements.SigmoidFunction;
import playground.amit.Delhi.overlap.algo.optimizer.OverlapOptimizer;

/**
 * Created by Amit on 23/04/2021
 */
public class DelhiGTFSOverlapIdentifier {

	public static void main(String[] args) {
		String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\\\12052021\\GTFS_DIMTS_12052021.zip";
		int timebinSize = 24*60*60;
		int freqDataPointsDevice = 1;
		int minDataPointsPerTimeBin = 40; // configurable
		int minDevicesPerTimeBin = (int) Math.ceil( (freqDataPointsDevice* timebinSize/60) / minDataPointsPerTimeBin );

		String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\25052021\\gtfs_overlap_prob_24hTimebin_excludingSelfTrips\\";

		OverlapOptimizer optimizer = new OverlapOptimizer(timebinSize, outFilePath, SigmoidFunction.BipolarSigmoid, minDevicesPerTimeBin);
		optimizer.initializeWithGTFS(GTFS_PATH);
//		optimizer.run(10);
		optimizer.optimizeTillProb(0.0);
//		optimizer.optimizeTillRoutes(50);
		OverlapOptimizer.LOG.info("Completed.");
	}
}
