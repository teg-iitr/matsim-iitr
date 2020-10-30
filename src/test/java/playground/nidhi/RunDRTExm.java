package playground.nidhi;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.run.examples.RunOneSharedTaxiExample;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RunDRTExm {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	
	
	@Test
	public void testRunDrtExample() {
	     String test_config_file="C:\\Users\\Nidhi\\Documents\\PhD 2nd yr\\MATSim\\StreetHailing\\one_shared_taxi_config.xml";
	     URL configUrl=IOUtils.getFileUrl(test_config_file);
	     RunOneSharedTaxiExample.run(configUrl, true, 10);
	}
}
