package playground.amit.Delhi.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import java.util.*;
import java.util.stream.Collectors;

public class TransitScheduleMerger {

    private static final String osm_transit_DMRC_schedule_file = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromOSM/transitSchedule_fromOverpass_DMRC.xml.gz";
    private static final String trnasit_schedule_OTD_Delhi = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromDIMTS/transitSchedule.xml.gz";
    private static final String trnasit_network_OTD_Delhi = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromDIMTS/transit_network.xml.gz";
    private static final String trnasit_schedule_merged = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/transitSchedule_merged.xml.gz";

    public static void main(String[] args) {
        TransitSchedule transitSchedule_OSM = getTransitScheduleFromFile(osm_transit_DMRC_schedule_file, null);
        TransitSchedule transitSchedule_OTD = getTransitScheduleFromFile(trnasit_schedule_OTD_Delhi, trnasit_network_OTD_Delhi);

         // no DMRC line in OTD Delhi
        System.out.println("Number of transit lines in DIMTS data which has DMRC in the name, are: "+ (int) transitSchedule_OTD.getTransitLines().keySet()
              .stream()
              .filter(l -> l.toString().contains("DMRC"))
              .count());

        // common transit line names
        Set<Id<TransitLine>> DIMTS_transitLines = transitSchedule_OTD.getTransitLines().keySet();
        System.out.println("Number of transit lines which have same IDs, are " + transitSchedule_OSM.getTransitLines().keySet()
                .stream()
                .filter(DIMTS_transitLines::contains)
                .count());

        // common transit route names
        Set<Id<TransitRoute>> DIMTS_transitRoutes = transitSchedule_OTD.getTransitLines().values()
                .stream()
                .flatMap(l->l.getRoutes().keySet().stream())
                .collect(Collectors.toSet());
        System.out.println("Number of transit routes which have same IDs, are " + transitSchedule_OSM.getTransitLines().values()
                .stream()
                .flatMap(l->l.getRoutes().values().stream())
                .filter(r->DIMTS_transitRoutes.contains(r.getId()))
                .count());

        transitSchedule_OSM.getTransitLines().values().forEach(transitSchedule_OTD::addTransitLine);
        transitSchedule_OSM.getFacilities().values().forEach(transitSchedule_OTD::addStopFacility);

        new TransitScheduleWriter(transitSchedule_OTD).writeFile(trnasit_schedule_merged);
    }

    private static TransitSchedule getTransitScheduleFromFile(String file, String transitNet){
        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(file);
        if (transitNet!=null) config.network().setInputFile(transitNet);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getTransitSchedule();
    }
}
