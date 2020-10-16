package playground.amit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.examples.RunDrtExample;
import org.matsim.contrib.drt.run.examples.RunOneSharedTaxiExample;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class DRTExampleTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test@Ignore
    public void testRunDrtExample() {
        URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_shared_taxi_config.xml");
        RunOneSharedTaxiExample.run(configUrl, false, 1);

//        URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_stop_based_drt_config.xml");
//        Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
//                new OTFVisConfigGroup());
//
//        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//        config.controler().setOutputDirectory(utils.getOutputDirectory());
//        RunDrtExample.run(config, true);
    }
}
