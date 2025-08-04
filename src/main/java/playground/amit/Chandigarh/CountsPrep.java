package playground.amit.Chandigarh;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CountsPrep {
    private final String counts_input_data = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/counts_data_validation.txt";
    private final String countsFile = "C:/Users/Amit Agarwal/Google Drive/iitr_amit.ce.iitr/projects/chandigarh_satyajit/inputs/chandigarh_matsim_counts.xml.gz";

    public static void main(String[] args) {
        new CountsPrep().run();
    }

    private void run(){
        Counts<Link> counts = new Counts<>();
        counts.setName("Chandigarh_signalCounts");
        counts.setDescription("Check_comments_input_counts_data_file.");
        counts.setYear(2018); //TODO check with SM about the year.

        readFile(counts);

        new CountsWriter(counts).write(countsFile);
    }

    private void readFile(Counts<Link> counts){
        boolean header = true;
        try (BufferedReader reader = IOUtils.getBufferedReader(counts_input_data)) {
            String line = reader.readLine();
            List<String> labels = null;
            while(line!= null){
                String[] parts = line.split("\t");
                if (header) {
                    labels = Arrays.asList(parts);
                    header=false;
                }
                else {
                    Id<Link> linkId = Id.createLinkId(parts[labels.indexOf("linkId")]);
                    String location = parts[labels.indexOf("direction_from")] + "_to_" + parts[labels.indexOf("direction_to")];
                    Count<Link> count = counts.createAndAddCount(linkId, location );
                    int time = Integer.parseInt( parts[labels.indexOf("time")]) + 1; // 00:00 -- 01:00 is 1; 10:00 -10:15 is 11.
                    double actual_count = Double.parseDouble( parts[labels.indexOf("count")]);
                    // this is required since two directions traffic is coming on one link; refer to comments in the input file.
                    double additional_count = Double.parseDouble( parts[labels.indexOf("additional_count")]);
                    count.createVolume(time, actual_count+additional_count);
                    for (int i=1; i <=24 ; i++) {
                        if (i==11) continue;
                        count.createVolume(i, 0.);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read. Aborting... ");
        }
    }
}

