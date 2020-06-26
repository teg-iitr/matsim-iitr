package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.SortedMap;

public class ActivityDepartureAnalyzer {

   private final String eventsFile;
   private final ActivityDepartureHandler activityDepartureHandler = new ActivityDepartureHandler();

   public ActivityDepartureAnalyzer(String eventsFile) {
       this.eventsFile = eventsFile;
   }

    public static void main(String[] args) {

        String runCases [] = new String[] {"bau","timeMutation3600","timeMutation7200"};
        String dir = "../../patna/output/";
        for (String run : runCases) {
            String eventsFile = dir+"/"+run+"/output_events.xml.gz";
            String outputFile = dir+"/"+run+"/analysis/activityDepartureCoutner2.txt";

            ActivityDepartureAnalyzer activityDepartureAnalyzer = new ActivityDepartureAnalyzer(eventsFile);
            activityDepartureAnalyzer.run();
            activityDepartureAnalyzer.writeResults(outputFile);
        }
    }

   public void run(){
       EventsManager eventsManager = EventsUtils.createEventsManager();
       eventsManager.addHandler(activityDepartureHandler);

       MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
       reader.readFile(this.eventsFile);
   }

   public void writeResults(String outputFile){
       SortedMap<String, SortedMap<Integer, Integer>> counters = this.activityDepartureHandler.getActivityDepartureCounter();

       try(BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)){
            writer.write("ActivityType\t");
            for(int i = 1; i <=24; i++) {
                writer.write(i+"\t");
            }
            writer.newLine();
           for (String act : counters.keySet()){
               writer.write(act+"\t");
               for (int i =1 ; i <=24; i++) {
                   writer.write(counters.get(act).get(i)+"\t");
               }
               writer.newLine();
           }
       }catch (IOException e){
           throw new RuntimeException("Data can not be written to the file. Reason - "+e);
       }
   }
}
