package playground.amit.Delhi.MalviyaNagarPT;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Amit on 25/10/2020
 */
public class TransitScheduleMVNagar {

    private static final String transit_schedule_OTD_Delhi = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromDIMTS/transitSchedule.xml.gz";
    private static final String transit_network_OTD_Delhi = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromDIMTS/transit_network.xml.gz";

    //    private static final List<String> linesToExtract = List.of("448","534","764");
    private static final List<String> linesToExtract = List.of("XXX---406", "XXX---89", "XXX---97"); //764


    public static void main(String[] args) {

        Set<String> transitMode = new HashSet<>();
        transitMode.add("bus");

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(transit_network_OTD_Delhi);
        config.transit().setTransitModes(transitMode);
        config.transit().setTransitScheduleFile(transit_schedule_OTD_Delhi);
        config.transit().setUseTransit(true);

        Scenario scenarioIn = ScenarioUtils.loadScenario(config);

        Scenario scenarioOut = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        TransitSchedule transitSchedule = scenarioOut.getTransitSchedule();

        for (TransitLine tl : scenarioIn.getTransitSchedule().getTransitLines().values()) {
            if (linesToExtract.contains(tl.getId().toString())) {
                transitSchedule.addTransitLine(tl);
                List<TransitStopFacility> tsfs = new ArrayList<>();

//                tl.getRoutes().values().stream().flatMap(tr->tr.getStops().stream()).collect(trs -> trs.getStopFacility());
                for (TransitStopFacility tsf : tl.getRoutes()
                        .values()
                        .stream()
                        .flatMap(tr -> tr.getStops()
                                .stream())
                        .map(TransitRouteStop::getStopFacility)
                        .collect(Collectors.toList())) {
                    if (!transitSchedule.getFacilities().containsKey(tsf.getId())) {
                        transitSchedule.addStopFacility(tsf);
                    }
                }
            }
        }

        new TransitScheduleWriter(transitSchedule).writeFile("C:\\Users\\Amit Agarwal\\Google Drive\\iitr_gmail_drive\\project_data\\delhiMalviyaNagar_PT\\matsimFiles\\transitSchedule_MVNagar.xml");

    }

}
