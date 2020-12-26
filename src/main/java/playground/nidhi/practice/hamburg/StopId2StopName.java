package playground.nidhi.practice.hamburg;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StopId2StopName {
    public static void main(String[] args) throws IOException {
        String outFile="C:\\Users\\Nidhi\\Workspace\\MATSimData\\stopId2StopName.csv";
        String split = ",";

        FileOutputStream fileOutputStream = new FileOutputStream(outFile);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter( fileOutputStream, StandardCharsets.UTF_8);

        BufferedWriter bw = new BufferedWriter(outputStreamWriter);
        bw.write("id"+ split+"linkId");

        String configFile = "C:\\Users\\Nidhi\\Documents\\GitHub\\matsim-iitr\\output\\output_config.xml";

        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        for(TransitStopFacility tsf : scenario.getTransitSchedule().getFacilities().values()){
            bw.newLine();
            bw.write(tsf.getId()+split+tsf.getLinkId());
        }
        bw.close();




    }
}
