package playground.nidhi;

import java.net.URL;


import org.junit.Test;
import org.matsim.contrib.drt.run.examples.RunOneSharedTaxiExample;
import org.matsim.core.utils.io.IOUtils;



public class RunDRTExm2 {
	
	
	
	
	@Test
	public void testRunDrtExample() {
	     String test_config_file="C:\\Users\\Nidhi\\Documents\\PhD 2nd yr\\MATSim\\StreetHailing\\eight_shared_taxi_config.xml";
	     URL configUrl=IOUtils.getFileUrl(test_config_file);
	     RunOneSharedTaxiExample.run(configUrl, true, 10);
	}
	
//
//			URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "generic_dvrp_one_taxi_config.xml");
//			RunOneTaxiExample.run(configUrl, "one_taxi_vehicles.xml", false, 0);
//		}

	
	
	
}
