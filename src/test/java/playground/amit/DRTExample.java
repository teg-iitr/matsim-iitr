package playground.amit;

import org.junit.Test;
import org.matsim.contrib.drt.run.examples.RunOneSharedTaxiExample;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

public class DRTExample {

    @Test
    public void testRunDrtExample() {
        URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_shared_taxi_config.xml");
        RunOneSharedTaxiExample.run(configUrl, false, 1);
    }
}
