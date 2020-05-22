package playground.agarwalamit.Delhi.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OSMTransitSchedulePrep {

    private static final String osm_transit_schedule_file = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromOSM/transitSchedule_fromOSM.xml.gz";
    private static final String osm_transit_cleaned_schedule_file = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromOSM/transitSchedule_fromOSM_cleaned.xml.gz";
    private static final String osm_transit_DMRC_schedule_file = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/delhi/matsimFiles/gtfs/fromOSM/transitSchedule_fromOSM_DMRC.xml.gz";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(osm_transit_schedule_file);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        TransitSchedule transitSchedule =  scenario.getTransitSchedule();

        {
            List<TransitLine> empty_lines = transitSchedule.getTransitLines().values()
                    .stream()
                    .filter(l -> l.getRoutes().size() == 0)
                    .collect(Collectors.toList());

            empty_lines.forEach(transitSchedule::removeTransitLine);

            new TransitScheduleWriter(transitSchedule).writeFile(osm_transit_cleaned_schedule_file);
        }
        {
            List<TransitLine> non_dmrc_lines = transitSchedule.getTransitLines().values()
                    .stream()
                    .filter(l -> ! (l.getId().toString().contains("DMRC")) )
                    .collect(Collectors.toList());

            non_dmrc_lines.forEach(transitSchedule::removeTransitLine);
            non_dmrc_lines.forEach(tl -> tl.getRoutes().values()
                    .stream()
                    .flatMap(r -> r.getStops().stream())
                    .forEach(rs -> transitSchedule.removeStopFacility(rs.getStopFacility())));

            //remove transit stops which are not required anymore.
            Map<Id<TransitStopFacility>, TransitStopFacility> OSM_transitStopFacilities = new HashMap<>(transitSchedule.getFacilities()); // 2265
            Set<Id<TransitStopFacility>> OSM_transitStopFacilities_toKeep = transitSchedule.getTransitLines().values()
                    .stream()
                    .flatMap(l -> l.getRoutes().values().stream())
                    .flatMap(r -> r.getStops()
                            .stream()
                            .map(s-> s.getStopFacility().getId()))
                    .collect(Collectors.toSet()); //147

            OSM_transitStopFacilities.values()
                    .stream()
                    .filter(s->!OSM_transitStopFacilities_toKeep.contains(s.getId()))
                    .forEach(s->transitSchedule.removeStopFacility(s));

            new TransitScheduleWriter(transitSchedule).writeFile(osm_transit_DMRC_schedule_file);
        }
    }
}
