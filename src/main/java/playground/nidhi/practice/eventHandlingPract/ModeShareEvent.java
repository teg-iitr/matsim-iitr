package playground.nidhi.practice.eventHandlingPract;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.utils.MapUtils;
import playground.amit.utils.PersonFilter;

import java.io.BufferedWriter;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class ModeShareEvent implements ModeShare {

    private final String eventsFile;
    private final ModeShareHandler msh;
    private SortedMap<String, Integer> mode2numberOflegs = new TreeMap<>();
    private SortedMap<String, Double> mode2PctOflegs = new TreeMap<>();

    public ModeShareEvent(final String eventsFile) {
        this.eventsFile = eventsFile;
        this.msh = new ModeShareHandler();
    }

    public ModeShareEvent(final String eventsFile, final String userGroup, final PersonFilter personFilter) {
        this.eventsFile = eventsFile;
        this.msh = new ModeShareHandler();
    }

    public void run() {
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(this.msh);
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(this.eventsFile);
        this.mode2numberOflegs = this.msh.getMode2numberOflegs();
        this.mode2PctOflegs = MapUtils.getIntPercentShare(this.mode2numberOflegs);
    }

    @Override
    public SortedSet<String> getUsedModes() {
        return new TreeSet<>(this.mode2numberOflegs.keySet());
    }

    @Override
    public SortedMap<String, Integer> getModeToNumberOfLegs() {
        return this.mode2numberOflegs;
    }

    @Override
    public SortedMap<String, Double> getModeToPercentOfLegs() {
        return this.mode2PctOflegs;
    }

    @Override
    public void writeResults(String outputFile) {
        BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
        try {
            for (String str : mode2numberOflegs.keySet()) {
                writer.write(str + "\t");
            }
            writer.write("total \t");
            writer.newLine();

            for (String str : mode2numberOflegs.keySet()) { // write Absolute No Of Legs
                writer.write(mode2numberOflegs.get(str) + "\t");
            }
            writer.write(MapUtils.intValueSum(mode2numberOflegs) + "\t");
            writer.newLine();

            for (String str : mode2PctOflegs.keySet()) { // write percentage no of legs
                writer.write(mode2PctOflegs.get(str) + "\t");
            }
            writer.write(MapUtils.doubleValueSum(mode2PctOflegs) + "\t");
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Data can not be written to file. Reason - " + e);
        }
    }
}