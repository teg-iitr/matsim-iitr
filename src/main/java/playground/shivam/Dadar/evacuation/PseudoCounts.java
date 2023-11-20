package playground.shivam.Dadar.evacuation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static playground.shivam.Dadar.evacuation.DadarUtils.MATSIM_COUNTS;
import static playground.shivam.Dadar.evacuation.DadarUtils.PSEUDO_COUNTS_FROM_STATION;

public class PseudoCounts {
    private static final Map<Tuple<Id<Link>, String>, Map<Integer, Double>> countStation2time2countInfo = new HashMap<>();

    private static void readCountStationData(final String file) {
        try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
            String line = reader.readLine();

            while (line != null) {
                if (line.startsWith("countStation")) {
                    line = reader.readLine();
                    continue;
                }
                String parts[] = line.split("\t");
                String surveyLocation = parts[0];
                Id<Link> linkId = Id.createLinkId(parts[1]);
                Integer time = Integer.valueOf(parts[2]);
                double sum = 0.;
                for (int index = 3; index < parts.length - 1; index++) {
                    sum += Double.valueOf(parts[index]);
                }

                double count = Double.valueOf(parts[parts.length - 1]);
                if (sum != count)
                    throw new RuntimeException("sum of individual modal counts does not match total count.");

                Tuple<Id<Link>, String> myCountStationInfo = new Tuple<>(linkId, surveyLocation);
                if (countStation2time2countInfo.containsKey(myCountStationInfo)) {
                    Map<Integer, Double> time2count = countStation2time2countInfo.get(myCountStationInfo);
                    time2count.put(time, count);
                } else {
                    Map<Integer, Double> time2count = new HashMap<>();
                    time2count.put(time, count);
                    countStation2time2countInfo.put(myCountStationInfo, time2count);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not Read. Reason :" + e);
        }
    }

    public static void createDadarPseudoCounts() {
        readCountStationData(PSEUDO_COUNTS_FROM_STATION);

        Counts<Link> counts = new Counts<>();
        counts.setName("Dadar_Multi_Modal_Counts");
        counts.setYear(2014);
        counts.setDescription("DadarAllModesCountBasedOnCMP_Mumbai_2016");

        for (Tuple<Id<Link>, String> mcs : countStation2time2countInfo.keySet()) {
            Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
            for (Integer i : countStation2time2countInfo.get(mcs).keySet()) {
                c.createVolume(i, countStation2time2countInfo.get(mcs).get(i));
            }
        }

        new CountsWriter(counts).write(MATSIM_COUNTS);
    }
}
