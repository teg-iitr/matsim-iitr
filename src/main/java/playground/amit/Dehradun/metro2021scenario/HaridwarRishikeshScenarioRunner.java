package playground.amit.Dehradun.metro2021scenario;

import org.apache.log4j.Logger;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.utils.FileUtils;

/**
 * @author Amit, created on 28-11-2021
 */

public class HaridwarRishikeshScenarioRunner {
    public static final Logger LOG = Logger.getLogger(HaridwarRishikeshScenarioRunner.class);

    public static final String total_trips = "total";
    public static final String metro_trips_old = "metro_old";
    public static final String metro_trips_new = "metro_new";
    public static final String METRO_ASC = "metro_asc";

    public enum HRScenario { RingRoadOnly, NHOnly, Integrated}

    private final HRScenario hr_scenario = HRScenario.NHOnly;
    private static final String date = "28-11-2021";
    private static final boolean runASCCalibration = true;
    public static final int numberOfPoints2DrawInEachZone = 20;
    public static final double threshold_access_egress_distance = 2.5;

    public static void main(String[] args) {
        new HaridwarRishikeshScenarioRunner().run();
    }

    public void run() {
        String outFile_post_ASC_calibration = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/OD_2021_metro_ASC_"+date+".txt";
        String outFile_metro_share = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/metro_trips_comparison_ODLevel_"+hr_scenario+"_"+date+".txt";
        String outFile_stops_ridership_compare = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/metro_ridership_at_stops_"+hr_scenario+"_"+date+".txt";

        DMAZonesProcessor dmaZonesProcessor = new DMAZonesProcessor();

        if (runASCCalibration){
            LOG.info("Running ASC Calibration for metro trips .... ");
            // Run it together with gh_configured_router normal network
            Metro2021ScenarioASCCalibration metro2021ScenarioASCCalibration = new Metro2021ScenarioASCCalibration(dmaZonesProcessor);
            metro2021ScenarioASCCalibration.run(outFile_post_ASC_calibration);
        } else {
            LOG.info("Running "+this.hr_scenario+" scenario to estimate the metro trips.");
            // following should not be run with ASC calibration.
            MetroShareEstimator metroShareEstimator= new MetroShareEstimator(dmaZonesProcessor,hr_scenario);
            metroShareEstimator.run(outFile_post_ASC_calibration, outFile_metro_share);

            LOG.info("Running "+this.hr_scenario+" scenario to compare the new vs old metro trips.");
            MetroTripsComparator metroTripsComparator = new MetroTripsComparator(dmaZonesProcessor);
            metroTripsComparator.run(outFile_metro_share, outFile_stops_ridership_compare );
            LOG.info("Completed.");
        }
    }
}
