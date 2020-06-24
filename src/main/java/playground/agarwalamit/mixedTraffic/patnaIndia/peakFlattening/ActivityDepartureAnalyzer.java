package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

public class ActivityDepartureAnalyzer {

   private final String eventsFile;
   private final ActivityDepartureHandler activityDepartureHandler = new ActivityDepartureHandler();

   public ActivityDepartureAnalyzer(String eventsFile) {
       this.eventsFile = eventsFile;
   }

    public static void main(String[] args) {
        String eventsFile = "../../patna/output/bau/output_events.xml.gz";
        String outputFile = "../../patna/output/bau/analysis/activityDepartureCoutner2.txt";

        ActivityDepartureAnalyzer activityDepartureAnalyzer = new ActivityDepartureAnalyzer(eventsFile);
        activityDepartureAnalyzer.run();
        activityDepartureAnalyzer.writeResults(outputFile);
    }

   public void run(){
       EventsManager eventsManager = EventsUtils.createEventsManager();
       eventsManager.addHandler(activityDepartureHandler);

       MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
       reader.readFile(this.eventsFile);
   }

   public void writeResults(String outputFile){
       SortedMap<Tuple<String, Integer>, Integer> counters = this.activityDepartureHandler.getActivityDepartureCounter();

       try(BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)){
            writer.write("ActivityType\tTimeBin\tDepartureCount\n");
           for (Map.Entry<Tuple<String, Integer>, Integer> entry : counters.entrySet()) {
               writer.write(entry.getKey().getFirst() + "\t" + entry.getKey().getSecond() + "\t" + entry.getValue() + "\n");
           }
       }catch (IOException e){
           throw new RuntimeException("Data can not be written to the file. Reason - "+e);
       }

   }


}
