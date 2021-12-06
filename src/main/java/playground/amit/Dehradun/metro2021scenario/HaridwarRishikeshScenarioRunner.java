package playground.amit.Dehradun.metro2021scenario;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import playground.amit.Dehradun.DMAZonesProcessor;
import playground.amit.Dehradun.GHNetworkDistanceCalculator;
import playground.amit.Dehradun.OD;
import playground.amit.utils.FileUtils;

import java.util.Map;

/**
 * @author Amit, created on 28-11-2021
 */

public class HaridwarRishikeshScenarioRunner {
    public static final Logger LOG = Logger.getLogger(HaridwarRishikeshScenarioRunner.class);

    private static final String date = "06-12-2021";
    public static final String OD_2_metro_trips_characteristics = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/OD2MetroTripChars_"+date+".txt";

    public static final String total_trips = "total";
    public static final String metro_trips_old = "metro_old";
    public static final String metro_trips_new = "metro_new";
    public static final String METRO_ASC = "metro_asc";

    public enum HRScenario { RingRoadOnly, NHOnly, Integrated}

    private final HRScenario hr_scenario = HRScenario.Integrated;
    private static final boolean runASCCalibration = false;
    public static final int numberOfPoints2DrawInEachZone = 20;
    public static final double threshold_access_egress_distance = 2.5;

    public static final double walk_speed = 5.;
    public static final double walk_beeline_distance_factor = 1.1;

    public final boolean writeOD2MetroTripCharsFiles = false;

    public static void main(String[] args) {
        new HaridwarRishikeshScenarioRunner().run();
    }

    public void run() {
        String outFile_post_ASC_calibration = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/OD_2021_metro_ASC_"+date+".txt";
        String outFile_metro_share = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/metro_trips_comparison_ODLevel_"+hr_scenario+"_"+date+".txt";
        String outFile_stops_ridership_compare = FileUtils.SVN_PROJECT_DATA_DRIVE + "DehradunMetroArea_MetroNeo_data/atIITR/HR/metro_ridership_at_stops_"+hr_scenario+"_"+date+".txt";

        DMAZonesProcessor dmaZonesProcessor = new DMAZonesProcessor();
        MetroStopsQuadTree metroStopsQuadTree = new MetroStopsQuadTree();
        GHNetworkDistanceCalculator ghNetworkDistanceCalculator = new GHNetworkDistanceCalculator(metroStopsQuadTree);

        if(writeOD2MetroTripCharsFiles) {
            LOG.info("Generating metro trip characteristics for each OD pair .... ");
            // following will write OD to metro stops, metro trips details to a file, which can be further used in other cases without running again and again.
            // it needs to run only once
            OD2MetroTripCharsWriter od2MetroStatsWriter = new OD2MetroTripCharsWriter(dmaZonesProcessor, ghNetworkDistanceCalculator, metroStopsQuadTree);
            od2MetroStatsWriter.run();
            LOG.info("Writing metro trip characteristics for each OD pair to "+OD_2_metro_trips_characteristics);
            od2MetroStatsWriter.writeMetroData(OD_2_metro_trips_characteristics);
        }

        Map<Id<OD>, TripChar> od2metroTripChar = OD2MetroTripCharsWriter.readMetroData(OD_2_metro_trips_characteristics);

        if (runASCCalibration){
            LOG.info("Running ASC Calibration for metro trips .... ");
            // Run it together with gh_configured_router normal network
            Metro2021ScenarioASCCalibration metro2021ScenarioASCCalibration = new Metro2021ScenarioASCCalibration(dmaZonesProcessor, ghNetworkDistanceCalculator);
            metro2021ScenarioASCCalibration.run(outFile_post_ASC_calibration, od2metroTripChar);
        } else {
            LOG.info("Running "+this.hr_scenario+" scenario to estimate the metro trips.");
            // following should not be run with ASC calibration.
            MetroShareEstimator metroShareEstimator= new MetroShareEstimator(dmaZonesProcessor, ghNetworkDistanceCalculator,hr_scenario);
            metroShareEstimator.run(outFile_post_ASC_calibration, outFile_metro_share, od2metroTripChar);

            LOG.info("Running "+this.hr_scenario+" scenario to compare the new vs old metro trips.");
            MetroTripsComparator metroTripsComparator = new MetroTripsComparator(dmaZonesProcessor, metroStopsQuadTree);
            metroTripsComparator.run(outFile_metro_share, outFile_stops_ridership_compare, od2metroTripChar);
            LOG.info("Completed.");
        }
    }
}
