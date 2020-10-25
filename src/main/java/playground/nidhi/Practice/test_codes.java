package playground.nidhi.Practice;
//import org.junit.Test;

//import org.matsim.contrib.drt.run.examples.RunOneSharedTaxiExample;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.amit.gridNet.GridNetwork;

import org.matsim.api.core.v01.TransportMode;
//import org.matsim.core.utils.io.IOUtils;



//import org.matsim.examples.ExamplesUtils;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;


public class test_codes {


private static final String GridPlans = null;
//		@Test
//	    public void testRunDrtExample() {
//	        URL configUrl = IOUtils.extendUrl( ExampleUtils.getTestScenarioURL("dvrp-grid"), "one_shared_taxi_config.xml");
//	        RunOneSharedTaxiExample.run(configUrl, false, 1);
//	    }

	
	
	





	test_codes rgs =new test_codes();
	Config config = rgs.prepareConfig();
	private Config prepareConfig() {
		// TODO Auto-generated method stub
		Collection<String> mainModes =Arrays.asList(TransportMode.car, "bicycle", "motorcycle");
		 Config config = ConfigUtils.createConfig();
//		 config.plans().setInputFile(GridPlans.GRID_PLANS);
		 config.network().setInputFile(GridNetwork.NETWORK_FILE);
		
		return null;
	}
	
	
	
	
	
}
