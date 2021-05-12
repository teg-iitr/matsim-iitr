package playground.amit.Delhi.gtfs;

import playground.amit.Delhi.gtfs.elements.SigmoidFunction;

import java.util.List;

/**
 * Created by Amit on 23/04/2021
 */
public class DelhiGTFSOverlapIdentifier {

	public static void main(String[] args) {
		String GTFS_PATH = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\\\12052021\\GTFS_DIMTS_12052021.zip";
		int timebinSize = 24*60*60;
		String date = "12052021";
		String outFilePath = "..\\..\\repos\\sl-repos\\shared\\data\\project_data\\delhi\\gtfs_files\\12052021\\gtfs_overlap\\";

		GTFSOverlapOptimizer optimizer = new GTFSOverlapOptimizer(timebinSize, outFilePath, SigmoidFunction.BipolarSigmoid);
		optimizer.initialize(GTFS_PATH);
		optimizer.writeIterationFiles();

		List<String> route2Remove = optimizer.getLeastProbRoute();
		for (String s : route2Remove) {
			System.out.println("Removing vehicle route "+s);
			optimizer.remove(s);
			optimizer.writeIterationFiles();
		}
		optimizer.done();
		System.out.println("Completed.");
	}
}
