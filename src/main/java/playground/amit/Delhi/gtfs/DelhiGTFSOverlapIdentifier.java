package playground.amit.Delhi.gtfs;

import playground.amit.Delhi.gtfs.elements.SigmoidFunction;

/**
 * Created by Amit on 23/04/2021
 */
public class DelhiGTFSOverlapIdentifier {

	public static void main(String[] args) {
		String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\\\12052021\\GTFS_DIMTS_12052021.zip";
		int timebinSize = 1*60*60;
		String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\22052021\\gtfs_overlap_prob_1hTimebin_excludingSelfTrips\\";

		GTFSOverlapOptimizer optimizer = new GTFSOverlapOptimizer(timebinSize, outFilePath, SigmoidFunction.BipolarSigmoid);
		optimizer.initialize(GTFS_PATH);
//		optimizer.run(10);
//		optimizer.optimizeTillProb(0.1);
		optimizer.optimizeTillRoutes(50);
		optimizer.done();
		GTFSOverlapOptimizer.LOG.info("Completed.");
	}
}
