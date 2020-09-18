package playground.amit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.analysis.linkVolume.LinkVolumeHandler;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.utils.MapUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class PatnaLinkVolumePCUWriter {

    public static void main(String[] args) {

        String outputDir = "C:\\Users\\Amit Agarwal\\Google Drive\\iitr_gmail_drive\\project_data\\patna\\results\\policy\\";
        String baseCase = "run2020_17_bau";
        String policyCase = "run2020_18";

        PatnaPersonFilter personFilter = new PatnaPersonFilter();
        String ug = PatnaPersonFilter.PatnaUserGroup.urban.toString();

        LinkVolumeHandler baseHandler = new LinkVolumeHandler(personFilter,ug, getOutputVehiclesFile(outputDir, baseCase));
        process(outputDir, baseCase, baseHandler);

        LinkVolumeHandler policyHandler = new LinkVolumeHandler(personFilter,ug, getOutputVehiclesFile(outputDir, policyCase));
        process(outputDir, policyCase, policyHandler);

        Map<Id<Link>, Map<Integer, Double>> link2timebin2pcu_base = baseHandler.getLinkId2TimeSlot2LinkVolumePCU();
        Map<Id<Link>, Map<Integer, Double>> link2timebin2pcu_policy = policyHandler.getLinkId2TimeSlot2LinkVolumePCU();

        Map<Id<Link>, Map<Integer, Double>> link2timebin2pcu_diff = new HashMap<>();
        Map<Id<Link>, Double> link2pcu_diff = new HashMap<>();
        Set<Id<Link>> links = new HashSet<>(link2timebin2pcu_base.keySet());
        links.addAll(link2timebin2pcu_policy.keySet());

        for (Id<Link> linkId : links) {
            Map<Integer, Double> timebin2pcu_diff = new HashMap<>();
            SortedSet<Integer> timebins = new TreeSet<>(link2timebin2pcu_policy.getOrDefault(linkId,new HashMap<>()).keySet());
            timebins.addAll(link2timebin2pcu_base.getOrDefault(linkId, new HashMap<>()).keySet());

            for (int timebin : timebins) {
                timebin2pcu_diff.put(timebin*3600, link2timebin2pcu_policy.getOrDefault(linkId, new HashMap<>()).getOrDefault(timebin,0.) -
                        link2timebin2pcu_base.getOrDefault(linkId, new HashMap<>()).getOrDefault(timebin,0.));
            }
            link2timebin2pcu_diff.put(linkId, timebin2pcu_diff);
            link2pcu_diff.put(linkId, MapUtils.doubleValueSum(timebin2pcu_diff));
        }

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/link2time2VolTotalCount_diff_"+policyCase+".txt")) {
            writer.write("linkId\ttimeBin\tpcu_diff\n");
            for(Id<Link> l : links) {
                for (int i : link2timebin2pcu_diff.get(l).keySet()) {
                    writer.write(l + "\t" + i + "\t" + link2timebin2pcu_diff.get(l).get(i) + "\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e );
        }

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/link2VolTotalCount_diff_"+policyCase+".txt")) {
            writer.write("linkId\tpcu_diff\n");
            for(Id<Link> l : links) {
                writer.write(l + "\t" + link2pcu_diff.get(l) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+e );
        }

//
//        plvw.writeCountData(outputDir+"/analysis/link2time2VolTotalCount_"+rc+".txt");
//        plvw.writePCUData(outputDir+"/analysis/link2time2VolTotalPCU_"+rc+".txt");

    }

    private static void process(String outputDir, String rc, LinkVolumeHandler handler){
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(handler);
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(getOutputEventsFile(outputDir, rc));
    }

    public static String getOutputEventsFile(String outputDir, String rc){
        return outputDir + "/" + rc + "/" + rc + ".output_events.xml.gz";
    }

    public static String getOutputVehiclesFile(String outputDir, String rc){
        return outputDir + "/" + rc + "/" + rc + ".output_vehicles.xml.gz";
    }
}



