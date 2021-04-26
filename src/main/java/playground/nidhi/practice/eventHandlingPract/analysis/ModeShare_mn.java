package playground.nidhi.practice.eventHandlingPract.analysis;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.util.*;

public class ModeShare_mn implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    private SortedMap<String, Double> modePercent = new TreeMap<String, Double>();
    private SortedMap<String, Integer> modeNumLegs = new TreeMap<String, Integer>();
    private SortedMap<String, Integer> mode2LegCount;;
    private SortedMap<String, Double> mode2Share;
    private final Map<Id<Person>, List<String>> agentMode = new HashMap<Id<Person>, List<String>>();
    private int totalNoOfLegs = 0;
    private final SortedSet<String> usedModes;


    public ModeShare_mn() {
        this.usedModes = new TreeSet<String>();
        mode2LegCount = modeLegCount();
        mode2Share = modeShare(mode2LegCount);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
       Id<Person> pi= event.getPersonId();
       String lm = event.getLegMode();
//        agentMode.put(pi,lm);

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {

    }





    public SortedMap<String, Double> modeShare(SortedMap<String, Integer> mode2NoOfLegs) {
        for(String mode : modeNumLegs.keySet()){
            int modeLegs = modeNumLegs.get(mode);
            totalNoOfLegs += modeLegs;
        }
        for(String mode : modeNumLegs.keySet()){
            double share = 100. * (double) modeNumLegs.get(mode) / totalNoOfLegs;
            modePercent.put(mode, share);
        }
        return modePercent;
    }

    private SortedMap<String, Integer> modeLegCount() {

        return modeNumLegs;
    }




    public void writeResults(String outputFolder) {
        String outFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/legModeShare.txt";
        try {
            BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFolder + "legModeShare.txt");
            writer2.write("# mode\tshare");
            writer2.newLine();
            for (Map.Entry<String, Double> modeShareEntry : this.mode2Share.entrySet()) {
                writer2.write(modeShareEntry.getKey() + "\t" + modeShareEntry.getValue());
                writer2.newLine();
            }
            writer2.flush();
            writer2.close();
        }catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }
}
