package playground.sapan.dma;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Sapan
 *
 */

public class AddBicycleModeToNetwork {

    public static void main(String[] args) {
        String inputNetworkFile = "scenario/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned_20092021.xml.gz";
        String outputNetworkFile = "scenario/DehradunMetropolitanArea_matsim_network_fromPBF_cleaned_14052025_3.xml.gz";

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(inputNetworkFile);

        for (Link link : network.getLinks().values()) {
            Set<String> modes = new HashSet<>(link.getAllowedModes());
            if (!modes.contains("bicycle")) {
                modes.add("bicycle");
                link.setAllowedModes(modes);
            }
        }

        new NetworkWriter(network).write(outputNetworkFile);

        System.out.println("Network updated and written to: " + outputNetworkFile);
    }
}
